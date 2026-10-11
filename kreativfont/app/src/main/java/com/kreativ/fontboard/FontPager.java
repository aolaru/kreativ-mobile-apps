package com.kreativ.fontboard;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/** UI-thread state; failed pages are retried and late responses cannot overwrite a new query. */
final class FontPager {
    final List<FontItem> fonts = new ArrayList<>();
    int page, totalPages = Integer.MAX_VALUE;
    boolean loading, failed;
    private int generation;

    static final class Request {
        final int page, generation;
        Request(int page, int generation) { this.page = page; this.generation = generation; }
    }

    boolean hasMore() { return page < totalPages; }

    void invalidate() { generation++; loading = false; failed = false; }

    void restore(List<FontItem> cached, int page, int totalPages) {
        invalidate();
        fonts.clear();
        fonts.addAll(cached);
        this.page = page;
        this.totalPages = totalPages;
    }

    void reset() { restore(new ArrayList<>(), 0, Integer.MAX_VALUE); }

    Request begin() {
        if (loading || !hasMore()) return null;
        loading = true;
        failed = false;
        return new Request(page + 1, generation);
    }

    boolean complete(Request request, FontApi.Page result) {
        if (!current(request)) return false;
        // New posts between requests can shift page boundaries. Keep one card per article ID.
        LinkedHashMap<String, FontItem> unique = new LinkedHashMap<>();
        for (FontItem font : fonts) unique.put(font.key(), font);
        for (FontItem font : result.fonts) unique.put(font.key(), font);
        fonts.clear();
        fonts.addAll(unique.values());
        page = request.page;
        totalPages = result.totalPages;
        loading = false;
        failed = false;
        return true;
    }

    boolean fail(Request request) {
        if (!current(request)) return false;
        loading = false;
        failed = true;
        return true;
    }

    private boolean current(Request request) {
        return loading && request.generation == generation && request.page == page + 1;
    }
}
