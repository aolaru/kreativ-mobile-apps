package com.kreativ.paperbuild;

import android.content.Context;
import android.app.Instrumentation;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import com.tom_roush.pdfbox.cos.COSName;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font;
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageFitDestination;
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDAcroForm;
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDTextField;
import com.tom_roush.pdfbox.text.PDFTextStripper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class Version12Test {
    private Context context;
    private DocumentProcessor processor;
    @Before public void setup() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertTrue("Only run in an isolated QA app", context.getPackageName().endsWith(".qa"));
        processor = new DocumentProcessor(context);
    }
    private File output(String name) throws IOException {
        File folder = new File(context.getExternalFilesDir(null), "v12-review"); assertTrue(folder.isDirectory() || folder.mkdirs());
        return new File(folder, name + ".pdf");
    }
    private File fixture() throws Exception {
        File file = output("source");
        try (PDDocument pdf = new PDDocument()) {
            for (int i = 0; i < 3; i++) {
                PDPage page = new PDPage(i == 1 ? PDRectangle.LETTER : PDRectangle.A4); pdf.addPage(page);
                if (i == 1) { page.setCropBox(new PDRectangle(20, 30, 560, 720)); page.setRotation(90); }
                try (PDPageContentStream stream = new PDPageContentStream(pdf, page)) {
                    stream.beginText(); stream.setFont(PDType1Font.HELVETICA_BOLD, 28); stream.newLineAtOffset(50, 500);
                    stream.showText("Original page " + (i + 1)); stream.endText();
                    stream.setStrokingColor(60, 100, 190); stream.addRect(40, 450, 400, 120); stream.stroke();
                }
            }
            PDAnnotationLink link = new PDAnnotationLink(); link.setRectangle(new PDRectangle(40, 450, 100, 60));
            PDPageFitDestination target = new PDPageFitDestination(); target.setPage(pdf.getPage(1)); link.setDestination(target);
            pdf.getPage(0).setAnnotations(Collections.singletonList(link));
            pdf.save(file);
        }
        return file;
    }
    private String text(PDDocument pdf, int page) throws IOException {
        PDFTextStripper text = new PDFTextStripper(); text.setStartPage(page); text.setEndPage(page); return text.getText(pdf);
    }
    @Test public void editorPreservesVectorsGeometryAndIndependentDuplicates() throws Exception {
        File source = fixture(); byte[] before = java.nio.file.Files.readAllBytes(source.toPath());
        PdfPagePlan plan = new PdfPagePlan(3); plan.move(1, 0); plan.rotate(0); plan.duplicate(1); plan.remove(3);
        File edited = processor.editPdf(source, plan.snapshot(), "Editor regression", ExportOptions.NONE, p -> {});
        try (PDDocument pdf = PDDocument.load(edited)) {
            assertEquals(3, pdf.getNumberOfPages()); assertTrue(text(pdf, 1).contains("Original page 2"));
            assertTrue(text(pdf, 2).contains("Original page 1")); assertTrue(text(pdf, 3).contains("Original page 1"));
            assertEquals(180, pdf.getPage(0).getRotation()); assertEquals(560, pdf.getPage(0).getCropBox().getWidth(), .01);
            assertEquals(20, pdf.getPage(0).getCropBox().getLowerLeftX(), .01);
            for (int i = 1; i < 3; i++) {
                PDAnnotationLink link = (PDAnnotationLink) pdf.getPage(i).getAnnotations().get(0);
                assertEquals(pdf.getPage(i).getCOSObject(), link.getPage().getCOSObject());
                assertEquals(pdf.getPage(0).getCOSObject(), ((PDPageFitDestination) link.getDestination()).getPage().getCOSObject());
            }
            assertNotSame(pdf.getPage(1).getAnnotations().get(0).getCOSObject(), pdf.getPage(2).getAnnotations().get(0).getCOSObject());
            pdf.save(output("edited"));
        }
        assertArrayEquals(before, java.nio.file.Files.readAllBytes(source.toPath()));
        edited.delete();
    }
    @Test public void removedPageDestinationsAreDroppedAndInheritedResourcesSurvive() throws Exception {
        File source = fixture();
        try (PDDocument pdf = PDDocument.load(source)) {
            pdf.getPage(0).getCOSObject().getCOSDictionary(COSName.PARENT).setItem(COSName.RESOURCES, pdf.getPage(0).getResources());
            pdf.getPage(0).getCOSObject().removeItem(COSName.RESOURCES); pdf.save(output("inherited"));
        }
        File result = processor.editPdf(output("inherited"), Collections.singletonList(new PdfPagePlan.Page(0, 0)), "Removed link", ExportOptions.NONE, p -> {});
        try (PDDocument pdf = PDDocument.load(result)) {
            assertEquals(1, pdf.getNumberOfPages()); assertTrue(text(pdf, 1).contains("Original page 1"));
            assertTrue(pdf.getPage(0).getAnnotations().isEmpty()); pdf.save(output("removed-link"));
        } finally { result.delete(); }
    }
    @Test public void encryptedAndFillableInputsAreRejectedWithoutChangingThem() throws Exception {
        File source = fixture();
        File encrypted = processor.split(Uri.fromFile(source), 1, 1, new ExportOptions("", false, true, "test-password"));
        try { processor.importEditablePdf(Uri.fromFile(encrypted)); fail(); } catch (IOException expected) { }
        finally { encrypted.delete(); }
        try (PDDocument pdf = PDDocument.load(source)) {
            PDAcroForm form = new PDAcroForm(pdf); pdf.getDocumentCatalog().setAcroForm(form);
            PDTextField field = new PDTextField(form); field.setPartialName("Keep interactive"); form.getFields().add(field); pdf.save(output("form"));
        }
        try { processor.importEditablePdf(Uri.fromFile(output("form"))); fail(); }
        catch (IOException expected) { assertTrue(expected.getMessage().contains("fillable")); }
        try (PDDocument pdf = PDDocument.load(output("form"))) { assertNotNull(pdf.getDocumentCatalog().getAcroForm().getField("Keep interactive")); }
    }
    private String logo() throws Exception {
        File image = new File(context.getCacheDir(), "brand-fixture.png");
        Bitmap bitmap = Bitmap.createBitmap(480, 180, Bitmap.Config.ARGB_8888); Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(3); paint.setColor(Color.rgb(55, 85, 210)); paint.setTextSize(65); canvas.drawText("KREATIV", 12, 105, paint);
        try (FileOutputStream out = new FileOutputStream(image)) { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)); } bitmap.recycle();
        try { return new LogoStore(context).importLogo(Uri.fromFile(image)); } finally { image.delete(); }
    }
    @Test public void logoPresetsRemainPrivateAndWorkOnAllFourExportPaths() throws Exception {
        String logo = logo(); File file = new LogoStore(context).file(logo); assertTrue(file.isFile());
        ExportOptions options = new ExportOptions("BRAND REVIEW", true, false, "", logo);
        PresetStore store = new PresetStore(context); assertTrue(store.save(new PresetStore.Preset("v12 branded", 0, 0, 0, options)));
        assertEquals(logo, store.list().stream().filter(p -> p.name.equals("v12 branded")).findFirst().get().options.logo);
        File source = fixture(); File edited = processor.editPdf(source, new PdfPagePlan(3).snapshot(), "Logo edit", options, p -> {});
        File extracted = processor.split(Uri.fromFile(source), 1, 2, options);
        File merged = processor.merge(Arrays.asList(Uri.fromFile(source), Uri.fromFile(source)), options, p -> {});
        File images = processor.exportImages(Collections.singletonList(new PageItem(Uri.fromFile(file), "Logo image")), "Logo export", 595, 842, 0, false, options, p -> {});
        File[] outputs = {edited, extracted, merged, images}; String[] names = {"logo-edited", "logo-extracted", "logo-merged", "logo-images"};
        try {
            for (int i = 0; i < outputs.length; i++) try (PDDocument pdf = PDDocument.load(outputs[i])) {
                for (PDPage page : pdf.getPages()) {
                    int count = 0; boolean transparent = false;
                    for (COSName key : page.getResources().getXObjectNames()) {
                        com.tom_roush.pdfbox.pdmodel.graphics.PDXObject object = page.getResources().getXObject(key);
                        if (object instanceof com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject) {
                            count++; transparent |= ((com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject) object).getSoftMask() != null;
                        }
                    }
                    assertTrue("Logo and text watermark expected", count >= 2); assertTrue("Transparent PNG must retain its alpha", transparent);
                }
                pdf.save(output(names[i]));
            }
        } finally { for (File pdf : outputs) pdf.delete(); store.remove("v12 branded"); }
    }
    @Test public void freeUsersCannotEnterThePdfEditor() {
        context.getSharedPreferences("paperbuild_pro", 0).edit().putBoolean("owned", false).commit(); ReviewAccess.clear(context);
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(ProActivity.class.getName(), null, false);
        try (ActivityScenario<MainActivity> main = ActivityScenario.launch(MainActivity.class)) {
            main.onActivity(activity -> activity.findViewById(R.id.edit_pdf).performClick());
            android.app.Activity pro = monitor.waitForActivityWithTimeout(5000); assertNotNull(pro); instrumentation.runOnMainSync(pro::finish);
        } finally { instrumentation.removeMonitor(monitor); }
    }
    @Test public void editorDraftSurvivesRecreationAndExportsWithoutChangingPhotoDraft() throws Exception {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        String reviewCode = InstrumentationRegistry.getArguments().getString("reviewCode");
        assertNotNull("Supply the private reviewer code at invocation", reviewCode);
        assertTrue(ReviewAccess.activate(context, reviewCode));
        PdfEditorSession editor = PdfEditorSession.get(context); DocumentSession documents = DocumentSession.get(context);
        File fixture = fixture(); java.util.concurrent.atomic.AtomicBoolean ready = new java.util.concurrent.atomic.AtomicBoolean();
        instrumentation.runOnMainSync(() -> { editor.clear(); documents.options = ExportOptions.NONE; editor.open(Uri.fromFile(fixture)); });
        long deadline = System.currentTimeMillis() + 15000;
        while (!ready.get() && System.currentTimeMillis() < deadline) {
            instrumentation.runOnMainSync(() -> ready.set(!editor.busy && editor.plan != null)); if (!ready.get()) Thread.sleep(50);
        }
        assertTrue("PDF draft should load", ready.get());
        java.util.List<PageItem> photoDraft = new java.util.ArrayList<>(documents.pages);
        try (ActivityScenario<PdfEditorActivity> screen = ActivityScenario.launch(PdfEditorActivity.class)) {
            screen.onActivity(activity -> {
                editor.plan.move(2, 0); editor.plan.rotate(0); editor.plan.duplicate(0); editor.changed();
            });
            screen.recreate(); screen.onActivity(activity -> {
                assertEquals(4, editor.plan.size()); assertEquals(2, editor.plan.get(1).source); assertEquals(90, editor.plan.get(1).rotation);
                activity.findViewById(R.id.editor_undo).performClick(); assertEquals(3, editor.plan.size());
                activity.findViewById(R.id.editor_export).performClick();
            });
            ready.set(false); deadline = System.currentTimeMillis() + 20000;
            while (!ready.get() && System.currentTimeMillis() < deadline) {
                instrumentation.runOnMainSync(() -> ready.set(!documents.busy && documents.latestPdf != null)); if (!ready.get()) Thread.sleep(50);
            }
            assertTrue("Edited PDF should be archived", ready.get());
            try (PDDocument pdf = PDDocument.load(documents.latestPdf)) {
                assertEquals(3, pdf.getNumberOfPages()); assertEquals(90, pdf.getPage(0).getRotation());
                // A 90-degree page may extract as vertical text with whitespace between glyphs.
                assertEquals("Originalpage3", text(pdf, 1).replaceAll("\\s+", ""));
            }
            assertEquals(photoDraft, documents.pages);
        } finally { ReviewAccess.clear(context); context.getSharedPreferences("paperbuild_pro", 0).edit().putBoolean("owned", false).commit(); }
    }
}
