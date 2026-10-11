package com.kreativ.sharesafe;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.zip.ZipFile;

@RunWith(AndroidJUnit4.class)
public class ImageAndExportTest {
    private final Context context = ApplicationProvider.getApplicationContext();

    static File fixture(Context context, String name, int width, int height) throws Exception {
        File folder = new File(context.getCacheDir(), "test-fixtures"); folder.mkdirs();
        File file = new File(folder, name);
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        try {
            Canvas canvas = new Canvas(bitmap); canvas.drawColor(Color.WHITE);
            Paint paint = new Paint(); paint.setColor(Color.RED); canvas.drawRect(0, 0, width / 2f, height / 2f, paint);
            paint.setColor(Color.GREEN); canvas.drawRect(width / 2f, 0, width, height / 2f, paint);
            paint.setColor(Color.BLUE); canvas.drawRect(0, height / 2f, width / 2f, height, paint);
            paint.setColor(Color.YELLOW); canvas.drawRect(width / 2f, height / 2f, width, height, paint);
            try (FileOutputStream stream = new FileOutputStream(file)) { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream); }
        } finally { bitmap.recycle(); }
        return file;
    }
    private static void near(int expected, int actual) {
        assertTrue("Red channel", Math.abs(Color.red(expected) - Color.red(actual)) < 25);
        assertTrue("Green channel", Math.abs(Color.green(expected) - Color.green(actual)) < 25);
        assertTrue("Blue channel", Math.abs(Color.blue(expected) - Color.blue(actual)) < 25);
    }

    @Test public void handlesAllEightExifOrientations() throws Exception {
        int[] topLeft = {Color.RED, Color.GREEN, Color.YELLOW, Color.BLUE, Color.RED, Color.BLUE, Color.YELLOW, Color.GREEN};
        for (int orientation = 1; orientation <= 8; orientation++) {
            File file = fixture(context, "orientation-" + orientation + ".jpg", 80, 40);
            try {
                ExifInterface exif = new ExifInterface(file.getAbsolutePath());
                exif.setAttribute(ExifInterface.TAG_ORIENTATION, Integer.toString(orientation)); exif.saveAttributes();
                Bitmap bitmap = ImageProcessor.decode(context, Uri.fromFile(file)).bitmap;
                try {
                    assertEquals(orientation >= 5 ? 40 : 80, bitmap.getWidth());
                    assertEquals(orientation >= 5 ? 80 : 40, bitmap.getHeight());
                    near(topLeft[orientation - 1], bitmap.getPixel(bitmap.getWidth() / 4, bitmap.getHeight() / 4));
                } finally { bitmap.recycle(); }
            } finally { file.delete(); }
        }
    }

    @Test public void removesMetadataAndSensitiveFilenamesWithoutChangingOriginal() throws Exception {
        File input = fixture(context, "passport-John-Smith-123.jpg", 160, 80);
        ExifInterface exif = new ExifInterface(input.getAbsolutePath());
        exif.setAttribute(ExifInterface.TAG_MAKE, "Private camera");
        exif.setAttribute(ExifInterface.TAG_DATETIME, "2026:10:10 12:00:00");
        exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, "44/1,24/1,0/1");
        exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "N");
        exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, "26/1,6/1,0/1");
        exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "E"); exif.saveAttributes();
        byte[] original = Files.readAllBytes(input.toPath());
        ExportSession session = new ExportSession(context, null);
        ExportSession.Result result = null;
        try {
            result = session.export(Collections.singletonList(Uri.fromFile(input)), 95, true);
            assertEquals(1, result.photos.size()); assertEquals("ShareSafe-photo-001.jpg", result.photos.get(0).getName());
            ExifInterface cleaned = new ExifInterface(result.photos.get(0).getAbsolutePath());
            assertNull(cleaned.getAttribute(ExifInterface.TAG_MAKE));
            assertNull(cleaned.getAttribute(ExifInterface.TAG_DATETIME)); assertFalse(cleaned.getLatLong(new float[2]));
            try (ZipFile zip = new ZipFile(result.zipFile)) {
                assertEquals(1, zip.size()); assertNotNull(zip.getEntry("ShareSafe-photo-001.jpg"));
            }
            assertArrayEquals(original, Files.readAllBytes(input.toPath()));
        } finally {
            session.close(); input.delete(); if (result != null) PrivateCache.delete(result.photos.get(0).getParentFile());
        }
    }

    @Test public void reportsSkippedFilesAndResizing() throws Exception {
        File input = fixture(context, "large.jpg", 5000, 16);
        ExportSession session = new ExportSession(context, null); ExportSession.Result result = null;
        try {
            result = session.export(Arrays.asList(Uri.fromFile(new File(input.getParentFile(), "missing.jpg")), Uri.fromFile(input)), 95, false);
            assertEquals(1, result.photos.size()); assertEquals(1, result.skipped); assertEquals(1, result.resized);
            assertTrue(result.summary().contains("Skipped / failed images: 1"));
            assertTrue(ImageProcessor.sampleSize(4096, 4096) > 1);
            assertEquals(1, ImageProcessor.sampleSize(2000, 1000));
        } finally { session.close(); input.delete(); if (result != null) PrivateCache.delete(result.photos.get(0).getParentFile()); }
    }

    @Test public void coverReplacesPixelsAndDoesNotModifyInput() {
        Bitmap input = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888); input.eraseColor(Color.RED);
        Bitmap output = RedactionRenderer.render(input, Collections.singletonList(new RedactionRenderer.Mask(new RectF(.2f, .2f, .8f, .8f), RedactionRenderer.COVER)));
        try { assertEquals(Color.BLACK, output.getPixel(50, 50)); assertEquals(Color.RED, output.getPixel(5, 5)); assertEquals(Color.RED, input.getPixel(50, 50)); }
        finally { input.recycle(); output.recycle(); }
    }

    @Test public void fullImageBlurAndPixelationDoNotRecycleInput() {
        Bitmap input = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888); input.eraseColor(Color.RED);
        try {
            for (int tool : new int[]{RedactionRenderer.BLUR, RedactionRenderer.PIXELATE}) {
                Bitmap output = RedactionRenderer.render(input, Collections.singletonList(new RedactionRenderer.Mask(new RectF(0, 0, 1, 1), tool)));
                assertFalse(input.isRecycled()); assertEquals(100, output.getWidth()); output.recycle();
            }
        } finally { input.recycle(); }
    }

    @Test public void transparentPixelsBecomeWhite() throws Exception {
        File file = new File(context.getCacheDir(), "transparent-test.png");
        Bitmap input = Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888);
        try (FileOutputStream stream = new FileOutputStream(file)) { input.compress(Bitmap.CompressFormat.PNG, 100, stream); }
        input.recycle();
        try { Bitmap bitmap = ImageProcessor.decode(context, Uri.fromFile(file)).bitmap; assertEquals(Color.WHITE, bitmap.getPixel(10, 10)); bitmap.recycle(); }
        finally { file.delete(); }
    }

    @Test public void gallerySavePublishesCompleteFileInShareSafeFolder() throws Exception {
        File input = fixture(context, "gallery-fixture.jpg", 80, 40);
        ExportSession session = new ExportSession(context, null); Uri saved = null;
        try {
            saved = session.savePhoto(input);
            try (android.database.Cursor cursor = context.getContentResolver().query(saved, null, null, null, null)) {
                assertNotNull(cursor); assertTrue(cursor.moveToFirst());
                if (android.os.Build.VERSION.SDK_INT >= 29) {
                    assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow(android.provider.MediaStore.Images.Media.IS_PENDING)));
                    assertEquals("Pictures/ShareSafe/", cursor.getString(cursor.getColumnIndexOrThrow(android.provider.MediaStore.Images.Media.RELATIVE_PATH)));
                }
            }
        } finally { if (saved != null) context.getContentResolver().delete(saved, null, null); input.delete(); session.close(); }
    }

    @Test public void restoresMainSelectionAndExportFromSavedState() throws Exception {
        File input = fixture(context, "state-fixture.jpg", 80, 40);
        ExportSession session = new ExportSession(context, null), restored = null; ExportSession.Result result = null;
        try {
            session.selected.add(Uri.fromFile(input)); result = session.export(session.selected, 95, false); session.result = result;
            Bundle saved = new Bundle(); session.save(saved); restored = new ExportSession(context, saved);
            assertEquals(1, restored.selected.size()); assertNotNull(restored.result); assertEquals(1, restored.result.photos.size());
        } finally { session.close(); if (restored != null) restored.close(); input.delete(); if (result != null) PrivateCache.delete(result.photos.get(0).getParentFile()); }
    }
}
