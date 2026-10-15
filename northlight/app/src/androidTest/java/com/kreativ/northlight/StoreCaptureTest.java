package com.kreativ.northlight;

import android.app.UiAutomation;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.MotionEvent;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Opt-in real-phone captures. Never seeds headings, altitude, purchases or prices. */
@RunWith(AndroidJUnit4.class)
public class StoreCaptureTest {
    private static final String PREVIEW = "com.kreativ.northlight.preview";
    private UiAutomation automation;
    private Context context;

    @Test public void captureRealPreviewScreens() throws Exception {
        Assume.assumeTrue("Run explicitly with northlightCapture=true",
                "true".equals(InstrumentationRegistry.getArguments().getString("northlightCapture")));
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertTrue(context.getPackageName().endsWith(".debug"));
        automation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
        android.accessibilityservice.AccessibilityServiceInfo service = automation.getServiceInfo();
        service.flags |= android.accessibilityservice.AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
                | android.accessibilityservice.AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;
        automation.setServiceInfo(service);
        launchHome();
        show("field_compass");
        capture("01-compass.png");

        show("altimeter_panel");
        // A real foreground GPS session, only with permission already granted by the user.
        click("altimeter_start");
        long gpsDeadline = SystemClock.uptimeMillis() + 20000;
        while (node("altimeter_value").getText().toString().startsWith("—")
                && SystemClock.uptimeMillis() < gpsDeadline) SystemClock.sleep(500);
        align("altimeter_panel", 130);
        capture("02-altimeter.png");

        align("speech_speak", 450);
        click("speech_speak");
        SystemClock.sleep(3000);
        show("speech_speak");
        capture("03-spoken-compass.png");

        align("field_maker_credit", 1550);
        capture("04-version-and-credit.png");
        click("field_pro_tools");
        show("pro_benefits");
        capture("05-pro-benefits.png");
        align("pro_free_features", 130);
        capture("06-free-features.png");

        automation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK);
        show("altimeter_start");
        assertEquals("GPS must not resume after leaving home", context.getString(R.string.altimeter_start),
                node("altimeter_start").getText().toString());
        assertEquals(context.getString(R.string.altimeter_paused), node("altimeter_status").getText().toString());
        show("field_night_light");
        click("field_night_light");
        SystemClock.sleep(500);
        capture("07-red-light.png");
        automation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK);
        show("field_compass");
    }

    private void launchHome() {
        context.startActivity(new Intent().setClassName(PREVIEW, "com.kreativ.northlight.MainActivity")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        node("field_compass");
    }

    private AccessibilityNodeInfo node(String id) {
        long until = SystemClock.uptimeMillis() + 8000;
        do {
            AccessibilityNodeInfo root = automation.getRootInActiveWindow();
            if (root != null && PREVIEW.contentEquals(root.getPackageName())) {
                List<AccessibilityNodeInfo> matches = root.findAccessibilityNodeInfosByViewId(PREVIEW + ":id/" + id);
                if (!matches.isEmpty()) return matches.get(0);
                // Find nodes outside the current viewport by scrolling the app, never another app.
                scroll(root);
            }
            SystemClock.sleep(200);
        } while (SystemClock.uptimeMillis() < until);
        throw new AssertionError("Preview control not found: " + id);
    }

    private boolean scroll(AccessibilityNodeInfo root) {
        if (root.isScrollable() && root.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)) return true;
        for (int i = 0; i < root.getChildCount(); i++) {
            AccessibilityNodeInfo child = root.getChild(i);
            if (child != null && scroll(child)) return true;
        }
        return false;
    }

    private void show(String id) {
        node(id).performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SHOW_ON_SCREEN.getId());
        SystemClock.sleep(500);
    }

    private void click(String id) {
        show(id);
        assertTrue("Could not activate " + id, node(id).performAction(AccessibilityNodeInfo.ACTION_CLICK));
        SystemClock.sleep(500);
    }

    private void align(String id, int top) {
        show(id);
        int height = context.getResources().getDisplayMetrics().heightPixels;
        for (int pass = 0; pass < 4; pass++) {
            android.graphics.Rect bounds = new android.graphics.Rect();
            node(id).getBoundsInScreen(bounds);
            int delta = Math.max(-height / 2, Math.min(height / 2, bounds.top - top));
            if (Math.abs(delta) < 30) break;
            // The blank left margin belongs to the ScrollView, not a slider or a button.
            float from = delta > 0 ? height - 250 : 250;
            float to = from - delta;
            long down = SystemClock.uptimeMillis();
            touch(down, MotionEvent.ACTION_DOWN, from);
            for (int step = 1; step <= 20; step++) {
                touch(down, MotionEvent.ACTION_MOVE, from + (to - from) * step / 20f);
                SystemClock.sleep(25);
            }
            SystemClock.sleep(200);
            touch(down, MotionEvent.ACTION_UP, to);
            SystemClock.sleep(400);
        }
    }

    private void touch(long down, int action, float y) {
        MotionEvent event = MotionEvent.obtain(down, SystemClock.uptimeMillis(), action, 20, y, 0);
        event.setSource(android.view.InputDevice.SOURCE_TOUCHSCREEN);
        try { assertTrue(automation.injectInputEvent(event, true)); }
        finally { event.recycle(); }
    }

    private void capture(String name) throws Exception {
        File folder = new File(context.getExternalFilesDir(null), "northlight-store-1.2.0");
        assertTrue(folder.isDirectory() || folder.mkdirs());
        Bitmap frame = automation.takeScreenshot();
        assertNotNull(frame);
        assertEquals(1080, frame.getWidth()); assertEquals(1920, frame.getHeight());
        frame.setHasAlpha(false); // Encode the opaque screenshot as 24-bit PNG, without retouching pixels.
        try (FileOutputStream stream = new FileOutputStream(new File(folder, name))) {
            assertTrue(frame.compress(Bitmap.CompressFormat.PNG, 100, stream));
        } finally { frame.recycle(); }
    }
}
