package com.kreativ.paperbuild;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Independent persisted PDF draft; opening the editor leaves the photo draft intact. */
final class PdfEditorSession {
    private static PdfEditorSession instance;
    static synchronized PdfEditorSession get(Context context) {
        if (instance == null) instance = new PdfEditorSession(context.getApplicationContext());
        return instance;
    }
    final ExecutorService worker = Executors.newSingleThreadExecutor();
    final DocumentProcessor processor;
    private final SharedPreferences prefs;
    private final Handler main = new Handler(Looper.getMainLooper());
    private Runnable listener;
    File source;
    PdfPagePlan plan;
    String title = "Edited PDF", status = "Choose an unlocked PDF to get started.";
    int selected, revision, sourceCount;
    boolean busy;
    boolean exporting;
    File exportedPdf;
    private PdfEditorSession(Context context) {
        processor = new DocumentProcessor(context); prefs = context.getSharedPreferences("paperbuild_editor", 0);
        String path = prefs.getString("source", "");
        File folder = new File(context.getFilesDir(), "paperbuild/editor");
        File retained = path.isEmpty() ? null : new File(folder, new File(path).getName());
        if (retained != null && retained.isFile()) {
            source = retained; title = prefs.getString("title", "Edited PDF"); sourceCount = prefs.getInt("count", 0);
            try {
                ArrayList<PdfPagePlan.Page> saved = new ArrayList<>(); JSONArray list = new JSONArray(prefs.getString("pages", "[]"));
                for (int i = 0; i < list.length(); i++) { JSONObject page = list.getJSONObject(i); saved.add(new PdfPagePlan.Page(page.getInt("source"), page.getInt("rotation"))); }
                plan = new PdfPagePlan(saved, sourceCount); selected = Math.max(0, Math.min(plan.size() - 1, prefs.getInt("selected", 0)));
                status = "PDF draft restored.";
            } catch (Exception ignored) { source = null; plan = null; }
        }
        // Clean interrupted imports only; a resumed draft's input is kept in durable private storage.
        File keep = source;
        worker.execute(() -> { File[] files = folder.listFiles(); if (files != null) for (File file : files) if (!file.equals(keep)) file.delete(); });
    }
    void attach(Runnable callback) { listener = callback; callback.run(); }
    void detach(Runnable callback) { if (listener == callback) listener = null; }
    private void notifyUi() { if (listener != null) listener.run(); }
    void open(Uri uri) {
        if (busy) return; busy = true; status = "Opening PDF…"; notifyUi();
        worker.execute(() -> {
            File imported = null; int count = 0; String failure = null;
            try { imported = processor.importEditablePdf(uri); count = processor.editablePageCount(imported); }
            catch (Exception | OutOfMemoryError error) { failure = "Couldn’t open this PDF. " + (error instanceof OutOfMemoryError ? "Try a smaller file." : error.getMessage()); }
            File ready = imported; int total = count; String message = failure;
            main.post(() -> {
                busy = false;
                if (ready != null && message == null) {
                    File previous = source; source = ready; sourceCount = total; plan = new PdfPagePlan(total); selected = 0; exportedPdf = null;
                    title = "Edited PDF"; status = "PDF draft saved. Your original stays untouched."; changed();
                    if (previous != null) worker.execute(previous::delete);
                } else { if (ready != null) worker.execute(ready::delete); status = message; notifyUi(); }
            });
        });
    }
    void changed() { revision++; selected = Math.max(0, Math.min(plan.size() - 1, selected)); save(); notifyUi(); }
    void save() {
        if (source == null || plan == null) return;
        JSONArray list = new JSONArray();
        try { for (PdfPagePlan.Page page : plan.snapshot()) list.put(new JSONObject().put("source", page.source).put("rotation", page.rotation)); }
        catch (org.json.JSONException ignored) { return; }
        prefs.edit().putString("source", source.getName()).putInt("count", sourceCount).putString("pages", list.toString())
                .putInt("selected", selected).putString("title", title).apply();
    }
    void clear() {
        if (busy) return;
        File old = source; source = null; plan = null; selected = 0; exportedPdf = null; revision++; prefs.edit().clear().apply();
        status = "PDF draft cleared. Your original and Recent PDFs stay available."; notifyUi();
        if (old != null) worker.execute(old::delete);
    }
}
