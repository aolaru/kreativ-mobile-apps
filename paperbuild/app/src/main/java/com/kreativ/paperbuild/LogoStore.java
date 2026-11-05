package com.kreativ.paperbuild;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/** Immutable private PNGs: changing a logo never changes an existing preset. */
final class LogoStore {
    private final Context context;
    LogoStore(Context context) { this.context = context.getApplicationContext(); }
    File file(String id) throws IOException {
        if (id == null || !id.matches("[a-f0-9-]{36}\\.png")) throw new IOException("Choose a logo image again.");
        return new File(context.getFilesDir(), "paperbuild/logos/" + id);
    }
    String importLogo(Uri uri) throws IOException {
        File folder = new File(context.getFilesDir(), "paperbuild/logos");
        if (!folder.isDirectory() && !folder.mkdirs()) throw new IOException("Couldn’t save the logo. Free some space and try again.");
        File input = File.createTempFile("logo-", ".input", folder);
        File output = new File(folder, UUID.randomUUID() + ".png");
        Bitmap bitmap = null;
        boolean complete = false;
        try {
            try (InputStream in = context.getContentResolver().openInputStream(uri); FileOutputStream out = new FileOutputStream(input)) {
                if (in == null) throw new IOException("This image is unavailable. Choose it again.");
                byte[] buffer = new byte[16384]; int count, total = 0;
                while ((count = in.read(buffer)) != -1) {
                    total += count;
                    if (total > 20 * 1024 * 1024) throw new IOException("Choose a logo smaller than 20 MB.");
                    out.write(buffer, 0, count);
                }
            }
            BitmapFactory.Options bounds = new BitmapFactory.Options(); bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(input.getPath(), bounds);
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw new IOException("Choose a PNG, JPEG or WebP logo.");
            BitmapFactory.Options sampled = new BitmapFactory.Options(); sampled.inSampleSize = 1;
            while (Math.max(bounds.outWidth, bounds.outHeight) / sampled.inSampleSize > 2048) sampled.inSampleSize *= 2;
            bitmap = BitmapFactory.decodeFile(input.getPath(), sampled);
            if (bitmap == null) throw new IOException("This logo cannot be read. Choose another image.");
            Matrix orientation = new DocumentProcessor(context).orientation(Uri.fromFile(input));
            if (!orientation.isIdentity()) {
                Bitmap oriented = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), orientation, true);
                if (oriented != bitmap) bitmap.recycle(); bitmap = oriented;
            }
            int longest = Math.max(bitmap.getWidth(), bitmap.getHeight());
            if (longest > 1024) {
                float scale = 1024f / longest;
                Bitmap smaller = Bitmap.createScaledBitmap(bitmap, Math.max(1, Math.round(bitmap.getWidth() * scale)),
                        Math.max(1, Math.round(bitmap.getHeight() * scale)), true);
                if (smaller != bitmap) bitmap.recycle(); bitmap = smaller;
            }
            try (FileOutputStream out = new FileOutputStream(output)) {
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) throw new IOException("Couldn’t save this logo.");
                out.getFD().sync();
            }
            complete = true; return output.getName();
        } finally { if (bitmap != null) bitmap.recycle(); input.delete(); if (!complete) output.delete(); }
    }
}
