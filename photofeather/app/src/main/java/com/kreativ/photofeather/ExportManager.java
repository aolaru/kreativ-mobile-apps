package com.kreativ.photofeather;

import android.content.*;
import android.net.Uri;
import android.os.Build;
import android.provider.*;
import com.kreativ.imageengine.*;
import java.io.*;
import java.security.*;

/** Only creates new rows/documents; rollback only ever deletes the newly created destination. */
final class ExportManager {
  interface Journal {
    void pending(Uri uri) throws IOException;
  }

  static Uri save(
      Context context, PhotoItem p, Uri tree, ImageEngine.Cancellation cancel, Journal journal)
      throws IOException {
    return save(context, p, tree, cancel, journal, File::getUsableSpace);
  }

  static Uri save(
      Context context,
      PhotoItem p,
      Uri tree,
      ImageEngine.Cancellation cancel,
      Journal journal,
      ImageEngine.DiskSpace space)
      throws IOException {
    File file = new File(p.output);
    ImageEngine.verifyOutput(file, p.w, p.h);
    if (file.length() != p.after)
      throw new IOException("The prepared copy changed. Preview it again.");
    if (space.available(context.getFilesDir()) < file.length() + 2_000_000)
      throw new IOException("Low storage. Free some space and try again.");
    ContentResolver resolver = context.getContentResolver();
    Uri destination = null;
    boolean gallery = Build.VERSION.SDK_INT >= 29 && tree == null;
    try {
      cancel.check();
      String name = "PhotoFeather_" + p.id + ".jpg";
      if (gallery) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, name);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PhotoFeather");
        values.put(MediaStore.Images.Media.IS_PENDING, 1);
        destination = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
      } else {
        if (tree == null) throw new IOException("Choose a folder for your copies.");
        Uri parent =
            DocumentsContract.buildDocumentUriUsingTree(
                tree, DocumentsContract.getTreeDocumentId(tree));
        destination = DocumentsContract.createDocument(resolver, parent, "image/jpeg", name);
      }
      if (destination == null) throw new IOException("Could not create a new photo copy.");
      journal.pending(destination);
      try (InputStream in = new FileInputStream(file);
          OutputStream out = resolver.openOutputStream(destination, "w")) {
        if (out == null) throw new IOException("The destination is not writable.");
        byte[] buf = new byte[65536];
        int n;
        while ((n = in.read(buf)) != -1) {
          cancel.check();
          out.write(buf, 0, n);
        }
        out.flush();
      }
      cancel.check();
      // Byte-for-byte verification also proves that destination metadata matches the checked JPEG.
      if (!MessageDigest.isEqual(
          digest(new FileInputStream(file), cancel),
          digest(resolver.openInputStream(destination), cancel)))
        throw new IOException("Saved copy verification failed.");
      if (gallery) {
        ContentValues ready = new ContentValues();
        ready.put(MediaStore.Images.Media.IS_PENDING, 0);
        if (resolver.update(destination, ready, null, null) != 1)
          throw new IOException("Could not finish saving the photo.");
      }
      return destination;
    } catch (IOException | RuntimeException e) {
      if (destination != null) rollback(context, destination);
      throw e;
    }
  }

  private static byte[] digest(InputStream input, ImageEngine.Cancellation cancel)
      throws IOException {
    if (input == null) throw new IOException("Saved copy cannot be reopened.");
    try (InputStream in = input) {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] buf = new byte[65536];
      int n;
      while ((n = in.read(buf)) != -1) {
        cancel.check();
        digest.update(buf, 0, n);
      }
      return digest.digest();
    } catch (NoSuchAlgorithmException e) {
      throw new IOException(e);
    }
  }

  enum Recovery {
    SAVED,
    REMOVED,
    RETAINED
  }

  static String fingerprint(File file, ImageEngine.Cancellation cancel) throws IOException {
    return hex(digest(new FileInputStream(file), cancel));
  }

  private static String hex(byte[] digest) {
    StringBuilder out = new StringBuilder();
    for (byte value : digest) out.append(String.format(java.util.Locale.US, "%02x", value & 255));
    return out.toString();
  }

  /**
   * A stale journal is not evidence that a published copy is incomplete. Verify against the
   * prepared JPEG's durable fingerprint before adopting or removing anything.
   */
  static Recovery recover(Context context, Uri uri, String expected) {
    ContentResolver resolver = context.getContentResolver();
    try {
      String actual = hex(digest(resolver.openInputStream(uri), () -> {}));
      if (!expected.isEmpty() && expected.equals(actual)) {
        if (isGallery(uri) && Build.VERSION.SDK_INT >= 29) {
          Integer pending = pendingFlag(context, uri);
          if (pending == null) return Recovery.RETAINED;
          if (pending == 1) {
            ContentValues ready = new ContentValues();
            ready.put(MediaStore.Images.Media.IS_PENDING, 0);
            if (resolver.update(uri, ready, null, null) != 1) return Recovery.RETAINED;
          }
        }
        return Recovery.SAVED;
      }
      // Never remove a published MediaStore copy, even if it changed or is unverifiable.
      if (isGallery(uri) && !Integer.valueOf(1).equals(pendingFlag(context, uri)))
        return Recovery.RETAINED;
      if (expected.isEmpty()) return Recovery.RETAINED;
      return rollback(context, uri) ? Recovery.REMOVED : Recovery.RETAINED;
    } catch (Exception e) {
      // A confirmed unpublished gallery placeholder was never reported as saved.
      if (isGallery(uri) && Integer.valueOf(1).equals(pendingFlag(context, uri)))
        return rollback(context, uri) ? Recovery.REMOVED : Recovery.RETAINED;
      // Unavailable access can be temporary. Never delete a published or unknown destination.
      return Boolean.FALSE.equals(exists(context, uri)) ? Recovery.REMOVED : Recovery.RETAINED;
    }
  }

  private static boolean isGallery(Uri uri) {
    return "media".equals(uri.getAuthority());
  }

  private static Integer pendingFlag(Context context, Uri uri) {
    if (Build.VERSION.SDK_INT < 29) return null;
    try (android.database.Cursor cursor =
        context
            .getContentResolver()
            .query(uri, new String[] {MediaStore.Images.Media.IS_PENDING}, null, null, null)) {
      if (cursor != null && cursor.moveToFirst()) return cursor.getInt(0);
    } catch (Exception ignored) {
    }
    return null;
  }

  private static Boolean exists(Context context, Uri uri) {
    try (android.database.Cursor cursor =
        context.getContentResolver().query(uri, null, null, null, null)) {
      return cursor == null ? null : cursor.getCount() > 0;
    } catch (Exception ignored) {
      return null;
    }
  }

  static boolean rollback(Context context, Uri uri) {
    try {
      if (DocumentsContract.isDocumentUri(context, uri))
        return DocumentsContract.deleteDocument(context.getContentResolver(), uri);
      return context.getContentResolver().delete(uri, null, null) > 0
          || Boolean.FALSE.equals(exists(context, uri));
    } catch (Exception ignored) {
      return false;
    }
  }
}
