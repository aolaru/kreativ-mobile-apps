package com.kreativ.paperbuild;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.Assert.*;

/** Genuine device captures of the release UI using fictional documents. Not part of the regression source set. */
@RunWith(AndroidJUnit4.class)
public class Listing12ScreenshotsTest {
    private final android.app.Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
    private final Context context = instrumentation.getTargetContext();
    private final DocumentProcessor processor = new DocumentProcessor(context);
    private ScrollView scroll(Activity activity) { return (ScrollView) ((ViewGroup) activity.findViewById(android.R.id.content)).getChildAt(0); }
    private View described(View root, String description) {
        if (description.contentEquals(root.getContentDescription() == null ? "" : root.getContentDescription())) return root;
        if (root instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) root).getChildCount(); i++) {
            View found = described(((ViewGroup) root).getChildAt(i), description); if (found != null) return found;
        }
        return null;
    }
    private void capture(String name) throws Exception {
        instrumentation.waitForIdleSync(); Thread.sleep(250);
        Bitmap screenshot = instrumentation.getUiAutomation().takeScreenshot(); assertNotNull(screenshot);
        File folder = new File(context.getExternalFilesDir(null), "listing-1.2"); assertTrue(folder.isDirectory() || folder.mkdirs());
        try (FileOutputStream output = new FileOutputStream(new File(folder, name + ".png"))) {
            assertTrue(screenshot.compress(Bitmap.CompressFormat.PNG, 100, output));
        } finally { screenshot.recycle(); }
    }
    private void open(File pdf, String title) throws Exception {
        PdfEditorSession editor = PdfEditorSession.get(context);
        instrumentation.runOnMainSync(() -> editor.open(Uri.fromFile(pdf)));
        AtomicBoolean ready = new AtomicBoolean(); long deadline = System.currentTimeMillis() + 15000;
        while (!ready.get() && System.currentTimeMillis() < deadline) {
            instrumentation.runOnMainSync(() -> ready.set(!editor.busy && editor.plan != null)); if (!ready.get()) Thread.sleep(50);
        }
        assertTrue("Fictional PDF should load", ready.get());
        instrumentation.runOnMainSync(() -> { editor.title = title; editor.save(); });
    }
    private void line(PDPageContentStream stream, String text, float x, float y, float size, boolean bold) throws Exception {
        stream.beginText(); stream.setFont(bold ? PDType1Font.HELVETICA_BOLD : PDType1Font.HELVETICA, size);
        stream.newLineAtOffset(x, y); stream.showText(text); stream.endText();
    }
    private File brief() throws Exception {
        File file = new File(context.getCacheDir(), "studio-north-brief.pdf");
        String[] headings = {"Project brief", "The schedule", "Ready to deliver"};
        String[][] content = {{"A fresh start for the studio.", "A clear plan, in one place.", "Simple. Considered. Ready to share."},
                {"Week 1   /   Explore the direction", "Week 2   /   Build the first draft", "Week 3   /   Review and refine"},
                {"01   Project overview", "02   Agreed milestones", "03   Final files and next steps"}};
        try (PDDocument pdf = new PDDocument()) {
            for (int i = 0; i < 3; i++) {
                PDPage page = new PDPage(PDRectangle.A4); pdf.addPage(page);
                try (PDPageContentStream stream = new PDPageContentStream(pdf, page)) {
                    stream.setNonStrokingColor(76, 95, 223); line(stream, "STUDIO NORTH", 48, 774, 12, true);
                    stream.setNonStrokingColor(24, 43, 67); line(stream, headings[i], 48, 681, 38, true);
                    stream.setNonStrokingColor(94, 109, 131); line(stream, "STUDIO REFRESH / PROJECT NOTES", 48, 643, 10, true);
                    stream.setNonStrokingColor(76, 95, 223); stream.addRect(48, 595, 499, 4); stream.fill();
                    for (int j = 0; j < 3; j++) {
                        stream.setNonStrokingColor(243, 245, 251); stream.addRect(48, 475 - j * 85, 499, 62); stream.fill();
                        stream.setNonStrokingColor(24, 43, 67); line(stream, content[i][j], 66, 498 - j * 85, 16, j == 0);
                    }
                    stream.setNonStrokingColor(94, 109, 131); line(stream, "Fictional sample document", 48, 70, 10, false);
                }
            }
            pdf.save(file);
        }
        return file;
    }
    private String logo() throws Exception {
        Bitmap bitmap = Bitmap.createBitmap(640, 150, Bitmap.Config.ARGB_8888); Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(3); paint.setColor(Color.rgb(76, 95, 223)); canvas.drawCircle(53, 75, 40, paint);
        paint.setColor(Color.WHITE); paint.setStrokeWidth(8); canvas.drawLine(30, 75, 48, 92, paint); canvas.drawLine(48, 92, 78, 56, paint);
        paint.setColor(Color.rgb(24, 43, 67)); paint.setTypeface(android.graphics.Typeface.create("sans-serif-medium", 0)); paint.setTextSize(62);
        canvas.drawText("STUDIO NORTH", 110, 96, paint);
        File file = new File(context.getCacheDir(), "studio-north-logo.png");
        try (FileOutputStream output = new FileOutputStream(file)) { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)); } bitmap.recycle();
        return new LogoStore(context).importLogo(Uri.fromFile(file));
    }
    @Test public void captureReleaseScreensWithFictionalDocuments() throws Exception {
        assertEquals("com.kreativ.paperbuild.v120.qa", context.getPackageName());
        assertEquals("1.2.0", context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName);
        String reviewCode = InstrumentationRegistry.getArguments().getString("reviewCode"); assertNotNull(reviewCode); assertTrue(ReviewAccess.activate(context, reviewCode));
        File source = brief(); open(source, "Studio project brief");
        try (ActivityScenario<PdfEditorActivity> screen = ActivityScenario.launch(PdfEditorActivity.class)) {
            screen.onActivity(a -> scroll(a).scrollTo(0, Ui.dp(a, 350))); capture("editor-pages");
            screen.onActivity(a -> { View more = described(a.getWindow().getDecorView(), "Edit page 1"); assertNotNull(more); more.performClick(); });
            capture("editor-menu"); androidx.test.espresso.Espresso.pressBack();
        }
        ExportOptions options = new ExportOptions("STUDIO COPY", true, false, "", logo());
        assertTrue(new PresetStore(context).save(new PresetStore.Preset("Studio branding", 0, 0, 0, options)));
        instrumentation.runOnMainSync(() -> { DocumentSession.get(context).options = options; DocumentSession.get(context).save(); });
        File branded = processor.editPdf(source, Collections.singletonList(new PdfPagePlan.Page(0, 0)), "Studio brief", options, p -> {});
        open(branded, "Studio brief");
        try (ActivityScenario<PdfEditorActivity> screen = ActivityScenario.launch(PdfEditorActivity.class)) {
            screen.onActivity(a -> scroll(a).scrollTo(0, Ui.dp(a, 350))); capture("logo-result");
        }
        try (ActivityScenario<ProActivity> screen = ActivityScenario.launch(ProActivity.class)) {
            capture("pro-top"); screen.onActivity(a -> scroll(a).scrollTo(0, Ui.dp(a, 350))); capture("pro-features");
        }
        branded.delete(); source.delete();
    }
}
