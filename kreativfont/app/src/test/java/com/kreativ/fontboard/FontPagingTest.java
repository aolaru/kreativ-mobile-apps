package com.kreativ.fontboard;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

public class FontPagingTest {
    private FontItem font(long id) {
        return new FontItem(id, "Font " + id, "Font " + id, "Description", "Commercial",
                "https://kreativfont.com/fonts/" + id, "", 2);
    }

    @Test public void requestsTenAtATimeAndKeepsSectionsSeparate() {
        assertTrue(FontApi.feedUrl("Commercial", 2).contains("categories=3&categories_exclude=23584&per_page=10&page=2"));
        assertTrue(FontApi.feedUrl("Free", 3).contains("categories=23584&per_page=10&page=3"));
        assertThrows(IllegalArgumentException.class, () -> FontApi.feedUrl("Free", 0));
        assertThrows(IllegalArgumentException.class, () -> FontApi.feedUrl("Unknown", 1));
    }

    @Test public void searchesBothCategoriesAcrossAllDatesWithSafeEncoding() {
        String url = FontApi.searchUrl(" A&B + café / serif? ", 2);
        assertTrue(url.contains("categories=3,23584&search=A%26B+%2B+caf%C3%A9+%2F+serif%3F"));
        assertTrue(url.contains("per_page=10&page=2&orderby=relevance"));
        assertTrue(url.contains("excerpt,categories"));
        assertFalse(url.contains("after="));
        assertThrows(IllegalArgumentException.class, () -> FontApi.searchUrl("  ", 1));
    }

    @Test public void classifiesSearchResultsWithFreeTakingPriority() throws Exception {
        JSONObject post = new JSONObject().put("id", 1).put("title", new JSONObject().put("rendered", "Example"))
                .put("link", "https://kreativfont.com/example").put("categories", new JSONArray().put(3).put(23584));
        assertEquals("Free", FontCatalog.parseFeed(new JSONArray().put(post).toString(), null).get(0).availability);
        post.put("categories", new JSONArray().put(3));
        assertEquals("Commercial", FontCatalog.parseFeed(new JSONArray().put(post).toString(), null).get(0).availability);
        post.put("categories", new JSONArray().put(99));
        assertThrows(org.json.JSONException.class, () -> FontCatalog.parseFeed(new JSONArray().put(post).toString(), null));
    }

    @Test public void searchFiltersAreSentToTheServerOnEveryPage() {
        String free = FontApi.searchUrl("serif", 2, "Free");
        assertTrue(free.contains("categories=23584&search=serif"));
        assertTrue(free.contains("page=2"));
        assertFalse(free.contains("categories=3,"));
        String commercial = FontApi.searchUrl("serif", 3, "Commercial");
        assertTrue(commercial.contains("categories=3&categories_exclude=23584&search=serif"));
        assertTrue(commercial.contains("page=3"));
        assertEquals(FontApi.searchUrl("serif", 1), FontApi.searchUrl("serif", 1, "All"));
        assertThrows(IllegalArgumentException.class, () -> FontApi.searchUrl("serif", 1, "Unknown"));
    }

    @Test public void appendsDeduplicatedPagesAndStopsAtTheEnd() {
        FontPager pager = new FontPager();
        FontPager.Request first = pager.begin();
        assertNull(pager.begin());
        assertTrue(pager.complete(first, new FontApi.Page(Arrays.asList(font(1), font(2)), 2)));
        assertTrue(pager.hasMore());
        FontPager.Request next = pager.begin();
        assertEquals(2, next.page);
        assertTrue(pager.complete(next, new FontApi.Page(Arrays.asList(font(2), font(3)), 2)));
        assertEquals(3, pager.fonts.size());
        assertEquals(1, pager.fonts.get(0).id);
        assertEquals(3, pager.fonts.get(2).id);
        assertFalse(pager.hasMore());
        assertNull(pager.begin());
    }

    @Test public void failedPageKeepsExistingResultsAndRetriesSamePage() {
        FontPager pager = new FontPager();
        pager.restore(Collections.singletonList(font(1)), 1, 4);
        assertTrue(pager.fail(pager.begin()));
        assertEquals(1, pager.fonts.size());
        assertEquals(1, pager.page);
        assertTrue(pager.failed);
        FontPager.Request retry = pager.begin();
        assertEquals(2, retry.page);
        assertFalse(pager.failed);
        assertTrue(pager.complete(retry, new FontApi.Page(Collections.singletonList(font(2)), 4)));
        assertEquals(2, pager.fonts.size());
    }

    @Test public void ignoresLateResultsAndFailuresAfterQueryChange() {
        FontPager pager = new FontPager();
        FontPager.Request old = pager.begin();
        pager.reset();
        FontPager.Request current = pager.begin();
        assertFalse(pager.complete(old, new FontApi.Page(Collections.singletonList(font(99)), 1)));
        assertFalse(pager.fail(old));
        assertTrue(pager.loading);
        assertTrue(pager.complete(current, new FontApi.Page(Collections.singletonList(font(1)), 1)));
        assertEquals(1, pager.fonts.get(0).id);
    }

    @Test public void exhaustedOrShrinkingCatalogueFinishesWithoutLosingCachedFonts() {
        FontPager pager = new FontPager();
        pager.restore(Collections.singletonList(font(1)), 1, 2);
        assertTrue(pager.complete(pager.begin(), new FontApi.Page(Collections.emptyList(), 1)));
        assertEquals(1, pager.fonts.size());
        assertFalse(pager.hasMore());
        pager.reset();
        assertTrue(pager.complete(pager.begin(), new FontApi.Page(Collections.emptyList(), 0)));
        assertTrue(pager.fonts.isEmpty());
        assertFalse(pager.hasMore());
    }

    @Test public void independentSectionsAndRefreshDoNotSkipPages() {
        FontPager commercial = new FontPager(), free = new FontPager();
        FontPager.Request stale = commercial.begin();
        assertEquals(1, free.begin().page);
        commercial.invalidate();
        commercial.restore(Collections.singletonList(font(1)), 1, 20);
        assertFalse(commercial.complete(stale, new FontApi.Page(Collections.singletonList(font(99)), 20)));
        assertEquals(2, commercial.begin().page);
        assertEquals(0, free.page);
    }
}
