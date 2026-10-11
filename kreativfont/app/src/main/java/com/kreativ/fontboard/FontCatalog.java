package com.kreativ.fontboard;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

final class FontCatalog {
    private FontCatalog() { }

    static String text(String html) {
        return Jsoup.parse(html).text().replace('\u00a0', ' ').trim();
    }

    static String name(String title) {
        return title.replaceFirst("(?i)\\s+font\\s+free\\s+download.*$", "")
                .replaceFirst("\\s+[–—-]\\s+.*$", "").trim();
    }

    static boolean safeUrl(String value, boolean image) {
        try {
            URI uri = new URI(value);
            String host = uri.getHost();
            if (!"https".equalsIgnoreCase(uri.getScheme()) || host == null || uri.getUserInfo() != null) return false;
            host = host.toLowerCase(Locale.ROOT);
            return host.equals("kreativfont.com") || host.equals("www.kreativfont.com")
                    || (image && (host.equals("i0.wp.com") || host.equals("i1.wp.com") || host.equals("i2.wp.com")));
        } catch (Exception invalid) {
            return false;
        }
    }

    static List<FontItem> parseFeed(String body, String availability) throws JSONException {
        JSONArray posts = new JSONArray(body);
        List<FontItem> fonts = new ArrayList<>();
        for (int i = 0; i < posts.length(); i++) {
            JSONObject post = posts.getJSONObject(i);
            String title = text(post.getJSONObject("title").getString("rendered"));
            String url = post.getString("link");
            long id = post.getLong("id");
            if (id <= 0 || title.isEmpty() || !safeUrl(url, false)) throw new JSONException("Invalid article");
            JSONObject excerpt = post.optJSONObject("excerpt");
            String description = excerpt == null ? "" : text(excerpt.optString("rendered"));
            if (description.isEmpty()) description = title;
            String imageUrl = "";
            double ratio = 2;
            JSONObject embedded = post.optJSONObject("_embedded");
            JSONArray media = embedded == null ? null : embedded.optJSONArray("wp:featuredmedia");
            JSONObject image = media == null ? null : media.optJSONObject(0);
            if (image != null && "image".equals(image.optString("media_type"))) {
                imageUrl = image.optString("source_url");
                JSONObject details = image.optJSONObject("media_details");
                if (details != null) {
                    ratio = details.optDouble("width", 2) / Math.max(1, details.optDouble("height", 1));
                    JSONObject sizes = details.optJSONObject("sizes");
                    JSONObject large = sizes == null ? null : sizes.optJSONObject("large");
                    if (large != null && safeUrl(large.optString("source_url"), true)) {
                        imageUrl = large.optString("source_url");
                        ratio = large.optDouble("width", 2) / Math.max(1, large.optDouble("height", 1));
                    }
                }
            }
            if (!safeUrl(imageUrl, true)) imageUrl = "";
            String kind = availability;
            if (kind == null) {
                JSONArray categories = post.getJSONArray("categories");
                boolean free = false, commercial = false;
                for (int c = 0; c < categories.length(); c++) {
                    free |= categories.getInt(c) == FontApi.FREE_CATEGORY;
                    commercial |= categories.getInt(c) == FontApi.FONT_CATEGORY;
                }
                if (!free && !commercial) throw new JSONException("Not a font article");
                kind = free ? "Free" : "Commercial";
            }
            FontItem font = new FontItem(id, name(title), title, description, kind, url, imageUrl, ratio);
            for (FontItem existing : fonts) if (existing.id == id) throw new JSONException("Duplicate article");
            fonts.add(font);
        }
        return fonts;
    }

    static String encode(List<FontItem> fonts) throws JSONException {
        JSONArray items = new JSONArray();
        for (FontItem font : fonts) items.put(font.toJson());
        return items.toString();
    }

    static List<FontItem> decode(String data) throws JSONException {
        JSONArray items = new JSONArray(data);
        List<FontItem> result = new ArrayList<>();
        for (int i = 0; i < items.length(); i++) {
            FontItem font = FontItem.fromJson(items.getJSONObject(i));
            if (!font.name.isEmpty()) result.add(font);
        }
        return result;
    }

    static List<FontItem> refreshFavorites(List<FontItem> saved, List<FontItem> latest) {
        LinkedHashMap<String, FontItem> result = new LinkedHashMap<>();
        for (FontItem font : saved) {
            FontItem updated = font;
            for (FontItem candidate : latest) {
                if ((candidate.id == font.id && font.id > 0)
                        || (font.id == 0 && candidate.name.equalsIgnoreCase(font.name))) {
                    updated = candidate;
                    break;
                }
            }
            result.put(updated.key(), updated);
        }
        return new ArrayList<>(result.values());
    }
}
