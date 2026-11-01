package com.kreativ.fontboard;

import org.json.JSONException;
import org.json.JSONObject;

/** A saved article retains its own metadata independently of the rolling latest feed. */
final class FontItem {
    final long id;
    final String name, title, description, availability, articleUrl, imageUrl, fullImageUrl;
    final double imageRatio;

    FontItem(long id, String name, String title, String description, String availability,
             String articleUrl, String imageUrl, double imageRatio) {
        this(id, name, title, description, availability, articleUrl, imageUrl, imageRatio, imageUrl);
    }

    FontItem(long id, String name, String title, String description, String availability,
             String articleUrl, String imageUrl, double imageRatio, String fullImageUrl) {
        this.id = id;
        this.name = name;
        this.title = title;
        this.description = description;
        this.availability = availability;
        this.articleUrl = articleUrl;
        this.imageUrl = imageUrl;
        this.fullImageUrl = fullImageUrl;
        this.imageRatio = Double.isFinite(imageRatio) && imageRatio > 0 ? imageRatio : 2;
    }

    String key() { return id > 0 ? "post:" + id : "legacy:" + name.toLowerCase(java.util.Locale.ROOT); }

    JSONObject toJson() throws JSONException {
        return new JSONObject().put("id", id).put("name", name).put("title", title)
                .put("description", description).put("availability", availability)
                .put("url", articleUrl).put("image", imageUrl).put("ratio", imageRatio).put("full_image", fullImageUrl);
    }

    static FontItem fromJson(JSONObject json) {
        String name = json.optString("name");
        return new FontItem(json.optLong("id"), name, json.optString("title", name),
                json.optString("description"), json.optString("availability", "Saved"),
                json.optString("url"), json.optString("image"), json.optDouble("ratio", 2),
                json.optString("full_image", json.optString("image")));
    }
}
