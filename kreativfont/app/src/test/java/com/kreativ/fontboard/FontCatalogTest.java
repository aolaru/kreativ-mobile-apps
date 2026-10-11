package com.kreativ.fontboard;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class FontCatalogTest {
    private JSONObject article(long id) throws Exception {
        JSONObject large = new JSONObject().put("source_url", "https://i0.wp.com/kreativfont.com/preview.png?fit=900%2C450")
                .put("width", 900).put("height", 450);
        JSONObject media = new JSONObject().put("media_type", "image")
                .put("source_url", "https://kreativfont.com/preview.png")
                .put("media_details", new JSONObject().put("width", 1440).put("height", 720)
                        .put("sizes", new JSONObject().put("large", large)));
        return new JSONObject().put("id", id).put("link", "https://kreativfont.com/fonts/example")
                .put("title", new JSONObject().put("rendered", "Megalme &#8211; Sophisticated Modern Serif"))
                .put("excerpt", new JSONObject().put("rendered", "<p>Elegant <strong>serif</strong> for branding &amp; editorial.</p>"))
                .put("_embedded", new JSONObject().put("wp:featuredmedia", new JSONArray().put(media)));
    }

    @Test public void usesRealDescriptionsAndWebsiteArtwork() throws Exception {
        FontItem font = FontCatalog.parseFeed(new JSONArray().put(article(123)).toString(), "Commercial").get(0);
        assertEquals("Megalme", font.name);
        assertEquals("Megalme – Sophisticated Modern Serif", font.title);
        assertEquals("Elegant serif for branding & editorial.", font.description);
        assertTrue(font.imageUrl.startsWith("https://i0.wp.com/"));
        assertEquals(2, font.imageRatio, .001);
        assertEquals("Commercial", font.availability);
        assertEquals("post:123", font.key());
    }

    @Test public void missingPreviewStillHasUsefulArticle() throws Exception {
        JSONObject article = article(123);
        article.remove("_embedded");
        article.remove("excerpt");
        FontItem font = FontCatalog.parseFeed(new JSONArray().put(article).toString(), "Free").get(0);
        assertEquals("", font.imageUrl);
        assertEquals(font.title, font.description);
        assertEquals("https://kreativfont.com/fonts/example", font.articleUrl);
    }

    @Test public void freeTitleAndHyphenatedNamesAreHandled() {
        assertEquals("Cookie", FontCatalog.name("Cookie Font Free Download (Commercial Use)"));
        assertEquals("Mono-Express", FontCatalog.name("Mono-Express – A modern sans"));
    }

    @Test public void fullFavoriteMetadataSurvivesStorage() throws Exception {
        FontItem original = FontCatalog.parseFeed(new JSONArray().put(article(123)).toString(), "Commercial").get(0);
        FontItem restored = FontCatalog.decode(FontCatalog.encode(Collections.singletonList(original))).get(0);
        assertEquals(original.key(), restored.key());
        assertEquals(original.title, restored.title);
        assertEquals(original.articleUrl, restored.articleUrl);
        assertEquals(original.imageUrl, restored.imageUrl);
        assertEquals(original.description, restored.description);
    }

    @Test public void rollingFeedDoesNotDeleteAnOlderFavorite() throws Exception {
        FontItem saved = FontCatalog.parseFeed(new JSONArray().put(article(123)).toString(), "Commercial").get(0);
        FontItem newer = FontCatalog.parseFeed(new JSONArray().put(article(456)).toString(), "Commercial").get(0);
        List<FontItem> retained = FontCatalog.refreshFavorites(Collections.singletonList(saved), Collections.singletonList(newer));
        assertEquals(1, retained.size());
        assertEquals(123, retained.get(0).id);
        assertEquals(saved.articleUrl, retained.get(0).articleUrl);
    }

    @Test public void updatesByIdEvenIfArticleNameChanges() throws Exception {
        FontItem saved = FontCatalog.parseFeed(new JSONArray().put(article(123)).toString(), "Commercial").get(0);
        JSONObject changed = article(123).put("title", new JSONObject().put("rendered", "Renamed – Modern serif"));
        FontItem updated = FontCatalog.parseFeed(new JSONArray().put(changed).toString(), "Commercial").get(0);
        assertEquals("Renamed", FontCatalog.refreshFavorites(Collections.singletonList(saved), Collections.singletonList(updated)).get(0).name);
    }

    @Test public void upgradesLegacyFavoriteWithoutDuplicatingIt() throws Exception {
        FontItem legacy = FontItem.fromJson(new JSONObject().put("name", "Megalme").put("url", "https://kreativfont.com/?s=Megalme"));
        FontItem latest = FontCatalog.parseFeed(new JSONArray().put(article(123)).toString(), "Commercial").get(0);
        List<FontItem> updated = FontCatalog.refreshFavorites(Arrays.asList(legacy, latest), Collections.singletonList(latest));
        assertEquals(1, updated.size());
        assertEquals(123, updated.get(0).id);
        assertEquals(latest.imageUrl, updated.get(0).imageUrl);
    }

    @Test public void rejectsUnexpectedAndInsecureArticleUrls() throws Exception {
        assertFalse(FontCatalog.safeUrl("http://kreativfont.com/fonts/a", false));
        assertFalse(FontCatalog.safeUrl("https://kreativfont.com.evil.test/fonts/a", false));
        assertFalse(FontCatalog.safeUrl("javascript:alert(1)", false));
        assertFalse(FontCatalog.safeUrl("https://user@kreativfont.com/a", false));
        assertFalse(FontCatalog.safeUrl("https://i0.wp.com/image.png", false));
        assertTrue(FontCatalog.safeUrl("https://i0.wp.com/image.png", true));
        JSONObject unsafe = article(123).put("link", "https://unexpected.test/");
        assertThrows(JSONException.class, () -> FontCatalog.parseFeed(new JSONArray().put(unsafe).toString(), "Commercial"));
    }

    @Test public void invalidFeedCannotBecomeAnEmptySuccessfulUpdate() throws Exception {
        assertThrows(JSONException.class, () -> FontCatalog.parseFeed("{\"error\":\"unavailable\"}", "Commercial"));
        JSONArray duplicate = new JSONArray().put(article(123)).put(article(123));
        assertThrows(JSONException.class, () -> FontCatalog.parseFeed(duplicate.toString(), "Commercial"));
    }
}
