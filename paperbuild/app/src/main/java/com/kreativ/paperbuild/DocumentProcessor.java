package com.kreativ.paperbuild;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import androidx.exifinterface.media.ExifInterface;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.io.MemoryUsageSetting;
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory;
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory;
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject;
import com.tom_roush.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission;
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** All document work runs on a worker with application context and disk-backed PDF storage. */
final class DocumentProcessor {
    interface Progress { void update(int percent); }
    private final Context context;

    DocumentProcessor(Context context) {
        this.context = context.getApplicationContext();
        PDFBoxResourceLoader.init(this.context);
    }

    private MemoryUsageSetting memory() throws IOException {
        File scratch = folder(new File(context.getCacheDir(), "paperbuild/scratch"));
        return MemoryUsageSetting.setupTempFileOnly().setTempDir(scratch);
    }

    File output(String title) throws IOException {
        return File.createTempFile(safeName(title) + "-", ".pdf",
                folder(new File(context.getCacheDir(), "paperbuild")));
    }

    File capture() throws IOException {
        return File.createTempFile("scan-", ".jpg", pagesFolder());
    }

    File pagesFolder() throws IOException {
        return folder(new File(context.getFilesDir(), "paperbuild/pages"));
    }

    PageItem importImage(Uri uri, String name) throws IOException {
        File copy = File.createTempFile("page-", ".image", pagesFolder());
        boolean complete = false;
        try {
            copy(uri, copy);
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(copy.getPath(), bounds);
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                throw new IOException("This image format cannot be read. Try a JPEG or PNG.");
            }
            complete = true;
            return new PageItem(Uri.fromFile(copy), name);
        } finally {
            if (!complete) copy.delete();
        }
    }

    File exportImages(List<PageItem> pages, String title, int width, int height,
                      int enhancement, boolean compact, Progress progress) throws IOException {
        return exportImages(pages, title, width, height, enhancement, compact, ExportOptions.NONE, progress);
    }
    File exportImages(List<PageItem> pages, String title, int width, int height,
                      int enhancement, boolean compact, ExportOptions options, Progress progress) throws IOException {
        options.validate();
        if (pages.isEmpty()) throw new IOException("Add a page before exporting.");
        File output = output(title);
        boolean complete = false;
        try (PDDocument pdf = new PDDocument(memory())) {
            for (int i = 0; i < pages.size(); i++) {
                Bitmap bitmap = loadImage(pages.get(i), compact ? 1440 : 2560, enhancement);
                if (bitmap == null) throw new IOException("A page could not be read. Add it again.");
                try {
                    PDPage page = new PDPage(new PDRectangle(width, height));
                    pdf.addPage(page);
                    float scale = Math.min((width - 60f) / bitmap.getWidth(), (height - 60f) / bitmap.getHeight());
                    float imageWidth = bitmap.getWidth() * scale;
                    float imageHeight = bitmap.getHeight() * scale;
                    try (PDPageContentStream stream = new PDPageContentStream(pdf, page)) {
                        stream.drawImage(JPEGFactory.createFromImage(pdf, bitmap, compact ? .65f : .90f),
                                (width - imageWidth) / 2f, (height - imageHeight) / 2f, imageWidth, imageHeight);
                    }
                } finally { bitmap.recycle(); }
                progress.update((i + 1) * 100 / pages.size());
            }
            finish(pdf, options); pdf.save(output);
            complete = true;
            return output;
        } finally { if (!complete) output.delete(); }
    }

    File merge(List<Uri> sources, Progress progress) throws IOException {
        return merge(sources, ExportOptions.NONE, progress);
    }
    File merge(List<Uri> sources, ExportOptions options, Progress progress) throws IOException {
        options.validate();
        if (sources.size() < 2) throw new IOException("Choose at least two PDFs.");
        File output = output("Merged PDF");
        boolean complete = false;
        try (PDDocument destination = new PDDocument(memory())) {
            PDFMergerUtility merger = new PDFMergerUtility();
            for (int i = 0; i < sources.size(); i++) {
                File input = temporaryPdf(sources.get(i));
                try (PDDocument source = PDDocument.load(input, memory())) {
                    checkPdf(source);
                    merger.appendDocument(destination, source);
                } finally { input.delete(); }
                progress.update((i + 1) * 100 / sources.size());
            }
            finish(destination, options); destination.save(output);
            complete = true;
            return output;
        } finally { if (!complete) output.delete(); }
    }

    File split(Uri uri, int first, int last) throws IOException {
        return split(uri, first, last, ExportOptions.NONE);
    }
    File split(Uri uri, int first, int last, ExportOptions options) throws IOException {
        options.validate();
        File input = temporaryPdf(uri);
        File output = output("Extracted pages");
        boolean complete = false;
        try (PDDocument source = PDDocument.load(input, memory())) {
            checkPdf(source);
            if (first < 1 || last < first || last > source.getNumberOfPages()) {
                throw new IOException("This PDF has " + source.getNumberOfPages() + " pages. Choose a range within it.");
            }
            // Keep the original page objects, resources, annotations, and geometry, without rasterizing.
            for (int i = source.getNumberOfPages() - 1; i >= 0; i--) {
                if (i < first - 1 || i >= last) source.removePage(i);
            }
            // Bookmarks/page labels can point to pages outside the extracted range.
            source.getDocumentCatalog().setDocumentOutline(null);
            source.getDocumentCatalog().setPageLabels(null);
            finish(source, options); source.save(output);
            complete = true;
            return output;
        } finally {
            input.delete();
            if (!complete) output.delete();
        }
    }

    private void finish(PDDocument pdf, ExportOptions options) throws IOException {
        PDImageXObject watermark = null;
        if (!options.watermark.isEmpty()) {
            // Native text rendering supports Unicode without bundling or downloading fonts.
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG); paint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            paint.setTextSize(100); paint.setColor(Color.rgb(75, 84, 105));
            float measured = Math.max(1, paint.measureText(options.watermark));
            float scale = Math.min(1, 2000f / measured); paint.setTextSize(100 * scale);
            int width = Math.max(1, (int) Math.ceil(paint.measureText(options.watermark)) + 16);
            Paint.FontMetrics metrics = paint.getFontMetrics();
            int height = (int) Math.ceil(metrics.bottom - metrics.top) + 16;
            Bitmap mark = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            try {
                new Canvas(mark).drawText(options.watermark, 8, 8 - metrics.top, paint);
                watermark = LosslessFactory.createFromImage(pdf, mark);
            } finally { mark.recycle(); }
        }
        int total = pdf.getNumberOfPages(), number = 0;
        for (PDPage page : pdf.getPages()) {
            number++;
            if (watermark == null && !options.pageNumbers) continue;
            PDRectangle box = page.getCropBox(); float w = box.getWidth(), h = box.getHeight();
            int rotation = ((page.getRotation() % 360) + 360) % 360;
            boolean sideways = rotation == 90 || rotation == 270;
            float visibleW = sideways ? h : w, visibleH = sideways ? w : h;
            try (PDPageContentStream stream = new PDPageContentStream(pdf, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                stream.saveGraphicsState();
                stream.transform(com.tom_roush.pdfbox.util.Matrix.getTranslateInstance(box.getLowerLeftX(), box.getLowerLeftY()));
                if (rotation == 90) stream.transform(new com.tom_roush.pdfbox.util.Matrix(0, 1, -1, 0, w, 0));
                else if (rotation == 180) stream.transform(new com.tom_roush.pdfbox.util.Matrix(-1, 0, 0, -1, w, h));
                else if (rotation == 270) stream.transform(new com.tom_roush.pdfbox.util.Matrix(0, -1, 1, 0, 0, h));
                if (watermark != null) {
                    stream.saveGraphicsState(); PDExtendedGraphicsState opacity = new PDExtendedGraphicsState();
                    opacity.setNonStrokingAlphaConstant(.20f); stream.setGraphicsStateParameters(opacity);
                    float markW = visibleW * .70f;
                    float markH = markW * watermark.getHeight() / watermark.getWidth();
                    if (markH > visibleH * .20f) { markW *= visibleH * .20f / markH; markH = visibleH * .20f; }
                    stream.drawImage(watermark, (visibleW - markW) / 2, (visibleH - markH) / 2, markW, markH);
                    stream.restoreGraphicsState();
                }
                if (options.pageNumbers) {
                    String text = number + " / " + total;
                    float size = Math.min(11, Math.min(visibleW, visibleH) / 20);
                    float textW = PDType1Font.HELVETICA.getStringWidth(text) * size / 1000;
                    stream.setNonStrokingColor(70, 80, 100); stream.beginText(); stream.setFont(PDType1Font.HELVETICA, size);
                    stream.newLineAtOffset((visibleW - textW) / 2, Math.min(18, visibleH * .04f));
                    stream.showText(text); stream.endText();
                }
                stream.restoreGraphicsState();
            }
        }
        if (options.protect) {
            StandardProtectionPolicy policy = new StandardProtectionPolicy(UUID.randomUUID().toString(), options.password, new AccessPermission());
            policy.setEncryptionKeyLength(256); policy.setPreferAES(true); pdf.protect(policy);
        }
    }

    private void checkPdf(PDDocument pdf) throws IOException {
        if (pdf.isEncrypted()) throw new IOException("Password-protected PDFs are not supported. Use an unlocked copy.");
        if (pdf.getNumberOfPages() == 0) throw new IOException("This PDF has no pages.");
        if (!pdf.getCurrentAccessPermission().canAssembleDocument()) {
            throw new IOException("This PDF does not allow page editing.");
        }
    }

    private File temporaryPdf(Uri uri) throws IOException {
        File input = File.createTempFile("input-", ".pdf", folder(new File(context.getCacheDir(), "paperbuild/scratch")));
        try { copy(uri, input); return input; }
        catch (IOException | RuntimeException error) { input.delete(); throw error; }
    }

    private void copy(Uri uri, File destination) throws IOException {
        try (InputStream input = context.getContentResolver().openInputStream(uri);
             FileOutputStream output = new FileOutputStream(destination)) {
            if (input == null) throw new IOException("The selected file is unavailable. Choose it again.");
            byte[] buffer = new byte[16384];
            int read;
            while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
        }
    }

    Bitmap loadImage(PageItem item, int maxSide, int enhancement) throws IOException {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream input = context.getContentResolver().openInputStream(item.uri)) {
            BitmapFactory.decodeStream(input, null, bounds);
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 1;
        while (Math.max(bounds.outWidth, bounds.outHeight) / options.inSampleSize > maxSide * 2) options.inSampleSize *= 2;
        Bitmap bitmap;
        try (InputStream input = context.getContentResolver().openInputStream(item.uri)) {
            bitmap = BitmapFactory.decodeStream(input, null, options);
        }
        if (bitmap == null) return null;
        try {
            int longest = Math.max(bitmap.getWidth(), bitmap.getHeight());
            if (longest > maxSide) {
                float scale = maxSide / (float) longest;
                Bitmap smaller = Bitmap.createScaledBitmap(bitmap, Math.max(1, Math.round(bitmap.getWidth() * scale)),
                        Math.max(1, Math.round(bitmap.getHeight() * scale)), true);
                if (smaller != bitmap) bitmap.recycle();
                bitmap = smaller;
            }
            Matrix matrix = orientation(item.uri);
            matrix.postRotate(item.rotation);
            if (!matrix.isIdentity()) {
                Bitmap oriented = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                if (oriented != bitmap) bitmap.recycle();
                bitmap = oriented;
            }
            if (!item.crop.full()) {
                int left = Math.min(bitmap.getWidth() - 1, Math.round(item.crop.left * bitmap.getWidth()));
                int top = Math.min(bitmap.getHeight() - 1, Math.round(item.crop.top * bitmap.getHeight()));
                int right = Math.max(left + 1, Math.min(bitmap.getWidth(), Math.round(item.crop.right * bitmap.getWidth())));
                int bottom = Math.max(top + 1, Math.min(bitmap.getHeight(), Math.round(item.crop.bottom * bitmap.getHeight())));
                Bitmap cropped = Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top);
                if (cropped != bitmap) bitmap.recycle(); bitmap = cropped;
            }
            // Flatten transparency onto white; process one scanline rather than a full-size pixel array.
            Bitmap adjusted = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(adjusted);
            canvas.drawColor(Color.WHITE);
            canvas.drawBitmap(bitmap, 0, 0, null);
            bitmap.recycle();
            bitmap = adjusted;
            if (enhancement != 0) {
                int[] row = new int[bitmap.getWidth()];
                for (int y = 0; y < bitmap.getHeight(); y++) {
                    bitmap.getPixels(row, 0, row.length, 0, y, row.length, 1);
                    for (int x = 0; x < row.length; x++) {
                        int pixel = row[x];
                        int luma = (Color.red(pixel) * 299 + Color.green(pixel) * 587 + Color.blue(pixel) * 114) / 1000;
                        if (enhancement == 2) luma = luma < 160 ? 0 : 255;
                        row[x] = Color.rgb(luma, luma, luma);
                    }
                    bitmap.setPixels(row, 0, row.length, 0, y, row.length, 1);
                }
            }
            return bitmap;
        } catch (RuntimeException | OutOfMemoryError error) { bitmap.recycle(); throw error; }
    }

    private Matrix orientation(Uri uri) {
        Matrix matrix = new Matrix();
        try (InputStream input = context.getContentResolver().openInputStream(uri)) {
            if (input == null) return matrix;
            int orientation = new ExifInterface(input).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_FLIP_HORIZONTAL: matrix.setScale(-1, 1); break;
                case ExifInterface.ORIENTATION_ROTATE_180: matrix.setRotate(180); break;
                case ExifInterface.ORIENTATION_FLIP_VERTICAL: matrix.setScale(1, -1); break;
                case ExifInterface.ORIENTATION_TRANSPOSE: matrix.setRotate(90); matrix.postScale(-1, 1); break;
                case ExifInterface.ORIENTATION_ROTATE_90: matrix.setRotate(90); break;
                case ExifInterface.ORIENTATION_TRANSVERSE: matrix.setRotate(270); matrix.postScale(-1, 1); break;
                case ExifInterface.ORIENTATION_ROTATE_270: matrix.setRotate(270); break;
                default: break;
            }
        } catch (IOException ignored) { }
        return matrix;
    }

    static String safeName(String raw) {
        String name = raw == null ? "" : raw.trim().replaceAll("[\\p{Cntrl}/\\\\:*?\"<>|]", "").replaceAll("\\s+", " ");
        if (name.isEmpty() || name.matches("\\.+")) name = "My document";
        return name.length() > 64 ? name.substring(0, 64).trim() : name;
    }

    private static File folder(File folder) throws IOException {
        if (!folder.isDirectory() && !folder.mkdirs()) throw new IOException("Could not prepare storage. Free some space and try again.");
        return folder;
    }
}
