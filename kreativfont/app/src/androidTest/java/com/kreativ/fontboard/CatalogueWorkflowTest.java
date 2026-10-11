package com.kreativ.fontboard;

import android.os.SystemClock;
import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import static org.junit.Assert.*;

/** Uses the real public API; does not add/remove favorites or change device/network settings. */
@RunWith(AndroidJUnit4.class)
public class CatalogueWorkflowTest {
    static class State {
        int commercial, free, commercialPage, freePage, searchPage;
        boolean loading, searchLoading;
        String query;
        List<FontItem> results;
    }

    @SuppressWarnings("unchecked")
    private static <T> T field(MainActivity activity, String name) {
        try {
            Field field = MainActivity.class.getDeclaredField(name);
            field.setAccessible(true);
            return (T) field.get(activity);
        } catch (Exception failed) { throw new AssertionError(failed); }
    }

    private State state(ActivityScenario<MainActivity> scenario) {
        AtomicReference<State> result = new AtomicReference<>();
        scenario.onActivity(activity -> {
            State s = new State();
            FontPager commercial = field(activity, "commercialPager"), free = field(activity, "freePager");
            FontPager search = field(activity, "searchPager");
            s.commercial = commercial.fonts.size(); s.free = free.fonts.size();
            s.commercialPage = commercial.page; s.freePage = free.page;
            s.loading = (boolean) field(activity, "loading") || commercial.loading || free.loading;
            s.searchLoading = search.loading || (boolean) field(activity, "searchWaiting");
            s.searchPage = search.page;
            s.query = ((EditText) field(activity, "search")).getText().toString();
            s.results = new ArrayList<>(search.fonts);
            result.set(s);
        });
        return result.get();
    }

    private State await(ActivityScenario<MainActivity> scenario, Predicate<State> condition) {
        long until = SystemClock.elapsedRealtime() + 45000;
        State current;
        do {
            current = state(scenario);
            if (condition.test(current)) return current;
            SystemClock.sleep(200);
        } while (SystemClock.elapsedRealtime() < until);
        fail("Catalogue state timed out: commercial=" + current.commercial + ", free=" + current.free
                + ", searchPage=" + current.searchPage + ", results=" + current.results.size());
        return current;
    }

    private Button loadButton(View view) {
        if (view instanceof Button && ((Button) view).getText().toString().equals("Load 10 more →")) return (Button) view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                Button button = loadButton(group.getChildAt(i));
                if (button != null) return button;
            }
        }
        return null;
    }

    private void screenshot(String name) {
        SystemClock.sleep(250);
        Bitmap image = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        assertNotNull(image);
        File folder = InstrumentationRegistry.getInstrumentation().getTargetContext().getExternalFilesDir("review");
        assertNotNull(folder);
        try (FileOutputStream output = new FileOutputStream(new File(folder, name + ".png"))) {
            assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, output));
        } catch (Exception failed) { throw new AssertionError(failed); }
        finally { image.recycle(); }
    }

    @Test public void tenPerSectionLoadMoreWholeCatalogueSearchAndRotation() {
        FontStore store = new FontStore(InstrumentationRegistry.getInstrumentation().getTargetContext());
        Set<String> saved = new HashSet<>();
        for (FontItem font : store.favorites()) saved.add(font.key());
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
            await(scenario, s -> !s.loading && s.commercialPage == 1 && s.freePage == 1);
            assertEquals(10, state(scenario).commercial);
            assertEquals(10, state(scenario).free);
            screenshot("home-ten-per-section");
            scenario.onActivity(activity -> {
                Map<String, HorizontalScrollView> shelves = field(activity, "shelves");
                for (String availability : new String[]{"Commercial", "Free"}) {
                    HorizontalScrollView shelf = shelves.get(availability);
                    assertNotNull(shelf);
                    shelf.fullScroll(View.FOCUS_RIGHT);
                    Button more = loadButton(shelf);
                    assertNotNull("Missing end-of-shelf control", more);
                    assertTrue(more.performClick());
                }
            });
            await(scenario, s -> !s.loading && s.commercialPage == 2 && s.freePage == 2);
            assertEquals(20, state(scenario).commercial);
            assertEquals(20, state(scenario).free);
            assertEquals(2, store.page("Commercial"));
            assertEquals(2, store.page("Free"));
            Set<Long> displayed = new HashSet<>();
            for (FontItem font : store.feed()) assertTrue(displayed.add(font.id));
            assertEquals(40, displayed.size());
            scenario.onActivity(activity -> {
                Map<String, HorizontalScrollView> shelves = field(activity, "shelves");
                shelves.get("Commercial").fullScroll(View.FOCUS_RIGHT);
            });
            screenshot("commercial-load-more");

            scenario.onActivity(activity -> ((EditText) field(activity, "search")).setText("Cookie"));
            State cookie = await(scenario, s -> !s.searchLoading && s.searchPage == 1);
            assertFalse(cookie.results.isEmpty());
            assertTrue("Search must find older articles outside the loaded shelves",
                    cookie.results.stream().anyMatch(font -> !displayed.contains(font.id)));
            assertTrue(cookie.results.stream().anyMatch(font -> font.availability.equals("Free")));
            screenshot("whole-catalogue-search");

            scenario.onActivity(activity -> ((EditText) field(activity, "search")).setText("serif"));
            await(scenario, s -> !s.searchLoading && s.searchPage == 1);
            assertEquals(10, state(scenario).results.size());
            scenario.onActivity(activity -> {
                Button more = loadButton(field(activity, "content"));
                assertNotNull(more);
                assertTrue(more.performClick());
            });
            await(scenario, s -> !s.searchLoading && s.searchPage == 2);
            assertEquals(20, state(scenario).results.size());
            screenshot("paged-search-results");

            scenario.recreate();
            scenario.onActivity(activity -> activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
            await(scenario, s -> !s.searchLoading && s.searchPage == 1 && !s.loading);
            assertEquals("serif", state(scenario).query);
            scenario.onActivity(activity -> ((EditText) field(activity, "search")).setText(""));
            assertEquals(10, state(scenario).commercial);
            assertEquals(10, state(scenario).free);
        }
        Set<String> after = new HashSet<>();
        for (FontItem font : store.favorites()) after.add(font.key());
        assertEquals("Existing favorites must survive", saved, after);
    }
}
