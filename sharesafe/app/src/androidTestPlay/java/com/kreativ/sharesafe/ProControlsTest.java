package com.kreativ.sharesafe;

import android.content.Context;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.File;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ProControlsTest {
    private final Context context = ApplicationProvider.getApplicationContext();
    private static void main(Runnable action) { InstrumentationRegistry.getInstrumentation().runOnMainSync(action); }
    private static void ready(EditorSession session) throws Exception {
        long deadline = System.currentTimeMillis() + 10_000; AtomicBoolean ready = new AtomicBoolean();
        while (System.currentTimeMillis() < deadline) {
            main(() -> ready.set(session.ready && !session.previewPending && !session.detecting));
            if (ready.get()) return; Thread.sleep(20);
        }
        fail("Editor operation timed out");
    }

    @Test public void editDeleteUndoRedoAndResetAreGroupedAndRestored() throws Exception {
        File input = ImageAndExportTest.fixture(context, "pro-history.jpg", 200, 100);
        EditorSession session = new EditorSession(context, Uri.fromFile(input), null); EditorSession[] restored = new EditorSession[1];
        try {
            ready(session);
            main(() -> {
                session.add(new RectF(.1f, .1f, .3f, .3f)); session.add(new RectF(.6f, .6f, .9f, .9f));
                session.updateMask(0, new RectF(.2f, .2f, .5f, .5f));
                assertEquals(.2f, session.masks.get(0).area.left, .001f); assertEquals(0, session.selectedMask);
                session.deleteSelected(); assertEquals(1, session.masks.size());
                session.undo(); assertEquals(2, session.masks.size()); assertEquals(.2f, session.masks.get(0).area.left, .001f);
                session.undo(); assertEquals(.1f, session.masks.get(0).area.left, .001f);
                session.redo(); assertEquals(.2f, session.masks.get(0).area.left, .001f);
                Bundle saved = new Bundle(); session.save(saved); restored[0] = new EditorSession(context, Uri.fromFile(input), saved);
            });
            ready(restored[0]);
            main(() -> {
                restored[0].redo(); assertEquals(1, restored[0].masks.size());
                restored[0].undo(); assertEquals(2, restored[0].masks.size());
                restored[0].reset(); assertTrue(restored[0].masks.isEmpty()); restored[0].undo(); assertEquals(2, restored[0].masks.size());
                restored[0].add(new RectF(0, 0, .1f, .1f)); assertTrue(restored[0].redoHistory.isEmpty());
            });
        } finally { main(session::close); if (restored[0] != null) main(restored[0]::close); input.delete(); }
    }

    @Test public void faceSuggestionsRequireReviewApplySolidCoversAndUndoTogether() throws Exception {
        File input = ImageAndExportTest.fixture(context, "face-suggestions.jpg", 200, 100);
        EditorSession session = new EditorSession(context, Uri.fromFile(input), null,
                bitmap -> Arrays.asList(new RectF(.1f, .1f, .3f, .4f), new RectF(.6f, .1f, .9f, .4f)));
        try {
            ready(session); main(session::findFaces); ready(session);
            main(() -> {
                assertEquals(2, session.faceSuggestions.size()); assertTrue(session.masks.isEmpty());
                session.export(); assertFalse(session.busy); assertNull(session.outputPath);
                session.tool = RedactionRenderer.BLUR; session.applyFaceSuggestions();
                assertTrue(session.faceSuggestions.isEmpty()); assertEquals(2, session.masks.size());
                for (RedactionRenderer.Mask mask : session.masks) assertEquals(RedactionRenderer.COVER, mask.tool);
                session.undo(); assertTrue(session.masks.isEmpty()); session.redo(); assertEquals(2, session.masks.size());
            });
            ready(session);
            main(() -> assertEquals(android.graphics.Color.BLACK, session.preview.getPixel(40, 25)));
        } finally { main(session::close); input.delete(); }
    }

    @Test public void choicesSurviveProcessStateAndOnlySelectedFacesAreApplied() throws Exception {
        File input = ImageAndExportTest.fixture(context, "face-choices.jpg", 200, 100);
        EditorSession session = new EditorSession(context, Uri.fromFile(input), null,
                bitmap -> Arrays.asList(new RectF(.1f, .1f, .3f, .4f), new RectF(.6f, .1f, .9f, .4f)));
        EditorSession[] restored = new EditorSession[1];
        try {
            ready(session); main(session::findFaces); ready(session);
            main(() -> {
                session.faceChoices[0] = false; Bundle saved = new Bundle(); session.save(saved);
                restored[0] = new EditorSession(context, Uri.fromFile(input), saved);
            });
            ready(restored[0]);
            main(() -> {
                assertEquals(2, restored[0].faceSuggestions.size()); assertFalse(restored[0].faceChoices[0]);
                restored[0].applyFaceSuggestions(); assertEquals(1, restored[0].masks.size());
                assertEquals(.6f, restored[0].masks.get(0).area.left, .001f);
            });
        } finally { main(session::close); if (restored[0] != null) main(restored[0]::close); input.delete(); }
    }

    @Test public void detectionFreezesEditsAndCloseDoesNotRecycleBitmapUnderDetector() throws Exception {
        File input = ImageAndExportTest.fixture(context, "face-close.jpg", 200, 100);
        CountDownLatch started = new CountDownLatch(1), release = new CountDownLatch(1);
        AtomicBoolean validBitmap = new AtomicBoolean();
        EditorSession session = new EditorSession(context, Uri.fromFile(input), null, bitmap -> {
            started.countDown(); if (!release.await(5, TimeUnit.SECONDS)) throw new IllegalStateException();
            validBitmap.set(!bitmap.isRecycled()); return java.util.Collections.emptyList();
        });
        try {
            ready(session); main(() -> session.add(new RectF(.1f, .1f, .3f, .3f))); ready(session);
            main(session::findFaces); assertTrue(started.await(5, TimeUnit.SECONDS));
            main(() -> {
                assertTrue(session.detecting); session.deleteSelected(); session.undo(); session.redo(); session.reset(); session.export();
                session.updateMask(0, new RectF(.2f, .2f, .4f, .4f)); assertEquals(.1f, session.masks.get(0).area.left, .001f);
                session.close();
            });
            release.countDown(); assertTrue(session.worker.awaitTermination(5, TimeUnit.SECONDS)); assertTrue(validBitmap.get());
        } finally { release.countDown(); main(session::close); input.delete(); }
    }

    @Test public void emptyAndFailedDetectionKeepManualEditingAvailable() throws Exception {
        File input = ImageAndExportTest.fixture(context, "face-fallback.jpg", 200, 100);
        try {
            for (FaceAssist finder : new FaceAssist[] {bitmap -> java.util.Collections.emptyList(), bitmap -> { throw new java.io.IOException("Synthetic failure"); }}) {
                EditorSession session = new EditorSession(context, Uri.fromFile(input), null, finder);
                try {
                    ready(session); main(session::findFaces); ready(session);
                    main(() -> {
                        assertTrue(session.faceSuggestions.isEmpty()); assertFalse(session.status.isEmpty()); assertTrue(session.editable());
                        session.add(new RectF(.1f, .1f, .4f, .4f)); assertEquals(1, session.masks.size());
                    });
                } finally { main(session::close); }
            }
        } finally { input.delete(); }
    }

    @Test public void historyAndFaceCapacityAreBoundedWithoutPartialApplication() throws Exception {
        File input = ImageAndExportTest.fixture(context, "face-capacity.jpg", 200, 100);
        EditorSession session = new EditorSession(context, Uri.fromFile(input), null,
                bitmap -> Arrays.asList(new RectF(.1f, .1f, .3f, .4f), new RectF(.6f, .1f, .9f, .4f)));
        try {
            ready(session);
            main(() -> {
                for (int i = 0; i < 35; i++) session.add(new RectF(0, 0, .01f, .01f));
                assertEquals(20, session.undoHistory.size());
                for (int i = 0; i < 25; i++) session.undo(); assertEquals(15, session.masks.size());
                session.masks.clear();
                for (int i = 0; i < 199; i++) session.masks.add(new RedactionRenderer.Mask(new RectF(0, 0, .01f, .01f), RedactionRenderer.COVER));
            });
            ready(session); main(session::findFaces); ready(session);
            main(() -> {
                session.applyFaceSuggestions(); assertEquals(199, session.masks.size()); assertEquals(2, session.faceSuggestions.size());
                assertTrue(session.status.contains("200 areas")); session.discardFaceSuggestions(); assertTrue(session.faceSuggestions.isEmpty());
            });
        } finally { main(session::close); input.delete(); }
    }

    @Test public void movementResizePaddingAndInvalidAreasStayWithinImage() {
        RectF moved = MaskGeometry.move(new RectF(.2f, .3f, .6f, .8f), 2, -2);
        assertEquals(.6f, moved.left, .001f); assertEquals(0, moved.top, .001f); assertEquals(1, moved.right, .001f);
        for (int corner = 0; corner < 4; corner++) assertTrue(MaskGeometry.valid(MaskGeometry.resize(moved, corner, 2, -2)));
        RectF padded = FaceAssist.padded(new RectF(0, 0, 100, 100), 200, 100);
        assertNotNull(padded); assertEquals(0, padded.left, 0); assertEquals(1, padded.bottom, 0); assertTrue(padded.right > .5f);
        assertNull(FaceAssist.padded(new RectF(Float.NaN, 0, 100, 100), 200, 100));
        assertFalse(MaskGeometry.valid(new RectF(.3f, .3f, .2f, .8f)));
    }
}
