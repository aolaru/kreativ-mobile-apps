package com.kreativfont.studio.releasechecks;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Base64;
import android.util.Xml;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import java.io.InputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.CountDownLatch;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;

/** Temporary same-signer instrumentation. Never included in the release APK/AAB. */
public class ReleaseChecks extends Instrumentation {
    private Activity activity;
    private WebView webView;
    private BroadcastReceiver receiver;
    private final CountDownLatch stopped = new CountDownLatch(1);
    private Bundle arguments;
    @Override public void onCreate(Bundle arguments) { this.arguments = arguments; start(); }
    private WebView findWebView(View view) {
        if (view instanceof WebView) return (WebView)view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup)view;
            for (int i = 0; i < group.getChildCount(); i++) {
                WebView result = findWebView(group.getChildAt(i));
                if (result != null) return result;
            }
        }
        return null;
    }
    private String sha(String text) throws Exception {
        StringBuilder out = new StringBuilder();
        for (byte b : MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8))) out.append(String.format("%02x", b & 255));
        return out.toString();
    }
    private void restorePreferences() throws Exception {
        SharedPreferences.Editor edit = getTargetContext().getSharedPreferences("CapacitorStorage", Context.MODE_PRIVATE).edit().clear();
        try (InputStream in = getContext().getAssets().open("original-preferences.xml")) {
            XmlPullParser xml = Xml.newPullParser(); xml.setInput(in, "UTF-8");
            for (int type = xml.getEventType(); type != XmlPullParser.END_DOCUMENT; type = xml.next()) {
                if (type != XmlPullParser.START_TAG || "map".equals(xml.getName())) continue;
                String tag = xml.getName(), key = xml.getAttributeValue(null, "name"), value = xml.getAttributeValue(null, "value");
                if ("string".equals(tag)) edit.putString(key, xml.nextText());
                else if ("boolean".equals(tag)) edit.putBoolean(key, Boolean.parseBoolean(value));
                else if ("int".equals(tag)) edit.putInt(key, Integer.parseInt(value));
                else if ("long".equals(tag)) edit.putLong(key, Long.parseLong(value));
                else if ("float".equals(tag)) edit.putFloat(key, Float.parseFloat(value));
                else throw new IllegalStateException("Unsupported preference type: " + tag);
            }
        }
        if (!edit.commit()) throw new IllegalStateException("Preference restore failed");
    }
    private JSONObject info() throws Exception {
        JSONObject out = new JSONObject(), hashes = new JSONObject();
        for (java.util.Map.Entry<String,?> item : getTargetContext().getSharedPreferences("CapacitorStorage", Context.MODE_PRIVATE).getAll().entrySet()) {
            hashes.put(item.getKey(), sha(String.valueOf(item.getValue())));
        }
        out.put("preferenceHashes", hashes);
        out.put("version", getTargetContext().getPackageManager().getPackageInfo(getTargetContext().getPackageName(), 0).versionName);
        android.content.pm.PackageInfo packageInfo = getTargetContext().getPackageManager().getPackageInfo(getTargetContext().getPackageName(), 0);
        out.put("versionCode", android.os.Build.VERSION.SDK_INT >= 28 ? packageInfo.getLongVersionCode() : packageInfo.versionCode);
        SharedPreferences billing = getTargetContext().getSharedPreferences("kreativ_billing", Context.MODE_PRIVATE);
        String receipt = billing.getString("verified_data", null);
        out.put("cachedPlayReceiptPresent", receipt != null);
        out.put("cachedPlaySignaturePresent", billing.contains("verified_signature"));
        if (receipt != null) {
            out.put("cachedPlayReceiptSha256", sha(receipt));
            out.put("cachedPlayReceiptAcknowledged", new JSONObject(receipt).optBoolean("acknowledged", false));
        }
        out.put("debuggable", (getTargetContext().getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0);
        out.put("fontScale", getTargetContext().getResources().getConfiguration().fontScale);
        out.put("webViewTextZoom", webView.getSettings().getTextZoom());
        File folder = new File(getTargetContext().getCacheDir(), "kreativ-export");
        File[] exports = folder.listFiles(); out.put("pendingSaveCacheFiles", exports == null ? 0 : exports.length);
        File share = new File(getTargetContext().getCacheDir(), "kreativ-share");
        org.json.JSONArray images = new org.json.JSONArray();
        File[] files = share.listFiles();
        if (files != null) for (File file : files) {
            android.graphics.BitmapFactory.Options options = new android.graphics.BitmapFactory.Options(); options.inJustDecodeBounds = true;
            android.graphics.BitmapFactory.decodeFile(file.getAbsolutePath(), options);
            images.put(new JSONObject().put("name", file.getName()).put("bytes", file.length()).put("width", options.outWidth).put("height", options.outHeight));
        }
        out.put("shareImages", images);
        return out;
    }
    @Override public void onStart() {
        try {
            if ("true".equals(arguments.getString("restoreBefore"))) restorePreferences();
            Intent launch = new Intent(); launch.setClassName(getTargetContext(), "com.kreativfont.studio.MainActivity"); launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity = startActivitySync(launch);
            for (int i = 0; i < 100 && webView == null; i++) {
                runOnMainSync(() -> webView = findWebView(activity.getWindow().getDecorView()));
                Thread.sleep(100);
            }
            if (webView == null) throw new IllegalStateException("Release WebView not found");
            receiver = new BroadcastReceiver() {
                @Override public void onReceive(Context context, Intent intent) {
                    PendingResult pending = goAsync();
                    try {
                        String action = intent.getStringExtra("command");
                        if ("eval".equals(action)) {
                            String script = new String(Base64.decode(intent.getStringExtra("source"), Base64.DEFAULT), StandardCharsets.UTF_8);
                            webView.evaluateJavascript(script, result -> { pending.setResultData(result); pending.finish(); });
                            return;
                        }
                        if ("info".equals(action)) pending.setResultData(info().toString());
                        else if ("snapshot".equals(action)) pending.setResultData(new JSONObject(getTargetContext().getSharedPreferences("CapacitorStorage", Context.MODE_PRIVATE).getAll()).toString());
                        // Private backup only: the host must never publish or print receipt values.
                        else if ("billingSnapshot".equals(action)) pending.setResultData(new JSONObject(getTargetContext().getSharedPreferences("kreativ_billing", Context.MODE_PRIVATE).getAll()).toString());
                        else if ("finish".equals(action)) { stopped.countDown(); pending.setResultData("finished"); }
                        else throw new IllegalArgumentException("Unknown command");
                        pending.finish();
                    } catch (Exception error) { pending.setResultCode(1); pending.setResultData(error.toString()); pending.finish(); }
                }
            };
            getTargetContext().registerReceiver(receiver, new IntentFilter("com.kreativfont.studio.RELEASE_CHECK"), "android.permission.DUMP", null, Context.RECEIVER_EXPORTED);
            Bundle ready = new Bundle(); ready.putString("releaseChecks", "ready"); sendStatus(0, ready);
            stopped.await(); Thread.sleep(500);
            getTargetContext().unregisterReceiver(receiver);
            finish(Activity.RESULT_OK, new Bundle());
        } catch (Exception error) {
            Bundle failed = new Bundle(); failed.putString("error", error.toString()); finish(Activity.RESULT_CANCELED, failed);
        }
    }
}
