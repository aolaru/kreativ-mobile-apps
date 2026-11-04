package com.kreativ.paperbuild;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;

/** Ordered black-box workflows. No app classes, reflection, or test-only app keep rules. */
public final class OptimizedReleaseTest {
    private final OptimizedReleaseRunner instrumentation = OptimizedReleaseRunner.instance;
    private final Context context = instrumentation.getTargetContext();
    private Uri freePdf, compactPdf, image;
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }

    private final class Screen implements AutoCloseable {
        Activity activity;
        Screen(String name) {
            String className = "com.kreativ.paperbuild." + name;
            Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(className, null, false);
            try {
                // Use a bounded launch monitor: startActivitySync can wait indefinitely when
                // another screen from the same task remains after a previous workflow.
                instrumentation.runOnMainSync(() -> context.startActivity(new Intent().setClassName(context, className)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)));
                activity = instrumentation.waitForMonitorWithTimeout(monitor, 10000);
                require(activity != null, "Activity launch timed out: " + name);
            } finally { instrumentation.removeMonitor(monitor); }
            on(a -> a.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
        }
        void on(Consumer<Activity> action) {
            AtomicReference<Throwable> error = new AtomicReference<>();
            instrumentation.runOnMainSync(() -> { try { action.accept(activity); } catch (Throwable t) { error.set(t); } });
            if (error.get() != null) throw new AssertionError(error.get());
        }
        void recreate() {
            Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(activity.getClass().getName(), null, false);
            try { on(Activity::recreate); activity = instrumentation.waitForMonitorWithTimeout(monitor, 10000);
                require(activity != null, "Activity recreation timed out"); instrumentation.waitForIdleSync();
            } finally { instrumentation.removeMonitor(monitor); }
        }
        @Override public void close() { on(Activity::finish); instrumentation.waitForIdleSync(); }
    }
    private List<View> views(View root, Predicate<View> predicate) {
        List<View> result = new ArrayList<>(); if (predicate.test(root)) result.add(root);
        if (root instanceof ViewGroup) { ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) result.addAll(views(group.getChildAt(i), predicate)); }
        return result;
    }
    private boolean text(View view, String value) { return view instanceof TextView && value.contentEquals(((TextView) view).getText()); }
    private View find(Activity activity, Predicate<View> predicate) {
        List<View> found = views(activity.getWindow().getDecorView(), predicate); require(!found.isEmpty(), "UI control not found"); return found.get(0);
    }
    private void click(Screen screen, String label) {
        screen.on(a -> {
            View target = find(a, v -> text(v, label) && v.isClickable());
            boolean handled = target.performClick();
            // CompoundButton toggles even when View.performClick reports no OnClickListener.
            require(handled || target instanceof android.widget.CompoundButton && ((android.widget.CompoundButton) target).isChecked(), "Click failed: " + label);
        });
    }
    private void describeClick(Screen screen, String description) {
        screen.on(a -> require(find(a, v -> v.getContentDescription() != null && description.contentEquals(v.getContentDescription())).performClick(), "Click failed: " + description));
    }
    private void name(Screen screen, String title) { screen.on(a -> ((EditText) find(a, v -> v.getContentDescription() != null && "Document name".contentEquals(v.getContentDescription()))).setText(title)); }
    private void await(Screen screen, Predicate<Activity> ready) {
        long deadline = SystemClock.elapsedRealtime() + 30000;
        AtomicReference<Boolean> success = new AtomicReference<>(false);
        do { screen.on(a -> success.set(ready.test(a))); if (success.get()) return; SystemClock.sleep(100); }
        while (SystemClock.elapsedRealtime() < deadline);
        AtomicReference<String> labels = new AtomicReference<>();
        screen.on(a -> labels.set(views(a.getWindow().getDecorView(), v -> v instanceof TextView)
                .stream().map(v -> ((TextView) v).getText().toString()).collect(java.util.stream.Collectors.joining(" | "))));
        throw new AssertionError("Export/UI timed out: " + labels.get());
    }
    private boolean has(Activity activity, String part) { return !views(activity.getWindow().getDecorView(), v -> v instanceof TextView && ((TextView) v).getText().toString().contains(part)).isEmpty(); }

    private AccessibilityNodeInfo node(AccessibilityNodeInfo root, Predicate<AccessibilityNodeInfo> predicate) {
        if (root == null) return null; if (predicate.test(root)) return root;
        for (int i = 0; i < root.getChildCount(); i++) { AccessibilityNodeInfo found = node(root.getChild(i), predicate); if (found != null) return found; }
        return null;
    }
    private AccessibilityNodeInfo dialogNode(Predicate<AccessibilityNodeInfo> predicate) {
        long deadline = SystemClock.elapsedRealtime() + 10000;
        do { AccessibilityNodeInfo found = node(instrumentation.getUiAutomation().getRootInActiveWindow(), predicate);
            if (found != null) return found; SystemClock.sleep(100);
        } while (SystemClock.elapsedRealtime() < deadline);
        throw new AssertionError("Dialog control not found");
    }
    private void dialogClick(String label) {
        AccessibilityNodeInfo target = dialogNode(n -> n.getText() != null && label.equalsIgnoreCase(n.getText().toString()) && n.isClickable() && n.isEnabled());
        target.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SHOW_ON_SCREEN.getId()); instrumentation.waitForIdleSync();
        require(target.performAction(AccessibilityNodeInfo.ACTION_CLICK), "Dialog click failed: " + label); instrumentation.waitForIdleSync();
    }
    private void dialogText(String description, String value) {
        AccessibilityNodeInfo target = dialogNode(n -> n.isEditable() && (description == null || n.getContentDescription() != null && description.contentEquals(n.getContentDescription())));
        Bundle args = new Bundle(); args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value);
        require(target.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args), "Dialog input failed"); instrumentation.waitForIdleSync();
    }
    private Instrumentation.ActivityMonitor picker(Intent result) {
        Instrumentation.ActivityMonitor monitor = new Instrumentation.ActivityMonitor() {
            @Override public Instrumentation.ActivityResult onStartActivity(Intent intent) {
                Intent requested = Intent.ACTION_CHOOSER.equals(intent.getAction()) ? intent.getParcelableExtra(Intent.EXTRA_INTENT) : intent;
                return requested != null && Intent.ACTION_OPEN_DOCUMENT.equals(requested.getAction()) ? new Instrumentation.ActivityResult(Activity.RESULT_OK, result) : null;
            }
        }; instrumentation.addMonitor(monitor); return monitor;
    }
    private Intent selected(Uri... uris) {
        Intent intent = new Intent().setData(uris[0]).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        ClipData clip = ClipData.newRawUri("QA fixture", uris[0]);
        for (int i = 1; i < uris.length; i++) clip.addItem(new ClipData.Item(uris[i])); intent.setClipData(clip); return intent;
    }
    private Uri share(Screen screen, String filename) throws Exception {
        AtomicReference<Intent> shared = new AtomicReference<>();
        Instrumentation.ActivityMonitor monitor = new Instrumentation.ActivityMonitor() {
            @Override public Instrumentation.ActivityResult onStartActivity(Intent intent) {
                if (!Intent.ACTION_CHOOSER.equals(intent.getAction())) return null;
                shared.set(intent.getParcelableExtra(Intent.EXTRA_INTENT)); return new Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null);
            }
        };
        instrumentation.addMonitor(monitor); try { click(screen, "Share PDF"); } finally { instrumentation.removeMonitor(monitor); }
        require(shared.get() != null && "application/pdf".equals(shared.get().getType()), "PDF share intent missing");
        require((shared.get().getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0, "Reader permission missing");
        Uri uri = shared.get().getParcelableExtra(Intent.EXTRA_STREAM); require(uri != null, "Shared file URI missing");
        File folder = new File(context.getExternalFilesDir(null), "optimized-review"); require(folder.isDirectory() || folder.mkdirs(), "Output directory unavailable");
        try (InputStream input = context.getContentResolver().openInputStream(uri); FileOutputStream output = new FileOutputStream(new File(folder, filename))) {
            byte[] bytes = new byte[8192]; int count; while ((count = input.read(bytes)) != -1) output.write(bytes, 0, count);
        }
        return uri;
    }
    private void build(Screen screen, String title) {
        name(screen, title); click(screen, "Build PDF"); await(screen, a -> has(a, title + ".pdf") && has(a, "Your PDF is ready."));
    }
    private void render(Uri pdf, int expected) throws Exception {
        try (ParcelFileDescriptor fd = context.getContentResolver().openFileDescriptor(pdf, "r"); PdfRenderer renderer = new PdfRenderer(fd)) {
            require(renderer.getPageCount() == expected, "Unexpected PDF page count");
            for (int i = 0; i < expected; i++) try (PdfRenderer.Page page = renderer.openPage(i)) {
                Bitmap bitmap = Bitmap.createBitmap(300, 420, Bitmap.Config.ARGB_8888); bitmap.eraseColor(Color.WHITE);
                try {
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                    // All fictional fixtures contain a dark text block and divider. A logo
                    // or page number alone must not count as successfully rendered content.
                    int[] pixels = new int[300 * 420]; bitmap.getPixels(pixels, 0, 300, 0, 0, 300, 420);
                    int dark = 0;
                    for (int pixel : pixels) if ((Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3 < 180) dark++;
                    require(dark > 100, "PDF page content rendered blank at thumbnail size: page " + (i + 1));
                } finally { bitmap.recycle(); }
            }
        }
    }
    public void freeCropExportAndRecovery() throws Exception {
        require(context.getPackageName().equals("com.kreativ.paperbuild.r8test"), "Never run these fixtures in production");
        require((context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) == 0, "Must test a non-debuggable release");
        require("1.2.0".equals(context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName), "Wrong release version");
        require(context.getSharedPreferences("paperbuild_pro", 0).getAll().isEmpty(), "Use a fresh isolated test package");
        File source = new File(context.getFilesDir(), "paperbuild/pages/optimized-source.png"); require(source.getParentFile().mkdirs(), "Fresh fixture directory expected");
        Bitmap bitmap = Bitmap.createBitmap(1400, 1900, Bitmap.Config.ARGB_8888); Canvas canvas = new Canvas(bitmap); canvas.drawColor(Color.WHITE);
        Paint paint = new Paint(3); paint.setColor(Color.rgb(35, 55, 95)); paint.setTextSize(65);
        canvas.drawText("PaperBuild 1.2", 130, 280, paint); paint.setTextSize(40);
        canvas.drawText("Optimized release regression", 130, 400, paint); canvas.drawText("FICTIONAL QA DOCUMENT", 130, 520, paint);
        paint.setColor(Color.rgb(80, 100, 220)); canvas.drawRect(130, 650, 1250, 700, paint);
        try (FileOutputStream output = new FileOutputStream(source)) { bitmap.compress(Bitmap.CompressFormat.PNG, 100, output); } bitmap.recycle();
        image = Uri.parse("content://" + context.getPackageName() + ".fileprovider/pages/optimized-source.png");
        JSONArray pages = new JSONArray();
        for (int i = 0; i < 2; i++) pages.put(new JSONObject().put("uri", Uri.fromFile(source).toString()).put("name", "QA page " + (i + 1))
                .put("rotation", i == 0 ? 0 : 90).put("left", .05).put("top", .05).put("right", .95).put("bottom", .95));
        require(context.getSharedPreferences("paperbuild_draft", 0).edit().putString("pages", pages.toString()).putString("name", "R8 Free").commit(), "Draft fixture write failed");
        try (Screen screen = new Screen("MainActivity")) {
            await(screen, a -> has(a, "2 pages")); require(!context.getSharedPreferences("paperbuild_pro", 0).getBoolean("owned", false), "Free must remain free");
            Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor("com.kreativ.paperbuild.CropActivity", null, false);
            describeClick(screen, "Crop page"); Activity crop = instrumentation.waitForMonitorWithTimeout(monitor, 10000); instrumentation.removeMonitor(monitor);
            require(crop != null, "Crop Activity failed to open");
            long deadline = SystemClock.elapsedRealtime() + 10000;
            while (SystemClock.elapsedRealtime() < deadline) {
                AtomicReference<Boolean> ready = new AtomicReference<>(false); instrumentation.runOnMainSync(() -> ready.set(find(crop, v -> text(v, "Apply crop")).isEnabled()));
                if (ready.get()) break; SystemClock.sleep(100);
            }
            instrumentation.runOnMainSync(() -> { find(crop, v -> text(v, "Restore full page")).performClick(); find(crop, v -> text(v, "Apply crop")).performClick(); });
            instrumentation.waitForIdleSync(); screen.recreate();
            build(screen, "R8 Free"); freePdf = share(screen, "free.pdf"); render(freePdf, 2);
            screen.recreate(); await(screen, a -> has(a, "R8 Free.pdf"));
            require(context.getSharedPreferences("paperbuild_draft", 0).getString("pages", "").contains("QA page 2"), "Draft recovery failed");
            click(screen, "Recent PDFs · Free");
        }
        try (Screen recent = new Screen("RecentPdfsActivity")) { await(recent, a -> has(a, "R8 Free")); }
    }
    private void options(Screen screen, boolean protect) {
        screen.on(a -> find(a, v -> v instanceof Button && ((Button) v).getText().toString().endsWith("Pro export options")).performClick());
        dialogText("Watermark text", "R8 VERIFIED");
        AccessibilityNodeInfo numbers = dialogNode(n -> n.getText() != null && n.getText().toString().equals("Add page numbers · 1 / total"));
        if (!numbers.isChecked()) dialogClick("Add page numbers · 1 / total");
        AccessibilityNodeInfo password = dialogNode(n -> n.getText() != null && n.getText().toString().equals("Require a password to open"));
        if (password.isChecked() != protect) dialogClick("Require a password to open");
        if (protect) { dialogText("Password · 8–64 characters", "R8-test-only-123"); dialogText("Confirm password", "R8-test-only-123"); }
        dialogClick("Apply");
    }
    public void protectedExportAndPasswordFreePresets() throws Exception {
        require(OptimizedReleaseRunner.reviewCode != null, "Supply the existing private reviewer code at invocation, never in source");
        try (Screen pro = new Screen("ProActivity")) {
            click(pro, "Review access"); dialogText("Signed review code", OptimizedReleaseRunner.reviewCode); dialogClick("Activate");
            await(pro, a -> has(a, "Review access active. No purchase required."));
            require(!context.getSharedPreferences("paperbuild_pro", 0).getBoolean("owned", false), "Review grant must not forge a Play purchase");
        }
        try (Screen screen = new Screen("MainActivity")) {
            options(screen, true); click(screen, "Saved presets · Pro"); dialogClick("Save current settings…");
            dialogText("Preset name", "R8 protected"); dialogClick("Save");
            build(screen, "R8 Protected"); share(screen, "protected.pdf");
            click(screen, "Saved presets · Pro"); dialogClick("R8 protected"); dialogClick("Use preset");
            String stored = context.getSharedPreferences("paperbuild_presets", 0).getAll().toString()
                    + context.getSharedPreferences("paperbuild_draft", 0).getAll().toString();
            require(!stored.contains("R8-test-only-123"), "Password leaked to persistent storage");
            // Trigger a protected export after loading its preset: must ask for a fresh password.
            name(screen, "R8 Protected preset"); click(screen, "Build PDF");
            AccessibilityNodeInfo field = dialogNode(n -> n.getContentDescription() != null && "Password · 8–64 characters".contentEquals(n.getContentDescription()));
            require(field.getText() == null || field.getText().toString().equals("Password · 8–64 characters") || field.getText().length() == 0, "Preset retained a password");
            dialogClick("Cancel");
        }
    }
    public void compactMergeExtractAndUnlimitedPages() throws Exception {
        try (Screen screen = new Screen("MainActivity")) {
            options(screen, false);
            screen.on(a -> find(a, v -> v instanceof Button && v.getContentDescription() != null && v.getContentDescription().toString().startsWith("PDF settings.")).performClick());
            click(screen, "US Letter"); click(screen, "Grayscale"); click(screen, "Smaller · Pro");
            build(screen, "R8 Compact"); compactPdf = share(screen, "compact.pdf"); render(compactPdf, 2);
            Instrumentation.ActivityMonitor monitor = picker(selected(freePdf, compactPdf));
            try { click(screen, "Merge PDFs"); } finally { instrumentation.removeMonitor(monitor); }
            await(screen, a -> has(a, "Merged PDF is ready.")); Uri merged = share(screen, "merged.pdf"); render(merged, 4);
            monitor = picker(selected(freePdf)); try { click(screen, "Extract pages"); } finally { instrumentation.removeMonitor(monitor); }
            dialogText(null, "2"); dialogClick("Create PDF"); await(screen, a -> has(a, "Extracted pages are ready."));
            Uri extracted = share(screen, "extracted.pdf"); render(extracted, 1);
            Uri[] added = new Uri[11];
            for (int i = 0; i < added.length; i++) {
                File copy = new File(context.getFilesDir(), "paperbuild/pages/extra-" + i + ".png");
                try (InputStream in = context.getContentResolver().openInputStream(image); FileOutputStream out = new FileOutputStream(copy)) {
                    byte[] bytes = new byte[8192]; int count; while ((count = in.read(bytes)) != -1) out.write(bytes, 0, count);
                }
                added[i] = Uri.parse("content://" + context.getPackageName() + ".fileprovider/pages/" + copy.getName());
            }
            monitor = picker(selected(added));
            try { click(screen, "Add images"); } finally { instrumentation.removeMonitor(monitor); }
            await(screen, a -> has(a, "13 pages · Draft saved"));
            build(screen, "R8 Thirteen pages"); Uri unlimited = share(screen, "unlimited.pdf"); render(unlimited, 13);
        }
    }
    public void editPdfAndLogoPresets() throws Exception {
        File logo = new File(context.getFilesDir(), "paperbuild/pages/brand.png");
        Bitmap bitmap = Bitmap.createBitmap(480, 180, Bitmap.Config.ARGB_8888); Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(3); paint.setColor(Color.rgb(55, 85, 210)); paint.setTextSize(65); canvas.drawText("KREATIV", 12, 105, paint);
        try (FileOutputStream out = new FileOutputStream(logo)) { require(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out), "Logo fixture failed"); } bitmap.recycle();
        Uri logoUri = Uri.parse("content://" + context.getPackageName() + ".fileprovider/pages/brand.png");
        try (Screen screen = new Screen("MainActivity")) {
            screen.on(a -> find(a, v -> v instanceof Button && ((Button) v).getText().toString().endsWith("Pro export options")).performClick());
            Instrumentation.ActivityMonitor monitor = picker(selected(logoUri));
            try { dialogClick("Choose logo"); } finally { instrumentation.removeMonitor(monitor); }
            dialogClick("Apply");
            String id = context.getSharedPreferences("paperbuild_draft", 0).getString("logo", "");
            require(id.matches("[a-f0-9-]{36}\\.png"), "Logo must be an immutable private asset");
            click(screen, "Saved presets · Pro"); dialogClick("Save current settings…"); dialogText("Preset name", "R8 logo"); dialogClick("Save");
            require(context.getSharedPreferences("paperbuild_presets", 0).getString("items", "").contains(id), "Preset must retain its logo");
            screen.recreate(); build(screen, "R8 Logo"); Uri branded = share(screen, "logo.pdf"); render(branded, 13);
        }
        try (Screen editor = new Screen("PdfEditorActivity")) {
            Instrumentation.ActivityMonitor monitor = picker(selected(freePdf));
            try { click(editor, "Choose PDF"); } finally { instrumentation.removeMonitor(monitor); }
            await(editor, a -> has(a, "Page 1 of 2"));
            describeClick(editor, "Edit page 2"); dialogClick("Move earlier");
            describeClick(editor, "Edit page 1"); dialogClick("Rotate clockwise");
            describeClick(editor, "Edit page 1"); dialogClick("Duplicate page");
            await(editor, a -> has(a, "Page 2 of 3")); editor.recreate();
            await(editor, a -> has(a, "Page 2 of 3")); click(editor, "Undo last edit");
            await(editor, a -> has(a, "Page 2 of 2"));
            describeClick(editor, "Edit page 2"); dialogClick("Remove page");
            await(editor, a -> has(a, "Page 1 of 1"));
            click(editor, "Save edited PDF"); await(editor, a -> has(a, "Edited PDF saved."));
        }
        try (Screen main = new Screen("MainActivity")) { Uri edited = share(main, "edited.pdf"); render(edited, 1); }
    }
}
