package com.kreativ.fontboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

final class FontStore {
    private final SharedPreferences preferences;

    FontStore(Context context) {
        preferences = context.getSharedPreferences("catalogue_v1", Context.MODE_PRIVATE);
        if (!preferences.contains("favorites")) migrateProjects(context);
    }

    List<FontItem> feed() { return read("feed"); }
    List<FontItem> favorites() { return read("favorites"); }
    long updatedAt() { return preferences.getLong("updated_at", 0); }
    int page(String availability) {
        return preferences.getInt("page_size", 0) == FontApi.PAGE_SIZE
                ? preferences.getInt(availability + "_page", 0) : 0;
    }
    int totalPages(String availability) {
        return preferences.getInt("page_size", 0) == FontApi.PAGE_SIZE
                ? preferences.getInt(availability + "_pages", Integer.MAX_VALUE) : Integer.MAX_VALUE;
    }

    void saveFeed(List<FontItem> fonts, long time, FontPager commercial, FontPager free) {
        try {
            preferences.edit().putString("feed", FontCatalog.encode(fonts)).putLong("updated_at", time)
                    .putInt("page_size", FontApi.PAGE_SIZE)
                    .putInt("Commercial_page", commercial.page).putInt("Commercial_pages", commercial.totalPages)
                    .putInt("Free_page", free.page).putInt("Free_pages", free.totalPages).apply();
        } catch (Exception invalid) { Log.w("FontStore", "Could not save feed", invalid); }
    }

    void saveFavorites(List<FontItem> fonts) {
        try { preferences.edit().putString("favorites", FontCatalog.encode(fonts)).apply(); }
        catch (Exception invalid) { Log.w("FontStore", "Could not save favorites", invalid); }
    }

    private List<FontItem> read(String key) {
        try { return FontCatalog.decode(preferences.getString(key, "[]")); }
        catch (Exception invalid) {
            Log.w("FontStore", "Could not read " + key, invalid);
            return new ArrayList<>();
        }
    }

    private void migrateProjects(Context context) {
        List<FontItem> previous = new ArrayList<>();
        try {
            previous = FontCatalog.decode(context.getSharedPreferences("font_feed", Context.MODE_PRIVATE).getString("latest", "[]"));
        } catch (Exception invalid) { Log.w("FontStore", "Previous feed unavailable", invalid); }
        String projects = context.getSharedPreferences("font_projects", Context.MODE_PRIVATE).getString("projects", "");
        LinkedHashMap<String, FontItem> migrated = new LinkedHashMap<>();
        for (String project : projects.split("\\n")) {
            String[] parts = project.split("\\|", 2);
            if (parts.length != 2) continue;
            for (String name : parts[1].split(",")) {
                if (name.trim().isEmpty()) continue;
                FontItem candidate = null;
                for (FontItem old : previous) if (old.name.equals(name)) candidate = old;
                if (candidate == null || !FontCatalog.safeUrl(candidate.articleUrl, false)) {
                    String search;
                    try { search = URLEncoder.encode(name, StandardCharsets.UTF_8.name()); }
                    catch (Exception impossible) { continue; }
                    candidate = new FontItem(0, name, name,
                            "Saved in an earlier version. Open Kreativ Font to find the original article.",
                            "Saved", "https://kreativfont.com/?s=" + search, "", 2);
                }
                migrated.put(candidate.key(), candidate);
            }
        }
        saveFavorites(new ArrayList<>(migrated.values()));
    }
}
