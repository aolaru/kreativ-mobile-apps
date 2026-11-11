package com.kreativ.sharesafe;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/** Exercises the minified app through Android entry points and visible controls. */
@RunWith(AndroidJUnit4.class)
public class OptimizedReleaseWorkflowTest {
    private final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
    private final Context context = instrumentation.getTargetContext();

    private Intent activity(String name) {
        return new Intent().setClassName(context, "com.kreativ.sharesafe." + name)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    private static View find(View view, String prefix) {
        if (view instanceof TextView && ((TextView) view).getText().toString().startsWith(prefix)) return view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View result = find(group.getChildAt(i), prefix);
                if (result != null) return result;
            }
        }
        return null;
    }

    private static View control(Activity activity, String text) {
        View view = find(activity.findViewById(android.R.id.content), text);
        assertNotNull("Missing control: " + text, view);
        return view;
    }

    private static void awaitEnabled(ActivityScenario<Activity> scenario, String text, long timeoutMs) {
        AtomicBoolean ready = new AtomicBoolean();
        long deadline = SystemClock.uptimeMillis() + timeoutMs;
        while (!ready.get() && SystemClock.uptimeMillis() < deadline) {
            scenario.onActivity(activity -> {
                View view = find(activity.findViewById(android.R.id.content), text);
                ready.set(view != null && view.isShown() && view.isEnabled());
            });
            if (!ready.get()) SystemClock.sleep(50);
        }
        assertTrue("Control did not become available: " + text, ready.get());
    }

    private void tapDialogButton(String text) {
        long deadline = SystemClock.uptimeMillis() + 10000;
        while (SystemClock.uptimeMillis() < deadline) {
            AccessibilityNodeInfo root = instrumentation.getUiAutomation().getRootInActiveWindow();
            if (root != null) for (AccessibilityNodeInfo node : root.findAccessibilityNodeInfosByText(text)) {
                // Platform dialog buttons may expose their all-caps display label.
                if (node.getText() != null && text.equalsIgnoreCase(node.getText().toString())
                        && node.isEnabled() && node.isClickable()
                        && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return;
            }
            SystemClock.sleep(50);
        }
        fail("Dialog button not available: " + text);
    }

    private File photo() throws Exception {
        File file = File.createTempFile("optimized-workflow-", ".jpg", context.getCacheDir());
        Bitmap bitmap = Bitmap.createBitmap(160, 80, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(Color.rgb(38, 100, 80));
        try (FileOutputStream out = new FileOutputStream(file)) {
            assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out));
        } finally { bitmap.recycle(); }
        ExifInterface exif = new ExifInterface(file.getAbsolutePath());
        exif.setAttribute(ExifInterface.TAG_MAKE, "ShareSafe test camera");
        exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, "12/1,0/1,0/1");
        exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "N");
        exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, "23/1,0/1,0/1");
        exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "E");
        exif.saveAttributes();
        return file;
    }

    @Test public void cleanPhotoAndShareMetadataFreeCopy() throws Exception {
        File original = photo();
        AtomicReference<Intent> shared = new AtomicReference<>();
        Instrumentation.ActivityMonitor monitor = new Instrumentation.ActivityMonitor() {
            @Override public Instrumentation.ActivityResult onStartActivity(Intent intent) {
                if (!Intent.ACTION_CHOOSER.equals(intent.getAction())) return null;
                Intent nested = intent.getParcelableExtra(Intent.EXTRA_INTENT);
                if (nested == null) return null;
                if (Intent.ACTION_OPEN_DOCUMENT.equals(nested.getAction())) {
                    return new Instrumentation.ActivityResult(Activity.RESULT_OK,
                            new Intent().setData(Uri.fromFile(original)));
                }
                if (Intent.ACTION_SEND.equals(nested.getAction())) {
                    shared.set(new Intent(nested));
                    return new Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null);
                }
                return null;
            }
        };
        instrumentation.addMonitor(monitor);
        try (ActivityScenario<Activity> scenario = ActivityScenario.launch(activity("MainActivity"))) {
            scenario.onActivity(a -> assertTrue(control(a, "Choose photos").performClick()));
            awaitEnabled(scenario, "Clean photos", 15000);
            scenario.onActivity(a -> assertTrue(control(a, "Clean photos").performClick()));
            awaitEnabled(scenario, "Share cleaned photos", 30000);
            scenario.recreate();
            awaitEnabled(scenario, "Share cleaned photos", 15000);
            scenario.onActivity(a -> assertTrue(control(a, "Share cleaned photos").performClick()));
            Intent send = shared.get();
            assertNotNull("Share chooser was not launched", send);
            assertEquals("image/jpeg", send.getType());
            assertNotEquals(0, send.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION);
            assertEquals(0, send.getFlags() & Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            Uri output = send.getParcelableExtra(Intent.EXTRA_STREAM);
            assertNotNull(output);
            assertEquals(context.getPackageName() + ".fileprovider", output.getAuthority());
            try (InputStream stream = context.getContentResolver().openInputStream(output)) {
                assertNotNull(stream);
                ExifInterface cleaned = new ExifInterface(stream);
                assertNull(cleaned.getAttribute(ExifInterface.TAG_MAKE));
                assertNull(cleaned.getAttribute(ExifInterface.TAG_GPS_LATITUDE));
                assertNull(cleaned.getAttribute(ExifInterface.TAG_GPS_LONGITUDE));
            }
            assertEquals("ShareSafe test camera", new ExifInterface(original.getAbsolutePath()).getAttribute(ExifInterface.TAG_MAKE));
        } finally {
            instrumentation.removeMonitor(monitor);
            assertTrue(original.delete());
        }
    }

    @Test public void bundledFaceDetectorFindsFacesThroughEditor() throws Exception {
        File fixture = File.createTempFile("optimized-face-", ".jpg", context.getCacheDir());
        try {
            try (InputStream input = instrumentation.getContext().getAssets().open("face-assist-nasa.jpg");
                 FileOutputStream out = new FileOutputStream(fixture)) {
                byte[] buffer = new byte[8192]; int count;
                while ((count = input.read(buffer)) != -1) out.write(buffer, 0, count);
            }
            Intent intent = activity("RedactActivity").setData(Uri.fromFile(fixture));
            try (ActivityScenario<Activity> scenario = ActivityScenario.launchActivityForResult(intent)) {
                awaitEnabled(scenario, "Find faces · Pro", 30000);
                scenario.onActivity(a -> assertTrue(control(a, "Find faces · Pro").performClick()));
                awaitEnabled(scenario, "Review ", 60000);
                scenario.onActivity(a -> {
                    View review = control(a, "Review ");
                    assertTrue(((TextView) review).getText().toString().contains("face suggestion(s)"));
                    assertTrue(review.performClick());
                });
                tapDialogButton("Apply selected covers");
                awaitEnabled(scenario, "Create redacted copy", 30000);
                scenario.onActivity(a -> assertTrue(control(a, "Undo").performClick()));
                awaitEnabled(scenario, "Redo", 15000);
                scenario.onActivity(a -> assertTrue(control(a, "Redo").performClick()));
                awaitEnabled(scenario, "Create redacted copy", 15000);
                scenario.recreate();
                awaitEnabled(scenario, "Create redacted copy", 15000);
                scenario.onActivity(a -> assertTrue(control(a, "Create redacted copy").performClick()));
                Instrumentation.ActivityResult result = scenario.getResult();
                assertEquals(Activity.RESULT_OK, result.getResultCode());
                String path = result.getResultData().getStringExtra("com.kreativ.sharesafe.REDACTED_PATH");
                assertNotNull(path);
                assertNotEquals(fixture.getAbsolutePath(), path);
                Bitmap copy = BitmapFactory.decodeFile(path);
                assertNotNull("Flattened face-cover JPEG is unreadable", copy);
                copy.recycle();
            }
        } finally { assertTrue(fixture.delete()); }
    }

    @Test public void durableBillingWorkerRetainsFrameworkConstructor() throws Exception {
        // WorkManager restores these names from persisted jobs and constructs by reflection.
        Class<?> worker = Class.forName("com.kreativ.sharesafe.AcknowledgementWorker");
        boolean found = false;
        for (java.lang.reflect.Constructor<?> constructor : worker.getConstructors()) {
            Class<?>[] parameters = constructor.getParameterTypes();
            if (parameters.length == 2 && parameters[0] == Context.class) found = true;
        }
        assertTrue("Public WorkManager constructor was removed", found);
    }
}
