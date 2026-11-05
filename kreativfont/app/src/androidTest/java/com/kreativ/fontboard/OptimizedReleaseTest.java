package com.kreativ.fontboard;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.graphics.Matrix;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.function.Consumer;

import static org.junit.Assert.*;

/** Exercises the actual optimized APK without app-private types or reflection/keep rules. */
public class OptimizedReleaseTest {
    /** Platform-only lifecycle wrapper: no dependency on stripped AndroidX app classes. */
    private static final class Scenario implements AutoCloseable {
        private final Instrumentation instrumentation = OptimizedReleaseRunner.instance;
        private Activity activity;

        static Scenario launch(Intent intent) { return new Scenario(intent); }
        private Scenario(Intent intent) { activity = instrumentation.startActivitySync(intent); }
        void onActivity(Consumer<Activity> action) {
            AtomicReference<Throwable> failure = new AtomicReference<>();
            instrumentation.runOnMainSync(() -> {
                try { action.accept(activity); } catch (Throwable failed) { failure.set(failed); }
            });
            // Do not let an assertion escape on the app's main thread and strand
            // Instrumentation.runOnMainSync's waiting test thread.
            if (failure.get() != null) throw new AssertionError(failure.get());
        }
        void recreate() {
            Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(activity.getClass().getName(), null, false);
            try {
                onActivity(Activity::recreate);
                activity = instrumentation.waitForMonitorWithTimeout(monitor, 10000);
                assertNotNull("Activity did not recreate", activity);
                instrumentation.waitForIdleSync();
                onActivity(a -> a.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
            } finally { instrumentation.removeMonitor(monitor); }
        }
        @Override public void close() { onActivity(Activity::finish); instrumentation.waitForIdleSync(); }
    }

    private Context context() {
        return OptimizedReleaseRunner.instance.getTargetContext();
    }

    private Scenario launch(String activity) {
        Intent intent = new Intent(Intent.ACTION_MAIN)
                .setClassName(context().getPackageName(), "com.kreativ.fontboard." + activity)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Scenario scenario = Scenario.launch(intent);
        scenario.onActivity(a -> a.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
        return scenario;
    }

    private List<View> views(View root, Predicate<View> condition) {
        List<View> result = new ArrayList<>();
        if (condition.test(root)) result.add(root);
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) result.addAll(views(group.getChildAt(i), condition));
        }
        return result;
    }

    private View find(Activity activity, Predicate<View> condition) {
        List<View> matches = views(activity.getWindow().getDecorView(), condition);
        assertFalse("Expected UI control not found", matches.isEmpty());
        return matches.get(0);
    }

    private View tagged(Activity activity, String tag) {
        View result = activity.getWindow().getDecorView().findViewWithTag(tag);
        assertNotNull("Missing UI tag: " + tag, result);
        return result;
    }

    private String description(View view) {
        return view.getContentDescription() == null ? "" : view.getContentDescription().toString();
    }

    private void clickDialog(String text) {
        long deadline = SystemClock.elapsedRealtime() + 10000;
        while (SystemClock.elapsedRealtime() < deadline) {
            for (android.view.accessibility.AccessibilityWindowInfo window :
                    OptimizedReleaseRunner.instance.automation().getWindows()) {
                AccessibilityNodeInfo root = window.getRoot();
                if (root != null) for (AccessibilityNodeInfo node : root.findAccessibilityNodeInfosByText(text)) {
                    if (node.getText() != null && text.equalsIgnoreCase(node.getText().toString())
                            && node.isClickable() && node.isEnabled()
                            && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return;
                }
            }
            SystemClock.sleep(100);
        }
        fail("Dialog control unavailable: " + text);
    }

    private AccessibilityNodeInfo editable(AccessibilityNodeInfo node) {
        if (node == null) return null;
        if (node.isEditable()) return node;
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo result = editable(node.getChild(i));
            if (result != null) return result;
        }
        return null;
    }

    @Test public void shareAndProjectCollectionRoundTrip() throws Exception {
        String project = "Kreativ test " + System.currentTimeMillis();
        Context ctx = context();
        Set<String> favoritesBefore = favoriteKeys();
        JSONObject before = new JSONObject(ctx.getSharedPreferences("catalogue_v1", Context.MODE_PRIVATE)
                .getString("collections", "{}"));
        Instrumentation instrumentation = OptimizedReleaseRunner.instance;
        AtomicReference<Intent> shared = new AtomicReference<>();
        Instrumentation.ActivityMonitor monitor = new Instrumentation.ActivityMonitor() {
            @Override public Instrumentation.ActivityResult onStartActivity(Intent intent) {
                if (!Intent.ACTION_CHOOSER.equals(intent.getAction())) return null;
                shared.set(intent.getParcelableExtra(Intent.EXTRA_INTENT));
                return new Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null);
            }
        };
        try (Scenario scenario = launch("MainActivity")) {
            await(scenario, a -> resultCount(a) >= 20 && tagged(a, "load_more_footer.Commercial").isEnabled());
            scenario.onActivity(a -> find(a, v -> v instanceof TextView && !(v instanceof Button)
                    && !(v instanceof EditText) && v.isClickable()).performClick());
            instrumentation.addMonitor(monitor);
            try { clickDialog("Share font"); }
            finally { instrumentation.removeMonitor(monitor); }
            assertNotNull("Share chooser should open", shared.get());
            assertEquals(Intent.ACTION_SEND, shared.get().getAction());
            assertEquals("text/plain", shared.get().getType());
            assertTrue(shared.get().getStringExtra(Intent.EXTRA_TEXT).startsWith("https://kreativfont.com/"));
            clickDialog("Add to collection");
            clickDialog("New collection");
            AccessibilityNodeInfo field = editable(OptimizedReleaseRunner.instance.automation().getRootInActiveWindow());
            assertNotNull("Collection name input", field);
            Bundle value = new Bundle();
            value.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, project);
            assertTrue(field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, value));
            clickDialog("Save");
            JSONObject after = new JSONObject(ctx.getSharedPreferences("catalogue_v1", Context.MODE_PRIVATE)
                    .getString("collections", "{}"));
            assertEquals(1, after.getJSONArray(project).length());
            scenario.recreate();
            assertEquals(1, new JSONObject(ctx.getSharedPreferences("catalogue_v1", Context.MODE_PRIVATE)
                    .getString("collections", "{}")).getJSONArray(project).length());
            scenario.onActivity(a -> find(a, v -> v instanceof Button && ((Button) v).getText().toString().startsWith("Favorites (")).performClick());
            scenario.onActivity(a -> find(a, v -> v instanceof Button && text(v, "Project collections")).performClick());
            clickDialog(project);
            clickDialog("Delete collection");
            long confirmDeadline = SystemClock.elapsedRealtime() + 10000;
            boolean confirmationVisible = false;
            while (SystemClock.elapsedRealtime() < confirmDeadline) {
                AccessibilityNodeInfo root = OptimizedReleaseRunner.instance.automation().getRootInActiveWindow();
                if (root != null && !root.findAccessibilityNodeInfosByText("Delete this collection?").isEmpty()) {
                    confirmationVisible = true;
                    break;
                }
                SystemClock.sleep(100);
            }
            assertTrue("Delete confirmation should appear", confirmationVisible);
            clickDialog("Delete collection");
            instrumentation.waitForIdleSync();
            JSONObject finalProjects = new JSONObject(ctx.getSharedPreferences("catalogue_v1", Context.MODE_PRIVATE)
                    .getString("collections", "{}"));
            assertFalse(finalProjects.has(project));
            assertEquals(before.length(), finalProjects.length());
            java.util.Iterator<String> keys = before.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                assertEquals(before.getJSONArray(key).toString(), finalProjects.getJSONArray(key).toString());
            }
            assertEquals(favoritesBefore, favoriteKeys());
        } finally {
            // If an assertion interrupted the UI cleanup, remove only this uniquely named test collection.
            android.content.SharedPreferences prefs = ctx.getSharedPreferences("catalogue_v1", Context.MODE_PRIVATE);
            JSONObject cleanup = new JSONObject(prefs.getString("collections", "{}"));
            if (cleanup.has(project)) {
                cleanup.remove(project);
                assertTrue(prefs.edit().putString("collections", cleanup.toString()).commit());
            }
        }
    }

    private boolean text(View view, String value) {
        return view instanceof TextView && value.contentEquals(((TextView) view).getText());
    }

    private int resultCount(Activity activity) {
        // Each actual card has exactly one article button; footer and paging controls do not.
        return views(activity.getWindow().getDecorView(), v -> v instanceof Button
                && description(v).startsWith("Open ") && description(v).endsWith(" on Kreativ Font")).size();
    }

    private void await(Scenario scenario, Predicate<Activity> ready) {
        long deadline = SystemClock.elapsedRealtime() + 45000;
        AtomicReference<Boolean> result = new AtomicReference<>(false);
        do {
            scenario.onActivity(a -> result.set(ready.test(a)));
            if (result.get()) return;
            SystemClock.sleep(200);
        } while (SystemClock.elapsedRealtime() < deadline);
        AtomicReference<String> diagnostic = new AtomicReference<>();
        scenario.onActivity(a -> diagnostic.set("cards=" + resultCount(a) + ", controls="
                + views(a.getWindow().getDecorView(), v -> v instanceof Button).stream()
                .map(v -> ((Button) v).getText().toString()).collect(java.util.stream.Collectors.joining(" | "))));
        fail("Optimized release UI did not reach the expected state within 45 seconds: " + diagnostic.get());
    }

    private void searchReady(Scenario scenario, int expected) {
        await(scenario, a -> resultCount(a) == expected && !views(a.getWindow().getDecorView(),
                v -> text(v, "Results from the full website font catalogue.")).isEmpty());
    }

    private Set<String> favoriteKeys() throws Exception {
        JSONArray saved = new JSONArray(context().getSharedPreferences("catalogue_v1", Context.MODE_PRIVATE)
                .getString("favorites", "[]"));
        Set<String> keys = new HashSet<>();
        for (int i = 0; i < saved.length(); i++) {
            JSONObject item = saved.getJSONObject(i);
            keys.add(item.optLong("id") > 0 ? "post:" + item.optLong("id")
                    : "legacy:" + item.optString("name").toLowerCase(java.util.Locale.ROOT));
        }
        return keys;
    }

    private void assertFooter(Activity activity) throws Exception {
        ViewGroup footer = (ViewGroup) tagged(activity, "version_credit");
        String version = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionName;
        TextView versionLabel = (TextView) footer.getChildAt(0);
        assertEquals("Version " + version + " ·", versionLabel.getText().toString());
        assertFalse(versionLabel.isClickable());
        Button maker = (Button) tagged(activity, "maker_credit");
        assertTrue(maker.isClickable());
        assertTrue(maker.getMinimumHeight() >= Math.round(48 * activity.getResources().getDisplayMetrics().density));
    }

    @Test public void catalogueSearchFavoritesAndRecreationSurviveOptimization() throws Exception {
        Set<String> original = favoriteKeys();
        AtomicReference<String> temporaryName = new AtomicReference<>();
        try (Scenario scenario = launch("MainActivity")) {
            try {
                await(scenario, a -> resultCount(a) == 20 && tagged(a, "load_more_footer.Commercial").isEnabled()
                        && tagged(a, "load_more_footer.Free").isEnabled());
                int expectedCards = 20;
                for (String section : new String[]{"Commercial", "Free"}) {
                    scenario.onActivity(a -> {
                        HorizontalScrollView shelf = (HorizontalScrollView) find(a, v -> v instanceof HorizontalScrollView
                                && description(v).equals(section + " Fonts"));
                        assertEquals(11, ((ViewGroup) shelf.getChildAt(0)).getChildCount());
                        assertTrue(tagged(a, "load_more_footer." + section).performClick());
                    });
                    expectedCards += 10;
                    final int expected = expectedCards;
                    await(scenario, a -> resultCount(a) == expected);
                }
                scenario.onActivity(a -> {
                    Button save = (Button) find(a, v -> v instanceof Button && description(v).startsWith("Save ")
                            && description(v).endsWith(" to favorites"));
                    String label = description(save);
                    temporaryName.set(label.substring(5, label.length() - " to favorites".length()));
                    assertTrue(save.performClick());
                });
                assertEquals(original.size() + 1, favoriteKeys().size());
                scenario.recreate();
                assertEquals(original.size() + 1, favoriteKeys().size());
                scenario.onActivity(a -> {
                    assertTrue(find(a, v -> v instanceof Button && ((Button) v).getText().toString().startsWith("Favorites (")).performClick());
                    assertNotNull(find(a, v -> description(v).equals("Remove " + temporaryName.get() + " from favorites")));
                    assertTrue(find(a, v -> description(v).equals("Remove " + temporaryName.get() + " from favorites")).performClick());
                    temporaryName.set(null);
                    assertTrue(find(a, v -> v instanceof Button && text(v, "Fonts")).performClick());
                    ((EditText) find(a, v -> v instanceof EditText)).setText("serif");
                });
                assertEquals(original, favoriteKeys());
                for (String filter : new String[]{"All", "Commercial", "Free"}) {
                    // Pace real-server requests; this is not an API load test.
                    SystemClock.sleep(3000);
                    scenario.onActivity(a -> tagged(a, "search_filter." + filter).performClick());
                    searchReady(scenario, 10);
                    SystemClock.sleep(3000);
                    scenario.onActivity(a -> {
                        assertTrue(tagged(a, "search_filter." + filter).isSelected());
                        if (!filter.equals("All")) assertEquals(10, views(a.getWindow().getDecorView(),
                                v -> text(v, filter.toUpperCase(java.util.Locale.ROOT))).size());
                        assertTrue(find(a, v -> v instanceof Button && text(v, "Load 10 more →")).performClick());
                    });
                    searchReady(scenario, 20);
                }
                scenario.recreate();
                searchReady(scenario, 10);
                scenario.onActivity(a -> {
                    assertTrue(tagged(a, "search_filter.Free").isSelected());
                    try { assertFooter(a); } catch (Exception e) { throw new AssertionError(e); }
                });
            } finally {
                // Only remove the UI-created temporary favorite, never clear or overwrite user data.
                if (temporaryName.get() != null) scenario.onActivity(a -> {
                    find(a, v -> v instanceof Button && ((Button) v).getText().toString().startsWith("Favorites (")).performClick();
                    find(a, v -> description(v).equals("Remove " + temporaryName.get() + " from favorites")).performClick();
                });
            }
        }
        assertEquals(original, favoriteKeys());
    }

    @Test public void originalArtworkZoomAndBrowserLinksSurviveOptimization() throws Exception {
        try (Scenario fonts = launch("MainActivity")) {
            await(fonts, a -> resultCount(a) >= 20 && tagged(a, "load_more_footer.Commercial").isEnabled());
        }
        JSONArray feed = new JSONArray(context().getSharedPreferences("catalogue_v1", Context.MODE_PRIVATE).getString("feed", "[]"));
        assertTrue("The loaded catalogue should be cached", feed.length() > 0);
        JSONObject font = feed.getJSONObject(0);
        Intent previewIntent = new Intent().setClassName(context().getPackageName(), "com.kreativ.fontboard.FontPreviewActivity")
                .putExtra("image", font.optString("full_image", font.optString("image")))
                .putExtra("name", font.getString("name")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try (Scenario preview = Scenario.launch(previewIntent)) {
            await(preview, a -> ((ImageView) tagged(a, "full_screen_preview")).getDrawable() != null
                    && tagged(a, "preview_zoom_in").isEnabled());
            AtomicReference<Float> fittedScale = new AtomicReference<>();
            preview.onActivity(a -> {
                a.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                float[] matrix = new float[9];
                ((ImageView) tagged(a, "full_screen_preview")).getImageMatrix().getValues(matrix);
                fittedScale.set(matrix[Matrix.MSCALE_X]);
                tagged(a, "preview_zoom_in").performClick();
                ((ImageView) tagged(a, "full_screen_preview")).getImageMatrix().getValues(matrix);
                assertEquals(fittedScale.get() * 1.5f, matrix[Matrix.MSCALE_X], .001f);
            });
            preview.recreate();
            await(preview, a -> tagged(a, "preview_zoom_in").isEnabled());
            preview.onActivity(a -> {
                float[] matrix = new float[9];
                ImageView image = (ImageView) tagged(a, "full_screen_preview");
                image.getImageMatrix().getValues(matrix);
                assertEquals(fittedScale.get() * 1.5f, matrix[Matrix.MSCALE_X], .001f);
                tagged(a, "preview_zoom_fit").performClick();
                image.getImageMatrix().getValues(matrix);
                assertEquals(fittedScale.get(), matrix[Matrix.MSCALE_X], .001f);
            });
        }
        Instrumentation instrumentation = OptimizedReleaseRunner.instance;
        AtomicReference<Intent> opened = new AtomicReference<>();
        Instrumentation.ActivityMonitor monitor = new Instrumentation.ActivityMonitor() {
            @Override public Instrumentation.ActivityResult onStartActivity(Intent intent) {
                opened.set(intent);
                return new Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null);
            }
        };
        try (Scenario scenario = launch("MainActivity")) {
            instrumentation.addMonitor(monitor);
            try {
                scenario.onActivity(a -> {
                    try { assertFooter(a); } catch (Exception e) { throw new AssertionError(e); }
                    tagged(a, "maker_credit").performClick();
                });
                assertNotNull(opened.get());
                assertEquals(Intent.ACTION_VIEW, opened.get().getAction());
                assertEquals("https://madebykreativ.com/apps", opened.get().getDataString());
                assertNull(opened.get().getComponent());
            } finally { instrumentation.removeMonitor(monitor); }
        }
    }
}
