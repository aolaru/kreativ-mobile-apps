package com.kreativ.paperbuild;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font;
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException;
import com.tom_roush.pdfbox.rendering.PDFRenderer;
import com.tom_roush.pdfbox.text.PDFTextStripper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class Version11Test {
    private Context context;
    private DocumentProcessor processor;
    private final List<File> files = new ArrayList<>();
    private final List<PdfLibrary.Entry> entries = new ArrayList<>();
    private final List<String> presets = new ArrayList<>();
    private PdfLibrary library;
    @Before public void setup() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertTrue(context.getPackageName().endsWith(".qa")); processor = new DocumentProcessor(context); library = new PdfLibrary(context);
    }
    @After public void cleanup() throws Exception {
        for (File file : files) file.delete();
        for (PdfLibrary.Entry entry : entries) {
            try { library.setRemoved(entry, true); library.deleteForever(new PdfLibrary.Entry(entry.id, entry.title, entry.created, entry.pages, entry.protectedPdf, true)); }
            catch (java.io.IOException ignored) { }
        }
        for (String preset : presets) new PresetStore(context).remove(preset);
    }
    private File track(File file) { files.add(file); return file; }
    private File image() throws Exception {
        File file = track(File.createTempFile("v11-", ".png", context.getCacheDir()));
        Bitmap bitmap = Bitmap.createBitmap(800, 1100, Bitmap.Config.ARGB_8888); Canvas canvas = new Canvas(bitmap); canvas.drawColor(Color.WHITE);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG); paint.setColor(Color.rgb(24, 43, 67)); paint.setTextSize(46);
        canvas.drawText("PAPERBUILD", 65, 120, paint); paint.setTextSize(28); canvas.drawText("Project notes · sample document", 65, 180, paint);
        paint.setTextSize(23); canvas.drawText("A private PDF, ready to share.", 65, 270, paint);
        for (int i = 0; i < 7; i++) { paint.setColor(i % 2 == 0 ? 0xffedf0f7 : 0xfff7f8fb); canvas.drawRect(65, 350 + i * 60, 735, 400 + i * 60, paint); }
        paint.setColor(Color.RED); canvas.drawRect(0, 0, 20, 1100, paint);
        try (FileOutputStream output = new FileOutputStream(file)) { bitmap.compress(Bitmap.CompressFormat.PNG, 100, output); } bitmap.recycle(); return file;
    }
    @Test public void cropIsNonDestructiveAndSurvivesDraftRestoreAndRotation() throws Exception {
        File original = image(); byte[] before = Files.readAllBytes(original.toPath());
        PageItem page = new PageItem(Uri.fromFile(original), "Crop test", 0, new CropBounds(.1f, .1f, .9f, .9f));
        Bitmap cropped = processor.loadImage(page, 1600, 0); assertEquals(640, cropped.getWidth()); assertEquals(880, cropped.getHeight()); cropped.recycle();
        Bitmap rotated = processor.loadImage(page.clockwise(), 1600, 0); assertEquals(880, rotated.getWidth()); assertEquals(640, rotated.getHeight()); rotated.recycle();
        assertArrayEquals(before, Files.readAllBytes(original.toPath()));
        DocumentSession draft = new DocumentSession(context); draft.worker.submit(() -> { }).get(10, TimeUnit.SECONDS);
        try {
            draft.pages.clear(); draft.pages.add(page); draft.options = new ExportOptions("Sample", true, true, "test-only-password"); draft.save();
            DocumentSession restored = new DocumentSession(context); restored.worker.submit(() -> { }).get(10, TimeUnit.SECONDS);
            try {
                assertEquals(.1f, restored.pages.get(0).crop.left, .0001f); assertEquals(.9f, restored.pages.get(0).crop.bottom, .0001f);
                assertEquals("", restored.options.password); assertTrue(restored.options.protect); assertTrue(restored.options.pageNumbers);
                assertFalse(context.getSharedPreferences("paperbuild_draft", 0).getAll().toString().contains("test-only-password"));
            } finally { restored.worker.shutdown(); }
        } finally { draft.pages.clear(); draft.options = ExportOptions.NONE; draft.save(); draft.worker.shutdown(); }
    }
    @Test public void protectedExportRejectsMissingAndWrongPasswordsAndLibraryPreservesCiphertext() throws Exception {
        ExportOptions options = new ExportOptions("CONFIDENTIAL · KREATIV", true, true, "v11-test-password");
        File pdf = track(processor.exportImages(Collections.singletonList(new PageItem(Uri.fromFile(image()), "Sample")), "Protected", 595, 842, 0, false, options, v -> { }));
        for (String password : new String[]{"", "wrong-password"}) {
            try (PDDocument ignored = PDDocument.load(pdf, password)) { fail("Must require the correct password"); }
            catch (InvalidPasswordException expected) { }
        }
        try (PDDocument doc = PDDocument.load(pdf, options.password)) {
            assertTrue(doc.isEncrypted()); assertEquals(256, doc.getEncryption().getLength()); assertEquals(1, doc.getNumberOfPages());
            assertTrue(new PDFTextStripper().getText(doc).replaceAll("\\s+", "").contains("1/1"));
            Bitmap rendered = new PDFRenderer(doc).renderImage(0); assertNotNull(rendered); rendered.recycle();
        }
        PdfLibrary.Entry entry = library.add(pdf, "Protected sample", 1, true); entries.add(entry);
        assertArrayEquals(Files.readAllBytes(pdf.toPath()), Files.readAllBytes(library.file(entry).toPath()));
        assertTrue(new PdfLibrary(context).list(false).stream().anyMatch(e -> e.id.equals(entry.id) && e.protectedPdf));
    }
    @Test public void libraryRenameRemovalAndRestoreNeverTouchTheOriginal() throws Exception {
        File pdf = track(processor.exportImages(Collections.singletonList(new PageItem(Uri.fromFile(image()), "Sample")), "Sample", 595, 842, 0, false, v -> { }));
        PdfLibrary.Entry entry = library.add(pdf, "Original title", 1, false); entries.add(entry);
        library.rename(entry, "Renamed document");
        PdfLibrary.Entry renamed = library.list(false).stream().filter(e -> e.id.equals(entry.id)).findFirst().get();
        assertEquals("Renamed document", renamed.title); library.setRemoved(renamed, true);
        assertFalse(library.list(false).stream().anyMatch(e -> e.id.equals(entry.id)));
        PdfLibrary.Entry removed = library.list(true).stream().filter(e -> e.id.equals(entry.id)).findFirst().get();
        library.setRemoved(removed, false); assertTrue(library.file(entry).isFile()); assertTrue(pdf.isFile());
        library.setRemoved(entry, true); library.deleteForever(new PdfLibrary.Entry(entry.id, "Renamed document", entry.created, 1, false, true));
        assertFalse(library.file(entry).exists()); assertTrue(pdf.isFile()); entries.clear();
    }
    @Test public void presetsRoundTripWithoutPasswords() {
        PresetStore store = new PresetStore(context); String name = "Test " + UUID.randomUUID(); presets.add(name);
        assertTrue(store.save(new PresetStore.Preset(name, 1, 2, 1, new ExportOptions("INVOICE", true, true, "never-store-me"))));
        PresetStore.Preset saved = new PresetStore(context).list().stream().filter(p -> p.name.equals(name)).findFirst().get();
        assertEquals(1, saved.paper); assertEquals(2, saved.enhancement); assertEquals(1, saved.compression);
        assertTrue(saved.options.protect); assertTrue(saved.options.pageNumbers); assertEquals("INVOICE", saved.options.watermark); assertEquals("", saved.options.password);
        assertFalse(context.getSharedPreferences("paperbuild_presets", 0).getAll().toString().contains("never-store-me"));
        assertTrue(store.remove(name));
    }
    @Test public void watermarksAndNumbersWorkOnRotatedMergedAndExtractedPages() throws Exception {
        File source = track(File.createTempFile("v11-pages-", ".pdf", context.getCacheDir()));
        try (PDDocument doc = new PDDocument()) {
            for (int rotation : new int[]{0, 90, 180, 270}) {
                PDPage page = new PDPage(new PDRectangle(30, 40, 400, 600)); page.setRotation(rotation); doc.addPage(page);
                try (PDPageContentStream stream = new PDPageContentStream(doc, page)) {
                    stream.beginText(); stream.setFont(PDType1Font.HELVETICA, 18); stream.newLineAtOffset(80, 260); stream.showText("Original page"); stream.endText();
                }
            }
            doc.save(source);
        }
        ExportOptions options = new ExportOptions("CONFIDENTIAL", true, false, "");
        File merged = track(processor.merge(Arrays.asList(Uri.fromFile(source), Uri.fromFile(source)), options, v -> { }));
        try (PDDocument doc = PDDocument.load(merged)) {
            assertEquals(8, doc.getNumberOfPages()); String text = new PDFTextStripper().getText(doc).replaceAll("\\s+", "");
            assertTrue(text.contains("1/8")); assertTrue(text.contains("8/8"));
            File reviewFolder = new File(InstrumentationRegistry.getArguments().getString("additionalTestOutputDir", context.getExternalFilesDir("review-v11").getPath()));
            assertTrue(reviewFolder.isDirectory() || reviewFolder.mkdirs());
            File review = new File(reviewFolder, "watermarked-pages.pdf"); doc.save(review);
            for (int i = 0; i < 4; i++) {
                assertEquals(i * 90, doc.getPage(i).getRotation()); assertTrue(doc.getPage(i).getResources().getXObjectNames().iterator().hasNext());
                Bitmap render = new PDFRenderer(doc).renderImage(i);
                File png = new File(reviewFolder, "page-" + (i + 1) + ".png");
                try (FileOutputStream output = new FileOutputStream(png)) { render.compress(Bitmap.CompressFormat.PNG, 100, output); } render.recycle();
            }
        }
        File extracted = track(processor.split(Uri.fromFile(source), 2, 3, options));
        try (PDDocument doc = PDDocument.load(extracted)) {
            assertEquals(2, doc.getNumberOfPages()); String text = new PDFTextStripper().getText(doc).replaceAll("\\s+", ""); assertTrue(text.contains("1/2")); assertTrue(text.contains("2/2"));
        }
    }
}
