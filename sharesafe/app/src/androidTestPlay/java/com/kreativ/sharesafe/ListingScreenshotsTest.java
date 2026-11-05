package com.kreativ.sharesafe;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
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

/** Store artwork sources: actual release-shared UI, synthetic input, no personal data. */
@RunWith(AndroidJUnit4.class)
public class ListingScreenshotsTest {
    private final Context context = ApplicationProvider.getApplicationContext();
    private static Object field(Object object, String name) {
        try { Field f = object.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(object); }
        catch (Exception e) { throw new AssertionError(e); }
    }
    private static View find(View root, String prefix) {
        if (root instanceof TextView && ((TextView) root).getText().toString().startsWith(prefix)) return root;
        if (root instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) root).getChildCount(); i++) {
            View result = find(((ViewGroup) root).getChildAt(i), prefix); if (result != null) return result;
        }
        return null;
    }
    private static View control(Activity activity, String prefix) {
        View view = find(activity.findViewById(android.R.id.content), prefix);
        assertNotNull("Missing control: " + prefix, view); return view;
    }
    private static void idle() throws Exception {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync(); Thread.sleep(500);
    }
    private void save(Bitmap image, String filename) throws Exception {
        File folder = new File(context.getExternalFilesDir(null), "listing-screenshots");
        assertTrue(folder.isDirectory() || folder.mkdirs());
        try (FileOutputStream out = new FileOutputStream(new File(folder, filename))) {
            assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, out));
        }
    }
    private static void align(View view) {
        View ancestor = view;
        while (!(ancestor instanceof ScrollView)) ancestor = (View) ancestor.getParent();
        ScrollView scroll = (ScrollView) ancestor;
        Rect rect = new Rect(); view.getDrawingRect(rect);
        scroll.offsetDescendantRectToMyCoords(view, rect);
        Rect viewport = new Rect(); assertTrue(scroll.getGlobalVisibleRect(viewport));
        int[] origin = new int[2]; scroll.getLocationOnScreen(origin);
        int visibleTopInset = Math.max(scroll.getPaddingTop(), Math.max(0, viewport.top - origin[1]));
        scroll.setSmoothScrollingEnabled(false); scroll.scrollTo(0, Math.max(0, rect.top - visibleTopInset));
    }
    private void capture(ActivityScenario<?> scenario, String filename, java.util.function.Function<Activity, View> target) throws Exception {
        capture(scenario, filename, target, target);
    }
    private void capture(ActivityScenario<?> scenario, String filename, java.util.function.Function<Activity, View> target,
                         java.util.function.Function<Activity, View> endTarget) throws Exception {
        Rect rect = new Rect();
        scenario.onActivity(activity -> align(target.apply(activity))); idle();
        scenario.onActivity(activity -> {
            View view = target.apply(activity); int[] position = new int[2]; view.getLocationOnScreen(position);
            rect.set(position[0], position[1], position[0] + view.getWidth(), position[1] + view.getHeight());
            Rect visible = new Rect(); assertTrue(view.getGlobalVisibleRect(visible));
            assertEquals("Capture must include the complete real UI panel", rect, visible);
            View end = endTarget.apply(activity); int[] endPosition = new int[2]; end.getLocationOnScreen(endPosition);
            Rect endVisible = new Rect(); assertTrue(end.getGlobalVisibleRect(endVisible));
            assertEquals(endPosition[1] + end.getHeight(), endVisible.bottom);
            rect.bottom = endVisible.bottom;
        });
        Bitmap screen = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        assertNotNull(screen); assertTrue(rect.left >= 0 && rect.top >= 0 && rect.right <= screen.getWidth() && rect.bottom <= screen.getHeight());
        Bitmap crop = Bitmap.createBitmap(screen, rect.left, rect.top, rect.width(), rect.height());
        try { save(screen, filename.replace(".png", "-full.png")); save(crop, filename); }
        finally { crop.recycle(); screen.recycle(); }
    }
    private static void ready(ActivityScenario<MainActivity> scenario, boolean exported) throws Exception {
        AtomicBoolean done = new AtomicBoolean();
        for (int i = 0; i < 200 && !done.get(); i++) {
            scenario.onActivity(activity -> {
                ExportSession s = (ExportSession) field(activity, "session");
                done.set(exported ? s.result != null && !s.busy : s.privacyBefore != null);
            }); Thread.sleep(50);
        }
        assertTrue("Photo processing timed out", done.get());
    }
    @Test public void captureFreePanels() throws Exception {
        assertEquals("1.2.0", BuildConfig.VERSION_NAME);
        File input = ImageAndExportTest.fixture(context, "listing-demo.jpg", 1200, 800);
        ExifInterface exif = new ExifInterface(input.getAbsolutePath());
        exif.setAttribute(ExifInterface.TAG_MAKE, "Synthetic demo camera");
        exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, "2026:10:17 12:00:00");
        exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, "0/1,0/1,0/1");
        exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "N"); exif.saveAttributes();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> activity.onActivityResult(44, Activity.RESULT_OK, new Intent().setData(Uri.fromFile(input))));
            ready(scenario, false);
            capture(scenario, "02-originals.png", activity -> (View) control(activity, "ShareSafe").getParent(), activity -> control(activity, "Clean photos"));
            scenario.onActivity(activity -> control(activity, "Export options · Change").performClick());
            capture(scenario, "03-options.png", activity -> (View) control(activity, "Export options · Hide").getParent());
            scenario.onActivity(activity -> {
                control(activity, "Export options · Hide").performClick(); control(activity, "Clean photos").performClick();
            }); ready(scenario, true);
            scenario.onActivity(activity -> {
                ExportSession s = (ExportSession) field(activity, "session");
                assertArrayEquals(new int[]{1, 1, 1}, s.privacyBefore.found);
                assertArrayEquals(new int[]{0, 0, 0}, s.result.privacyAfter.found);
                control(activity, "Privacy Check · Free").performClick();
            });
            capture(scenario, "01-metadata.png", activity -> (View) field(activity, "privacyCard"));
            capture(scenario, "04-sharing.png", activity -> (View) field(activity, "resultCard"));
        } finally { assertTrue(input.delete()); }
    }
    private File demoListing() throws Exception {
        Bitmap image = Bitmap.createBitmap(1200, 720, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(image); c.drawColor(Color.rgb(246, 239, 222));
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG); p.setColor(Color.rgb(202, 219, 200));
        c.drawRoundRect(50, 50, 1150, 430, 24, 24, p);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(12); p.setColor(Color.rgb(33, 62, 53));
        c.drawCircle(370, 300, 95, p); c.drawCircle(820, 300, 95, p);
        c.drawLine(370, 300, 510, 300, p); c.drawLine(510, 300, 445, 145, p);
        c.drawLine(445, 145, 370, 300, p); c.drawLine(445, 145, 725, 145, p);
        c.drawLine(725, 145, 510, 300, p); c.drawLine(725, 145, 820, 300, p);
        c.drawLine(725, 145, 710, 95, p); c.drawLine(710, 95, 775, 95, p);
        c.drawLine(445, 145, 425, 100, p); c.drawLine(385, 100, 465, 100, p);
        p.setStyle(Paint.Style.FILL); p.setTextSize(45); p.setTypeface(android.graphics.Typeface.create("sans-serif-medium", 0));
        c.drawText("City bicycle", 65, 500, p);
        p.setTextSize(34); c.drawText("Collection: 24 Example Lane", 65, 570, p);
        p.setTextSize(28); p.setColor(Color.rgb(92, 110, 103));
        c.drawText("Illustrative listing · Fictional address", 65, 652, p);
        File file = new File(context.getCacheDir(), "fictional-listing.jpg");
        try (FileOutputStream out = new FileOutputStream(file)) { assertTrue(image.compress(Bitmap.CompressFormat.JPEG, 95, out)); }
        finally { image.recycle(); }
        return file;
    }
    private static void editorReady(ActivityScenario<RedactActivity> scenario) throws Exception {
        AtomicBoolean done = new AtomicBoolean();
        for (int i = 0; i < 200 && !done.get(); i++) {
            scenario.onActivity(activity -> {
                EditorSession s = (EditorSession) field(activity, "session"); done.set(s.ready && !s.previewPending);
            }); Thread.sleep(50);
        }
        assertTrue("Editor timed out", done.get());
    }
    @Test public void captureActualProCover() throws Exception {
        File file = demoListing();
        try (ActivityScenario<RedactActivity> scenario = ActivityScenario.launch(new Intent(context, RedactActivity.class).setData(Uri.fromFile(file)))) {
            editorReady(scenario);
            capture(scenario, "05-pro-before.png", activity -> (View) ((View) field(activity, "editor")).getParent());
            scenario.onActivity(activity -> {
                EditorSession s = (EditorSession) field(activity, "session");
                assertEquals(RedactionRenderer.COVER, s.tool);
                s.add(new RectF(.185f, .725f, .72f, .82f));
            }); editorReady(scenario);
            scenario.onActivity(activity -> {
                EditorSession s = (EditorSession) field(activity, "session");
                assertEquals(1, s.masks.size());
                assertEquals(Color.BLACK, s.preview.getPixel((int)(s.preview.getWidth() * .4), (int)(s.preview.getHeight() * .77)));
            });
            capture(scenario, "05-pro-after.png", activity -> (View) ((View) field(activity, "editor")).getParent(),
                    activity -> control(activity, "Find faces"));
        } finally { assertTrue(file.delete()); }
    }
}
