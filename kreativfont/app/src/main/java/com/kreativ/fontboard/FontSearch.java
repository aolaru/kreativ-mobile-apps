package com.kreativ.fontboard;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** One search session, used only by its worker task. Server pages may overlap. */
final class FontSearch {
    interface Fetcher { FontApi.Page fetch(int page) throws Exception; }

    private static final int MAX_PAGES_PER_LOAD = 5;
    private final Fetcher fetcher;
    private final ArrayDeque<FontItem> pending = new ArrayDeque<>();
    private final Set<String> seen = new HashSet<>();
    private int nextPage = 1, totalPages = Integer.MAX_VALUE;

    FontSearch(String query, String availability) {
        this(page -> FontApi.fetch(FontApi.searchUrl(query, page, availability), null, page));
    }

    FontSearch(Fetcher fetcher) { this.fetcher = fetcher; }

    FontApi.Page load(int displayPage) throws Exception {
        int fetched = 0;
        while (pending.size() < FontApi.PAGE_SIZE && nextPage <= totalPages) {
            if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
            // Bound work per tap. Successful pages stay queued even if a later request
            // fails or this bound is reached, so Retry continues without losing fonts.
            if (fetched++ == MAX_PAGES_PER_LOAD) throw new IllegalStateException("Search page overlap; retry");
            FontApi.Page result = fetcher.fetch(nextPage);
            for (FontItem font : result.fonts) if (seen.add(font.key())) pending.addLast(font);
            totalPages = result.totalPages;
            nextPage++;
        }
        List<FontItem> batch = new ArrayList<>();
        while (!pending.isEmpty() && batch.size() < FontApi.PAGE_SIZE) batch.add(pending.removeFirst());
        boolean more = !pending.isEmpty() || nextPage <= totalPages;
        // The UI counts delivered batches, not the number of source pages consumed.
        return new FontApi.Page(batch, more ? displayPage + 1 : batch.isEmpty() ? displayPage - 1 : displayPage);
    }
}
