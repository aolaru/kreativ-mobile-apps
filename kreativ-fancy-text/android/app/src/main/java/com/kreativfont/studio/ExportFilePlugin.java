package com.kreativfont.studio;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import androidx.activity.result.ActivityResult;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.ActivityCallback;
import com.getcapacitor.annotation.CapacitorPlugin;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

/** Save via Android's document picker, retaining only a small cache URI in activity state. */
@CapacitorPlugin(name = "ExportFile")
public class ExportFilePlugin extends Plugin {
    private File sourceFor(PluginCall call) throws IOException {
        String path = call.getString("path");
        if (path == null) throw new IOException("Missing cached image");
        Uri uri = Uri.parse(path);
        if (!"file".equals(uri.getScheme()) || uri.getPath() == null) throw new IOException("Invalid cached image");
        File source = new File(uri.getPath()).getCanonicalFile();
        File folder = new File(getContext().getCacheDir(), "kreativ-export").getCanonicalFile();
        if (!folder.equals(source.getParentFile())) throw new IOException("Invalid cached image location");
        return source;
    }

    @PluginMethod
    public void savePng(PluginCall call) {
        try {
            File source = sourceFor(call);
            if (!source.isFile() || source.length() > 24 * 1024 * 1024) throw new IOException("Image missing or too large");
            byte[] expected = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
            try (InputStream input = new FileInputStream(source)) {
                for (byte value : expected) if (input.read() != (value & 0xff)) throw new IOException("Invalid PNG");
            }
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/png");
            String filename = call.getString("filename", "kreativ.png");
            intent.putExtra(Intent.EXTRA_TITLE, filename.replaceAll("[^a-zA-Z0-9._-]", "-"));
            startActivityForResult(call, intent, "saveResult");
        } catch (Exception exception) {
            call.reject("Could not open the file picker. Try Share image instead.", exception);
        }
    }

    @ActivityCallback
    private void saveResult(PluginCall call, ActivityResult result) {
        if (call == null) return;
        final File source;
        try { source = sourceFor(call); }
        catch (IOException exception) { call.reject("The cached image is unavailable. Please export again.", exception); return; }
        if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null) {
            source.delete();
            JSObject cancelled = new JSObject();
            cancelled.put("cancelled", true);
            call.resolve(cancelled);
            return;
        }
        Uri uri = result.getData().getData();
        if (uri == null) { source.delete(); call.reject("No destination was selected."); return; }
        getBridge().execute(() -> {
            try {
                try (InputStream input = new FileInputStream(source);
                     OutputStream output = getContext().getContentResolver().openOutputStream(uri, "wt")) {
                    if (output == null) throw new IOException("No writable destination");
                    byte[] buffer = new byte[65536];
                    int count;
                    while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
                    output.flush();
                }
                JSObject saved = new JSObject();
                saved.put("cancelled", false);
                call.resolve(saved);
            } catch (Exception exception) {
                call.reject("Could not save the image. Check the destination and available space.", exception);
            } finally {
                source.delete();
            }
        });
    }
}
