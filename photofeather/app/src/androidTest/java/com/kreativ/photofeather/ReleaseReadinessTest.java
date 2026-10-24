package com.kreativ.photofeather;

import static org.junit.Assert.*;

import android.content.*;
import android.graphics.*;
import android.net.Uri;
import android.util.AtomicFile;
import android.view.*;
import android.widget.*;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import com.kreativ.imageengine.*;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import org.junit.*;

/** New free-version release workflows; all inputs are generated in the isolated test package. */
public class ReleaseReadinessTest {
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
  public void cleanup() throws Exception {
    helper.cleanup();
  }

  void two() throws Exception {
    helper.importOne();
    session.add(Collections.singletonList(helper.uri(helper.fixture("second.jpg", 1600, 1200))));
    helper.idle();
    helper.process();
  }

  List<TextView> texts(View v) {
    List<TextView> result = new ArrayList<>();
    if (v instanceof TextView && v.getVisibility() == View.VISIBLE) result.add((TextView) v);
    if (v instanceof ViewGroup && v.getVisibility() == View.VISIBLE) {
      ViewGroup g = (ViewGroup) v;
      for (int i = 0; i < g.getChildCount(); i++) result.addAll(texts(g.getChildAt(i)));
    }
    return result;
  }

  TextView text(MainActivity a, String value) {
    for (TextView v : texts(a.getWindow().getDecorView()))
      if (v.getText().toString().equals(value)) return v;
    throw new AssertionError("Missing visible text: " + value);
  }

  @Test
  public void unreachableTargetLeadsWithFailureAndMeasuredAttempt() throws Exception {
    helper.importOne();
    session.process(new CompressionOptions(1000, 2560, 85, Color.WHITE, true));
    helper.idle();
    assertEquals("Target not reached", session.items.get(0).status);
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      assertTrue(
          "The failed-target result must become visible",
          androidx.test.uiautomator.UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
              .wait(
                  androidx.test.uiautomator.Until.hasObject(
                      androidx.test.uiautomator.By.text("Target not reached")),
                  5000));
      scenario.onActivity(
          a -> {
            assertTrue(a.viewingResults);
            text(a, "Target not reached");
            text(a, "Change target");
            boolean measured = false;
            for (TextView v : texts(a.root)) {
              assertFalse(
                  "A failed target must not lead with savings",
                  v.getText().toString().contains("% smaller"));
              measured |=
                  v.getText().toString().contains("Final attempt:")
                      && v.getText().toString().contains("bytes");
            }
            assertTrue(measured);
            assertTrue(session.selectedIds().isEmpty());
          });
      helper.screen("13-target-not-reached");
      scenario.onActivity(a -> helper.click(a, "Change target"));
      androidx.test.uiautomator.UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
          .pressBack();
      scenario.onActivity(a -> assertFalse(a.viewingResults));
    }
  }

  @Test
  public void settingsAndResultsHaveVisiblePrimaryActionsAndSurviveRecreation() throws Exception {
    helper.importOne();
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      Thread.sleep(200);
      scenario.onActivity(
          a -> {
            assertFalse(a.viewingResults);
            assertNotNull(a.edgeInput);
            assertTrue(text(a, "Preview copies").getGlobalVisibleRect(new Rect()));
          });
      helper.process();
      Thread.sleep(250);
      scenario.onActivity(
          a -> {
            assertTrue(a.viewingResults);
            assertNull(a.edgeInput);
            assertTrue(text(a, "Save selected").getGlobalVisibleRect(new Rect()));
            assertTrue(text(a, "Share selected").getGlobalVisibleRect(new Rect()));
          });
      scenario.recreate();
      Thread.sleep(250);
      scenario.onActivity(
          a -> {
            assertTrue(a.viewingResults);
            helper.click(a, "Settings");
            assertNotNull(a.edgeInput);
            assertFalse(a.viewingResults);
          });
      scenario.recreate();
      Thread.sleep(250);
      scenario.onActivity(a -> assertFalse(a.viewingResults));
    }
  }

  @Test
  public void selectedExportPersistsAndSharesOnlyChosenCopy() throws Exception {
    two();
    PhotoItem first = session.items.get(0), second = session.items.get(1);
    second.selectedForExport = false;
    assertTrue(session.persist());
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      scenario.onActivity(
          a -> {
            Intent share = a.buildShareIntent();
            assertNotNull(share);
            assertEquals(Intent.ACTION_SEND, share.getAction());
            assertEquals(1, share.getClipData().getItemCount());
            assertTrue(
                share
                    .getParcelableExtra(Intent.EXTRA_STREAM)
                    .toString()
                    .contains(new File(first.output).getName()));
          });
    }
    session.save(helper.saveTree(), session.selectedIds());
    helper.idle();
    assertFalse(first.savedUri.isEmpty());
    helper.saved.add(Uri.parse(first.savedUri));
    assertTrue(second.savedUri.isEmpty());
    Session restored = new Session(context);
    long end = System.currentTimeMillis() + 10000;
    while (restored.busy && System.currentTimeMillis() < end) Thread.sleep(20);
    assertFalse(restored.busy);
    assertEquals(Collections.singleton(first.id), restored.selectedIds());
    restored.worker.shutdownNow();
    first.selectedForExport = false;
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      scenario.onActivity(
          a -> {
            assertNull(a.buildShareIntent());
            assertFalse(text(a, "Save selected").isEnabled());
            assertFalse(text(a, "Share selected").isEnabled());
          });
    }
  }

  @Test
  public void selectedSettingsCheckIgnoresExcludedOldPreview() throws Exception {
    two();
    PhotoItem excluded = session.items.get(1);
    excluded.selectedForExport = false;
    excluded.requestTarget = 1000;
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      scenario.onActivity(a -> assertTrue(a.previewMatchesSettings()));
    }
  }

  @Test
  public void retryFailedDoesNotReencodeOrUnsavHealthyCopies() throws Exception {
    two();
    PhotoItem good = session.items.get(0), failed = session.items.get(1);
    session.save(helper.saveTree(), Collections.singleton(good.id));
    helper.idle();
    helper.saved.add(Uri.parse(good.savedUri));
    String goodOutput = good.output, goodSaved = good.savedUri, oldRetry = failed.output;
    failed.error = "Retry failed: simulated provider problem. Previous preview kept.";
    assertEquals(Collections.singleton(failed.id), session.retryIds());
    session.process(
        new CompressionOptions(200000, 2560, 85, Color.WHITE, true), session.retryIds());
    helper.idle();
    assertEquals(goodOutput, good.output);
    assertEquals(goodSaved, good.savedUri);
    assertNotEquals(oldRetry, failed.output);
    assertTrue(failed.error.isEmpty());
    assertTrue(session.retryIds().isEmpty());
  }

  @Test
  public void removeDeletesOnlyPrivateImportAndPreservesSavedAndSharedBytes() throws Exception {
    two();
    PhotoItem p = session.items.get(0);
    String other = session.items.get(1).id;
    session.save(helper.saveTree(), Collections.singleton(p.id));
    helper.idle();
    Uri saved = Uri.parse(p.savedUri);
    helper.saved.add(saved);
    byte[] bytes = Files.readAllBytes(new File(p.output).toPath());
    File imported = new File(p.source), original = new File(session.exports, "mountain-test.jpg");
    byte[] originalBytes = Files.readAllBytes(original.toPath());
    session.remove(p.id);
    helper.idle();
    assertEquals(1, session.items.size());
    assertEquals(other, session.items.get(0).id);
    assertFalse(imported.exists());
    assertArrayEquals(originalBytes, Files.readAllBytes(original.toPath()));
    assertArrayEquals(bytes, Files.readAllBytes(new File(p.output).toPath()));
    try (InputStream in = context.getContentResolver().openInputStream(saved)) {
      assertArrayEquals(bytes, helper.readAll(in));
    }
  }

  @Test
  public void removeJournalFailurePreservesRecoverableWorkingCopy() throws Exception {
    helper.importOne();
    PhotoItem p = session.items.get(0);
    java.lang.reflect.Field field = Session.class.getDeclaredField("state");
    field.setAccessible(true);
    AtomicFile original = session.state;
    field.set(
        session,
        new AtomicFile(original.getBaseFile()) {
          @Override
          public FileOutputStream startWrite() throws IOException {
            throw new IOException("Injected journal failure");
          }
        });
    try {
      session.remove(p.id);
      helper.idle();
      assertEquals(p, session.items.get(0));
      assertTrue(new File(p.source).isFile());
    } finally {
      field.set(session, original);
      session.persist();
    }
  }

  @Test
  public void cancelledSelectiveRetryLeavesOtherSelectedPhotoUntouched() throws Exception {
    two();
    PhotoItem untouched = session.items.get(0), retry = session.items.get(1);
    String output = untouched.output;
    untouched.error = "";
    session.process(
        new CompressionOptions(1000, 2560, 85, Color.WHITE, true), Collections.singleton(retry.id));
    session.cancel();
    helper.idle();
    assertEquals(output, untouched.output);
    assertEquals("Ready", untouched.status);
    assertTrue(untouched.error.isEmpty());
    assertTrue(untouched.exportable());
    assertTrue(retry.exportable());
  }

  @Test
  public void expandedDetailsAndSelectionDoNotMovePrimaryActions() throws Exception {
    two();
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      Thread.sleep(250);
      scenario.onActivity(
          a -> {
            Rect before = new Rect();
            assertTrue(text(a, "Save selected").getGlobalVisibleRect(before));
            helper.click(a, "Show details");
            Rect after = new Rect();
            assertTrue(text(a, "Save selected").getGlobalVisibleRect(after));
            assertEquals(before, after);
          });
      scenario.recreate();
      Thread.sleep(250);
      scenario.onActivity(a -> text(a, "Hide details"));
    }
  }

  Uri testTree() {
    return android.provider.DocumentsContract.buildTreeDocumentUri(
        context.getPackageName() + ".testdocuments", "root");
  }

  @Test
  public void folderSaveSelectionSurvivesActivityRecreationAndVerifiesBytes() throws Exception {
    two();
    PhotoItem chosen = session.items.get(0), excluded = session.items.get(1);
    excluded.selectedForExport = false;
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      scenario.onActivity(a -> a.folderItemIds = new ArrayList<>(session.selectedIds()));
      scenario.recreate();
      scenario.onActivity(
          a ->
              a.onActivityResult(
                  MainActivity.FOLDER,
                  android.app.Activity.RESULT_OK,
                  new Intent().setData(testTree())));
      helper.idle();
      assertFalse(chosen.savedUri.isEmpty());
      assertTrue(excluded.savedUri.isEmpty());
      Uri uri = Uri.parse(chosen.savedUri);
      helper.saved.add(uri);
      try (InputStream in = context.getContentResolver().openInputStream(uri)) {
        assertArrayEquals(Files.readAllBytes(new File(chosen.output).toPath()), helper.readAll(in));
      }
    }
  }

  @Test
  public void providerWriteRefusalCleansIncompleteDocumentAndRetainsPreview() throws Exception {
    helper.importOne();
    helper.process();
    PhotoItem p = session.items.get(0);
    String output = p.output;
    TestDocumentsProvider.rejectWrites = true;
    try {
      session.save(testTree(), session.selectedIds());
      helper.idle();
      assertTrue(p.savedUri.isEmpty());
      assertFalse(p.saveError.isEmpty());
      assertTrue(session.pending.isEmpty());
      assertEquals(output, p.output);
      assertTrue(p.exportable());
      assertTrue(new File(p.source).isFile());
    } finally {
      TestDocumentsProvider.rejectWrites = false;
    }
  }

  @Test
  public void largeTextResultsKeepExportActionsVisibleAndControlsLabelled() throws Exception {
    two();
    try (ActivityScenario<LargeTextActivity> scenario =
        ActivityScenario.launch(LargeTextActivity.class)) {
      Thread.sleep(250);
      scenario.onActivity(
          a -> {
            Rect save = new Rect(), share = new Rect();
            assertTrue(text(a, "Export selected").getGlobalVisibleRect(save));
            assertTrue(save.height() >= Ui.dp(a, 48));
            assertTrue(
                "Primary actions consume too much of the screen",
                a.scroll.getHeight() > a.dock.getHeight());
            for (TextView t : texts(a.dock))
              if (t instanceof Button) {
                assertNotNull(t.getLayout());
                assertEquals(
                    "Button label clipped",
                    t.length(),
                    t.getLayout().getLineEnd(t.getLineCount() - 1));
              }
            for (TextView t : texts(a.root))
              if (t instanceof CheckBox) assertNotNull(t.getContentDescription());
          });
      helper.screen("14-large-text-results");
      scenario.onActivity(
          a ->
              a.setRequestedOrientation(
                  android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));
      Thread.sleep(500);
      scenario.onActivity(
          a -> {
            assertTrue(text(a, "Export selected").getGlobalVisibleRect(new Rect()));
            assertTrue(
                "Landscape content must remain scrollable", a.scroll.getHeight() > Ui.dp(a, 80));
            helper.click(a, "Export selected");
          });
      androidx.test.uiautomator.UiDevice dialogDevice =
          androidx.test.uiautomator.UiDevice.getInstance(
              InstrumentationRegistry.getInstrumentation());
      assertNotNull(
          "Export choices must be reachable",
          dialogDevice.wait(
              androidx.test.uiautomator.Until.findObject(
                  androidx.test.uiautomator.By.text("Export copies")),
              3000));
      androidx.test.uiautomator.UiScrollable choices =
          new androidx.test.uiautomator.UiScrollable(
              new androidx.test.uiautomator.UiSelector().scrollable(true));
      assertTrue(
          "Save must be reachable",
          choices.scrollIntoView(new androidx.test.uiautomator.UiSelector().text("Save")));
      assertTrue(
          "Share must be reachable",
          choices.scrollIntoView(new androidx.test.uiautomator.UiSelector().text("Share")));
      choices.scrollToEnd(5);
      androidx.test.uiautomator.UiObject2 shareChoice =
          dialogDevice.findObject(androidx.test.uiautomator.By.text("Share"));
      assertNotNull(shareChoice);
      assertTrue(
          "Share touch target must be fully reachable",
          shareChoice.getVisibleBounds().height() >= Ui.dp(context, 48));
      Thread.sleep(250);
      helper.screen("16-large-text-export-choice");
      androidx.test.uiautomator.UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
          .pressBack();
    }
  }

  @Test
  @androidx.test.filters.SdkSuppress(maxSdkVersion = 28)
  public void android9SystemFolderPickerGrantsAccessAndSavesChosenCopy() throws Exception {
    helper.importOne();
    helper.process();
    androidx.test.uiautomator.UiDevice device =
        androidx.test.uiautomator.UiDevice.getInstance(
            InstrumentationRegistry.getInstrumentation());
    device.executeShellCommand("mkdir -p /sdcard/Pictures/PhotoFeatherQA");
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      scenario.onActivity(a -> a.savePreparedCopies());
      androidx.test.uiautomator.UiObject2 folder =
          device.wait(
              androidx.test.uiautomator.Until.findObject(
                  androidx.test.uiautomator.By.text("PhotoFeatherQA")),
              5000);
      assertNotNull("Generated QA folder missing from native picker", folder);
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
                            "USE THIS FOLDER|SELECT", java.util.regex.Pattern.CASE_INSENSITIVE))),
                2000);
      assertNotNull("Folder picker did not expose Select", select);
      select.click();
      androidx.test.uiautomator.UiObject2 allow =
          device.wait(
              androidx.test.uiautomator.Until.findObject(
                  androidx.test.uiautomator.By.text(
                      java.util.regex.Pattern.compile(
                          "ALLOW", java.util.regex.Pattern.CASE_INSENSITIVE))),
              1500);
      if (allow != null) allow.click();
      long end = System.currentTimeMillis() + 10000;
      while (session.items.get(0).savedUri.isEmpty() && System.currentTimeMillis() < end)
        Thread.sleep(50);
      helper.idle();
      PhotoItem p = session.items.get(0);
      assertFalse("Folder save failed: " + p.saveError, p.savedUri.isEmpty());
      helper.saved.add(Uri.parse(p.savedUri));
      assertTrue(new File(p.source).exists());
      helper.screen("15-android9-folder-saved");
    }
  }

  @Test
  public void folderResultWaitsForAnOpeningSessionInsteadOfDroppingTheSave() throws Exception {
    helper.importOne();
    helper.process();
    PhotoItem p = session.items.get(0);
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      assertTrue(session.begin("Restoring session…"));
      scenario.onActivity(
          a -> {
            a.folderItemIds = new ArrayList<>(session.selectedIds());
            a.onActivityResult(
                MainActivity.FOLDER,
                android.app.Activity.RESULT_OK,
                new Intent().setData(testTree()));
          });
      Thread.sleep(150);
      assertTrue(p.savedUri.isEmpty());
      session.busy = false;
      session.notifyUi();
      long end = System.currentTimeMillis() + 5000;
      while (p.savedUri.isEmpty() && System.currentTimeMillis() < end) Thread.sleep(30);
      helper.idle();
      assertFalse(p.saveError, p.savedUri.isEmpty());
      helper.saved.add(Uri.parse(p.savedUri));
    } finally {
      session.busy = false;
    }
  }
}
