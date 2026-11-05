package com.kreativ.photofeather;

import static org.junit.Assert.*;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.*;
import android.content.*;
import android.graphics.*;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.view.accessibility.*;
import androidx.core.content.FileProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.*;
import java.util.*;
import org.junit.Test;

/**
 * Dedicated opt-in test source set. Requires an enabled TalkBack service; never enables it itself.
 */
public class TalkBackAuditTest {
  Instrumentation instrument;
  UiAutomation automation;
  Session session;
  Context context;
  MainActivity activity;

  void idle() throws Exception {
    instrument.waitForIdleSync();
    long end = System.currentTimeMillis() + 30000;
    while (session.busy && System.currentTimeMillis() < end) Thread.sleep(30);
    assertFalse(session.message, session.busy);
    instrument.waitForIdleSync();
    Thread.sleep(200);
  }

  AccessibilityNodeInfo find(AccessibilityNodeInfo node, String label) {
    if (node == null) return null;
    if (label.contentEquals(node.getText() == null ? "" : node.getText())
        || label.contentEquals(
            node.getContentDescription() == null ? "" : node.getContentDescription())) return node;
    for (int i = 0; i < node.getChildCount(); i++) {
      AccessibilityNodeInfo found = find(node.getChild(i), label);
      if (found != null) return found;
    }
    return null;
  }

  AccessibilityNodeInfo node(String label) throws Exception {
    long end = System.currentTimeMillis() + 5000;
    AccessibilityNodeInfo n;
    do {
      n = find(automation.getRootInActiveWindow(), label);
      if (n != null) return n;
      Thread.sleep(80);
    } while (System.currentTimeMillis() < end);
    throw new AssertionError("Accessible control missing: " + label);
  }

  void click(String label) throws Exception {
    AccessibilityNodeInfo n = node(label);
    n.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SHOW_ON_SCREEN.getId());
    assertTrue(
        "Cannot focus " + label,
        n.isAccessibilityFocused()
            || n.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS));
    assertTrue("Cannot activate " + label, n.performAction(AccessibilityNodeInfo.ACTION_CLICK));
    idle();
  }

  void screen(String name) throws Exception {
    Bitmap b = automation.takeScreenshot();
    assertNotNull(b);
    File dir = new File(context.getExternalFilesDir(null), "accessibility-screenshots");
    dir.mkdirs();
    try (FileOutputStream out = new FileOutputStream(new File(dir, name + ".png"))) {
      b.compress(Bitmap.CompressFormat.PNG, 100, out);
    }
    b.recycle();
  }

  @Test
  public void talkBackEnabledWorkflowExposesAndActivatesMainControls() throws Exception {
    instrument = InstrumentationRegistry.getInstrumentation();
    automation = instrument.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES);
    context = instrument.getTargetContext();
    AccessibilityManager manager =
        (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
    boolean talkBack = false;
    for (AccessibilityServiceInfo info :
        manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_SPOKEN)) {
      talkBack |= info.getId().toLowerCase(Locale.US).contains("talkback");
    }
    assertTrue("Enable TalkBack before running this dedicated test", talkBack);
    session = ((FeatherApp) context.getApplicationContext()).session;
    idle();
    session.clear();
    idle();
    Bitmap image = Bitmap.createBitmap(1000, 750, Bitmap.Config.ARGB_8888);
    int[] pixels = new int[750000];
    Random r = new Random(25);
    for (int i = 0; i < pixels.length; i++) pixels[i] = 0xff000000 | r.nextInt(0x1000000);
    image.setPixels(pixels, 0, 1000, 0, 0, 1000, 750);
    File fixture = new File(session.exports, "talkback-fixture.jpg");
    try (FileOutputStream out = new FileOutputStream(fixture)) {
      image.compress(Bitmap.CompressFormat.JPEG, 100, out);
    }
    image.recycle();
    session.target = 200000;
    session.edge = 1200;
    session.advanced = false;
    session.resizeOnly = false;
    session.add(
        Collections.singletonList(
            FileProvider.getUriForFile(context, context.getPackageName() + ".files", fixture)));
    idle();
    Intent launch = new Intent(context, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    activity = (MainActivity) instrument.startActivitySync(launch);
    idle();
    Uri saved = null;
    try {
      click("Preview copies");
      assertTrue(session.items.get(0).exportable());
      click("Show details");
      screen("01-details-focused");
      click("Hide details");
      String include = "Include " + session.items.get(0).name + " in export";
      click(include);
      assertTrue(session.selectedIds().isEmpty());
      assertFalse(node("Share selected").isEnabled());
      click(include);
      assertEquals(1, session.selectedIds().size());
      click("Compare before & after");
      click("Original");
      click("Zoom in");
      screen("02-comparison-focused");
      click("Back");
      click("Save selected");
      assertFalse(session.items.get(0).savedUri.isEmpty());
      saved = Uri.parse(session.items.get(0).savedUri);
      click("Share selected");
      screen("03-system-share-sheet");
      try (ParcelFileDescriptor ignored = automation.executeShellCommand("input keyevent 4")) {}
      idle();
      click("Settings");
      click("Results");
      screen("04-results-focused");
    } finally {
      if (saved != null) ExportManager.rollback(context, saved);
      instrument.runOnMainSync(() -> activity.finish());
    }
  }
}
