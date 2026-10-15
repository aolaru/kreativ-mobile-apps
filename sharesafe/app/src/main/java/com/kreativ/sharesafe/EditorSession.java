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
import java.util.ArrayDeque;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Session owns worker bitmaps. Configuration changes only replace the UI observer. */
final class EditorSession {
    interface Listener { void changed(); }
    final ArrayList<RedactionRenderer.Mask> masks = new ArrayList<>();
    private static final int HISTORY_LIMIT = 20;
    final ArrayDeque<ArrayList<RedactionRenderer.Mask>> undoHistory = new ArrayDeque<>(), redoHistory = new ArrayDeque<>();
    final ArrayList<RectF> faceSuggestions = new ArrayList<>();
    boolean[] faceChoices = new boolean[0];
    private final FaceAssist faceAssist;
    boolean detecting, selectMode;
    int selectedMask = -1;
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
        this(context, source, saved, new StoreFaceAssist());
    }
    EditorSession(Context context, Uri source, Bundle saved, FaceAssist faceAssist) {
        this.context = context.getApplicationContext(); this.source = source;
        this.faceAssist = faceAssist;
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
                            ? "Export was interrupted. Your edits are restored; please create the copy again."
                            : saved != null && saved.getBoolean("face_detecting") ? "Face scan was interrupted. Tap Find faces to retry; existing edits are restored."
                            : faceSuggestions.isEmpty() ? "" : "Restored face suggestions. Review them before applying covers.";
                    changed();
                });
            } catch (Exception | OutOfMemoryError e) {
                main.post(() -> { if (!closed) { loadFailed = true; status = "Couldn’t open this image. Try a different or smaller file."; changed(); } });
            }
        });
    }

    List<RedactionRenderer.Mask> snapshot() { return new ArrayList<>(masks); }
    void changed() { if (!closed && listener != null) listener.changed(); }
    boolean editable() { return ready && !closed && !busy && !detecting; }
    boolean hasSelection() { return selectedMask >= 0 && selectedMask < masks.size(); }
    private void remember() {
        undoHistory.addLast(new ArrayList<>(masks));
        if (undoHistory.size() > HISTORY_LIMIT) undoHistory.removeFirst();
        redoHistory.clear();
    }

    void add(RectF area) {
        if (!editable() || masks.size() >= 200 || !MaskGeometry.valid(area)) return;
        remember(); masks.add(new RedactionRenderer.Mask(area, tool)); selectedMask = masks.size() - 1; requestPreview();
    }
    void updateMask(int index, RectF area) {
        if (!editable() || index < 0 || index >= masks.size() || !MaskGeometry.valid(area) || masks.get(index).area.equals(area)) return;
        remember(); masks.set(index, new RedactionRenderer.Mask(area, masks.get(index).tool)); selectedMask = index; requestPreview();
    }
    void deleteSelected() {
        if (!editable() || !hasSelection()) return;
        remember(); masks.remove(selectedMask); selectedMask = -1; requestPreview();
    }
    void undo() { history(undoHistory, redoHistory); }
    void redo() { history(redoHistory, undoHistory); }
    private void history(ArrayDeque<ArrayList<RedactionRenderer.Mask>> from, ArrayDeque<ArrayList<RedactionRenderer.Mask>> to) {
        if (!editable() || from.isEmpty()) return;
        to.addLast(new ArrayList<>(masks)); if (to.size() > HISTORY_LIMIT) to.removeFirst();
        masks.clear(); masks.addAll(from.removeLast()); selectedMask = -1; requestPreview();
    }
    void reset() {
        if (!editable()) return;
        if (!masks.isEmpty()) remember();
        masks.clear(); selectedMask = -1; faceSuggestions.clear(); faceChoices = new boolean[0];
        zoom = 1; panX = panY = 0; requestPreview();
    }
    void findFaces() {
        if (!BuildConfig.PRO_AVAILABLE || !editable() || previewPending) return;
        detecting = true; faceSuggestions.clear(); faceChoices = new boolean[0];
        status = "Finding possible faces on your device…"; changed();
        worker.execute(() -> {
            if (closed) return;
            try {
                List<RectF> found = faceAssist.find(base);
                ArrayList<RectF> valid = new ArrayList<>();
                for (RectF area : found) if (MaskGeometry.valid(area) && valid.size() < 200) valid.add(new RectF(area));
                main.post(() -> {
                    if (closed) return;
                    detecting = false; faceSuggestions.addAll(valid); faceChoices = new boolean[valid.size()];
                    java.util.Arrays.fill(faceChoices, true);
                    status = valid.isEmpty() ? "No faces detected. Faces may still be present; review the entire photo."
                            : valid.size() + " possible face(s). Suggestions are not covered yet. Review them before applying.";
                    changed();
                });
            } catch (Exception | OutOfMemoryError e) {
                main.post(() -> { if (!closed) { detecting = false; status = "Face Assist couldn’t finish. Retry or add covers manually; no faces have been marked safe."; changed(); } });
            }
        });
    }
    void discardFaceSuggestions() {
        if (!editable()) return;
        faceSuggestions.clear(); faceChoices = new boolean[0]; status = "Suggestions dismissed. Review visible details before exporting."; changed();
    }
    void applyFaceSuggestions() {
        if (!editable()) return;
        ArrayList<RedactionRenderer.Mask> additions = new ArrayList<>();
        for (int i = 0; i < faceSuggestions.size(); i++) if (faceChoices[i]) {
            RectF area = faceSuggestions.get(i); boolean covered = false;
            for (RedactionRenderer.Mask mask : masks) if (mask.tool == RedactionRenderer.COVER && mask.area.contains(area)) { covered = true; break; }
            if (!covered) additions.add(new RedactionRenderer.Mask(area, RedactionRenderer.COVER));
        }
        if (masks.size() + additions.size() > 200) { status = "Limit: 200 areas. Deselect suggestions or remove existing areas before applying."; changed(); return; }
        if (!additions.isEmpty()) { remember(); masks.addAll(additions); selectedMask = masks.size() - 1; }
        faceSuggestions.clear(); faceChoices = new boolean[0]; requestPreview();
    }
    void requestPreview() {
        if (!editable()) return;
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
        if (!editable() || masks.isEmpty() || previewPending || !previewValid || !faceSuggestions.isEmpty()) return;
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
        out.putFloatArray("masks", encode(masks)); out.putInt("tool", tool);
        out.putParcelableArrayList("editor_undo", encodeHistory(undoHistory));
        out.putParcelableArrayList("editor_redo", encodeHistory(redoHistory));
        ArrayList<RedactionRenderer.Mask> suggestions = new ArrayList<>();
        for (RectF area : faceSuggestions) suggestions.add(new RedactionRenderer.Mask(area, RedactionRenderer.COVER));
        out.putFloatArray("face_suggestions", encode(suggestions)); out.putBooleanArray("face_choices", faceChoices);
        out.putBoolean("face_detecting", detecting); out.putBoolean("select_mode", selectMode); out.putInt("selected_mask", selectedMask);
        out.putFloat("zoom", zoom); out.putFloat("pan_x", panX); out.putFloat("pan_y", panY);
        out.putBoolean("move_mode", moveMode); out.putBoolean("editor_busy", busy);
        out.putString("editor_output", outputPath);
    }
    private static float[] encode(List<RedactionRenderer.Mask> list) {
        float[] data = new float[list.size() * 5];
        for (int i = 0; i < list.size(); i++) {
            RedactionRenderer.Mask mask = list.get(i); int n = i * 5;
            data[n] = mask.area.left; data[n + 1] = mask.area.top; data[n + 2] = mask.area.right;
            data[n + 3] = mask.area.bottom; data[n + 4] = mask.tool;
        }
        return data;
    }
    private static ArrayList<RedactionRenderer.Mask> decode(float[] data) {
        ArrayList<RedactionRenderer.Mask> list = new ArrayList<>();
        if (data != null) for (int i = 0; i + 4 < data.length && list.size() < 200; i += 5) {
            RectF area = new RectF(data[i], data[i + 1], data[i + 2], data[i + 3]);
            int tool = (int) data[i + 4];
            if (MaskGeometry.valid(area) && tool >= 0 && tool <= 2) list.add(new RedactionRenderer.Mask(area, tool));
        }
        return list;
    }
    private static ArrayList<Bundle> encodeHistory(ArrayDeque<ArrayList<RedactionRenderer.Mask>> history) {
        ArrayList<Bundle> encoded = new ArrayList<>();
        for (ArrayList<RedactionRenderer.Mask> state : history) { Bundle bundle = new Bundle(); bundle.putFloatArray("areas", encode(state)); encoded.add(bundle); }
        return encoded;
    }
    private static void restoreHistory(Bundle saved, String key, ArrayDeque<ArrayList<RedactionRenderer.Mask>> history) {
        ArrayList<Bundle> states = saved.getParcelableArrayList(key);
        if (states != null) for (Bundle state : states) if (state != null && history.size() < HISTORY_LIMIT) history.addLast(decode(state.getFloatArray("areas")));
    }
    private void restore(Bundle saved) {
        if (saved == null) return;
        masks.addAll(decode(saved.getFloatArray("masks")));
        restoreHistory(saved, "editor_undo", undoHistory); restoreHistory(saved, "editor_redo", redoHistory);
        for (RedactionRenderer.Mask mask : decode(saved.getFloatArray("face_suggestions"))) faceSuggestions.add(mask.area);
        boolean[] choices = saved.getBooleanArray("face_choices");
        faceChoices = new boolean[faceSuggestions.size()]; java.util.Arrays.fill(faceChoices, true);
        if (choices != null && choices.length == faceChoices.length) faceChoices = choices.clone();
        selectedMask = saved.getInt("selected_mask", -1); if (!hasSelection()) selectedMask = -1;
        selectMode = saved.getBoolean("select_mode");
        tool = saved.getInt("tool", RedactionRenderer.COVER); zoom = saved.getFloat("zoom", 1);
        panX = saved.getFloat("pan_x"); panY = saved.getFloat("pan_y"); moveMode = saved.getBoolean("move_mode");
        String path = saved.getString("editor_output");
        if (path != null && new File(path).isFile()) outputPath = path;
    }
    void close() {
        if (closed) return;
        closed = true; listener = null; preview = null;
        // Drain in order: no bitmap can be freed underneath an export or preview task.
        worker.execute(() -> { if (base != null) { base.recycle(); base = null; } });
        worker.shutdown();
    }
}
