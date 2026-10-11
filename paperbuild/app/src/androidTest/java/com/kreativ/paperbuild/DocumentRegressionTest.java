package com.kreativ.paperbuild;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import androidx.exifinterface.media.ExifInterface;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font;
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import com.tom_roush.pdfbox.pdmodel.interactive.action.PDActionURI;
import com.tom_roush.pdfbox.text.PDFTextStripper;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class DocumentRegressionTest {
    private Context context;
    private DocumentProcessor processor;
    private final ArrayList<File> files = new ArrayList<>();
    @Before public void setup() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.getSharedPreferences("paperbuild_draft", Context.MODE_PRIVATE).edit().clear().commit();
        processor = new DocumentProcessor(context);
    }
    @After public void cleanup() { for (File file : files) file.delete(); }
    private File track(File file) { files.add(file); return file; }

    private File fixture(String label) throws Exception {
        File file = track(File.createTempFile("test-", ".pdf", context.getCacheDir()));
        try (PDDocument pdf = new PDDocument()) {
            for (int i = 0; i < 2; i++) {
                PDPage page = new PDPage(i == 0 ? new PDRectangle(400, 600) : new PDRectangle(700, 400));
                if (i == 1) page.setRotation(90);
                pdf.addPage(page);
                try (PDPageContentStream stream = new PDPageContentStream(pdf, page)) {
                    stream.beginText(); stream.setFont(PDType1Font.HELVETICA, 16); stream.newLineAtOffset(40, 200);
                    stream.showText(label + " page " + (i + 1)); stream.endText();
                }
                PDAnnotationLink link = new PDAnnotationLink(); link.setRectangle(new PDRectangle(40, 180, 120, 30));
                PDActionURI action = new PDActionURI(); action.setURI("https://example.com/" + label); link.setAction(action);
                page.getAnnotations().add(link);
            }
            pdf.save(file);
        }
        return file;
    }
    @Test public void mergePreservesTextSizesRotationAndExternalLinks() throws Exception {
        File first = fixture("Alpha"), second = fixture("Beta");
        File merged = track(processor.merge(Arrays.asList(Uri.fromFile(first), Uri.fromFile(second)), value -> { }));
        try (PDDocument pdf = PDDocument.load(merged)) {
            assertEquals(4, pdf.getNumberOfPages());
            String text = new PDFTextStripper().getText(pdf);
            assertTrue(text.contains("Alpha page 1")); assertTrue(text.contains("Beta page 2"));
            assertEquals(400, pdf.getPage(0).getMediaBox().getWidth(), .01f);
            assertEquals(700, pdf.getPage(1).getMediaBox().getWidth(), .01f);
            assertEquals(90, pdf.getPage(1).getRotation());
            assertFalse(pdf.getPage(0).getResources().getXObjectNames().iterator().hasNext());
            PDAnnotationLink link = (PDAnnotationLink) pdf.getPage(0).getAnnotations().get(0);
            assertEquals("https://example.com/Alpha", ((PDActionURI) link.getAction()).getURI());
        }
    }
    @Test public void extractionPreservesSelectedPageAndRejectsOutOfRange() throws Exception {
        File source = fixture("Gamma");
        File result = track(processor.split(Uri.fromFile(source), 2, 2));
        try (PDDocument pdf = PDDocument.load(result)) {
            assertEquals(1, pdf.getNumberOfPages());
            String text = new PDFTextStripper().getText(pdf);
            assertTrue(text.contains("Gamma page 2")); assertFalse(text.contains("Gamma page 1"));
            assertEquals(700, pdf.getPage(0).getMediaBox().getWidth(), .01f);
            assertEquals(90, pdf.getPage(0).getRotation());
            assertEquals(1, pdf.getPage(0).getAnnotations().size());
        }
        try { processor.split(Uri.fromFile(source), 1, 3); fail("Out-of-range input must fail, not silently truncate"); }
        catch (IOException expected) { assertTrue(expected.getMessage().contains("2 pages")); }
        try (PDDocument untouched = PDDocument.load(source)) { assertEquals(2, untouched.getNumberOfPages()); }
    }
    private File image(int width, int height) throws Exception {
        File file = track(File.createTempFile("image-", ".jpg", context.getCacheDir()));
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
            bitmap.setPixel(x, y, Color.rgb((x * 31 + y * 7) % 256, (x * 9 + y * 19) % 256, (x * 13 + y * 23) % 256));
        }
        try (FileOutputStream stream = new FileOutputStream(file)) { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream); }
        bitmap.recycle(); return file;
    }
    @Test public void compactExportsAreSmallerAndPageGeometryIsCorrect() throws Exception {
        File image = image(1800, 1200);
        PageItem page = new PageItem(Uri.fromFile(image), "Test");
        File standard = track(processor.exportImages(Collections.singletonList(page), "Normal", 595, 842, 0, false, value -> { }));
        File compact = track(processor.exportImages(Collections.singletonList(page), "Compact", 595, 842, 0, true, value -> { }));
        assertTrue(compact.length() < standard.length());
        try (PDDocument pdf = PDDocument.load(compact)) {
            assertEquals(1, pdf.getNumberOfPages()); assertEquals(595, pdf.getPage(0).getMediaBox().getWidth(), .01f);
            assertEquals(842, pdf.getPage(0).getMediaBox().getHeight(), .01f);
        }
    }
    @Test public void handlesMirroredExifAndGrayscale() throws Exception {
        File file = track(File.createTempFile("mirror-", ".png", context.getCacheDir()));
        Bitmap original = Bitmap.createBitmap(40, 20, Bitmap.Config.ARGB_8888);
        for (int y = 0; y < 20; y++) for (int x = 0; x < 40; x++) original.setPixel(x, y, x < 20 ? Color.RED : Color.BLUE);
        try (FileOutputStream stream = new FileOutputStream(file)) { original.compress(Bitmap.CompressFormat.PNG, 100, stream); }
        original.recycle();
        ExifInterface exif = new ExifInterface(file); exif.setAttribute(ExifInterface.TAG_ORIENTATION, "2"); exif.saveAttributes();
        Bitmap mirrored = processor.loadImage(new PageItem(Uri.fromFile(file), "Mirror"), 240, 0);
        assertEquals(Color.BLUE, mirrored.getPixel(0, 0)); assertEquals(Color.RED, mirrored.getPixel(39, 0)); mirrored.recycle();
        Bitmap gray = processor.loadImage(new PageItem(Uri.fromFile(file), "Gray", 90), 240, 1);
        assertEquals(20, gray.getWidth()); assertEquals(40, gray.getHeight());
        int pixel = gray.getPixel(0, 0); assertEquals(Color.red(pixel), Color.green(pixel)); assertEquals(Color.green(pixel), Color.blue(pixel)); gray.recycle();
    }
    @Test public void privateImageCopySurvivesOriginalRemovalAndDraftRestore() throws Exception {
        DocumentSession draft = new DocumentSession(context);
        draft.worker.submit(() -> { }).get(10, TimeUnit.SECONDS);
        File source = image(80, 60);
        PageItem copied = processor.importImage(Uri.fromFile(source), "Original");
        track(new File(copied.uri.getPath()));
        assertTrue(source.delete());
        draft.pages.add(new PageItem(copied.uri, copied.name, 270));
        draft.name = "Recovered name"; draft.paper = 1; draft.enhancement = 2; draft.compression = 1;
        draft.pendingCapture = track(processor.capture());
        draft.save();
        DocumentSession restored = new DocumentSession(context);
        restored.worker.submit(() -> { }).get(10, TimeUnit.SECONDS);
        assertEquals("Recovered name", restored.name); assertEquals(1, restored.pages.size());
        assertEquals(270, restored.pages.get(0).rotation); assertEquals(1, restored.paper);
        assertEquals(2, restored.enhancement); assertEquals(1, restored.compression);
        assertEquals(draft.pendingCapture, restored.pendingCapture);
        Bitmap preview = processor.loadImage(restored.pages.get(0), 240, 0); assertNotNull(preview); preview.recycle();
        draft.worker.shutdown(); restored.worker.shutdown();
    }
    @Test public void invalidInputIsCleanedAndNoInternetPermissionIsGranted() throws Exception {
        File invalid = track(File.createTempFile("bad-", ".pdf", context.getCacheDir()));
        try { processor.merge(Arrays.asList(Uri.fromFile(invalid), Uri.fromFile(invalid)), value -> { }); fail("Malformed PDF accepted"); }
        catch (IOException expected) { }
        File scratch = new File(context.getCacheDir(), "paperbuild/scratch");
        assertEquals(0, scratch.listFiles().length);
        assertEquals(PackageManager.PERMISSION_DENIED, context.checkSelfPermission("android.permission.INTERNET"));
        assertEquals(PackageManager.PERMISSION_DENIED, context.checkSelfPermission("android.permission.CAMERA"));
    }
}
