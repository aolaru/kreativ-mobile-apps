package com.kreativ.paperbuild;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** A private persisted draft and one application-scoped job, independent of Activity recreation. */
final class DocumentSession {
    interface Task { Result run(DocumentProcessor.Progress progress) throws Exception; }
    static final class Result {
        final File pdf;
        final String summary;
        final String message;
        final ArrayList<PageItem> imported;
        Result(File pdf, String summary, String message) { this(pdf, summary, message, null); }
        Result(File pdf, String summary, String message, ArrayList<PageItem> imported) {
            this.pdf = pdf; this.summary = summary; this.message = message; this.imported = imported;
        }
    }
    private static DocumentSession instance;
    static synchronized DocumentSession get(Context context) {
        if (instance == null) instance = new DocumentSession(context);
        return instance;
    }

    final ArrayList<PageItem> pages = new ArrayList<>();
    final ExecutorService worker = Executors.newSingleThreadExecutor();
    final DocumentProcessor processor;
    private final SharedPreferences prefs;
    private final Handler main = new Handler(Looper.getMainLooper());
    private Runnable listener;
    String name = "My document";
    int paper, enhancement, compression;
    File pendingCapture, latestPdf;
    String summary = "", status = "";
    boolean busy;
    int progress = -1;
    int revision;

    DocumentSession(Context context) {
        Context app = context.getApplicationContext();
        processor = new DocumentProcessor(app);
        prefs = app.getSharedPreferences("paperbuild_draft", Context.MODE_PRIVATE);
        restore();
        Set<String> keep = new HashSet<>();
        for (PageItem page : pages) if ("file".equals(page.uri.getScheme())) keep.add(page.uri.getPath());
        if (pendingCapture != null) keep.add(pendingCapture.getPath());
        File retainedPdf = latestPdf;
        long cleanupBefore = System.currentTimeMillis();
        worker.execute(() -> {
            cleanFiles(new File(app.getFilesDir(), "paperbuild/pages"), keep, 0, cleanupBefore);
            Set<String> exports = new HashSet<>();
            if (retainedPdf != null) exports.add(retainedPdf.getPath());
            cleanFiles(new File(app.getCacheDir(), "paperbuild"), exports, 7L * 24 * 60 * 60 * 1000, cleanupBefore);
            cleanFiles(new File(app.getCacheDir(), "paperbuild/scratch"), new HashSet<>(), 0, cleanupBefore);
        });
    }

    void attach(Runnable listener) { this.listener = listener; listener.run(); }
    void detach(Runnable listener) { if (this.listener == listener) this.listener = null; save(); }
    void notifyUi() { if (listener != null) listener.run(); }
    void changed() {
        latestPdf = null; summary = ""; status = ""; revision++;
        save(); notifyUi();
    }

    void run(String message, Task task) {
        if (busy) return;
        busy = true; progress = -1; status = message;
        save(); notifyUi();
        worker.execute(() -> {
            Result result = null;
            String error = null;
            try {
                result = task.run(value -> main.post(() -> { progress = value; notifyUi(); }));
            } catch (OutOfMemoryError exhausted) {
                error = "Not enough memory. Try fewer or smaller files.";
            } catch (Exception failure) {
                error = failure.getMessage();
                if (error == null || error.isEmpty()) error = "Couldn’t complete this action. Choose the files again.";
            }
            Result finished = result;
            String failure = error;
            main.post(() -> {
                busy = false; progress = -1;
                if (finished != null) {
                    if (finished.imported != null) {
                        pages.addAll(finished.imported); revision++;
                        latestPdf = null; summary = "";
                    }
                    if (finished.pdf != null) { latestPdf = finished.pdf; summary = finished.summary; }
                    status = finished.message;
                } else { status = failure; }
                save(); notifyUi();
            });
        });
    }

    void deletePageFile(PageItem page) {
        if (!"file".equals(page.uri.getScheme())) return;
        File file = new File(page.uri.getPath());
        worker.execute(() -> file.delete());
    }

    void save() {
        JSONArray list = new JSONArray();
        try {
            for (PageItem page : pages) list.put(new JSONObject().put("uri", page.uri.toString())
                    .put("name", page.name).put("rotation", page.rotation));
        } catch (JSONException ignored) { return; }
        prefs.edit().putString("pages", list.toString()).putString("name", name)
                .putInt("paper", paper).putInt("enhancement", enhancement).putInt("compression", compression)
                .putString("capture", pendingCapture == null ? "" : pendingCapture.getPath())
                .putString("pdf", latestPdf == null ? "" : latestPdf.getPath())
                .putString("summary", summary).putBoolean("busy", busy).apply();
    }

    private void restore() {
        name = prefs.getString("name", "My document");
        paper = prefs.getInt("paper", 0); enhancement = prefs.getInt("enhancement", 0); compression = prefs.getInt("compression", 0);
        try {
            JSONArray list = new JSONArray(prefs.getString("pages", "[]"));
            for (int i = 0; i < list.length(); i++) {
                JSONObject entry = list.getJSONObject(i);
                Uri uri = Uri.parse(entry.getString("uri"));
                if ("file".equals(uri.getScheme()) && new File(uri.getPath()).exists()) {
                    pages.add(new PageItem(uri, entry.getString("name"), entry.optInt("rotation")));
                }
            }
        } catch (JSONException ignored) { status = "Some draft pages could not be restored."; }
        String capture = prefs.getString("capture", "");
        if (!capture.isEmpty()) pendingCapture = new File(capture);
        String pdf = prefs.getString("pdf", "");
        if (!pdf.isEmpty() && new File(pdf).exists()) { latestPdf = new File(pdf); summary = prefs.getString("summary", ""); }
        if (prefs.getBoolean("busy", false)) status = "The previous action was interrupted. Your draft is saved; try again.";
    }

    private static void cleanFiles(File folder, Set<String> keep, long minimumAge, long cleanupBefore) {
        File[] files = folder.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isFile() && !keep.contains(file.getPath())
                    && file.lastModified() < cleanupBefore
                    && System.currentTimeMillis() - file.lastModified() >= minimumAge) file.delete();
        }
    }
}
