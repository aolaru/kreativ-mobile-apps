package com.kreativ.sharesafe;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.ExifInterface;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Real UI captures for the tutorial; never opens the user's gallery or share sheet. */
@RunWith(AndroidJUnit4.class)
public class TutorialScreenshotsTest {
    private View find(View root, String prefix) {
        if (root instanceof TextView && ((TextView) root).getText().toString().startsWith(prefix)) return root;
        if (root instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) root).getChildCount(); i++) {
            View found = find(((ViewGroup) root).getChildAt(i), prefix);
            if (found != null) return found;
        }
        return null;
    }
    private View control(MainActivity activity, String prefix) {
        View result = find(activity.findViewById(android.R.id.content), prefix);
        assertNotNull("Missing " + prefix, result); return result;
    }
    private ExportSession session(MainActivity activity) {
        try { Field field = MainActivity.class.getDeclaredField("session"); field.setAccessible(true); return (ExportSession) field.get(activity); }
        catch (Exception error) { throw new AssertionError(error); }
    }
    private ScrollView scroll(View view) {
        while (!(view instanceof ScrollView)) view = (View) view.getParent();
        return (ScrollView) view;
    }
    private void align(ActivityScenario<MainActivity> scenario, String prefix) throws Exception {
        scenario.onActivity(activity -> {
            View target = (View) control(activity, prefix).getParent(); ScrollView parent = scroll(target);
            android.graphics.Rect bounds = new android.graphics.Rect(); target.getDrawingRect(bounds);
            parent.offsetDescendantRectToMyCoords(target, bounds);
            parent.setSmoothScrollingEnabled(false);
            parent.scrollTo(0, Math.max(0, bounds.top - Ui.dp(activity, 64)));
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync(); Thread.sleep(400);
    }
    private void capture(Context context, String name) throws Exception {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync(); Thread.sleep(500);
        Bitmap screen = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        assertNotNull(screen);
        File folder = new File(context.getExternalFilesDir(null), "tutorial-screenshots");
        assertTrue(folder.isDirectory() || folder.mkdirs());
        try (FileOutputStream output = new FileOutputStream(new File(folder, name))) {
            assertTrue(screen.compress(Bitmap.CompressFormat.PNG, 100, output));
        } finally { screen.recycle(); }
    }
    @Test public void captureFreeWorkflowWithSyntheticMetadata() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        assertEquals("1.2.0", BuildConfig.VERSION_NAME);
        File fixture = ImageAndExportTest.fixture(context, "tutorial-demo.jpg", 1200, 800);
        ExifInterface exif = new ExifInterface(fixture.getAbsolutePath());
        exif.setAttribute(ExifInterface.TAG_MAKE, "Tutorial synthetic camera");
        exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, "2026:10:17 12:00:00");
        exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, "0/1,0/1,0/1");
        exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "N");
        exif.saveAttributes();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            capture(context, "01-choose-photos.png");
            scenario.onActivity(activity -> activity.onActivityResult(44, Activity.RESULT_OK, new Intent().setData(Uri.fromFile(fixture))));
            AtomicBoolean ready = new AtomicBoolean();
            for (int i = 0; i < 100 && !ready.get(); i++) {
                scenario.onActivity(activity -> ready.set(session(activity).privacyBefore != null)); Thread.sleep(50);
            }
            assertTrue(ready.get());
            scenario.onActivity(activity -> {
                assertArrayEquals(new int[]{1, 1, 1}, session(activity).privacyBefore.found);
                control(activity, "Privacy Check · Free").performClick();
            });
            align(scenario, "Privacy Check · Free"); capture(context, "02-original-metadata.png");
            scenario.onActivity(activity -> {
                control(activity, "Privacy Check · Free").performClick();
                control(activity, "Export options · Change").performClick();
            });
            align(scenario, "Export options · Hide"); capture(context, "03-export-options.png");
            scenario.onActivity(activity -> {
                control(activity, "Export options · Hide").performClick();
                control(activity, "Clean photos").performClick();
            });
            ready.set(false);
            for (int i = 0; i < 200 && !ready.get(); i++) {
                scenario.onActivity(activity -> ready.set(session(activity).result != null && !session(activity).busy)); Thread.sleep(50);
            }
            assertTrue(ready.get());
            scenario.onActivity(activity -> {
                assertArrayEquals(new int[]{0, 0, 0}, session(activity).result.privacyAfter.found);
                control(activity, "Privacy Check · Free").performClick();
            });
            align(scenario, "Privacy Check · Free"); capture(context, "04-before-after.png");
            align(scenario, "Ready to share"); capture(context, "05-ready-to-share.png");
        } finally { assertTrue(fixture.delete()); }
    }
}
