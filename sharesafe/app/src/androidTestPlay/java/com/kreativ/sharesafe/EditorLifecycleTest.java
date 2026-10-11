package com.kreativ.sharesafe;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import java.io.File;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class EditorLifecycleTest {
    private final Context context = ApplicationProvider.getApplicationContext();
    private static EditorSession session(RedactActivity activity) {
        try { Field field = RedactActivity.class.getDeclaredField("session"); field.setAccessible(true); return (EditorSession) field.get(activity); }
        catch (Exception e) { throw new AssertionError(e); }
    }
    private static void waitReady(ActivityScenario<RedactActivity> scenario) throws Exception {
        long deadline = System.currentTimeMillis() + 10_000;
        AtomicBoolean ready = new AtomicBoolean();
        while (System.currentTimeMillis() < deadline) {
            scenario.onActivity(activity -> ready.set(session(activity).ready && !session(activity).previewPending));
            if (ready.get()) return; Thread.sleep(30);
        }
        fail("Editor did not become ready");
    }
    @Test public void keepsMasksToolAndPreviewAcrossActivityRecreation() throws Exception {
        File input = ImageAndExportTest.fixture(context, "editor-rotation.jpg", 200, 100);
        try (ActivityScenario<RedactActivity> scenario = ActivityScenario.launch(new Intent(context, RedactActivity.class).setData(Uri.fromFile(input)))) {
            waitReady(scenario);
            AtomicReference<EditorSession> before = new AtomicReference<>();
            scenario.onActivity(activity -> { EditorSession session = session(activity); before.set(session); assertEquals(RedactionRenderer.COVER, session.tool); session.add(new RectF(.2f, .2f, .8f, .8f)); });
            waitReady(scenario); scenario.recreate(); waitReady(scenario);
            scenario.onActivity(activity -> {
                EditorSession session = session(activity); assertSame(before.get(), session); assertEquals(1, session.masks.size());
                assertEquals(Color.BLACK, session.preview.getPixel(session.preview.getWidth() / 2, session.preview.getHeight() / 2));
            });
        } finally { input.delete(); }
    }
    @Test public void restoresMasksAndViewStateFromBundleAfterProcessRecreation() throws Exception {
        File input = ImageAndExportTest.fixture(context, "editor-restoration.jpg", 200, 100);
        AtomicReference<EditorSession> restored = new AtomicReference<>(); Bundle saved = new Bundle();
        try (ActivityScenario<RedactActivity> scenario = ActivityScenario.launch(new Intent(context, RedactActivity.class).setData(Uri.fromFile(input)))) {
            waitReady(scenario);
            scenario.onActivity(activity -> {
                EditorSession session = session(activity); session.add(new RectF(.2f, .2f, .8f, .8f));
                session.zoom = 3; session.panX = .2f; session.moveMode = true; session.save(saved);
                restored.set(new EditorSession(context, Uri.fromFile(input), saved));
            });
            long deadline = System.currentTimeMillis() + 10_000; AtomicBoolean ready = new AtomicBoolean();
            while (System.currentTimeMillis() < deadline) {
                InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> ready.set(restored.get().ready));
                if (ready.get()) break; Thread.sleep(30);
            }
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                EditorSession session = restored.get(); assertTrue(session.ready); assertEquals(1, session.masks.size());
                assertEquals(3, session.zoom, .001f); assertTrue(session.moveMode);
                assertEquals(Color.BLACK, session.preview.getPixel(100, 50)); session.close();
            });
        } finally { input.delete(); }
    }
    @Test public void changesAreFrozenDuringExport() throws Exception {
        File input = ImageAndExportTest.fixture(context, "editor-export.jpg", 200, 100);
        try (ActivityScenario<RedactActivity> scenario = ActivityScenario.launch(new Intent(context, RedactActivity.class).setData(Uri.fromFile(input)))) {
            waitReady(scenario); scenario.onActivity(activity -> session(activity).add(new RectF(.2f, .2f, .8f, .8f))); waitReady(scenario);
            // Exercise guards in the same UI turn, before the worker can deliver completion.
            scenario.onActivity(activity -> {
                EditorSession session = session(activity); session.export(); assertTrue(session.busy);
                session.reset(); session.undo(); session.add(new RectF(0, 0, .1f, .1f));
                assertEquals(1, session.masks.size());
            });
        } finally { input.delete(); }
    }
}
