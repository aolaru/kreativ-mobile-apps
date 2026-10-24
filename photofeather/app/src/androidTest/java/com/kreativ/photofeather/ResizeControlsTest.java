package com.kreativ.photofeather;

import static org.junit.Assert.*;

import android.content.*;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.*;
import com.kreativ.imageengine.*;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.junit.*;

public class ResizeControlsTest {
  WorkflowTest helper;
  Session session;

  @Before
  public void setup() throws Exception {
    helper = new WorkflowTest();
    helper.setup();
    session = helper.session;
  }

  @After
  public void cleanup() throws Exception {
    helper.cleanup();
  }

  String plan(MainActivity a) {
    return a.resizeDimensions.getText().toString();
  }

  Button button(MainActivity a, String label) {
    for (Button b : a.resizeButtons.keySet()) if (b.getText().toString().equals(label)) return b;
    throw new AssertionError("Resize button missing: " + label);
  }

  void small(MainActivity a) {
    helper.click(a, "Small\n1280 px");
  }

  void flush() throws Exception {
    session.worker.submit(() -> {}).get(10, TimeUnit.SECONDS);
  }

  void showResizeSection(MainActivity a, boolean customField) {
    View target = a.edgeInput;
    if (!customField) {
      ArrayList<View> views = new ArrayList<>();
      a.root.findViewsWithText(views, "Output settings", View.FIND_VIEWS_WITH_TEXT);
      if (!views.isEmpty()) target = views.get(0);
    }
    int y = 0;
    while (target != a.scroll && target.getParent() instanceof View) {
      y += target.getTop();
      target = (View) target.getParent();
    }
    a.scroll.scrollTo(0, Math.max(0, y - Ui.dp(a, 8)));
  }

  @Test
  public void presetsUpdateEveryPhotoAndNeverUpscaleSmallImages() throws Exception {
    session.add(
        Arrays.asList(
            helper.uri(helper.fixture("landscape.jpg", 2400, 1800)),
            helper.uri(helper.fixture("portrait.jpg", 1800, 2400)),
            helper.uri(helper.fixture("small.jpg", 640, 480))));
    helper.idle();
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      scenario.onActivity(
          a -> {
            small(a);
            assertTrue(button(a, "Small\n1280 px").isSelected());
            assertEquals("1280", a.edgeInput.getText().toString());
            assertTrue(plan(a).contains("2400 × 1800 → up to 1280 × 960 px"));
            assertTrue(plan(a).contains("1800 × 2400 → up to 960 × 1280 px"));
            assertTrue(plan(a).contains("640 × 480 → up to 640 × 480 px · already within limit"));
            helper.click(a, "Medium\n1920 px");
            assertTrue(plan(a).contains("1920 × 1440 px"));
            helper.click(a, "Large\n2560 px");
            assertTrue(plan(a).contains("2400 × 1800 px · already within limit"));
            assertTrue(button(a, "Large\n2560 px").isSelected());
          });
      Thread.sleep(200);
      scenario.onActivity(
          a -> {
            small(a);
            showResizeSection(a, false);
          });
      Thread.sleep(150);
      helper.screen("resize-01-presets");
    }
  }

  @Test
  public void customInputUpdatesLiveAndRejectsInvalidPixelValues() throws Exception {
    helper.importOne();
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      scenario.onActivity(
          a -> {
            helper.click(a, "Custom\n64–8192 px");
            for (String invalid :
                new String[] {"", "0", "63", "8193", "1.5", "hello", "999999999999"}) {
              a.edgeInput.setText(invalid);
              assertNotNull(a.edgeInput.getError());
              assertFalse(a.previewButton.isEnabled());
              assertFalse(plan(a).contains("→"));
            }
            a.edgeInput.setText("1500");
            assertNull(a.edgeInput.getError());
            assertTrue(a.previewButton.isEnabled());
            assertTrue(button(a, "Custom\n64–8192 px").isSelected());
            assertTrue(plan(a).contains("1500 × 1125 px"));
            a.edgeInput.setText("64");
            assertTrue(plan(a).contains("64 × 48 px"));
            a.edgeInput.setText("8192");
            assertTrue(plan(a).contains("2400 × 1800 px · already within limit"));
            a.edgeInput.setText("1500");
            a.resizeDimensions.requestRectangleOnScreen(
                new Rect(0, 0, a.resizeDimensions.getWidth(), a.resizeDimensions.getHeight()),
                true);
          });
      Thread.sleep(200);
      scenario.onActivity(a -> showResizeSection(a, false));
      Thread.sleep(150);
      helper.screen("resize-02-custom");
    }
  }

  @Test
  public void resizeOnlyProducesThePreviewedDimensionsAndVerifiedBytes() throws Exception {
    helper.importOne();
    PhotoItem p = session.items.get(0);
    byte[] before = Files.readAllBytes(new File(p.source).toPath());
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      scenario.onActivity(
          a -> {
            small(a);
            ArrayList<View> views = new ArrayList<>();
            a.root.findViewsWithText(
                views, a.getString(R.string.resize_only), View.FIND_VIEWS_WITH_TEXT);
            for (View v : views) if (v instanceof CheckBox) ((CheckBox) v).setChecked(true);
            assertTrue(session.resizeOnly);
            assertTrue(plan(a).contains("1280 × 960 px"));
            a.resizeDimensions.requestRectangleOnScreen(
                new Rect(0, 0, a.resizeDimensions.getWidth(), a.resizeDimensions.getHeight()),
                true);
          });
      Thread.sleep(200);
      scenario.onActivity(a -> showResizeSection(a, false));
      Thread.sleep(150);
      helper.screen("resize-03-before-processing");
      scenario.onActivity(MainActivity::prepare);
      helper.idle();
      Thread.sleep(200);
      assertTrue(p.exportable());
      assertEquals(0, p.requestTarget);
      assertEquals(1280, p.w);
      assertEquals(960, p.h);
      assertEquals(p.after, new File(p.output).length());
      ImageEngine.verifyOutput(new File(p.output), 1280, 960);
      assertArrayEquals(before, Files.readAllBytes(new File(p.source).toPath()));
      scenario.onActivity(
          a -> {
            assertTrue(a.viewingResults);
            helper.click(a, "Show details");
          });
      Thread.sleep(200);
      helper.screen("resize-04-measured-result");
    }
  }

  @Test
  public void customModeAndValueSurviveRotationAndDiskRecovery() throws Exception {
    helper.importOne();
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      scenario.onActivity(
          a -> {
            helper.click(a, "Custom\n64–8192 px");
            a.edgeInput.setText("1500");
          });
      scenario.recreate();
      Thread.sleep(200);
      scenario.onActivity(
          a -> {
            assertEquals("1500", a.edgeInput.getText().toString());
            assertTrue(button(a, "Custom\n64–8192 px").isSelected());
          });
      flush();
      Session restored = new Session(helper.context);
      long end = System.currentTimeMillis() + 10000;
      while (restored.busy && System.currentTimeMillis() < end) Thread.sleep(20);
      assertFalse(restored.busy);
      assertTrue(restored.customResize);
      assertEquals(1500, restored.edge);
      restored.worker.shutdownNow();
      scenario.onActivity(
          a -> {
            helper.click(a, "Large\n2560 px");
            assertFalse(session.customResize);
          });
      flush();
      assertTrue(session.persist());
    }
  }

  @Test
  public void changedResizePresetCannotSilentlyExportEarlierDimensions() throws Exception {
    helper.importOne();
    helper.process();
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      scenario.onActivity(
          a -> {
            a.showPage(false);
            small(a);
            assertFalse(a.previewMatchesSettings());
            a.share();
          });
      UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
      assertNotNull(
          device.wait(
              Until.findObject(
                  By.text(
                      java.util.regex.Pattern.compile(
                          "Export shown copies", java.util.regex.Pattern.CASE_INSENSITIVE))),
              3000));
      device.pressBack();
    }
  }

  @Test
  public void largePhotoPreviewDisclosesMemoryLimit() throws Exception {
    Bitmap b = Bitmap.createBitmap(4000, 3000, Bitmap.Config.ARGB_8888);
    new Canvas(b).drawColor(Color.GREEN);
    File file = new File(session.exports, "large-memory.jpg");
    try (FileOutputStream out = new FileOutputStream(file)) {
      b.compress(Bitmap.CompressFormat.JPEG, 95, out);
    }
    b.recycle();
    session.add(Collections.singletonList(helper.uri(file)));
    helper.idle();
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      scenario.onActivity(
          a -> {
            assertTrue(plan(a).contains("4000 × 3000 → up to 2000 × 1500 px · memory limit"));
            small(a);
            assertTrue(plan(a).contains("1280 × 960 px"));
          });
    }
  }

  @Test
  public void resizeControlsRemainUsableAtLargeTextAndLandscape() throws Exception {
    helper.importOne();
    try (ActivityScenario<LargeTextActivity> scenario =
        ActivityScenario.launch(LargeTextActivity.class)) {
      Thread.sleep(200);
      scenario.onActivity(
          a -> {
            a.showPage(false);
          });
      Thread.sleep(200);
      scenario.onActivity(
          a -> {
            for (Button b : a.resizeButtons.keySet()) {
              assertTrue(b.getHeight() >= Ui.dp(a, 48));
              assertNotNull(b.getLayout());
              assertEquals(b.length(), b.getLayout().getLineEnd(b.getLineCount() - 1));
              assertNotNull(b.getContentDescription());
            }
            helper.click(a, "Custom\n64–8192 px");
            a.edgeInput.setText("1500");
            a.resizeDimensions.requestRectangleOnScreen(
                new Rect(0, 0, a.resizeDimensions.getWidth(), a.resizeDimensions.getHeight()),
                true);
          });
      Thread.sleep(200);
      scenario.onActivity(a -> showResizeSection(a, true));
      Thread.sleep(150);
      helper.screen("resize-05-large-text");
      scenario.onActivity(
          a ->
              a.setRequestedOrientation(
                  android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));
      Thread.sleep(500);
      scenario.onActivity(
          a -> {
            assertTrue(a.previewButton.getGlobalVisibleRect(new Rect()));
            assertTrue(plan(a).contains("1500 × 1125 px"));
          });
    }
  }
}
