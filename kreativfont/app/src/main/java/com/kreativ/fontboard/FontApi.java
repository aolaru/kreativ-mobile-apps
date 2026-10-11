package com.kreativ.fontboard;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Public, paged WordPress font catalogue. Search never needs to download the entire database. */
final class FontApi {
    static final int PAGE_SIZE = 10;
    static final int FONT_CATEGORY = 3;
    static final int FREE_CATEGORY = 23584;
    private static final String BASE = "https://kreativfont.com/wp-json/wp/v2/posts?";
    private static final String FIELDS = "&_embed=wp:featuredmedia&_fields=id,link,title,excerpt,categories,featured_media,_links,_embedded";

    static String feedUrl(String availability, int page) {
        if (page < 1) throw new IllegalArgumentException("Invalid page");
        String categories;
        if ("Free".equals(availability)) categories = "categories=" + FREE_CATEGORY;
        else if ("Commercial".equals(availability)) categories = "categories=" + FONT_CATEGORY + "&categories_exclude=" + FREE_CATEGORY;
        else throw new IllegalArgumentException("Invalid section");
        return BASE + categories + "&per_page=" + PAGE_SIZE + "&page=" + page + "&orderby=date&order=desc" + FIELDS;
    }

    static String searchUrl(String query, int page) {
        if (page < 1 || query.trim().isEmpty()) throw new IllegalArgumentException("Invalid search");
        String encoded;
        try { encoded = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8.name()); }
        catch (java.io.UnsupportedEncodingException impossible) { throw new AssertionError(impossible); }
        return BASE + "categories=" + FONT_CATEGORY + "," + FREE_CATEGORY + "&search=" + encoded
                + "&per_page=" + PAGE_SIZE + "&page=" + page + "&orderby=relevance&order=desc" + FIELDS;
    }

    static final class Page {
        final List<FontItem> fonts;
        final int totalPages;

        Page(List<FontItem> fonts, int totalPages) {
            this.fonts = fonts;
            this.totalPages = totalPages;
        }
    }

    static Page fetch(String url, String availability, int page) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(15000);
        connection.setRequestProperty("Accept", "application/json");
        try {
            int code = connection.getResponseCode();
            if (code == 400) {
                String body = read(connection.getErrorStream());
                if ("rest_post_invalid_page_number".equals(new JSONObject(body).optString("code"))) {
                    return new Page(java.util.Collections.emptyList(), Math.max(0, page - 1));
                }
                throw new IllegalStateException("Catalogue unavailable");
            }
            if (code != HttpURLConnection.HTTP_OK) throw new IllegalStateException("Catalogue unavailable");
            List<FontItem> fonts = FontCatalog.parseFeed(read(connection.getInputStream()), availability);
            if (fonts.size() > PAGE_SIZE) throw new IllegalStateException("Unexpected page size");
            int pages;
            try { pages = Integer.parseInt(connection.getHeaderField("X-WP-TotalPages")); }
            catch (Exception missing) { pages = fonts.size() == PAGE_SIZE ? page + 1 : page; }
            if (pages < 0) throw new IllegalStateException("Invalid page count");
            return new Page(fonts, fonts.isEmpty() ? Math.max(0, page - 1) : pages);
        } finally { connection.disconnect(); }
    }

    private static String read(InputStream stream) throws Exception {
        if (stream == null) throw new IllegalStateException("Missing response");
        try (InputStream input = stream; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) {
                if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
                if (output.size() + count > 2 * 1024 * 1024) throw new IllegalStateException("Response too large");
                output.write(buffer, 0, count);
            }
            return output.toString(StandardCharsets.UTF_8.name());
        }
    }
}
