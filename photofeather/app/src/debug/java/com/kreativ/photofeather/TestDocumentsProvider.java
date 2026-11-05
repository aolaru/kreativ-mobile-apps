package com.kreativ.photofeather;

import android.database.*;
import android.os.*;
import android.provider.DocumentsContract.Document;
import android.provider.DocumentsContract.Root;
import android.provider.DocumentsProvider;
import java.io.*;
import java.util.*;

/** Debug-only local SAF provider for deterministic storage/provider failure tests. */
public class TestDocumentsProvider extends DocumentsProvider {
  static volatile boolean rejectWrites;

  File root() {
    File dir = new File(getContext().getCacheDir(), "qa-documents");
    dir.mkdirs();
    return dir;
  }

  File file(String id) throws FileNotFoundException {
    if (id.equals("root")) return root();
    if (!id.matches("[a-zA-Z0-9_.-]+")) throw new FileNotFoundException("Invalid test document");
    return new File(root(), id);
  }

  public boolean onCreate() {
    return true;
  }

  public Cursor queryRoots(String[] projection) {
    // Not a user-facing destination. Tests address a known private root directly.
    return new MatrixCursor(
        projection != null
            ? projection
            : new String[] {
              Root.COLUMN_ROOT_ID, Root.COLUMN_DOCUMENT_ID, Root.COLUMN_TITLE, Root.COLUMN_FLAGS,
              Root.COLUMN_MIME_TYPES, Root.COLUMN_AVAILABLE_BYTES, Root.COLUMN_ICON,
                  Root.COLUMN_SUMMARY
            });
  }

  void add(MatrixCursor c, String id) throws FileNotFoundException {
    File f = file(id);
    Map<String, Object> values = new HashMap<>();
    values.put(Document.COLUMN_DOCUMENT_ID, id);
    values.put(Document.COLUMN_DISPLAY_NAME, f.getName());
    values.put(
        Document.COLUMN_MIME_TYPE, id.equals("root") ? Document.MIME_TYPE_DIR : "image/jpeg");
    values.put(
        Document.COLUMN_FLAGS,
        id.equals("root")
            ? Document.FLAG_DIR_SUPPORTS_CREATE
            : Document.FLAG_SUPPORTS_WRITE | Document.FLAG_SUPPORTS_DELETE);
    values.put(Document.COLUMN_SIZE, f.length());
    MatrixCursor.RowBuilder row = c.newRow();
    for (String name : c.getColumnNames()) row.add(name, values.get(name));
  }

  MatrixCursor cursor(String[] p) {
    return new MatrixCursor(
        p != null
            ? p
            : new String[] {
              Document.COLUMN_DOCUMENT_ID,
              Document.COLUMN_DISPLAY_NAME,
              Document.COLUMN_MIME_TYPE,
              Document.COLUMN_FLAGS,
              Document.COLUMN_SIZE
            });
  }

  public Cursor queryDocument(String id, String[] p) throws FileNotFoundException {
    File f = file(id);
    if (!f.exists()) throw new FileNotFoundException();
    MatrixCursor c = cursor(p);
    add(c, id);
    return c;
  }

  public Cursor queryChildDocuments(String parent, String[] p, String sort)
      throws FileNotFoundException {
    MatrixCursor c = cursor(p);
    File[] list = file(parent).listFiles();
    if (list != null) for (File f : list) add(c, f.getName());
    return c;
  }

  public String createDocument(String parent, String mime, String name)
      throws FileNotFoundException {
    if (!parent.equals("root")) throw new FileNotFoundException();
    String id = UUID.randomUUID() + ".jpg";
    try {
      if (!file(id).createNewFile()) throw new IOException();
    } catch (IOException e) {
      throw new FileNotFoundException();
    }
    return id;
  }

  public ParcelFileDescriptor openDocument(String id, String mode, CancellationSignal signal)
      throws FileNotFoundException {
    if (rejectWrites && mode.contains("w"))
      throw new FileNotFoundException("Injected provider write refusal");
    return ParcelFileDescriptor.open(file(id), ParcelFileDescriptor.parseMode(mode));
  }

  public void deleteDocument(String id) throws FileNotFoundException {
    if (id.equals("root") || !file(id).delete()) throw new FileNotFoundException();
  }

  public boolean isChildDocument(String parent, String child) {
    return parent.equals("root") && !child.equals("root");
  }
}
