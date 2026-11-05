package com.kreativ.photofeather;

import static org.junit.Assert.*;

import android.content.*;
import android.content.pm.*;
import android.graphics.*;
import android.net.Uri;
import androidx.core.content.FileProvider;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import com.kreativ.imageengine.*;
import java.io.*;
import java.util.*;
import org.junit.*;

/** Runs against non-debuggable release code signed with the QA certificate in .releasecheck. */
public class ReleaseSmokeTest {
  Context context;
  Session session;
  Uri saved;

  @Before
  public void setup() throws Exception {
    context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    session = ((FeatherApp) context.getApplicationContext()).session;
    idle();
    session.clear();
    idle();
    androidx.test.uiautomator.UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        .wakeUp();
  }

  @After
  public void cleanup() throws Exception {
    idle();
    if (saved != null) ExportManager.rollback(context, saved);
  }

  void idle() throws Exception {
    long end = System.currentTimeMillis() + 30000;
    while (session.busy && System.currentTimeMillis() < end) Thread.sleep(30);
    assertFalse(session.message, session.busy);
    InstrumentationRegistry.getInstrumentation().waitForIdleSync();
  }

  @Test
  public void releaseBuildIsNotDebuggableAndHasNoTestActivitiesOrSensitivePermissions()
      throws Exception {
    PackageInfo info =
        context
            .getPackageManager()
            .getPackageInfo(
                context.getPackageName(),
                PackageManager.GET_PERMISSIONS
                    | PackageManager.GET_ACTIVITIES
                    | PackageManager.GET_PROVIDERS);
    assertEquals("1.0.0", info.versionName);
    assertEquals(0, info.applicationInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE);
    for (ActivityInfo a : info.activities)
      assertFalse(
          a.name.contains("DarkActivity")
              || a.name.contains("LargeText")
              || a.name.contains("LargePreview"));
    if (info.requestedPermissions != null)
      for (String p : info.requestedPermissions)
        assertFalse(p, p.startsWith("android.permission."));
    for (ProviderInfo p : info.providers) assertFalse(p.name.contains("TestDocuments"));
  }

  @Test
  public void releaseCodeImportsMeasuresSelectsSavesAndBuildsShareIntent() throws Exception {
    File fixture = new File(session.exports, "release-smoke.jpg");
    Bitmap b = Bitmap.createBitmap(1000, 750, Bitmap.Config.ARGB_8888);
    int[] pixels = new int[750000];
    Random random = new Random(25);
    for (int i = 0; i < pixels.length; i++) pixels[i] = 0xff000000 | random.nextInt(0x1000000);
    b.setPixels(pixels, 0, 1000, 0, 0, 1000, 750);
    try (FileOutputStream out = new FileOutputStream(fixture)) {
      b.compress(Bitmap.CompressFormat.JPEG, 100, out);
    }
    b.recycle();
    session.add(
        Collections.singletonList(
            FileProvider.getUriForFile(context, context.getPackageName() + ".files", fixture)));
    idle();
    session.process(new CompressionOptions(200000, 1200, 85, Color.WHITE, true));
    idle();
    PhotoItem item = session.items.get(0);
    assertTrue(item.exportable());
    assertTrue(item.after <= 200000);
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      scenario.onActivity(
          a -> {
            assertTrue(a.viewingResults);
            assertNotNull(a.buildShareIntent());
          });
      scenario.recreate();
      scenario.onActivity(a -> assertTrue(a.viewingResults));
      if (android.os.Build.VERSION.SDK_INT >= 29) {
        session.save(null, session.selectedIds());
        idle();
      } else {
        androidx.test.uiautomator.UiDevice device =
            androidx.test.uiautomator.UiDevice.getInstance(
                InstrumentationRegistry.getInstrumentation());
        device.executeShellCommand("mkdir -p /sdcard/Pictures/PhotoFeatherQA");
        scenario.onActivity(a -> a.savePreparedCopies());
        androidx.test.uiautomator.UiObject2 folder =
            device.wait(
                androidx.test.uiautomator.Until.findObject(
                    androidx.test.uiautomator.By.text("PhotoFeatherQA")),
                5000);
        assertNotNull(folder);
        folder.click();
        androidx.test.uiautomator.UiObject2 select =
            device.wait(
                androidx.test.uiautomator.Until.findObject(
                    androidx.test.uiautomator.By.res(
                        "com.android.documentsui:id/action_menu_select")),
                3000);
        if (select == null)
          select =
              device.wait(
                  androidx.test.uiautomator.Until.findObject(
                      androidx.test.uiautomator.By.text(
                          java.util.regex.Pattern.compile(
                              "SELECT|USE THIS FOLDER", java.util.regex.Pattern.CASE_INSENSITIVE))),
                  2000);
        assertNotNull(select);
        select.click();
        androidx.test.uiautomator.UiObject2 allow =
            device.wait(
                androidx.test.uiautomator.Until.findObject(
                    androidx.test.uiautomator.By.text(
                        java.util.regex.Pattern.compile(
                            "ALLOW", java.util.regex.Pattern.CASE_INSENSITIVE))),
                1000);
        if (allow != null) allow.click();
        long end = System.currentTimeMillis() + 10000;
        while (item.savedUri.isEmpty() && System.currentTimeMillis() < end) Thread.sleep(50);
        idle();
      }
      saved = Uri.parse(item.savedUri);
      assertFalse(item.savedUri.isEmpty());
      File reopened = new File(context.getCacheDir(), "release-reopened.jpg");
      try (InputStream in = context.getContentResolver().openInputStream(saved);
          OutputStream out = new FileOutputStream(reopened)) {
        byte[] buffer = new byte[65536];
        int n;
        while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
      }
      assertEquals(item.after, reopened.length());
      ImageEngine.verifyOutput(reopened, item.w, item.h);
      assertEquals(
          ExportManager.fingerprint(new File(item.output), () -> {}),
          ExportManager.fingerprint(reopened, () -> {}));
      reopened.delete();
      assertTrue(new File(item.source).exists());
    }
  }
}
