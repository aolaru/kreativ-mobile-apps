package com.kreativ.sharesafe;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Retained across configuration changes; workers never hold an Activity or mutable UI list. */
final class ExportSession {
    interface Listener { void changed(); }
    final ArrayList<Uri> selected = new ArrayList<>();
    final Context context;
    final Handler main = new Handler(Looper.getMainLooper());
    final ExecutorService worker = Executors.newSingleThreadExecutor();
    Listener listener;
    Result result;
    boolean busy;
    boolean closed;
    int progress;
    int quality = 76;
    boolean zip;
    String status = "";

    ExportSession(Context context, Bundle saved) {
        this.context = context.getApplicationContext();
        if (saved != null) {
            ArrayList<String> uris = saved.getStringArrayList("selection");
            if (uris != null) for (String uri : uris) selected.add(Uri.parse(uri));
            quality = saved.getInt("quality", 76);
            zip = saved.getBoolean("zip");
            status = saved.getString("status", "");
            ArrayList<String> paths = saved.getStringArrayList("export_paths");
            if (paths != null) {
                ArrayList<File> files = new ArrayList<>();
                for (String path : paths) if (new File(path).isFile()) files.add(new File(path));
                String zipPath = saved.getString("zip_path");
                File zipFile = zipPath == null ? null : new File(zipPath);
                if (!files.isEmpty() && (zipFile == null || zipFile.isFile())) {
                    result = new Result(files, zipFile, saved.getInt("skipped"), saved.getInt("resized"));
                }
            }
            if (saved.getBoolean("busy")) status = "Previous operation was interrupted. Please try again.";
        }
        worker.execute(() -> PrivateCache.cleanExpired(this.context));
    }

    void save(Bundle out) {
        ArrayList<String> uris = new ArrayList<>();
        for (Uri uri : selected) uris.add(uri.toString());
        out.putStringArrayList("selection", uris);
        out.putInt("quality", quality);
        out.putBoolean("zip", zip);
        out.putBoolean("busy", busy);
        out.putString("status", status);
        if (result != null) {
            ArrayList<String> paths = new ArrayList<>();
            for (File file : result.photos) paths.add(file.getAbsolutePath());
            out.putStringArrayList("export_paths", paths);
            if (result.zipFile != null) out.putString("zip_path", result.zipFile.getAbsolutePath());
            out.putInt("skipped", result.skipped);
            out.putInt("resized", result.resized);
        }
    }

    void changed() { if (!closed && listener != null) listener.changed(); }

    void select(List<Uri> photos) {
        if (busy) return;
        selected.clear();
        selected.addAll(photos);
        result = null;
        status = selected.isEmpty() ? "" : "Ready to create clean copies.";
        changed();
    }

    void clean(int quality, boolean zip) {
        if (busy || selected.isEmpty()) return;
        this.quality = quality;
        this.zip = zip;
        List<Uri> snapshot = new ArrayList<>(selected);
        busy = true;
        result = null;
        progress = 0;
        status = "Preparing clean copies…";
        changed();
        worker.execute(() -> {
            try {
                Result output = export(snapshot, quality, zip);
                main.post(() -> {
                    if (closed) return;
                    result = output;
                    busy = false;
                    status = output.photos.size() + " clean photo(s) ready" + (zip ? " in a ZIP." : " to share.");
                    changed();
                });
            } catch (Exception | OutOfMemoryError e) {
                main.post(() -> {
                    if (closed) return;
                    busy = false;
                    status = "Couldn’t clean these images. Try a smaller selection or different files.";
                    changed();
                });
            }
        });
    }

    Result export(List<Uri> photos, int quality, boolean zip) throws IOException {
        File folder = PrivateCache.newFolder(context, "exports");
        ArrayList<File> output = new ArrayList<>();
        int skipped = 0, resized = 0;
        boolean complete = false;
        try {
            for (int i = 0; i < photos.size(); i++) {
                if (Thread.currentThread().isInterrupted()) throw new IOException("Export cancelled");
                Bitmap bitmap = null;
                File file = new File(folder, String.format(Locale.US, "ShareSafe-photo-%03d.jpg", i + 1));
                try {
                    ImageProcessor.Decoded decoded = ImageProcessor.decode(context, photos.get(i));
                    bitmap = decoded.bitmap;
                    try (FileOutputStream stream = new FileOutputStream(file)) {
                        if (!bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)) throw new IOException("JPEG export failed");
                    }
                    output.add(file);
                    if (decoded.resized) resized++;
                } catch (IOException | SecurityException | IllegalArgumentException e) {
                    file.delete();
                    skipped++;
                } finally {
                    if (bitmap != null) bitmap.recycle();
                }
                int percent = Math.round((i + 1) * 100f / photos.size());
                main.post(() -> { if (!closed) { progress = percent; changed(); } });
            }
            if (output.isEmpty()) throw new IOException("No compatible images could be exported");
            File zipFile = zip ? zip(folder, output) : null;
            complete = true;
            return new Result(output, zipFile, skipped, resized);
        } finally {
            if (!complete) PrivateCache.delete(folder);
        }
    }

    private File zip(File folder, List<File> photos) throws IOException {
        File zipFile = new File(folder, "ShareSafe-clean-photos.zip");
        byte[] buffer = new byte[8192];
        try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(zipFile))) {
            for (File photo : photos) {
                zip.putNextEntry(new ZipEntry(photo.getName()));
                try (InputStream input = new FileInputStream(photo)) {
                    int count;
                    while ((count = input.read(buffer)) != -1) zip.write(buffer, 0, count);
                }
                zip.closeEntry();
            }
        }
        return zipFile;
    }

    void saveToGallery() {
        if (busy || result == null) return;
        List<File> snapshot = new ArrayList<>(result.photos);
        busy = true;
        status = "Saving clean photos to gallery…";
        changed();
        worker.execute(() -> {
            int saved = 0;
            try {
                for (File photo : snapshot) { savePhoto(photo); saved++; }
                int count = saved;
                main.post(() -> finishSave("Saved " + count + " photo(s) to Pictures/ShareSafe."));
            } catch (Exception e) {
                int count = saved;
                main.post(() -> finishSave("Saved " + count + " of " + snapshot.size() + " photos. Please check available storage and try again."));
            }
        });
    }

    private void finishSave(String message) {
        if (closed) return;
        busy = false;
        status = message;
        changed();
    }

    Uri savePhoto(File photo) throws IOException {
        ContentValues values = new ContentValues();
        String name = "ShareSafe-" + UUID.randomUUID() + ".jpg";
        values.put(MediaStore.Images.Media.DISPLAY_NAME, name);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        File legacyFile = null;
        if (Build.VERSION.SDK_INT >= 29) {
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ShareSafe");
            values.put(MediaStore.Images.Media.IS_PENDING, 1);
        } else {
            File folder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "ShareSafe");
            if (!folder.exists() && !folder.mkdirs()) throw new IOException("Could not create gallery folder");
            legacyFile = new File(folder, name);
            values.put(MediaStore.Images.Media.DATA, legacyFile.getAbsolutePath());
        }
        Uri uri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (uri == null) throw new IOException("Could not create gallery image");
        boolean complete = false;
        try {
            try (InputStream input = new FileInputStream(photo);
                 OutputStream output = context.getContentResolver().openOutputStream(uri)) {
                if (output == null) throw new IOException("Could not open gallery image");
                byte[] buffer = new byte[8192];
                int count;
                while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            }
            // A close failure must not publish a partially written gallery file.
            if (Build.VERSION.SDK_INT >= 29) {
                ContentValues publish = new ContentValues();
                publish.put(MediaStore.Images.Media.IS_PENDING, 0);
                if (context.getContentResolver().update(uri, publish, null, null) != 1) throw new IOException("Could not publish gallery image");
            }
            complete = true;
            return uri;
        } finally {
            if (!complete) {
                context.getContentResolver().delete(uri, null, null);
                if (legacyFile != null) legacyFile.delete();
            }
        }
    }

    void close() {
        closed = true;
        listener = null;
        worker.shutdownNow();
    }

    static final class Result {
        final ArrayList<File> photos;
        final File zipFile;
        final int skipped, resized;
        Result(ArrayList<File> photos, File zipFile, int skipped, int resized) {
            this.photos = photos; this.zipFile = zipFile; this.skipped = skipped; this.resized = resized;
        }
        String summary() {
            return "Cleaned " + photos.size() + " photo(s)\nEmbedded metadata removed; original filenames replaced"
                    + "\nExport: " + (zipFile == null ? "individual JPEG photos" : "one ZIP file")
                    + "\nResized for safe memory use: " + resized + "\nSkipped / failed images: " + skipped
                    + "\nReview visible details before sharing.";
        }
    }
}
