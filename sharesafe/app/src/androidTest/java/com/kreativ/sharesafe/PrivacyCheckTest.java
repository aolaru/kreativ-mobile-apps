package com.kreativ.sharesafe;

import android.content.Context;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.File;
import java.nio.file.Files;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class PrivacyCheckTest {
    private final Context context = ApplicationProvider.getApplicationContext();

    @Test public void reportsAvailableOriginalTagsAndVerifiesCleanCopyWithoutChangingOriginal() throws Exception {
        File input = ImageAndExportTest.fixture(context, "privacy-check.jpg", 180, 100);
        ExifInterface exif = new ExifInterface(input.getAbsolutePath());
        exif.setAttribute(ExifInterface.TAG_MAKE, "Synthetic camera");
        exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, "2026:10:15 10:20:30");
        exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, "44/1,0/1,0/1");
        exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "N"); exif.saveAttributes();
        byte[] before = Files.readAllBytes(input.toPath());
        ExportSession session = new ExportSession(context, null); ExportSession.Result result = null;
        try {
            assertArrayEquals(new int[] {1, 1, 1}, PrivacyCheck.inspect(context, Uri.fromFile(input)));
            result = session.export(Collections.singletonList(Uri.fromFile(input)), 95, false);
            assertNotNull(result.privacyAfter); assertEquals(1, result.privacyAfter.total);
            assertArrayEquals(new int[] {0, 0, 0}, result.privacyAfter.found);
            assertArrayEquals(new int[] {0, 0, 0}, result.privacyAfter.unknown);
            assertTrue(result.privacyAfter.summary(true).contains("not found in readable metadata"));
            assertArrayEquals(before, Files.readAllBytes(input.toPath()));
        } finally { session.close(); input.delete(); if (result != null) PrivateCache.delete(result.photos.get(0).getParentFile()); }
    }

    @Test public void missingCorruptAndUnsupportedInputsAreUnknownNotClean() throws Exception {
        File input = new File(context.getCacheDir(), "check-corrupt.jpg");
        File gif = new File(context.getCacheDir(), "check-unsupported.gif");
        try {
            Files.write(input.toPath(), new byte[] {(byte) 0xff, (byte) 0xd8, 0, 1});
            Files.write(gif.toPath(), android.util.Base64.decode("R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7", android.util.Base64.DEFAULT));
            for (File file : new File[] {input, gif, new File(context.getCacheDir(), "missing-check.jpg")}) {
                int[] fields = PrivacyCheck.inspect(context, Uri.fromFile(file));
                assertArrayEquals(new int[] {2, 2, 2}, fields); assertFalse(PrivacyCheck.verified(fields));
            }
        } finally { input.delete(); gif.delete(); }
    }

    @Test public void reportCountsAndSavedStateContainOnlyPresenceNotPrivateValues() {
        PrivacyCheck.Report report = new PrivacyCheck.Report();
        report.add(new int[] {1, 0, 1}); report.add(new int[] {0, 1, 0}); report.add(PrivacyCheck.unknown());
        assertEquals(3, report.total); assertArrayEquals(new int[] {1, 1, 1}, report.found);
        assertTrue(report.summary(false).contains("found in 1 of 3 · 1 unknown / unreadable"));
        Bundle saved = new Bundle(); report.save(saved, "check");
        PrivacyCheck.Report restored = PrivacyCheck.Report.restore(saved, "check");
        assertNotNull(restored); assertEquals(report.summary(false), restored.summary(false));
        assertEquals(1, saved.keySet().size()); assertEquals(7, saved.getIntArray("check").length);
    }

    @Test public void obsoleteScanCannotReplaceNewSelectionAndRestoredReportSurvives() throws Exception {
        File input = ImageAndExportTest.fixture(context, "scan-race.jpg", 100, 100);
        ExportSession session = new ExportSession(context, null);
        try {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                session.select(Collections.singletonList(Uri.fromFile(input))); session.select(Collections.emptyList());
            });
            CountDownLatch drained = new CountDownLatch(1); session.worker.execute(drained::countDown);
            assertTrue(drained.await(5, TimeUnit.SECONDS)); InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            assertNull(session.privacyBefore); assertFalse(session.checkingPrivacy);
            PrivacyCheck.Report report = new PrivacyCheck.Report(); report.add(new int[] {0, 0, 0});
            Bundle saved = new Bundle();
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> { session.privacyBefore = report; session.save(saved); });
            ExportSession restored = new ExportSession(context, saved);
            try { assertEquals(report.summary(false), restored.privacyBefore.summary(false)); }
            finally { restored.close(); }
        } finally { session.close(); input.delete(); }
    }
}
