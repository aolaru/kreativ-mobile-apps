package com.kreativ.fontboard;

import android.os.SystemClock;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;

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

/** Uses the real public API; any temporary test favorite is removed without altering existing ones. */
@RunWith(AndroidJUnit4.class)
public class CatalogueWorkflowTest {
    static class State {
        int commercial, free, commercialPage, freePage, searchPage;
        boolean loading, searchLoading, searchFailed;
        String query, filter;
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
            s.searchFailed = search.failed;
            s.searchPage = search.page;
            s.query = ((EditText) field(activity, "search")).getText().toString();
            s.filter = field(activity, "searchFilter");
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
            assertFalse("Search request failed: filter=" + current.filter + ", page=" + current.searchPage, current.searchFailed);
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

    private Button favoriteButton(View view, String description) {
        if (view instanceof Button && description.contentEquals(view.getContentDescription() == null ? "" : view.getContentDescription())) return (Button) view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                Button button = favoriteButton(group.getChildAt(i), description);
                if (button != null) return button;
            }
        }
        return null;
    }

    private ImageView firstImage(View view) {
        if (view instanceof ImageView) return (ImageView) view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                ImageView image = firstImage(group.getChildAt(i));
                if (image != null) return image;
            }
        }
        return null;
    }

    private void awaitArtwork(ActivityScenario<MainActivity> scenario) {
        long until = SystemClock.elapsedRealtime() + 30000;
        AtomicReference<Boolean> ready = new AtomicReference<>(false);
        do {
            scenario.onActivity(activity -> {
                ImageView image = firstImage(field(activity, "content"));
                ready.set(image != null && (image.getDrawable() instanceof BitmapDrawable
                        || image.getDrawable() instanceof com.bumptech.glide.load.resource.gif.GifDrawable));
            });
            if (ready.get()) return;
            SystemClock.sleep(200);
        } while (SystemClock.elapsedRealtime() < until);
        fail("First visible website preview did not load");
    }

    private void assertLoadMoreLayout(ActivityScenario<MainActivity> scenario) {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        scenario.onActivity(activity -> {
            Map<String, HorizontalScrollView> shelves = field(activity, "shelves");
            View content = field(activity, "content");
            for (String availability : new String[]{"Commercial", "Free"}) {
                HorizontalScrollView shelf = shelves.get(availability);
                ViewGroup cards = (ViewGroup) shelf.getChildAt(0);
                View endCard = content.findViewWithTag("load_more_card." + availability);
                Button endButton = content.findViewWithTag("load_more_end." + availability);
                Button footer = content.findViewWithTag("load_more_footer." + availability);
                assertNotNull(endCard);
                assertNotNull(endButton);
                assertNotNull("Each section needs a directly discoverable action", footer);
                assertEquals("End card must align with the carousel height", cards.getHeight(), endCard.getHeight());
                assertTrue("End card must not be shorter than the font preview", endCard.getHeight() >= cards.getChildAt(0).getHeight());
                assertTrue("End action must have a 56dp tap target",
                        endButton.getHeight() >= Math.round(56 * activity.getResources().getDisplayMetrics().density));
                assertTrue("Footer must fill its section", footer.getWidth() >= shelf.getWidth());
                assertTrue("Footer must sit below the carousel", footer.getTop() >= shelf.getBottom());
                assertTrue(footer.getContentDescription().toString().contains(availability.equals("Free") ? "Free Fonts" : "Commercial Fonts"));
                // Both the centered end action and its touch area must be inside the end card.
                Rect bounds = new Rect();
                endButton.getDrawingRect(bounds);
                ((ViewGroup) endCard).offsetDescendantRectToMyCoords(endButton, bounds);
                assertTrue(bounds.left >= 0 && bounds.right <= endCard.getWidth());
                assertTrue(bounds.top >= 0 && bounds.bottom <= endCard.getHeight());
            }
        });
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
        AtomicReference<FontItem> temporary = new AtomicReference<>();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
            await(scenario, s -> !s.loading && s.commercialPage == 1 && s.freePage == 1);
            assertEquals(10, state(scenario).commercial);
            assertEquals(10, state(scenario).free);
            assertLoadMoreLayout(scenario);
            awaitArtwork(scenario);
            screenshot("home-ten-per-section");
            scenario.onActivity(activity -> {
                List<FontItem> loaded = field(activity, "fonts");
                for (FontItem font : loaded) if (!saved.contains(font.key())) {
                    temporary.set(font);
                    Button save = favoriteButton(field(activity, "content"), activity.getString(R.string.add_favorite, font.name));
                    assertNotNull(save);
                    assertTrue(save.performClick());
                    break;
                }
            });
            if (temporary.get() != null) assertTrue(store.favorites().stream().anyMatch(font -> font.key().equals(temporary.get().key())));
            scenario.onActivity(activity -> {
                Map<String, HorizontalScrollView> shelves = field(activity, "shelves");
                HorizontalScrollView shelf = shelves.get("Commercial");
                shelf.fullScroll(View.FOCUS_RIGHT);
                Button more = loadButton(shelf);
                assertNotNull("Missing end-of-shelf control", more);
                assertTrue(more.performClick());
                // Also exercise the always-visible alternative, without swiping Free.
                View content = field(activity, "content");
                Button footer = content.findViewWithTag("load_more_footer.Free");
                assertTrue(footer.performClick());
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

            scenario.onActivity(activity -> {
                Map<String, Button> filters = field(activity, "filterButtons");
                assertTrue(filters.get("Free").performClick());
            });
            State free = await(scenario, s -> !s.searchLoading && s.searchPage == 1 && s.filter.equals("Free"));
            assertEquals(10, free.results.size());
            for (FontItem font : free.results) assertEquals("Free", font.availability);
            scenario.onActivity(activity -> {
                Button more = loadButton(field(activity, "content"));
                assertNotNull(more);
                assertTrue(more.performClick());
            });
            free = await(scenario, s -> !s.searchLoading && s.searchPage == 2);
            assertEquals(20, free.results.size());
            for (FontItem font : free.results) assertEquals("Free", font.availability);
            scenario.onActivity(activity -> {
                Map<String, Button> filters = field(activity, "filterButtons");
                assertTrue(filters.get("Commercial").performClick());
            });
            State commercial = await(scenario, s -> !s.searchLoading && s.searchPage == 1 && s.filter.equals("Commercial"));
            assertEquals(10, commercial.results.size());
            for (FontItem font : commercial.results) assertEquals("Commercial", font.availability);
            scenario.onActivity(activity -> {
                Button more = loadButton(field(activity, "content"));
                assertNotNull(more);
                assertTrue(more.performClick());
            });
            commercial = await(scenario, s -> !s.searchLoading && s.searchPage == 2);
            assertEquals(20, commercial.results.size());
            for (FontItem font : commercial.results) assertEquals("Commercial", font.availability);
            screenshot("commercial-search-filter");

            scenario.recreate();
            scenario.onActivity(activity -> activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
            await(scenario, s -> !s.searchLoading && s.searchPage == 1 && !s.loading);
            assertEquals("serif", state(scenario).query);
            assertEquals("Commercial", state(scenario).filter);
            for (FontItem font : state(scenario).results) assertEquals("Commercial", font.availability);
            if (temporary.get() != null) {
                assertTrue("A newly saved favorite must survive recreation", store.favorites().stream().anyMatch(font -> font.key().equals(temporary.get().key())));
                scenario.onActivity(activity -> {
                    ((Button) field(activity, "favoritesTab")).performClick();
                    Button remove = favoriteButton(field(activity, "content"), activity.getString(R.string.remove_favorite, temporary.get().name));
                    assertNotNull(remove);
                    assertTrue(remove.performClick());
                    ((Button) field(activity, "discoverTab")).performClick();
                });
            }
            scenario.onActivity(activity -> ((EditText) field(activity, "search")).setText(""));
            assertEquals(10, state(scenario).commercial);
            assertEquals(10, state(scenario).free);
            awaitArtwork(scenario);
            screenshot("home-after-favorite-round-trip");
        } finally {
            // Only remove the specific item added by this test, even if an assertion fails.
            if (temporary.get() != null) {
                List<FontItem> after = store.favorites();
                after.removeIf(font -> font.key().equals(temporary.get().key()));
                store.saveFavorites(after);
            }
        }
        Set<String> after = new HashSet<>();
        for (FontItem font : store.favorites()) after.add(font.key());
        assertEquals("Existing favorites must survive", saved, after);
    }
}
