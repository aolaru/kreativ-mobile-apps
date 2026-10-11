package com.kreativ.sharesafe;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import androidx.exifinterface.media.ExifInterface;
import android.net.Uri;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/** Only pixel data enters exports. Embedded metadata and source filenames are not copied. */
final class ImageProcessor {
    static final int MAX_SIDE = 4096;
    static final long MAX_PIXELS = 8_000_000;
    private ImageProcessor() { }

    static final class Decoded {
        final Bitmap bitmap;
        final boolean resized;
        Decoded(Bitmap bitmap, boolean resized) { this.bitmap = bitmap; this.resized = resized; }
    }

    static Decoded decode(Context context, Uri uri) throws IOException {
        return decode(context, uri, MAX_SIDE, MAX_PIXELS);
    }

    static Decoded decode(Context context, Uri uri, int maxSide, long maxPixels) throws IOException {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream input = context.getContentResolver().openInputStream(uri)) {
            BitmapFactory.decodeStream(input, null, bounds);
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw new IOException("Unsupported image");
        int sample = sampleSize(bounds.outWidth, bounds.outHeight, maxSide, maxPixels);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sample;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap;
        try (InputStream input = context.getContentResolver().openInputStream(uri)) {
            bitmap = BitmapFactory.decodeStream(input, null, options);
        }
        if (bitmap == null) throw new IOException("Unsupported image");
        try {
            Matrix transform = orientationMatrix(orientation(context, uri));
            if (!transform.isIdentity()) {
                Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), transform, true);
                if (rotated != bitmap) bitmap.recycle();
                bitmap = rotated;
            }
            // JPEG has no alpha: explicitly flatten transparent images onto white.
            if (bitmap.hasAlpha()) {
                Bitmap flattened = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(flattened);
                canvas.drawColor(Color.WHITE);
                canvas.drawBitmap(bitmap, 0, 0, null);
                bitmap.recycle();
                bitmap = flattened;
            }
            return new Decoded(bitmap, sample > 1);
        } catch (RuntimeException | OutOfMemoryError e) {
            bitmap.recycle();
            throw e;
        }
    }

    static int sampleSize(int width, int height) {
        return sampleSize(width, height, MAX_SIDE, MAX_PIXELS);
    }

    private static int sampleSize(int width, int height, int maxSide, long maxPixels) {
        int sample = 1;
        while (Math.max(width, height) / sample > maxSide
                || ((long) width / sample) * (height / sample) > maxPixels) sample *= 2;
        return sample;
    }

    static Matrix orientationMatrix(int orientation) {
        Matrix matrix = new Matrix();
        switch (orientation) {
            case 2: matrix.setScale(-1, 1); break;
            case 3: matrix.setRotate(180); break;
            case 4: matrix.setScale(1, -1); break;
            case 5: matrix.setRotate(90); matrix.postScale(-1, 1); break;
            case 6: matrix.setRotate(90); break;
            case 7: matrix.setRotate(-90); matrix.postScale(-1, 1); break;
            case 8: matrix.setRotate(-90); break;
            default: break;
        }
        return matrix;
    }

    private static int orientation(Context context, Uri uri) {
        try (InputStream input = new BufferedInputStream(context.getContentResolver().openInputStream(uri))) {
            return new ExifInterface(input).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        } catch (IOException | RuntimeException ignored) { return 1; }
    }
}
