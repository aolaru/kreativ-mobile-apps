package com.kreativ.sharesafe;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Session owns worker bitmaps. Configuration changes only replace the UI observer. */
final class EditorSession {
    interface Listener { void changed(); }
    final ArrayList<RedactionRenderer.Mask> masks = new ArrayList<>();
    final Context context;
    final Uri source;
    final Handler main = new Handler(Looper.getMainLooper());
    final ExecutorService worker = Executors.newSingleThreadExecutor();
    Listener listener;
    Bitmap preview;
    private Bitmap base; // Worker thread only; never drawn by an Activity.
    volatile boolean closed;
    boolean ready, busy, previewPending, previewValid, loadFailed;
    int tool = RedactionRenderer.COVER;
    int revision;
    float zoom = 1, panX, panY;
    boolean moveMode;
    String status = "Opening photo…";
    String outputPath;

    EditorSession(Context context, Uri source, Bundle saved) {
        this.context = context.getApplicationContext(); this.source = source;
        restore(saved);
        List<RedactionRenderer.Mask> snapshot = snapshot();
        worker.execute(() -> {
            try {
                if (closed) return;
                base = ImageProcessor.decode(this.context, source, 1280, 1_600_000).bitmap;
                Bitmap rendered = RedactionRenderer.render(base, snapshot);
                main.post(() -> {
                    if (closed) { rendered.recycle(); return; }
                    preview = rendered; ready = true; previewValid = true;
                    status = saved != null && saved.getBoolean("editor_busy")
                            ? "Export was interrupted. Your edits are restored; please create the copy again." : "";
                    changed();
                });
            } catch (Exception | OutOfMemoryError e) {
                main.post(() -> { if (!closed) { loadFailed = true; status = "Couldn’t open this image. Try a different or smaller file."; changed(); } });
            }
        });
    }

    List<RedactionRenderer.Mask> snapshot() { return new ArrayList<>(masks); }
    void changed() { if (!closed && listener != null) listener.changed(); }

    void add(RectF area) {
        if (!ready || busy || masks.size() >= 200) return;
        masks.add(new RedactionRenderer.Mask(area, tool)); requestPreview();
    }
    void undo() { if (!busy && !masks.isEmpty()) { masks.remove(masks.size() - 1); requestPreview(); } }
    void reset() {
        if (busy) return;
        masks.clear(); zoom = 1; panX = panY = 0; requestPreview();
    }
    void requestPreview() {
        if (!ready || busy || closed) return;
        int requested = ++revision;
        List<RedactionRenderer.Mask> snapshot = snapshot();
        previewPending = true; previewValid = false; status = "Updating preview…"; changed();
        worker.execute(() -> {
            if (closed) return;
            try {
                Bitmap rendered = RedactionRenderer.render(base, snapshot);
                main.post(() -> {
                    if (closed || revision != requested) { rendered.recycle(); return; }
                    // Displayed bitmaps are left to GC, not recycled while the renderer may use them.
                    preview = rendered; previewPending = false; previewValid = true; status = ""; changed();
                });
            } catch (Exception | OutOfMemoryError e) {
                main.post(() -> {
                    if (!closed && revision == requested) {
                        previewPending = false; status = "Preview couldn’t update. Undo the last edit or try a smaller image."; changed();
                    }
                });
            }
        });
    }

    void export() {
        if (!ready || busy || closed || masks.isEmpty() || previewPending || !previewValid) return;
        List<RedactionRenderer.Mask> snapshot = snapshot();
        busy = true; status = "Creating flattened redacted copy…"; changed();
        worker.execute(() -> {
            Bitmap sourceBitmap = null, output = null;
            File folder = null;
            try {
                ImageProcessor.Decoded decoded = ImageProcessor.decode(context, source);
                sourceBitmap = decoded.bitmap;
                output = RedactionRenderer.render(sourceBitmap, snapshot);
                sourceBitmap.recycle(); sourceBitmap = null;
                folder = PrivateCache.newFolder(context, "redactions");
                File file = new File(folder, "ShareSafe-redacted.jpg");
                try (FileOutputStream stream = new FileOutputStream(file)) {
                    if (!output.compress(Bitmap.CompressFormat.JPEG, 95, stream)) throw new IOException("Could not write image");
                }
                main.post(() -> { if (!closed) { busy = false; outputPath = file.getAbsolutePath(); changed(); } });
            } catch (Exception | OutOfMemoryError e) {
                if (folder != null) PrivateCache.delete(folder);
                main.post(() -> { if (!closed) { busy = false; status = "Couldn’t create the copy. Your edits are kept; please try again."; changed(); } });
            } finally {
                if (sourceBitmap != null) sourceBitmap.recycle();
                if (output != null) output.recycle();
            }
        });
    }

    void save(Bundle out) {
        float[] data = new float[masks.size() * 5];
        for (int i = 0; i < masks.size(); i++) {
            RedactionRenderer.Mask mask = masks.get(i); int n = i * 5;
            data[n] = mask.area.left; data[n + 1] = mask.area.top; data[n + 2] = mask.area.right;
            data[n + 3] = mask.area.bottom; data[n + 4] = mask.tool;
        }
        out.putFloatArray("masks", data); out.putInt("tool", tool);
        out.putFloat("zoom", zoom); out.putFloat("pan_x", panX); out.putFloat("pan_y", panY);
        out.putBoolean("move_mode", moveMode); out.putBoolean("editor_busy", busy);
        out.putString("editor_output", outputPath);
    }
    private void restore(Bundle saved) {
        if (saved == null) return;
        float[] data = saved.getFloatArray("masks");
        if (data != null) for (int i = 0; i + 4 < data.length && masks.size() < 200; i += 5) {
            masks.add(new RedactionRenderer.Mask(new RectF(data[i], data[i + 1], data[i + 2], data[i + 3]), (int) data[i + 4]));
        }
        tool = saved.getInt("tool", RedactionRenderer.COVER); zoom = saved.getFloat("zoom", 1);
        panX = saved.getFloat("pan_x"); panY = saved.getFloat("pan_y"); moveMode = saved.getBoolean("move_mode");
        String path = saved.getString("editor_output");
        if (path != null && new File(path).isFile()) outputPath = path;
    }
    void close() {
        closed = true; listener = null; preview = null;
        // Drain in order: no bitmap can be freed underneath an export or preview task.
        worker.execute(() -> { if (base != null) { base.recycle(); base = null; } });
        worker.shutdown();
    }
}
