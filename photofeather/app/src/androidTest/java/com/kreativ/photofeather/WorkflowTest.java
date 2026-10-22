package com.kreativ.photofeather;

import static org.junit.Assert.*;

import android.app.*;
import android.content.*;
import android.content.pm.PackageInfo;
import android.graphics.*;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.core.content.FileProvider;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import com.kreativ.imageengine.*;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import org.junit.*;

public class WorkflowTest {
  Context context;
  Session session;
  final List<Uri> saved = new ArrayList<>();

  @Before
  public void setup() throws Exception {
    androidx.test.uiautomator.UiDevice device =
        androidx.test.uiautomator.UiDevice.getInstance(
            InstrumentationRegistry.getInstrumentation());
    device.wakeUp();
    device.executeShellCommand("wm dismiss-keyguard");
    context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    session = ((FeatherApp) context.getApplicationContext()).session;
    idle();
    session.clear();
    idle();
    session.target = 200_000;
    session.edge = 2560;
    session.quality = 85;
    session.resizeOnly = false;
    session.advanced = false;
  }

  @After
  public void cleanup() throws Exception {
    idle();
    for (Uri uri : saved) ExportManager.rollback(context, uri);
  }

  void idle() throws Exception {
    long end = System.currentTimeMillis() + 60000;
    while (session.busy && System.currentTimeMillis() < end) Thread.sleep(30);
    assertFalse("Worker timed out: " + session.message, session.busy);
    InstrumentationRegistry.getInstrumentation().waitForIdleSync();
  }

  File fixture(String name, int w, int h) throws Exception {
    File file = new File(session.exports, name);
    Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap);
    Paint paint = new Paint(3);
    paint.setShader(
        new LinearGradient(
            0,
            0,
            0,
            h,
            new int[] {0xff7CB8C2, 0xffDDE6C7, 0xff275D4F},
            null,
            Shader.TileMode.CLAMP));
    canvas.drawRect(0, 0, w, h, paint);
    paint.setShader(null);
    paint.setColor(0xffF4E5B8);
    canvas.drawCircle(w * .76f, h * .23f, w * .08f, paint);
    for (int i = 0; i < 3; i++) {
      Path path = new Path();
      path.moveTo(-w * .1f, h * (.8f + i * .1f));
      path.lineTo(w * .28f, h * (.27f + i * .15f));
      path.lineTo(w * .6f, h * (.66f + i * .1f));
      path.lineTo(w * .77f, h * (.38f + i * .18f));
      path.lineTo(w * 1.1f, h * (.85f + i * .1f));
      path.lineTo(w, h);
      path.lineTo(0, h);
      path.close();
      paint.setColor(new int[] {0xff56877A, 0xff346B5B, 0xff204D40}[i]);
      canvas.drawPath(path, paint);
    }
    Random r = new Random(75);
    paint.setStrokeWidth(1);
    for (int i = 0; i < w * h / 5; i++) {
      int x = r.nextInt(w), y = r.nextInt(h);
      paint.setColor(Color.argb(24, r.nextInt(256), r.nextInt(256), r.nextInt(256)));
      canvas.drawPoint(x, y, paint);
    }
    try (FileOutputStream out = new FileOutputStream(file)) {
      bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
    }
    bitmap.recycle();
    return file;
  }

  Uri uri(File file) {
    return FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".files", file);
  }

  void importOne() throws Exception {
    session.add(Collections.singletonList(uri(fixture("mountain-test.jpg", 2400, 1800))));
    idle();
    assertEquals(1, session.items.size());
  }

  void process() throws Exception {
    session.process(new CompressionOptions(200_000, 2560, 85, Color.WHITE, true));
    idle();
  }

  void click(Activity a, String text) {
    ArrayList<View> found = new ArrayList<>();
    a.getWindow().getDecorView().findViewsWithText(found, text, View.FIND_VIEWS_WITH_TEXT);
    for (View view : found)
      if (view instanceof Button && ((Button) view).getText().toString().equals(text)) {
        assertTrue(view.performClick());
        return;
      }
    fail("Button missing: " + text);
  }

  void screen(String name) throws Exception {
    File dir = new File(context.getExternalFilesDir(null), "screenshots");
    dir.mkdirs();
    File file = new File(dir, name + ".png");
    android.graphics.Bitmap capture =
        InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
    assertNotNull(capture);
    try (FileOutputStream out = new FileOutputStream(file)) {
      capture.compress(Bitmap.CompressFormat.PNG, 100, out);
    }
    capture.recycle();
  }

  byte[] readAll(InputStream in) throws IOException {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    byte[] buffer = new byte[65536];
    int n;
    while ((n = in.read(buffer)) != -1) bytes.write(buffer, 0, n);
    return bytes.toByteArray();
  }

  @Test
  public void batchFailuresKeepSuccessfulOutput() throws Exception {
    File good = fixture("valid.jpg", 1800, 1200), bad = new File(session.exports, "broken.jpg");
    Files.write(bad.toPath(), new byte[] {1, 2, 3});
    session.add(Arrays.asList(uri(good), uri(bad), Uri.parse("content://missing.photofeather/1")));
    idle();
    assertEquals(3, session.items.size());
    process();
    assertTrue(session.items.get(0).exportable());
    assertEquals("Unavailable", session.items.get(1).status);
    assertFalse(session.items.get(2).error.isEmpty());
    assertEquals(1, session.eligible());
  }

  @Test
  public void gallerySaveReopensExactBytesAndPartialFailuresKeepSuccess() throws Exception {
    importOne();
    process();
    PhotoItem good = session.items.get(0);
    PhotoItem bad = new PhotoItem(UUID.randomUUID().toString(), "Missing output");
    bad.output = new File(session.exports, "missing.jpg").getPath();
    bad.status = "Ready"; // Deletion before export must be reported per image.
    File missing = new File(bad.output);
    Files.write(missing.toPath(), new byte[] {1, 2, 3});
    bad.after = 3;
    session.items.add(bad);
    session.save(null);
    idle();
    assertFalse(good.savedUri.isEmpty());
    saved.add(Uri.parse(good.savedUri));
    assertFalse(bad.saveError.isEmpty());
    try (InputStream in = context.getContentResolver().openInputStream(saved.get(0))) {
      assertArrayEquals(Files.readAllBytes(new File(good.output).toPath()), readAll(in));
    }
    assertTrue(new File(good.source).isFile());
    assertTrue(new File(good.output).isFile());
  }

  @Test
  public void cancelledBatchKeepsCompletedResults() throws Exception {
    importOne();
    session.add(Collections.singletonList(uri(fixture("second.jpg", 2400, 1800))));
    idle();
    session.process(new CompressionOptions(1000, 2560, 85, Color.WHITE, true));
    long end = System.currentTimeMillis() + 30000;
    while (session.items.get(0).output.isEmpty()
        && session.busy
        && System.currentTimeMillis() < end) Thread.sleep(5);
    session.cancel();
    idle();
    assertTrue(session.items.get(0).hasOutput());
    assertFalse(session.items.get(0).status.equals("Processing"));
    assertTrue(session.message.contains("Cancelled"));
    for (PhotoItem p : session.items)
      assertFalse("Unprocessed item needs a cancellation result", p.status.equals("Selected"));
  }

  @Test
  public void interruptedJournalRetainsCompletedResults() throws Exception {
    importOne();
    process();
    PhotoItem first = session.items.get(0);
    PhotoItem interrupted = new PhotoItem(UUID.randomUUID().toString(), "Interrupted photo");
    interrupted.source = first.source;
    interrupted.status = "Processing";
    session.items.add(interrupted);
    session.busy = true;
    assertTrue(session.persist());
    session.busy = false;
    Session restored = new Session(context);
    long end = System.currentTimeMillis() + 10000;
    while (restored.busy && System.currentTimeMillis() < end) Thread.sleep(20);
    assertEquals(2, restored.items.size());
    assertTrue(restored.items.get(0).exportable());
    assertEquals("Interrupted", restored.items.get(1).status);
    restored.worker.shutdown();
  }

  @Test
  public void rotationKeepsSelectionsAndResults() throws Exception {
    importOne();
    process();
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      scenario.recreate();
      scenario.onActivity(
          a -> {
            assertEquals(1, a.session.eligible());
            assertTrue(a.session.items.get(0).hasOutput());
          });
    }
  }

  @Test
  public void changedSettingsCannotExportOldPreviews() throws Exception {
    importOne();
    process();
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      scenario.onActivity(
          a -> {
            a.session.target = 500_000;
            assertFalse(a.previewMatchesSettings());
          });
    }
  }

  @Test
  public void appRequestsNoNetworkOrStoragePermissions() throws Exception {
    PackageInfo info =
        context
            .getPackageManager()
            .getPackageInfo(
                context.getPackageName(), android.content.pm.PackageManager.GET_PERMISSIONS);
    if (info.requestedPermissions != null)
      for (String permission : info.requestedPermissions)
        assertFalse(permission, permission.startsWith("android.permission."));
  }

  @Test
  public void platformShareSheetOpensWithoutSending() throws Exception {
    importOne();
    process();
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      scenario.onActivity(MainActivity::share);
      androidx.test.uiautomator.UiDevice device =
          androidx.test.uiautomator.UiDevice.getInstance(
              InstrumentationRegistry.getInstrumentation());
      long end = System.currentTimeMillis() + 5000;
      String current;
      do {
        current = device.getCurrentPackageName();
        if (current != null && !context.getPackageName().equals(current)) break;
        Thread.sleep(50);
      } while (System.currentTimeMillis() < end);
      assertNotEquals(context.getPackageName(), current);
      assertTrue(
          "Unexpected share UI: " + current,
          current != null
              && (current.equals("android")
                  || current.contains("intentresolver")
                  || current.contains("resolver")));
      device.pressBack();
    }
  }

  @Test
  public void largerCopiesRequireExplicitOptIn() throws Exception {
    PhotoItem p = new PhotoItem("a", "small");
    File f = new File(session.exports, "tiny.jpg");
    Files.write(f.toPath(), new byte[] {1});
    p.output = f.getPath();
    p.status = "Larger copy";
    assertFalse(p.exportable());
    p.allowLarger = true;
    assertTrue(p.exportable());
    p.status = "Same size";
    p.allowLarger = false;
    assertFalse(p.exportable());
    p.allowLarger = true;
    assertTrue(p.exportable());
    p.status = "Target not reached";
    assertFalse(p.exportable());
  }

  @Test
  public void lowStorageExportKeepsPreparedCopy() throws Exception {
    importOne();
    process();
    PhotoItem p = session.items.get(0);
    final boolean[] created = {false};
    try {
      ExportManager.save(context, p, null, () -> {}, uri -> created[0] = true, directory -> 0);
      fail();
    } catch (IOException e) {
      assertTrue(e.getMessage().contains("Low storage"));
      assertFalse(created[0]);
      assertTrue(p.hasOutput());
    }
  }

  @Test
  public void cancellationRollsBackOnlyCurrentDestination() throws Exception {
    importOne();
    process();
    PhotoItem p = session.items.get(0);
    final Uri[] destination = {null};
    final int[] checks = {0};
    try {
      ExportManager.save(
          context,
          p,
          null,
          () -> {
            if (++checks[0] >= 3) throw new java.util.concurrent.CancellationException();
          },
          uri -> destination[0] = uri);
      fail();
    } catch (java.util.concurrent.CancellationException expected) {
      assertNotNull(destination[0]);
      try (InputStream ignored = context.getContentResolver().openInputStream(destination[0])) {
        fail("Cancelled destination still exists");
      } catch (FileNotFoundException good) {
      }
      assertTrue(p.hasOutput());
      assertTrue(new File(p.source).isFile());
    }
  }

  @Test
  public void completedPendingExportIsRecoveredAfterProcessRecreation() throws Exception {
    importOne();
    process();
    PhotoItem p = session.items.get(0);
    Uri destination = ExportManager.save(context, p, null, () -> {}, uri -> {});
    saved.add(destination);
    session.pending = destination.toString();
    session.persist();
    Session restored = new Session(context);
    long end = System.currentTimeMillis() + 10000;
    while (restored.busy && System.currentTimeMillis() < end) Thread.sleep(20);
    try (InputStream in = context.getContentResolver().openInputStream(destination)) {
      assertNotNull(in);
    }
    assertEquals(destination.toString(), restored.items.get(0).savedUri);
    assertTrue(restored.pending.isEmpty());
    session.pending = "";
    restored.worker.shutdown();
  }

  @Test
  public void imageShareIntentImportsWithoutStoragePermission() throws Exception {
    File f = fixture("shared-intent.jpg", 1200, 800);
    Intent intent =
        new Intent(context, MainActivity.class)
            .setAction(Intent.ACTION_SEND)
            .setType("image/jpeg")
            .putExtra(Intent.EXTRA_STREAM, uri(f))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(intent)) {
      long end = System.currentTimeMillis() + 10000;
      while (session.items.isEmpty() && System.currentTimeMillis() < end) Thread.sleep(30);
      idle();
      assertEquals(1, session.items.size());
      assertEquals("Selected", session.items.get(0).status);
    }
  }

  @Test
  public void nativeSystemPhotoPickerOpensAndCancels() throws Exception {
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      scenario.onActivity(MainActivity::pick);
      androidx.test.uiautomator.UiDevice device =
          androidx.test.uiautomator.UiDevice.getInstance(
              InstrumentationRegistry.getInstrumentation());
      long end = System.currentTimeMillis() + 5000;
      while ((context.getPackageName().equals(device.getCurrentPackageName())
              || device.getCurrentPackageName() == null)
          && System.currentTimeMillis() < end) Thread.sleep(50);
      String picker = device.getCurrentPackageName();
      assertNotEquals(context.getPackageName(), picker);
      assertTrue(
          "Unexpected picker: " + picker,
          picker.contains("providers.media")
              || picker.contains("documentsui")
              || picker.contains("photopicker"));
      device.pressBack();
      assertTrue(session.items.isEmpty());
    }
  }

  @Test
  public void largeTextAndDarkLayoutsAreUsable() throws Exception {
    try (ActivityScenario<DarkActivity> dark = ActivityScenario.launch(DarkActivity.class)) {
      Thread.sleep(250);
      screen("08-dark-home");
      dark.onActivity(a -> assertEquals(a.getColor(R.color.surface), 0xFF101C19));
    }
    importOne();
    process();
    try (ActivityScenario<LargeTextActivity> large =
        ActivityScenario.launch(LargeTextActivity.class)) {
      Thread.sleep(250);
      screen("09-large-text");
      large.onActivity(
          a -> {
            assertEquals(2f, a.getResources().getConfiguration().fontScale, .01f);
            ArrayList<View> found = new ArrayList<>();
            a.getWindow()
                .getDecorView()
                .findViewsWithText(found, "PhotoFeather", View.FIND_VIEWS_WITH_TEXT);
            for (View v : found)
              if (v instanceof TextView
                  && ((TextView) v).getText().toString().equals("PhotoFeather")) {
                TextView t = (TextView) v;
                assertTrue(
                    "Brand text clipped",
                    t.getLayout().getLineEnd(t.getLineCount() - 1) >= t.length());
              }
            found.clear();
            a.getWindow()
                .getDecorView()
                .findViewsWithText(found, "Preview copies", View.FIND_VIEWS_WITH_TEXT);
            assertFalse(found.isEmpty());
            found
                .get(0)
                .requestRectangleOnScreen(
                    new Rect(0, 0, found.get(0).getWidth(), found.get(0).getHeight()), true);
          });
      Thread.sleep(250);
      screen("10-large-text-settings");
    }
    Intent compare =
        new Intent(context, LargePreviewActivity.class)
            .putExtra("id", session.items.get(0).id)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    try (ActivityScenario<LargePreviewActivity> scenario = ActivityScenario.launch(compare)) {
      Thread.sleep(500);
      scenario.onActivity(
          a -> {
            a.setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
          });
      Thread.sleep(800);
      screen("11-landscape-large-comparison");
      scenario.onActivity(
          a -> {
            ArrayList<View> found = new ArrayList<>();
            a.getWindow()
                .getDecorView()
                .findViewsWithText(found, "Zoom in", View.FIND_VIEWS_WITH_TEXT);
            assertFalse(found.isEmpty());
            found
                .get(0)
                .requestRectangleOnScreen(
                    new Rect(0, 0, found.get(0).getWidth(), found.get(0).getHeight()), true);
          });
      Thread.sleep(250);
      screen("12-landscape-zoom-controls");
    }
  }

  @Test
  public void capturesRunningAppAndComparison() throws Exception {
    try (ActivityScenario<MainActivity> home = ActivityScenario.launch(MainActivity.class)) {
      Thread.sleep(300);
      screen("01-home");
    }
    importOne();
    process();
    session.persist();
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      Thread.sleep(300);
      screen("02-selected");
      scenario.onActivity(
          a -> {
            ArrayList<View> found = new ArrayList<>();
            a.getWindow()
                .getDecorView()
                .findViewsWithText(found, "Maximum file size", View.FIND_VIEWS_WITH_TEXT);
            if (!found.isEmpty())
              found
                  .get(0)
                  .requestRectangleOnScreen(
                      new Rect(0, 0, found.get(0).getWidth(), Ui.dp(a, 600)), true);
          });
      Thread.sleep(300);
      screen("03-settings");
      scenario.onActivity(
          a -> {
            ArrayList<View> found = new ArrayList<>();
            a.getWindow()
                .getDecorView()
                .findViewsWithText(found, "Your preview", View.FIND_VIEWS_WITH_TEXT);
            if (!found.isEmpty())
              found
                  .get(0)
                  .requestRectangleOnScreen(
                      new Rect(0, 0, found.get(0).getWidth(), Ui.dp(a, 600)), true);
          });
      Thread.sleep(350);
      screen("04-result");
      scenario.onActivity(a -> click(a, "Compare before & after"));
      Thread.sleep(800);
      screen("05-comparison-output");
      androidx.test.uiautomator.UiDevice device =
          androidx.test.uiautomator.UiDevice.getInstance(
              InstrumentationRegistry.getInstrumentation());
      device.findObject(androidx.test.uiautomator.By.text("Original")).click();
      Thread.sleep(250);
      screen("06-comparison-original");
      device.findObject(androidx.test.uiautomator.By.text("Zoom in")).click();
      Thread.sleep(250);
      screen("07-comparison-zoom");
      device.pressBack();
    }
  }
}
