package com.kreativ.photofeather;

import static org.junit.Assert.*;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import androidx.test.platform.app.InstrumentationRegistry;
import com.kreativ.imageengine.CompressionOptions;
import com.kreativ.imageengine.ImageEngine;
import java.io.*;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.zip.*;
import org.junit.Test;

public class ProFeaturesTest {
  @Test public void zipContainsVerifiedCopyAndCancelledArchiveIsRemoved() throws Exception {
    Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
    File dir = c.getCacheDir();
    File input = new File(dir, "pro-zip-input.jpg");
    File output = new File(dir, "pro-zip-output.jpg");
    File archive = new File(dir, "pro-test.zip");
    File cancelled = new File(dir, "pro-cancel.zip");
    try {
      Bitmap b = Bitmap.createBitmap(400, 300, Bitmap.Config.ARGB_8888);
      int[] pixels = new int[400 * 300];
      Random r = new Random(123);
      for (int i = 0; i < pixels.length; i++) pixels[i] = 0xff000000 | r.nextInt(0x1000000);
      b.setPixels(pixels, 0, 400, 0, 0, 400, 300);
      try (FileOutputStream stream = new FileOutputStream(input)) {
        assertTrue(b.compress(Bitmap.CompressFormat.JPEG, 100, stream));
      }
      b.recycle();
      ImageEngine.Result result = new ImageEngine().process(input, output,
          new CompressionOptions(0, 400, 70, Color.WHITE, true), () -> {});
      PhotoItem item = new PhotoItem("test", "Photo");
      item.output = result.file.getPath(); item.after = result.bytes;
      item.w = result.width; item.h = result.height; item.status = "Ready";
      PhotoItem second = new PhotoItem("test2", "Second photo");
      second.output = item.output; second.after = item.after; second.w = item.w;
      second.h = item.h; second.status = "Ready";
      List<PhotoItem> photos = Arrays.asList(item, second);
      List<String> names = Arrays.asList("export_0001_test.jpg", "export_0002_test2.jpg");
      ZipExporter.write(archive, photos, names, () -> {});
      try (ZipFile zip = new ZipFile(archive)) {
        assertEquals(2, zip.size());
        for (String name : names) {
          ZipEntry entry = zip.getEntry(name);
          assertNotNull(entry);
          try (InputStream in = zip.getInputStream(entry); InputStream original = new FileInputStream(output)) {
            int a, x; while ((a = in.read()) != -1) { x = original.read(); assertEquals(x, a); }
            assertEquals(-1, original.read());
          }
        }
      }
      if (android.os.Build.VERSION.SDK_INT >= 29) {
        String exportName = "PhotoFeather_pro_test_" + System.nanoTime() + ".jpg";
        Uri saved = null;
        try {
          saved = ExportManager.saveNamed(c, item, null, () -> {}, uri -> {}, exportName);
          try (android.database.Cursor cursor = c.getContentResolver().query(saved,
              new String[] {android.provider.OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            assertNotNull(cursor); assertTrue(cursor.moveToFirst()); assertEquals(exportName, cursor.getString(0));
          }
          try (InputStream in = c.getContentResolver().openInputStream(saved);
              InputStream original = new FileInputStream(output)) {
            assertNotNull(in);
            int value; while ((value = in.read()) != -1) assertEquals(value, original.read());
            assertEquals(-1, original.read());
          }
        } finally { if (saved != null) assertTrue(ExportManager.rollback(c, saved)); }
      }
      final int[] checks = {0};
      try {
        ZipExporter.write(cancelled, photos, names, () -> {
          if (++checks[0] >= 5) throw new CancellationException();
        });
        fail("Cancellation must stop ZIP creation");
      } catch (CancellationException expected) {
        assertTrue(checks[0] >= 5); assertFalse(cancelled.exists());
      }
      try {
        ZipExporter.write(cancelled, Collections.singletonList(item),
            Collections.singletonList("copy.jpg"), () -> {}, () -> 0);
        fail("Low storage must stop ZIP creation");
      } catch (IOException expected) { assertFalse(cancelled.exists()); }
      assertTrue(input.exists());
    } finally { input.delete(); output.delete(); archive.delete(); cancelled.delete(); }
  }

  @Test public void versionsKeepOriginalAndSharedImportWhenOriginalResultIsRemoved() throws Exception {
    Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
    Session session = ((FeatherApp)c.getApplicationContext()).session;
    waitIdle(session); session.clear(); waitIdle(session);
    File source = new File(session.imports, "pro-versions-source.jpg");
    File output = new File(session.exports, "pro-versions-first.jpg");
    try {
      Bitmap bitmap = Bitmap.createBitmap(2000, 1500, Bitmap.Config.ARGB_8888);
      int[] pixels = new int[2000 * 1500]; Random random = new Random(99);
      for (int i = 0; i < pixels.length; i++) pixels[i] = 0xff000000 | random.nextInt(0x1000000);
      bitmap.setPixels(pixels, 0, 2000, 0, 0, 2000, 1500);
      try (FileOutputStream stream = new FileOutputStream(source)) { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream); }
      bitmap.recycle();
      String originalFingerprint = ExportManager.fingerprint(source, () -> {});
      ImageEngine.Result result = new ImageEngine().process(source, output,
          new CompressionOptions(0, 2000, 70, Color.WHITE, true), () -> {});
      PhotoItem original = new PhotoItem("original-version-test", "Original");
      original.source = source.getPath(); original.before = source.length(); original.ow = 2000; original.oh = 1500;
      original.output = output.getPath(); original.after = result.bytes; original.w = result.width; original.h = result.height;
      original.status = "Ready"; session.items.add(original);
      session.createVersions(Collections.singleton(original.id), 0, 70, Color.WHITE);
      long end = System.currentTimeMillis() + 60000;
      while (System.currentTimeMillis() < end) {
        boolean finished = session.items.size() == 4 && !session.busy;
        if (finished) for (PhotoItem p : session.items) if (p.status.equals("Selected") || p.status.equals("Processing") || p.status.equals("Waiting")) finished = false;
        if (finished) break;
        Thread.sleep(50);
      }
      assertEquals(4, session.items.size());
      for (PhotoItem p : session.items) {
        assertTrue("Version did not finish: " + p.status + " " + p.error, p.exportable());
        int expectedWidth = p.name.endsWith("1280 px") ? 1280 : p.name.endsWith("1920 px") ? 1920 : 2000;
        assertEquals(expectedWidth, p.w); assertEquals(expectedWidth * 3 / 4, p.h);
        if (p.versionEdge != 0) {
          assertTrue(session.exportName(p).contains("_" + p.versionEdge + "px.jpg"));
          assertEquals(p.versionEdge, PhotoItem.read(p.json()).versionEdge);
        }
      }
      assertTrue(source.isFile()); assertTrue(output.isFile());
      assertEquals("Original import bytes changed", originalFingerprint, ExportManager.fingerprint(source, () -> {}));
      session.createVersions(Collections.singleton(original.id), 0, 70, Color.WHITE);
      session.cancel();
      long cancelBy = System.currentTimeMillis() + 30000;
      while (System.currentTimeMillis() < cancelBy) {
        boolean settled = !session.busy;
        for (PhotoItem p : session.items)
          if (p.status.equals("Waiting") || p.status.equals("Processing")) settled = false;
        if (settled) break;
        Thread.sleep(50);
      }
      for (PhotoItem p : session.items)
        assertFalse("Cancelled version kept running", p.status.equals("Waiting") || p.status.equals("Processing"));
      assertTrue("Existing preview must remain usable", original.exportable());
      int remaining = session.items.size() - 1;
      session.remove(original.id); waitIdle(session);
      assertTrue("Shared source is still needed by versions", source.isFile());
      assertEquals(remaining, session.items.size());
    } finally { waitIdle(session); session.clear(); waitIdle(session); }
  }

  private void waitIdle(Session session) throws Exception {
    long end = System.currentTimeMillis() + 30000;
    while (session.busy && System.currentTimeMillis() < end) Thread.sleep(50);
    assertFalse("Session worker did not finish", session.busy);
  }

  @Test public void savedPresetCanBeAppliedAfterSettingsChange() throws Exception {
    Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
    c.getSharedPreferences("output-presets", Context.MODE_PRIVATE).edit().clear().commit();
    androidx.test.uiautomator.UiDevice device = androidx.test.uiautomator.UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    try (androidx.test.core.app.ActivityScenario<MainActivity> scenario = androidx.test.core.app.ActivityScenario.launch(MainActivity.class)) {
      long deadline = System.currentTimeMillis() + 10000;
      Session session = ((FeatherApp)c.getApplicationContext()).session;
      while (session.busy && System.currentTimeMillis() < deadline) Thread.sleep(50);
      assertFalse(session.busy);
      scenario.onActivity(a -> { a.edgeDraft = "1920"; if (a.edgeInput != null) a.edgeInput.setText("1920"); a.qualityDraft = "85"; a.session.target = 200000; a.session.resizeOnly = false; a.session.advanced = false; a.session.customResize = false; a.presets(); });
      androidx.test.uiautomator.UiObject2 save = device.wait(androidx.test.uiautomator.Until.findObject(androidx.test.uiautomator.By.text("Save current settings")), 5000);
      assertNotNull(save); save.click();
      androidx.test.uiautomator.UiObject2 input = device.wait(androidx.test.uiautomator.Until.findObject(androidx.test.uiautomator.By.clazz("android.widget.EditText")), 5000);
      assertNotNull(input); input.setText("Upload preset");
      androidx.test.uiautomator.UiObject2 confirm = device.wait(androidx.test.uiautomator.Until.findObject(androidx.test.uiautomator.By.text(java.util.regex.Pattern.compile("save", java.util.regex.Pattern.CASE_INSENSITIVE))), 5000);
      assertNotNull(confirm); confirm.click();
      InstrumentationRegistry.getInstrumentation().waitForIdleSync();
      assertTrue(c.getSharedPreferences("output-presets", Context.MODE_PRIVATE).contains("Upload preset"));
      assertEquals("Saved edge", 1920, new org.json.JSONObject(c.getSharedPreferences("output-presets", Context.MODE_PRIVATE).getString("Upload preset", "{}")).getInt("edge"));
      assertEquals("Saved target", 200000, new org.json.JSONObject(c.getSharedPreferences("output-presets", Context.MODE_PRIVATE).getString("Upload preset", "{}")).getLong("target"));
      scenario.onActivity(a -> { a.edgeDraft = "2560"; a.session.target = 500000; a.presets(); });
      androidx.test.uiautomator.UiObject2 name = device.wait(androidx.test.uiautomator.Until.findObject(androidx.test.uiautomator.By.text("Upload preset")), 5000);
      assertNotNull(name); name.click();
      androidx.test.uiautomator.UiObject2 apply = device.wait(androidx.test.uiautomator.Until.findObject(androidx.test.uiautomator.By.text("Apply preset")), 5000);
      assertNotNull(apply); apply.click();
      InstrumentationRegistry.getInstrumentation().waitForIdleSync();
      long appliedBy = System.currentTimeMillis() + 5000;
      while ((session.edge != 1920 || session.target != 200000) && System.currentTimeMillis() < appliedBy) Thread.sleep(25);
      assertEquals(1920, session.edge); assertEquals(200000, session.target); assertFalse(session.customResize);
    } finally { c.getSharedPreferences("output-presets", Context.MODE_PRIVATE).edit().clear().commit(); }
  }

  @Test public void namingIsStableAndDoesNotUseOriginalName() throws Exception {
    Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
    Session session = ((FeatherApp)c.getApplicationContext()).session;
    waitIdle(session);
    String oldPrefix = session.namingPrefix; int oldStart = session.namingStart, oldStyle = session.namingStyle;
    PhotoItem item = new PhotoItem("1234", "private-filename.jpg");
    PhotoItem skipped = new PhotoItem("skip", "Not selected");
    File file = new File(session.exports, "pro-share-name-test.jpg");
    session.items.add(0, item); session.items.add(0, skipped);
    try {
      try (FileOutputStream out = new FileOutputStream(file)) { out.write(1); }
      item.output = file.getPath(); item.status = "Ready";
      session.namingPrefix = "upload"; session.namingStart = 12; session.namingStyle = 0;
      String name = session.exportName(item);
      assertEquals("upload_0012_1234.jpg", name);
      assertFalse(name.contains("private-filename"));
      assertEquals(name, session.exportName(item));
      Uri uri = androidx.core.content.FileProvider.getUriForFile(c, c.getPackageName() + ".files", file, name);
      try (android.database.Cursor cursor = c.getContentResolver().query(uri,
          new String[] {android.provider.OpenableColumns.DISPLAY_NAME}, null, null, null)) {
        assertNotNull(cursor); assertTrue(cursor.moveToFirst()); assertEquals(name, cursor.getString(0));
      }
      session.namingStyle = 1; assertEquals("0012_upload_1234.jpg", session.exportName(item));
    } finally {
      file.delete(); session.items.remove(item); session.items.remove(skipped);
      session.namingPrefix = oldPrefix; session.namingStart = oldStart; session.namingStyle = oldStyle;
    }
  }
}
