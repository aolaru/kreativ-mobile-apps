package com.kreativ.fontboard;

import org.junit.Assume;
import org.junit.Test;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/** Opt-in contract check against the public production feeds, with no external writes. */
public class LiveCatalogTest {
    @Test public void latestTwentyHaveDescriptionsArticleLinksAndAccessiblePreviews() throws Exception {
        Assume.assumeTrue("true".equals(System.getenv("KREATIVFONT_LIVE_TESTS")));
        Set<Long> ids = new HashSet<>();
        for (String availability : new String[]{"Commercial", "Free"}) {
            List<FontItem> fonts = FontApi.fetch(FontApi.feedUrl(availability, 1), availability, 1).fonts;
            assertEquals(10, fonts.size());
            for (FontItem font : fonts) {
                assertTrue("Duplicate article: " + font.name, ids.add(font.id));
                assertFalse(font.description.isEmpty());
                assertTrue(FontCatalog.safeUrl(font.articleUrl, false));
                assertTrue("Missing website artwork: " + font.name, FontCatalog.safeUrl(font.imageUrl, true));
                HttpURLConnection image = (HttpURLConnection) new URL(font.imageUrl).openConnection();
                image.setConnectTimeout(10000);
                image.setReadTimeout(10000);
                try {
                    assertEquals("Artwork request failed: " + font.name, 200, image.getResponseCode());
                    assertTrue(image.getContentType().startsWith("image/"));
                } finally { image.disconnect(); }
            }
        }
        assertEquals(20, ids.size());
    }

    @Test public void nextPagesAndSearchReachBeyondTheLatestFeed() throws Exception {
        Assume.assumeTrue("true".equals(System.getenv("KREATIVFONT_LIVE_TESTS")));
        Set<Long> latest = new HashSet<>();
        for (String availability : new String[]{"Commercial", "Free"}) {
            FontApi.Page first = FontApi.fetch(FontApi.feedUrl(availability, 1), availability, 1);
            FontApi.Page second = FontApi.fetch(FontApi.feedUrl(availability, 2), availability, 2);
            assertEquals(10, first.fonts.size());
            assertEquals(10, second.fonts.size());
            assertTrue(first.totalPages > 2);
            for (FontItem font : first.fonts) assertTrue(latest.add(font.id));
            for (FontItem font : second.fonts) assertFalse(latest.contains(font.id));
        }
        FontApi.Page results = FontApi.fetch(FontApi.searchUrl("Cookie", 1), null, 1);
        assertFalse(results.fonts.isEmpty());
        assertTrue(results.fonts.stream().anyMatch(font -> !latest.contains(font.id)));
        FontSearch search = new FontSearch("serif", "All");
        FontApi.Page serif = search.load(1);
        assertEquals(10, serif.fonts.size());
        assertTrue(serif.totalPages > 1);
        FontApi.Page next = search.load(2);
        Set<Long> searchIds = new HashSet<>();
        for (FontItem font : serif.fonts) assertTrue(searchIds.add(font.id));
        for (FontItem font : next.fonts) assertTrue(searchIds.add(font.id));
        assertEquals(20, searchIds.size());
    }

    @Test public void filteredSearchQueriesTheWholeCatalogue() throws Exception {
        Assume.assumeTrue("true".equals(System.getenv("KREATIVFONT_LIVE_TESTS")));
        for (String availability : new String[]{"Commercial", "Free"}) {
            FontSearch search = new FontSearch("serif", availability);
            FontApi.Page page = search.load(1);
            assertEquals(10, page.fonts.size());
            assertTrue(page.totalPages > 1);
            for (FontItem font : page.fonts) {
                assertEquals(availability, font.availability);
                assertTrue(FontCatalog.safeUrl(font.fullImageUrl, true));
            }
            Set<Long> ids = new HashSet<>();
            for (FontItem font : page.fonts) assertTrue(ids.add(font.id));
            FontApi.Page next;
            try { next = search.load(2); }
            catch (Exception unavailable) { throw new AssertionError(availability + " second search batch unavailable", unavailable); }
            assertEquals(10, next.fonts.size());
            for (FontItem font : next.fonts) {
                assertEquals(availability, font.availability);
                assertTrue("Search batches must not repeat article IDs", ids.add(font.id));
            }
            assertEquals(20, ids.size());
        }
    }
}
