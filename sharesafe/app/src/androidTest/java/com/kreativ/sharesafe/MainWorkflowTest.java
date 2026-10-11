package com.kreativ.sharesafe;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class MainWorkflowTest {
    private final Context context = ApplicationProvider.getApplicationContext();
    private static ExportSession session(MainActivity activity) {
        try { Field field = MainActivity.class.getDeclaredField("session"); field.setAccessible(true); return (ExportSession) field.get(activity); }
        catch (Exception e) { throw new AssertionError(e); }
    }
    private static View find(View root, String text) {
        if (root instanceof android.widget.TextView && text.contentEquals(((android.widget.TextView) root).getText())) return root;
        if (root instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) root).getChildCount(); i++) {
            View found = find(((ViewGroup) root).getChildAt(i), text); if (found != null) return found;
        }
        return null;
    }
    private static View control(MainActivity activity, String text) {
        View view = find(activity.findViewById(android.R.id.content), text); assertNotNull("Missing control " + text, view); return view;
    }
    private static void waitExport(ActivityScenario<MainActivity> scenario) throws Exception {
        long deadline = System.currentTimeMillis() + 10_000; AtomicBoolean ready = new AtomicBoolean();
        while (System.currentTimeMillis() < deadline) {
            scenario.onActivity(activity -> ready.set(!session(activity).busy && session(activity).result != null));
            if (ready.get()) return; Thread.sleep(30);
        }
        fail("No completed export");
    }
    @Test public void selectedPhotosAndResultSurviveRotationAndAreFrozenDuringCleaning() throws Exception {
        File file = ImageAndExportTest.fixture(context, "main-workflow.jpg", 800, 400);
        AtomicReference<ExportSession> state = new AtomicReference<>();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                assertFalse(control(activity, "Clean photos").isEnabled());
                activity.onActivityResult(44, Activity.RESULT_OK, new Intent().setData(Uri.fromFile(file)));
                assertTrue(control(activity, "Clean photos").isEnabled());
                state.set(session(activity)); control(activity, "Clean photos").performClick();
                assertTrue(state.get().busy); assertFalse(control(activity, "Choose photos").isEnabled());
                state.get().select(Collections.emptyList()); assertEquals(1, state.get().selected.size());
            });
            scenario.recreate(); waitExport(scenario);
            scenario.onActivity(activity -> {
                assertSame(state.get(), session(activity)); assertEquals(1, session(activity).result.photos.size());
                assertTrue(control(activity, "Share cleaned photos").isEnabled()); assertTrue(control(activity, "Save to gallery").isEnabled());
            });
        } finally {
            file.delete(); if (state.get() != null && state.get().result != null) PrivateCache.delete(state.get().result.photos.get(0).getParentFile());
        }
    }
    @Test public void individualAndZipShareUseGeneratedCopiesAndReadOnlyUriGrants() throws Exception {
        File file = ImageAndExportTest.fixture(context, "private-name-share.jpg", 160, 80);
        AtomicReference<Intent> captured = new AtomicReference<>(); AtomicReference<ExportSession> state = new AtomicReference<>();
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Instrumentation.ActivityMonitor monitor = new Instrumentation.ActivityMonitor() {
            @Override public Instrumentation.ActivityResult onStartActivity(Intent intent) {
                if (Intent.ACTION_CHOOSER.equals(intent.getAction())) {
                    captured.set(intent.getParcelableExtra(Intent.EXTRA_INTENT)); return new Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null);
                }
                return null;
            }
        };
        instrumentation.addMonitor(monitor);
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> { activity.onActivityResult(44, Activity.RESULT_OK, new Intent().setData(Uri.fromFile(file))); control(activity, "Clean photos").performClick(); state.set(session(activity)); });
            waitExport(scenario);
            scenario.onActivity(activity -> control(activity, "Share cleaned photos").performClick());
            Intent send = captured.get(); assertNotNull(send); assertEquals("image/jpeg", send.getType());
            assertEquals(Intent.ACTION_SEND, send.getAction()); assertNotEquals(0, send.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION);
            assertEquals(0, send.getFlags() & Intent.FLAG_GRANT_WRITE_URI_PERMISSION); assertNotNull(send.getClipData());
            Uri uri = send.getParcelableExtra(Intent.EXTRA_STREAM); assertEquals(context.getPackageName() + ".fileprovider", uri.getAuthority());
            assertFalse(uri.toString().contains("private-name-share"));
            try (InputStream input = context.getContentResolver().openInputStream(uri)) { assertNotNull(input); assertEquals(0xff, input.read()); assertEquals(0xd8, input.read()); }
            PrivateCache.delete(state.get().result.photos.get(0).getParentFile()); captured.set(null);
            scenario.onActivity(activity -> { ((RadioButton) control(activity, "One ZIP file")).setChecked(true); control(activity, "Clean photos").performClick(); });
            waitExport(scenario);
            scenario.onActivity(activity -> control(activity, "Share ZIP").performClick());
            assertNotNull(captured.get()); assertEquals("application/zip", captured.get().getType()); assertNotNull(captured.get().getClipData());
        } finally {
            instrumentation.removeMonitor(monitor); file.delete();
            if (state.get() != null && state.get().result != null) PrivateCache.delete(state.get().result.photos.get(0).getParentFile());
        }
    }
}
