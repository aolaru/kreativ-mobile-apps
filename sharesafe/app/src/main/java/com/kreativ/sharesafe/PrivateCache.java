package com.kreativ.sharesafe;

import android.content.Context;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

final class PrivateCache {
    static final long RETENTION_MS = 24 * 60 * 60 * 1000L;
    private PrivateCache() { }

    static File newFolder(Context context, String category) throws IOException {
        File folder = new File(new File(context.getCacheDir(), category), UUID.randomUUID().toString());
        if (!folder.mkdirs()) throw new IOException("Could not create private export folder");
        return folder;
    }

    // Called before starting new work; never traverses outside the app's own categories.
    static void cleanExpired(Context context) {
        long cutoff = System.currentTimeMillis() - RETENTION_MS;
        for (String category : new String[]{"exports", "redactions"}) {
            File[] files = new File(context.getCacheDir(), category).listFiles();
            if (files == null) continue;
            for (File file : files) if (file.lastModified() < cutoff) delete(file);
        }
    }

    static void delete(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) for (File child : children) delete(child);
        }
        file.delete();
    }
}
