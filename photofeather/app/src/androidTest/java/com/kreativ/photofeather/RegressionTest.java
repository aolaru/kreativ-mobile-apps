package com.kreativ.photofeather;

import static org.junit.Assert.*;

import android.content.*;
import android.graphics.*;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.AtomicFile;
import android.view.*;
import android.widget.*;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import com.kreativ.imageengine.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.*;

/**
 * Regression cases from the follow-up audit. Always run device tests with the isolated test suffix.
 */
public class RegressionTest {
  WorkflowTest helper;
  Session session;
  Context context;

  @Before
  public void setup() throws Exception {
    helper = new WorkflowTest();
    helper.setup();
    session = helper.session;
    context = helper.context;
  }

  @After
  public void clean() throws Exception {
    helper.cleanup();
  }

  void idle() throws Exception {
    helper.idle();
  }

  void prepare() throws Exception {
    helper.importOne();
    helper.process();
  }

  void restoredIdle(Session restored) throws Exception {
    long end = System.currentTimeMillis() + 10000;
    while (restored.busy && System.currentTimeMillis() < end) Thread.sleep(20);
    assertFalse(restored.busy);
  }

  ScrollView scroll(View view) {
    if (view instanceof ScrollView) return (ScrollView) view;
    if (view instanceof ViewGroup) {
      ViewGroup group = (ViewGroup) view;
      for (int i = 0; i < group.getChildCount(); i++) {
        ScrollView s = scroll(group.getChildAt(i));
        if (s != null) return s;
      }
    }
    return null;
  }

  @Test
  public void publishedCopySurvivesJournalCommitFailureAndRepeatedRecovery() throws Exception {
    prepare();
    class FailingAtomicFile extends AtomicFile {
      boolean armed;

      FailingAtomicFile() {
        super(session.state.getBaseFile());
      }

      @Override
      public FileOutputStream startWrite() throws IOException {
        if (armed && session.pending.isEmpty())
          throw new IOException("Injected storage failure at commit");
        return super.startWrite();
      }

      @Override
      public void finishWrite(FileOutputStream out) {
        super.finishWrite(out);
        if (!session.pending.isEmpty()) armed = true;
      }
    }
    FailingAtomicFile fault = new FailingAtomicFile();
    java.lang.reflect.Field field = Session.class.getDeclaredField("state");
    field.setAccessible(true);
    field.set(session, fault);
    Uri savedUri = null;
    try {
      session.save(helper.saveTree());
      idle();
      PhotoItem p = session.items.get(0);
      assertFalse(p.savedUri.isEmpty());
      savedUri = Uri.parse(p.savedUri);
      helper.saved.add(savedUri);
      assertTrue(fault.armed);
      assertFalse(session.pending.isEmpty());
      assertFalse(p.saveError.isEmpty());
      Session restored = new Session(context);
      restoredIdle(restored);
      assertEquals(savedUri.toString(), restored.items.get(0).savedUri);
      assertTrue(restored.pending.isEmpty());
      try (InputStream in = context.getContentResolver().openInputStream(savedUri)) {
        assertNotNull(in);
      }
      restored.worker.shutdownNow();
      Session twice = new Session(context);
      restoredIdle(twice);
      assertEquals(savedUri.toString(), twice.items.get(0).savedUri);
      assertFalse(twice.message.contains("interrupted"));
      twice.worker.shutdownNow();
    } finally {
      field.set(session, new AtomicFile(fault.getBaseFile()));
    }
  }

  @Test
  public void initialJournalWriteFailureNeverClaimsASavedCopy() throws Exception {
    prepare();
    AtomicFile failing =
        new AtomicFile(session.state.getBaseFile()) {
          @Override
          public FileOutputStream startWrite() throws IOException {
            throw new IOException("Injected full storage before destination write");
          }
        };
    java.lang.reflect.Field field = Session.class.getDeclaredField("state");
    field.setAccessible(true);
    field.set(session, failing);
    try {
      session.save(helper.saveTree());
      idle();
      assertTrue(session.items.get(0).savedUri.isEmpty());
      assertTrue(session.items.get(0).exportable());
      assertTrue(session.message.contains("No new copy was saved"));
      assertFalse(session.message.contains("verified saved copy"));
    } finally {
      field.set(session, new AtomicFile(failing.getBaseFile()));
      session.recoverPending();
    }
  }

  @Test
  @androidx.test.filters.SdkSuppress(minSdkVersion = 29)
  public void incompletePendingGalleryCopyIsRemoved() throws Exception {
    prepare();
    PhotoItem p = session.items.get(0);
    ContentValues values = new ContentValues();
    values.put(MediaStore.Images.Media.DISPLAY_NAME, "PhotoFeather_" + p.id + ".jpg");
    values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
    values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PhotoFeather");
    values.put(MediaStore.Images.Media.IS_PENDING, 1);
    Uri uri =
        context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    assertNotNull(uri);
    helper.saved.add(uri);
    session.pending = uri.toString();
    session.pendingItemId = p.id;
    session.pendingDigest = ExportManager.fingerprint(new File(p.output), () -> {});
    assertTrue(session.persist());
    Session restored = new Session(context);
    restoredIdle(restored);
    assertTrue(restored.pending.isEmpty());
    assertTrue(restored.items.get(0).savedUri.isEmpty());
    try (android.database.Cursor cursor =
        context.getContentResolver().query(uri, null, null, null, null)) {
      assertTrue(cursor == null || cursor.getCount() == 0);
    }
    restored.worker.shutdownNow();
    session.pending = "";
    session.pendingItemId = "";
    session.pendingDigest = "";
  }

  @Test
  public void truncatedJpegNeverProducesAnExportableResult() throws Exception {
    File original = helper.fixture("complete.jpg", 1200, 800);
    byte[] bytes = Files.readAllBytes(original.toPath());
    for (int cut : new int[] {bytes.length / 2, bytes.length - 2}) {
      File truncated = new File(session.exports, "truncated-" + cut + ".jpg");
      Files.write(truncated.toPath(), Arrays.copyOf(bytes, cut));
      session.add(Collections.singletonList(helper.uri(truncated)));
      idle();
      PhotoItem p = session.items.get(session.items.size() - 1);
      assertEquals("Unavailable", p.status);
      assertTrue(p.error.contains("damaged JPEG"));
      assertFalse(p.exportable());
    }
    assertEquals(0, session.eligible());
  }

  @Test
  public void cancelledRetryKeepsPreviousPreviewAndItsSettings() throws Exception {
    prepare();
    PhotoItem p = session.items.get(0);
    String output = p.output;
    long bytes = p.after;
    session.process(new CompressionOptions(1000, 2560, 85, Color.WHITE, true));
    session.cancel();
    idle();
    assertEquals(output, p.output);
    assertEquals(bytes, p.after);
    assertTrue(p.exportable());
    assertEquals(200000, p.requestTarget);
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      scenario.onActivity(
          a -> {
            assertFalse(a.previewMatchesSettings());
            a.share();
          });
      UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
      assertNotNull(
          device.wait(
              androidx.test.uiautomator.Until.findObject(
                  androidx.test.uiautomator.By.text(
                      java.util.regex.Pattern.compile(
                          "Export shown copies", java.util.regex.Pattern.CASE_INSENSITIVE))),
              3000));
      device.pressBack();
    }
  }

  @Test
  public void unreachableRetryKeepsPreviousSuccessfulPreview() throws Exception {
    prepare();
    PhotoItem p = session.items.get(0);
    String old = p.output;
    session.process(new CompressionOptions(1000, 2560, 85, Color.WHITE, true));
    idle();
    assertEquals(old, p.output);
    assertTrue(p.exportable());
    assertEquals(200000, p.requestTarget);
    assertTrue(p.error.contains("previous preview"));
    assertTrue(p.error.contains("measured"));
  }

  @Test
  public void failedRetryKeepsSavedStatusAndPreviousPreview() throws Exception {
    prepare();
    session.save(helper.saveTree());
    idle();
    PhotoItem p = session.items.get(0);
    helper.saved.add(Uri.parse(p.savedUri));
    String output = p.output, saved = p.savedUri;
    Files.write(new File(p.source).toPath(), new byte[] {1, 2, 3});
    session.process(new CompressionOptions(500000, 1200, 85, Color.WHITE, true));
    idle();
    assertEquals(output, p.output);
    assertEquals(saved, p.savedUri);
    assertTrue(p.exportable());
    assertTrue(p.error.contains("previous preview"));
  }

  @Test
  public void mixedRetryResultsRetainIndividualSettingsAcrossRecovery() throws Exception {
    helper.importOne();
    session.add(Collections.singletonList(helper.uri(helper.fixture("second.jpg", 1600, 1200))));
    idle();
    helper.process();
    PhotoItem second = session.items.get(1);
    String old = second.output;
    Files.write(new File(second.source).toPath(), new byte[] {1, 2, 3});
    session.process(new CompressionOptions(500000, 1200, 85, Color.WHITE, true));
    idle();
    assertEquals(500000, session.items.get(0).requestTarget);
    assertEquals(200000, second.requestTarget);
    assertEquals(old, second.output);
    Session restored = new Session(context);
    restoredIdle(restored);
    assertEquals(500000, restored.items.get(0).requestTarget);
    assertEquals(200000, restored.items.get(1).requestTarget);
    restored.worker.shutdownNow();
  }

  @Test
  public void settingsChangesPreserveScrollAndEditorFocus() throws Exception {
    prepare();
    final int[] positions = new int[2];
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      scenario.onActivity(a -> a.showPage(false));
      Thread.sleep(250);
      scenario.onActivity(
          a -> {
            scroll(a.getWindow().getDecorView()).scrollTo(0, 250);
            positions[0] = scroll(a.getWindow().getDecorView()).getScrollY();
            helper.click(a, "Advanced JPEG quality");
          });
      Thread.sleep(250);
      scenario.onActivity(
          a -> {
            positions[1] = scroll(a.getWindow().getDecorView()).getScrollY();
            assertEquals(positions[0], positions[1]);
            a.edgeInput.requestFocus();
            a.edgeInput.setSelection(2);
            helper.click(a, "500 KB");
          });
      Thread.sleep(250);
      scenario.onActivity(
          a -> {
            assertEquals(a.edgeInput, a.getCurrentFocus());
            assertEquals(2, a.edgeInput.getSelectionStart());
          });
    }
  }

  @Test
  public void comparisonReturnAndRotationPreserveScroll() throws Exception {
    prepare();
    final int[] position = {0};
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      scenario.onActivity(
          a -> {
            scroll(a.getWindow().getDecorView()).scrollTo(0, 1000);
            position[0] = scroll(a.getWindow().getDecorView()).getScrollY();
            helper.click(a, "Compare before & after");
          });
      Thread.sleep(350);
      UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack();
      Thread.sleep(250);
      scenario.onActivity(
          a -> assertEquals(position[0], scroll(a.getWindow().getDecorView()).getScrollY()));
      scenario.recreate();
      Thread.sleep(250);
      scenario.onActivity(
          a -> assertEquals(position[0], scroll(a.getWindow().getDecorView()).getScrollY()));
    }
  }

  @Test
  public void completedPreviewBringsResultsIntoView() throws Exception {
    helper.importOne();
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      helper.process();
      Thread.sleep(250);
      scenario.onActivity(
          a -> {
            assertNotNull(a.resultHeading);
            Rect visible = new Rect();
            assertTrue(a.resultHeading.getGlobalVisibleRect(visible));
            assertTrue(scroll(a.getWindow().getDecorView()).getScrollY() > 0);
          });
    }
  }
}
