package com.kreativ.paperbuild;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Durable app-private copies. Removal is reversible until the user empties an item from Removed PDFs. */
final class PdfLibrary {
    static final class Entry {
        final String id, title;
        final long created;
        final int pages;
        final boolean protectedPdf, removed;
        Entry(String id, String title, long created, int pages, boolean protectedPdf, boolean removed) {
            this.id = id; this.title = title; this.created = created; this.pages = pages;
            this.protectedPdf = protectedPdf; this.removed = removed;
        }
    }
    private static final Object LOCK = new Object();
    private final SharedPreferences prefs;
    private final File folder;
    PdfLibrary(Context context) {
        prefs = context.getSharedPreferences("paperbuild_library", Context.MODE_PRIVATE);
        folder = new File(context.getFilesDir(), "paperbuild/library");
    }
    File file(Entry entry) { return new File(folder, entry.id + ".pdf"); }
    private List<Entry> all() {
        List<Entry> result = new ArrayList<>();
        try {
            JSONArray data = new JSONArray(prefs.getString("items", "[]"));
            for (int i = 0; i < data.length(); i++) {
                JSONObject e = data.getJSONObject(i); String id = e.getString("id");
                if (!id.matches("[a-f0-9-]{36}")) continue;
                Entry entry = new Entry(id, e.getString("title"), e.getLong("created"), e.optInt("pages"),
                        e.optBoolean("protected"), e.optBoolean("removed"));
                if (file(entry).isFile()) result.add(entry);
            }
        } catch (JSONException ignored) { }
        result.sort((a, b) -> Long.compare(b.created, a.created));
        return result;
    }
    List<Entry> list(boolean removed) {
        synchronized (LOCK) { List<Entry> entries = all(); entries.removeIf(e -> e.removed != removed); return entries; }
    }
    Entry add(File source, String title, int pages, boolean protectedPdf) throws IOException {
        synchronized (LOCK) {
            if (!folder.isDirectory() && !folder.mkdirs()) throw new IOException("Could not save Recent PDFs. Free some space and try again.");
            for (Entry existing : all()) if (file(existing).equals(source)) return existing;
            Entry entry = new Entry(UUID.randomUUID().toString(), DocumentProcessor.safeName(title),
                    System.currentTimeMillis(), pages, protectedPdf, false);
            File destination = file(entry), temporary = new File(folder, entry.id + ".part");
            boolean complete = false;
            try {
                try (FileInputStream input = new FileInputStream(source); FileOutputStream output = new FileOutputStream(temporary)) {
                    byte[] buffer = new byte[32768]; int count;
                    while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
                    output.getFD().sync();
                }
                if (!temporary.renameTo(destination)) throw new IOException("Could not finish saving the PDF.");
                List<Entry> entries = all(); entries.add(entry); write(entries); complete = true; return entry;
            } finally { temporary.delete(); if (!complete) destination.delete(); }
        }
    }
    void rename(Entry entry, String title) throws IOException { update(entry, DocumentProcessor.safeName(title), entry.removed); }
    void setRemoved(Entry entry, boolean removed) throws IOException { update(entry, entry.title, removed); }
    private void update(Entry entry, String title, boolean removed) throws IOException {
        synchronized (LOCK) {
            List<Entry> entries = all(); boolean found = false;
            for (int i = 0; i < entries.size(); i++) if (entries.get(i).id.equals(entry.id)) {
                Entry current = entries.get(i);
                entries.set(i, new Entry(current.id, title, current.created, current.pages, current.protectedPdf, removed)); found = true; break;
            }
            if (!found) throw new IOException("This PDF is no longer available.");
            write(entries);
        }
    }
    void deleteForever(Entry entry) throws IOException {
        synchronized (LOCK) {
            if (!entry.removed) throw new IOException("Move the PDF to Removed PDFs first.");
            List<Entry> entries = all();
            Entry current = null;
            for (Entry e : entries) if (e.id.equals(entry.id)) current = e;
            if (current == null || !current.removed) throw new IOException("This PDF is no longer in Removed PDFs.");
            if (file(current).exists() && !file(current).delete()) throw new IOException("Could not remove this PDF.");
            entries.removeIf(e -> e.id.equals(entry.id)); write(entries);
        }
    }
    private void write(List<Entry> entries) throws IOException {
        JSONArray data = new JSONArray();
        try {
            for (Entry e : entries) data.put(new JSONObject().put("id", e.id).put("title", e.title).put("created", e.created)
                    .put("pages", e.pages).put("protected", e.protectedPdf).put("removed", e.removed));
        } catch (JSONException error) { throw new IOException("Could not update Recent PDFs.", error); }
        if (!prefs.edit().putString("items", data.toString()).commit()) throw new IOException("Could not save Recent PDFs.");
    }
}
