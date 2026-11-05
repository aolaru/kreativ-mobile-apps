package com.kreativ.paperbuild;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class Version11WorkflowTest {
    private Context context;
    @Before public void setup() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertTrue(context.getPackageName().endsWith(".qa"));
        context.getSharedPreferences("paperbuild_pro", 0).edit().putBoolean("owned", false).commit(); ReviewAccess.clear(context);
    }
    private TextView find(View view, String text) {
        if (view instanceof TextView && ((TextView) view).getText().toString().equals(text)) return (TextView) view;
        if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
            TextView result = find(((ViewGroup) view).getChildAt(i), text); if (result != null) return result;
        }
        return null;
    }
    @Test public void advancedOptionsAreGatedButRecentPdfsRemainFree() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(ProActivity.class.getName(), null, false);
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                assertTrue(activity.findViewById(R.id.recent_pdfs).isEnabled());
                assertTrue(activity.findViewById(R.id.saved_presets).isEnabled());
                activity.findViewById(R.id.export_options).performClick();
            });
            android.app.Activity pro = monitor.waitForActivityWithTimeout(5000); assertNotNull("Free users should see Pro, not export controls", pro);
            instrumentation.runOnMainSync(pro::finish);
        } finally { instrumentation.removeMonitor(monitor); }
        try (ActivityScenario<RecentPdfsActivity> recent = ActivityScenario.launch(RecentPdfsActivity.class)) {
            recent.onActivity(activity -> assertNotNull(find(activity.getWindow().getDecorView(), "Recent PDFs")));
        }
    }
    @Test public void freeCropCanRestoreFullPageWithoutChangingSource() throws Exception {
        File file = File.createTempFile("crop-ui-", ".png", context.getCacheDir());
        Bitmap bitmap = Bitmap.createBitmap(120, 180, Bitmap.Config.ARGB_8888); bitmap.eraseColor(android.graphics.Color.WHITE);
        try (FileOutputStream output = new FileOutputStream(file)) { bitmap.compress(Bitmap.CompressFormat.PNG, 100, output); } bitmap.recycle();
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation(); DocumentSession session = DocumentSession.get(context);
        instrumentation.runOnMainSync(() -> { session.pages.clear(); session.pages.add(new PageItem(Uri.fromFile(file), "UI fixture", 0, new CropBounds(.1f, .1f, .9f, .9f))); session.changed(); });
        Intent intent = new Intent(context, CropActivity.class).putExtra("index", 0).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try (ActivityScenario<CropActivity> crop = ActivityScenario.launch(intent)) {
            AtomicBoolean ready = new AtomicBoolean(); long deadline = System.currentTimeMillis() + 10000;
            while (!ready.get() && System.currentTimeMillis() < deadline) {
                crop.onActivity(activity -> ready.set(activity.findViewById(R.id.crop_apply).isEnabled()));
                if (!ready.get()) Thread.sleep(50);
            }
            assertTrue("Crop preview should load", ready.get());
            crop.onActivity(activity -> { activity.findViewById(R.id.crop_restore).performClick(); activity.findViewById(R.id.crop_apply).performClick(); });
            instrumentation.runOnMainSync(() -> assertTrue(session.pages.get(0).crop.full())); assertTrue(file.isFile());
        } finally { instrumentation.runOnMainSync(() -> { session.pages.clear(); session.changed(); }); file.delete(); }
    }
    @Test public void completedExportsAreSavedInLibraryAndSurviveAnewDraft() throws Exception {
        File file = File.createTempFile("export-ui-", ".png", context.getCacheDir());
        Bitmap bitmap = Bitmap.createBitmap(120, 180, Bitmap.Config.ARGB_8888); bitmap.eraseColor(android.graphics.Color.WHITE);
        try (FileOutputStream output = new FileOutputStream(file)) { bitmap.compress(Bitmap.CompressFormat.PNG, 100, output); } bitmap.recycle();
        DocumentSession session = DocumentSession.get(context); Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        AtomicBoolean complete = new AtomicBoolean();
        Runnable listener = () -> { if (!session.busy && session.latestPdf != null) complete.set(true); };
        instrumentation.runOnMainSync(() -> {
            session.latestPdf = null; session.attach(listener);
            session.run("Test export", progress -> {
                File pdf = session.processor.exportImages(Collections.singletonList(new PageItem(Uri.fromFile(file), "Workflow sample")), "Workflow sample", 595, 842, 0, false, progress);
                return new DocumentSession.Result(pdf, "Sample", "Ready", null, "Workflow sample");
            });
        });
        long deadline = System.currentTimeMillis() + 10000;
        while (!complete.get() && System.currentTimeMillis() < deadline) Thread.sleep(50);
        try {
            assertTrue("Export should complete and be archived", complete.get());
            assertTrue(session.latestPdf.getPath().contains("/files/paperbuild/library/")); assertTrue(session.latestPdf.isFile());
            instrumentation.runOnMainSync(() -> { session.pages.clear(); session.changed(); });
            assertTrue(new PdfLibrary(context).list(false).stream().anyMatch(e -> e.title.equals("Workflow sample")));
            // Keep only the explicit sample in the isolated QA library for the subsequent visual review.
        } finally { instrumentation.runOnMainSync(() -> session.detach(listener)); file.delete(); }
    }
}
