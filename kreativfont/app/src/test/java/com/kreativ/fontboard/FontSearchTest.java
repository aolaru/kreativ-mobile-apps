package com.kreativ.fontboard;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class FontSearchTest {
    private FontApi.Page page(int first, int last, int pages) {
        List<FontItem> fonts = new ArrayList<>();
        for (int id = first; id <= last; id++) fonts.add(new FontItem(id, "Font " + id, "Font " + id,
                "Description", "Commercial", "https://kreativfont.com/" + id, "", 2));
        return new FontApi.Page(fonts, pages);
    }

    @Test public void overlappingPagesStillDisplayTwentyUniqueResultsAndKeepAllExtras() throws Exception {
        List<Integer> requested = new ArrayList<>();
        FontSearch search = new FontSearch(number -> {
            requested.add(number);
            return number == 1 ? page(1, 10, 3) : number == 2 ? page(10, 19, 3) : page(20, 29, 3);
        });
        FontPager pager = new FontPager();
        FontPager.Request first = pager.begin();
        assertTrue(pager.complete(first, search.load(first.page)));
        assertEquals(10, pager.fonts.size());
        FontPager.Request second = pager.begin();
        assertTrue(pager.complete(second, search.load(second.page)));
        assertEquals(20, pager.fonts.size());
        assertEquals(java.util.Arrays.asList(1, 2, 3), requested);
        assertTrue(pager.hasMore());
        FontPager.Request third = pager.begin();
        assertTrue(pager.complete(third, search.load(third.page)));
        assertEquals(29, pager.fonts.size());
        assertEquals("Buffered fonts must not cause another network request", 3, requested.size());
        for (int i = 0; i < 29; i++) assertEquals(i + 1, pager.fonts.get(i).id);
        assertFalse(pager.hasMore());
    }

    @Test public void repeatedPagesCannotShowDuplicatesOrSkipLaterMatches() throws Exception {
        FontSearch search = new FontSearch(number -> number < 3 ? page(1, 10, 3) : page(11, 20, 3));
        Set<String> ids = new HashSet<>();
        for (int batch = 1; batch <= 2; batch++) {
            FontApi.Page result = search.load(batch);
            assertEquals(10, result.fonts.size());
            for (FontItem font : result.fonts) assertTrue(ids.add(font.key()));
        }
        assertEquals(20, ids.size());
    }

    @Test public void retryRetainsPartialBatchAndRetriesOnlyFailedServerPage() throws Exception {
        AtomicBoolean fail = new AtomicBoolean(true);
        List<Integer> requested = new ArrayList<>();
        FontSearch search = new FontSearch(number -> {
            requested.add(number);
            if (number == 3 && fail.getAndSet(false)) throw new java.io.IOException("Offline");
            return number == 1 ? page(1, 10, 3) : number == 2 ? page(10, 19, 3) : page(20, 29, 3);
        });
        search.load(1);
        assertThrows(java.io.IOException.class, () -> search.load(2));
        FontApi.Page result = search.load(2);
        assertEquals(10, result.fonts.size());
        for (int i = 0; i < 10; i++) assertEquals(11 + i, result.fonts.get(i).id);
        assertEquals(java.util.Arrays.asList(1, 2, 3, 3), requested);
        assertEquals(9, search.load(3).fonts.size());
    }

    @Test public void emptyAndPartialFinalBatchesStopCleanly() throws Exception {
        FontSearch empty = new FontSearch(number -> page(1, 0, 0));
        FontApi.Page result = empty.load(1);
        assertTrue(result.fonts.isEmpty());
        assertEquals(0, result.totalPages);
        FontSearch shortSearch = new FontSearch(number -> page(1, 7, 1));
        result = shortSearch.load(1);
        assertEquals(7, result.fonts.size());
        assertEquals(1, result.totalPages);
    }

    @Test public void repeatedOverlapsAreBoundedAndRetryKeepsProgress() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        FontSearch search = new FontSearch(number -> {
            calls.incrementAndGet();
            return number < 7 ? page(1, 10, 7) : page(11, 20, 7);
        });
        search.load(1);
        assertThrows(IllegalStateException.class, () -> search.load(2));
        assertEquals(6, calls.get());
        FontApi.Page next = search.load(2);
        assertEquals(10, next.fonts.size());
        assertEquals(11, next.fonts.get(0).id);
        assertEquals(7, calls.get());
        assertEquals(2, next.totalPages);
    }

    @Test public void aNewQuerySessionHasIndependentDuplicatesAndBuffers() throws Exception {
        FontSearch first = new FontSearch(number -> page(1, 10, 1));
        FontSearch replacement = new FontSearch(number -> page(1, 10, 1));
        assertEquals(10, first.load(1).fonts.size());
        assertEquals(10, replacement.load(1).fonts.size());
    }
}
