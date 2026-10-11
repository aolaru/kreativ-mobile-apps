package com.kreativ.sharesafe;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/** Photos are processed locally. Only Google Play billing uses the network. */
public class MainActivity extends Activity {
    private static final int PICK_PHOTOS = 44, WRITE_GALLERY_PERMISSION = 45, REDACT_PHOTO = 46;
    private ExportSession session;
    private BillingManager billing;
    private TextView selectionText, statusText, resultSummary;
    private Button pickButton, cleanButton, redactButton, shareButton, saveButton;
    private ProgressBar progress;
    private RadioGroup qualityGroup, formatGroup;
    private AlertDialog upgradeDialog;
    private final ExportSession.Listener sessionListener = this::renderState;

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        session = (ExportSession) getLastNonConfigurationInstance();
        if (session == null) session = new ExportSession(this, saved);
        setContentView(buildScreen());
        session.listener = sessionListener;
        billing = BillingManager.create(this, new BillingManager.Listener() {
            public void onProStatusChanged(boolean enabled) {
                if (isFinishing() || isDestroyed()) return;
                renderState();
                if (enabled && upgradeDialog != null) {
                    upgradeDialog.dismiss();
                    Toast.makeText(MainActivity.this, "ShareSafe Pro unlocked. Advanced edit is ready.", Toast.LENGTH_LONG).show();
                }
            }
            public void onBillingMessage(String message) {
                if (!isFinishing() && !isDestroyed()) Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
            }
            public void onProductChanged() { updateUpgradeDialog(); }
        });
        billing.connect();
        renderState();
    }

    @Override protected void onResume() { super.onResume(); if (billing != null) billing.refresh(); }
    @Override public Object onRetainNonConfigurationInstance() {
        session.quality = chosenQuality(); session.zip = isZipSelected(); return session;
    }
    @Override protected void onSaveInstanceState(Bundle out) {
        session.quality = chosenQuality(); session.zip = isZipSelected(); session.save(out);
        super.onSaveInstanceState(out);
    }
    @Override protected void onDestroy() {
        if (session.listener == sessionListener) session.listener = null;
        if (!isChangingConfigurations()) session.close();
        if (billing != null) billing.destroy();
        if (upgradeDialog != null) upgradeDialog.dismiss();
        super.onDestroy();
    }

    private View buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(247, 248, 243));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(20), dp(24), dp(24));
        Ui.applyInsets(scroll);
        scroll.addView(root);
        TextView badge = label("PRIVATE PHOTO UTILITY", 12, Color.rgb(52, 116, 99));
        badge.setLetterSpacing(.12f); root.addView(badge);
        TextView title = label("Share photos\nwithout sharing location.", 31, Color.rgb(25, 44, 41));
        title.setPadding(0, dp(12), 0, dp(10)); title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD); root.addView(title);
        root.addView(label("Create clean JPEG copies on your phone. Originals stay untouched. ShareSafe never uploads your photos; sharing happens only when you choose it.", 16, Color.rgb(96, 113, 109)));
        addSpacer(root, 24);
        pickButton = button("Choose photos", true);
        pickButton.setOnClickListener(v -> choosePhotos()); root.addView(pickButton, matchWrap());
        selectionText = label("No photos selected", 15, Color.rgb(96, 113, 109));
        selectionText.setGravity(Gravity.CENTER); selectionText.setPadding(0, dp(12), 0, dp(20)); root.addView(selectionText);
        root.addView(label("Export quality", 17, Color.rgb(25, 44, 41)));
        root.addView(label("JPEG copies have metadata removed. Large images are reduced to at most 4096 pixels per side / 8 megapixels. Transparency becomes white; animated images export one frame.", 14, Color.rgb(96, 113, 109)));
        qualityGroup = new RadioGroup(this);
        qualityGroup.addView(radio("Balanced — smaller files", 76, session.quality == 76));
        qualityGroup.addView(radio("High — sharper photos", 88, session.quality == 88));
        qualityGroup.addView(radio("Maximum — larger files", 95, session.quality == 95)); root.addView(qualityGroup);
        addSpacer(root, 18); root.addView(label("Share format", 17, Color.rgb(25, 44, 41)));
        root.addView(label("Individual photos for chat apps; ZIP for email or archives.", 14, Color.rgb(96, 113, 109)));
        formatGroup = new RadioGroup(this);
        formatGroup.addView(radio("Individual photos — recommended", 0, !session.zip));
        formatGroup.addView(radio("One ZIP file", 1, session.zip)); root.addView(formatGroup);
        addSpacer(root, 20);
        cleanButton = button("Clean photos", true);
        cleanButton.setOnClickListener(v -> session.clean(chosenQuality(), isZipSelected())); root.addView(cleanButton, matchWrap());
        addSpacer(root, 10);
        redactButton = button("Advanced edit • Pro", false);
        redactButton.setVisibility(BuildConfig.PRO_AVAILABLE ? View.VISIBLE : View.GONE);
        redactButton.setOnClickListener(v -> redactSelectedPhoto()); root.addView(redactButton, matchWrap());
        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100); root.addView(progress, matchWrap());
        statusText = label("", 14, Color.rgb(96, 113, 109));
        statusText.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        statusText.setPadding(0, dp(12), 0, dp(8)); root.addView(statusText);
        resultSummary = label("", 14, Color.rgb(32, 82, 70));
        resultSummary.setPadding(dp(16), dp(14), dp(16), dp(14)); resultSummary.setBackgroundResource(R.drawable.result_card); root.addView(resultSummary);
        addSpacer(root, 10); shareButton = button("Share cleaned photos", false);
        shareButton.setOnClickListener(v -> shareLatestExport()); root.addView(shareButton, matchWrap());
        addSpacer(root, 10); saveButton = button("Save to gallery", false);
        saveButton.setOnClickListener(v -> saveToGallery()); root.addView(saveButton, matchWrap());
        if (BuildConfig.PRO_AVAILABLE) {
            addSpacer(root, 10); Button restore = button("Restore Pro purchase", false);
            restore.setOnClickListener(v -> billing.restore()); root.addView(restore, matchWrap());
        }
        addSpacer(root, 10); Button about = button("Privacy & About", false);
        about.setOnClickListener(v -> startActivity(new Intent(this, AboutActivity.class))); root.addView(about, matchWrap());
        if (BuildConfig.DEBUG && BuildConfig.PRO_AVAILABLE) {
            addSpacer(root, 10); Button demo = button("Try editor with demo image • Test build", false);
            demo.setOnClickListener(v -> { if (!session.busy) startActivityForResult(new Intent().setClassName(this, "com.kreativ.sharesafe.DemoActivity"), REDACT_PHOTO); });
            root.addView(demo, matchWrap());
        }
        addSpacer(root, 20);
        root.addView(label("Metadata removal does not hide details visible inside a photo. Always review your clean copies before sharing.", 13, Color.rgb(96, 113, 109)));
        return scroll;
    }

    private void renderState() {
        boolean idle = !session.busy;
        pickButton.setEnabled(idle);
        cleanButton.setEnabled(idle && !session.selected.isEmpty());
        redactButton.setEnabled(idle && session.selected.size() == 1);
        for (int i = 0; i < qualityGroup.getChildCount(); i++) qualityGroup.getChildAt(i).setEnabled(idle);
        for (int i = 0; i < formatGroup.getChildCount(); i++) formatGroup.getChildAt(i).setEnabled(idle);
        selectionText.setText(session.selected.isEmpty() ? "No photos selected" : session.selected.size() + " photo(s) selected");
        statusText.setText(session.status);
        progress.setVisibility(session.busy ? View.VISIBLE : View.GONE); progress.setProgress(session.progress);
        boolean ready = session.result != null;
        resultSummary.setVisibility(ready ? View.VISIBLE : View.GONE);
        if (ready) resultSummary.setText(session.result.summary());
        shareButton.setVisibility(ready ? View.VISIBLE : View.GONE); shareButton.setEnabled(ready && idle);
        shareButton.setText(ready && session.result.zipFile != null ? "Share ZIP" : "Share cleaned photos");
        saveButton.setVisibility(ready ? View.VISIBLE : View.GONE); saveButton.setEnabled(ready && idle);
    }

    private void choosePhotos() {
        if (session.busy) return;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(Intent.createChooser(intent, "Choose photos to clean"), PICK_PHOTOS);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (session.busy || resultCode != RESULT_OK || data == null) return;
        if (requestCode == REDACT_PHOTO) {
            String path = data.getStringExtra(RedactActivity.EXTRA_REDACTED_PATH);
            if (path == null || !new File(path).isFile()) return;
            session.select(Collections.singletonList(Uri.fromFile(new File(path))));
            session.status = "Redacted copy ready. Tap Clean photos to prepare it for sharing.";
            renderState(); return;
        }
        if (requestCode != PICK_PHOTOS) return;
        Set<Uri> unique = new LinkedHashSet<>();
        if (data.getData() != null) unique.add(data.getData());
        ClipData clips = data.getClipData();
        if (clips != null) for (int i = 0; i < clips.getItemCount(); i++) unique.add(clips.getItemAt(i).getUri());
        for (Uri uri : unique) {
            try { getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); }
            catch (SecurityException ignored) { /* Some providers grant only temporary access. */ }
        }
        session.select(new ArrayList<>(unique));
    }

    private void redactSelectedPhoto() {
        if (session.busy || session.selected.size() != 1 || !BuildConfig.PRO_AVAILABLE) return;
        if (!billing.isProEnabled()) { showProUpgrade(); return; }
        startActivityForResult(new Intent(this, RedactActivity.class).setData(session.selected.get(0)), REDACT_PHOTO);
    }

    private void showProUpgrade() {
        upgradeDialog = new AlertDialog.Builder(this).setTitle("Unlock ShareSafe Pro")
                .setMessage(upgradeMessage()).setNegativeButton("Not now", null)
                .setNeutralButton("Restore purchase", (dialog, which) -> billing.restore())
                .setPositiveButton("Unlock Pro", (dialog, which) -> billing.launchPurchase(this)).create();
        upgradeDialog.show(); updateUpgradeDialog(); billing.refresh();
    }

    private String upgradeMessage() {
        String price = billing.price();
        return "Manual privacy tools for one photo at a time:\n\n• Solid Cover for sensitive text, IDs and faces\n• Optional blur and pixelation effects\n• Zoom, pan, undo and flattened JPEG copies\n\nBlur and pixelation may still reveal information. Use Cover for stronger privacy.\n\n"
                + (price == null ? billing.purchaseStatus()
                    : "One-time purchase: " + price + ". No subscription.");
    }

    private void updateUpgradeDialog() {
        if (isFinishing() || isDestroyed() || upgradeDialog == null || !upgradeDialog.isShowing() || billing == null) return;
        upgradeDialog.setMessage(upgradeMessage());
        upgradeDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(billing.canPurchase());
    }

    private void shareLatestExport() {
        if (session.busy || session.result == null) return;
        ArrayList<Uri> uris = new ArrayList<>();
        boolean zip = session.result.zipFile != null;
        ArrayList<File> files = zip ? new ArrayList<>(Collections.singletonList(session.result.zipFile)) : session.result.photos;
        for (File file : files) if (file.isFile()) uris.add(FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file));
        if (uris.isEmpty()) { session.status = "Temporary copies are no longer available. Please clean the photos again."; renderState(); return; }
        Intent send = new Intent(uris.size() == 1 ? Intent.ACTION_SEND : Intent.ACTION_SEND_MULTIPLE).setType(zip ? "application/zip" : "image/jpeg");
        if (uris.size() == 1) send.putExtra(Intent.EXTRA_STREAM, uris.get(0)); else send.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        ClipData clips = ClipData.newUri(getContentResolver(), "ShareSafe clean copies", uris.get(0));
        for (int i = 1; i < uris.size(); i++) clips.addItem(new ClipData.Item(uris.get(i)));
        send.setClipData(clips); send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(send, zip ? "Share cleaned photo ZIP" : "Share cleaned photos"));
    }

    private void saveToGallery() {
        if (session.busy || session.result == null) return;
        if (Build.VERSION.SDK_INT <= 28 && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_GALLERY_PERMISSION); return;
        }
        session.saveToGallery();
    }
    @Override public void onRequestPermissionsResult(int code, String[] permissions, int[] grants) {
        super.onRequestPermissionsResult(code, permissions, grants);
        if (code == WRITE_GALLERY_PERMISSION) {
            if (grants.length > 0 && grants[0] == PackageManager.PERMISSION_GRANTED) saveToGallery();
            else Toast.makeText(this, "Gallery permission is needed to save on this Android version.", Toast.LENGTH_LONG).show();
        }
    }
    private int chosenQuality() {
        View selected = qualityGroup.findViewById(qualityGroup.getCheckedRadioButtonId());
        return selected == null ? 76 : (Integer) selected.getTag();
    }
    private boolean isZipSelected() {
        View selected = formatGroup.findViewById(formatGroup.getCheckedRadioButtonId());
        return selected != null && (Integer) selected.getTag() == 1;
    }
    private RadioButton radio(String text, int tag, boolean checked) {
        RadioButton radio = new RadioButton(this); radio.setId(View.generateViewId()); radio.setText(text);
        radio.setTag(tag); radio.setChecked(checked); radio.setTextSize(15); radio.setTextColor(Color.rgb(25, 44, 41));
        radio.setPadding(0, dp(5), 0, dp(5)); return radio;
    }
    private Button button(String text, boolean primary) {
        Button button = new Button(this); button.setText(text); button.setTextSize(16); button.setAllCaps(false);
        button.setMinHeight(dp(54)); button.setTextColor(primary ? Color.WHITE : Color.rgb(25, 44, 41));
        button.setBackgroundResource(primary ? R.drawable.button_primary : R.drawable.button_secondary); return button;
    }
    private TextView label(String text, float size, int color) {
        TextView label = new TextView(this); label.setText(text); label.setTextSize(size); label.setTextColor(color);
        label.setLineSpacing(dp(2), 1f); return label;
    }
    private LinearLayout.LayoutParams matchWrap() { return new LinearLayout.LayoutParams(-1, -2); }
    private void addSpacer(LinearLayout root, int height) { root.addView(new View(this), new LinearLayout.LayoutParams(1, dp(height))); }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
