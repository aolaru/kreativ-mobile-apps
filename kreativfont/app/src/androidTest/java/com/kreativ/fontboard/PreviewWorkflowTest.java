package com.kreativ.fontboard;

import android.content.Intent;
import android.os.SystemClock;
import android.graphics.Bitmap;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.io.File;
import java.io.FileOutputStream;

import static org.junit.Assert.*;

/** Run only on an unlocked real device; never changes saved favorites. */
@RunWith(AndroidJUnit4.class)
public class PreviewWorkflowTest {
    private ZoomPreviewView preview(FontPreviewActivity activity) {
        return activity.getWindow().getDecorView().findViewWithTag("full_screen_preview");
    }

    private void awaitImage(ActivityScenario<FontPreviewActivity> scenario) {
        long until = SystemClock.elapsedRealtime() + 45000;
        AtomicBoolean ready = new AtomicBoolean();
        do {
            scenario.onActivity(activity -> ready.set(preview(activity).getDrawable() != null && preview(activity).transform.ready()));
            if (ready.get()) return;
            SystemClock.sleep(200);
        } while (SystemClock.elapsedRealtime() < until);
        fail("Full-screen preview did not load");
    }

    private void screenshot(String name) throws Exception {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        Bitmap image = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        assertNotNull(image);
        File folder = InstrumentationRegistry.getInstrumentation().getTargetContext().getExternalFilesDir("review");
        assertNotNull(folder);
        try (FileOutputStream output = new FileOutputStream(new File(folder, name + ".png"))) {
            assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, output));
        } finally { image.recycle(); }
    }

    private float[] center(ActivityScenario<FontPreviewActivity> scenario) {
        AtomicReference<float[]> result = new AtomicReference<>();
        scenario.onActivity(activity -> {
            ZoomPreviewView image = preview(activity);
            int[] location = new int[2];
            image.getLocationOnScreen(location);
            result.set(new float[]{location[0] + image.getWidth() / 2f, location[1] + image.getHeight() / 2f, image.getWidth()});
        });
        return result.get();
    }

    private void tap(float x, float y) {
        long now = SystemClock.uptimeMillis();
        MotionEvent down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, x, y, 0);
        MotionEvent up = MotionEvent.obtain(now, now + 40, MotionEvent.ACTION_UP, x, y, 0);
        down.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        up.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        try {
            InstrumentationRegistry.getInstrumentation().sendPointerSync(down);
            InstrumentationRegistry.getInstrumentation().sendPointerSync(up);
        } finally { down.recycle(); up.recycle(); }
    }

    private void pinch(float centerX, float centerY, float width) {
        long downTime = SystemClock.uptimeMillis();
        MotionEvent.PointerProperties[] properties = new MotionEvent.PointerProperties[2];
        MotionEvent.PointerCoords[] coordinates = new MotionEvent.PointerCoords[2];
        for (int i = 0; i < 2; i++) {
            properties[i] = new MotionEvent.PointerProperties();
            properties[i].id = i;
            properties[i].toolType = MotionEvent.TOOL_TYPE_FINGER;
            coordinates[i] = new MotionEvent.PointerCoords();
            coordinates[i].pressure = 1;
            coordinates[i].size = 1;
            coordinates[i].y = centerY;
        }
        for (int step = 0; step < 12; step++) {
            float distance = width * (.08f + Math.min(step, 9) * .015f);
            coordinates[0].x = centerX - distance;
            coordinates[1].x = centerX + distance;
            int action = step == 0 ? MotionEvent.ACTION_DOWN
                    : step == 1 ? MotionEvent.ACTION_POINTER_DOWN | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT)
                    : step == 10 ? MotionEvent.ACTION_POINTER_UP | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT)
                    : step == 11 ? MotionEvent.ACTION_UP : MotionEvent.ACTION_MOVE;
            int pointers = step == 0 || step == 11 ? 1 : 2;
            MotionEvent event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, pointers,
                    properties, coordinates, 0, 0, 1, 1, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0);
            try { InstrumentationRegistry.getInstrumentation().sendPointerSync(event); }
            finally { event.recycle(); }
            SystemClock.sleep(30);
        }
    }

    @Test public void zoomControlsFitAndRecreationKeepArtworkUsable() throws Exception {
        FontApi.Page page = FontApi.fetch(FontApi.feedUrl("Commercial", 1), "Commercial", 1);
        FontItem font = page.fonts.get(0);
        Intent intent = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(), FontPreviewActivity.class)
                .putExtra("image", font.fullImageUrl).putExtra("name", font.name);
        try (ActivityScenario<FontPreviewActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
            awaitImage(scenario);
            screenshot("full-screen-preview-fit");
            scenario.onActivity(activity -> {
                ZoomPreviewView image = preview(activity);
                assertEquals(1, image.transform.zoom, .001f);
                View zoomIn = activity.getWindow().getDecorView().findViewWithTag("preview_zoom_in");
                assertTrue(zoomIn.isEnabled());
                assertTrue(zoomIn.performClick());
                assertEquals(1.5f, image.transform.zoom, .001f);
            });
            scenario.recreate();
            scenario.onActivity(activity -> activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
            awaitImage(scenario);
            scenario.onActivity(activity -> {
                assertEquals(1.5f, preview(activity).transform.zoom, .001f);
                View fit = activity.getWindow().getDecorView().findViewWithTag("preview_zoom_fit");
                assertTrue(fit.performClick());
                assertEquals(1, preview(activity).transform.zoom, .001f);
                View zoomOut = activity.getWindow().getDecorView().findViewWithTag("preview_zoom_out");
                assertTrue(zoomOut.performClick());
                assertEquals(1, preview(activity).transform.zoom, .001f);
            });
            float[] center = center(scenario);
            tap(center[0], center[1]);
            SystemClock.sleep(100);
            tap(center[0], center[1]);
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(activity -> assertTrue("Double-tap should zoom", preview(activity).transform.zoom > 1));
            screenshot("full-screen-preview-double-tap");
            scenario.onActivity(activity -> preview(activity).fit());
            SystemClock.sleep(350);
            pinch(center[0], center[1], center[2]);
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(activity -> assertTrue("A real two-finger pinch should zoom", preview(activity).transform.zoom > 1.05f));
            screenshot("full-screen-preview-pinch");
        }
    }
}
