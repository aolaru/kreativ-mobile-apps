package com.kreativ.fontboard;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.bumptech.glide.Glide;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MainActivity extends Activity {
    private static final String PRIVACY = "https://kreativfont.com/blog/terms-of-use-privacy-policy";
    private final ExecutorService network = Executors.newSingleThreadExecutor();
    private final ExecutorService searchNetwork = Executors.newFixedThreadPool(2);
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final FontPager commercialPager = new FontPager(), freePager = new FontPager(), searchPager = new FontPager();
    private Future<?> searchTask;
    private Runnable searchDebounce;
    private String activeQuery = "";
    private boolean searchWaiting;
    private List<FontItem> fonts = new ArrayList<>();
    private List<FontItem> favorites = new ArrayList<>();
    private FontStore store;
    private LinearLayout content;
    private ScrollView scroll;
    private EditText search;
    private TextView status;
    private Button discoverTab, favoritesTab, refresh;
    private final Map<String, HorizontalScrollView> shelves = new HashMap<>();
    private final Map<String, Integer> shelfPositions = new HashMap<>();
    private boolean showingFavorites, loading, destroyed;
    private String refreshError = "";
    private long updatedAt;
    private int ink, muted, accent;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        ink = getColor(R.color.ink);
        muted = getColor(R.color.muted);
        accent = getColor(R.color.primary);
        store = new FontStore(this);
        fonts = store.feed();
        for (String availability : new String[]{"Commercial", "Free"}) {
            List<FontItem> cached = new ArrayList<>();
            for (FontItem font : fonts) if (font.availability.equals(availability)) cached.add(font);
            pager(availability).restore(cached, store.page(availability), store.totalPages(availability));
        }
        favorites = store.favorites();
        updatedAt = store.updatedAt();
        showingFavorites = state != null && state.getBoolean("favorites");
        if (state != null) {
            shelfPositions.put("Commercial", state.getInt("commercial_scroll"));
            shelfPositions.put("Free", state.getInt("free_scroll"));
        }
        buildScreen(state == null ? "" : state.getString("query", ""));
        renderFonts(false);
        if (state != null) scroll.post(() -> scroll.scrollTo(0, state.getInt("scroll")));
        refreshFeed();
        if (!search.getText().toString().trim().isEmpty()) onSearchChanged();
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        state.putBoolean("favorites", showingFavorites);
        state.putString("query", search.getText().toString());
        state.putInt("scroll", scroll.getScrollY());
        rememberShelves();
        state.putInt("commercial_scroll", shelfPositions.getOrDefault("Commercial", 0));
        state.putInt("free_scroll", shelfPositions.getOrDefault("Free", 0));
        super.onSaveInstanceState(state);
    }

    @Override
    protected void onDestroy() {
        destroyed = true;
        handler.removeCallbacksAndMessages(null);
        if (searchTask != null) searchTask.cancel(true);
        searchNetwork.shutdownNow();
        network.shutdownNow();
        super.onDestroy();
    }

    private void buildScreen(String query) {
        LinearLayout shell = column();
        shell.setBackgroundColor(Color.WHITE);
        ViewCompat.setOnApplyWindowInsetsListener(shell, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            Insets keyboard = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
            view.setPadding(bars.left, bars.top, bars.right, Math.max(bars.bottom, keyboard.bottom));
            return windowInsets;
        });
        LinearLayout header = column();
        header.setPadding(dp(20), dp(12), dp(20), 0);
        LinearLayout lockup = row();
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.kreativ_logo);
        logo.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        lockup.addView(logo, new LinearLayout.LayoutParams(dp(44), dp(44)));
        gap(lockup, 12);
        LinearLayout brandText = column();
        TextView brand = label(getString(R.string.app_name), 22, ink);
        brand.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        brand.setLetterSpacing(.04f);
        brandText.addView(brand);
        TextView tagline = label(getString(R.string.tagline), 10, muted);
        tagline.setTypeface(Typeface.DEFAULT_BOLD);
        tagline.setLetterSpacing(.08f);
        brandText.addView(tagline);
        lockup.addView(brandText, weighted());
        header.addView(lockup);
        search = new EditText(this);
        search.setSingleLine(true);
        search.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        search.setTextSize(16);
        search.setTextColor(ink);
        search.setHintTextColor(muted);
        search.setHint(R.string.search_hint);
        search.setBackgroundResource(R.drawable.secondary_button);
        search.setPadding(dp(14), dp(8), dp(14), dp(8));
        search.setMinHeight(dp(48));
        search.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_search, 0, 0, 0);
        search.setCompoundDrawablePadding(dp(10));
        search.setText(query);
        header.addView(search, margin(matchWrap(), 10));
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) { onSearchChanged(); }
            public void afterTextChanged(Editable editable) { }
        });
        search.setOnEditorActionListener((view, action, event) -> {
            if (action != EditorInfo.IME_ACTION_SEARCH) return false;
            if (!showingFavorites && searchDebounce != null) {
                handler.removeCallbacks(searchDebounce);
                searchDebounce.run();
            }
            ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(search.getWindowToken(), 0);
            return true;
        });
        LinearLayout statusRow = row();
        status = label("", 12, muted);
        status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        statusRow.addView(status, weighted());
        refresh = textButton(getString(R.string.refresh));
        refresh.setOnClickListener(v -> {
            if (!search.getText().toString().trim().isEmpty()) restartSearch();
            else refreshFeed();
        });
        statusRow.addView(refresh, wrap());
        header.addView(statusRow, matchWrap());
        shell.addView(header);
        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        content = column();
        content.setPadding(dp(20), dp(4), dp(20), dp(24));
        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));
        shell.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        View divider = new View(this);
        divider.setBackgroundColor(getColor(R.color.line));
        shell.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));
        LinearLayout tabs = row();
        tabs.setPadding(dp(20), dp(8), dp(20), dp(8));
        discoverTab = button(getString(R.string.discover), false);
        favoritesTab = button("", false);
        discoverTab.setOnClickListener(v -> switchTab(false));
        favoritesTab.setOnClickListener(v -> switchTab(true));
        tabs.addView(discoverTab, weighted());
        gap(tabs, 10);
        tabs.addView(favoritesTab, weighted());
        shell.addView(tabs);
        setContentView(shell);
        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), shell);
        controller.setAppearanceLightStatusBars(true);
        controller.setAppearanceLightNavigationBars(true);
        ViewCompat.requestApplyInsets(shell);
    }

    private void switchTab(boolean saved) {
        showingFavorites = saved;
        search.setText("");
        search.clearFocus();
        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(search.getWindowToken(), 0);
        renderFonts(false);
    }

    private void updateHeader() {
        boolean searching = !showingFavorites && !search.getText().toString().trim().isEmpty();
        search.setHint(showingFavorites ? R.string.search_favorites_hint : R.string.search_hint);
        discoverTab.setBackgroundResource(!showingFavorites ? R.drawable.selected_tab : R.drawable.text_button);
        discoverTab.setTextColor(!showingFavorites ? accent : muted);
        favoritesTab.setBackgroundResource(showingFavorites ? R.drawable.selected_tab : R.drawable.text_button);
        favoritesTab.setTextColor(showingFavorites ? accent : muted);
        favoritesTab.setText(getString(R.string.favorites_count, favorites.size()));
        discoverTab.setSelected(!showingFavorites);
        favoritesTab.setSelected(showingFavorites);
        refresh.setVisibility(showingFavorites ? View.GONE : View.VISIBLE);
        refresh.setText(searching ? R.string.search_again : R.string.refresh);
        refresh.setEnabled(searching ? !searchWaiting && !searchPager.loading : !loading);
        if (showingFavorites) {
            status.setText(R.string.saved_on_device);
        } else if (searching) {
            status.setText(searchWaiting || searchPager.loading ? R.string.searching_catalogue
                    : searchPager.failed ? R.string.search_failed : R.string.search_scope);
        } else if (loading) {
            status.setText(R.string.refreshing);
        } else if (!refreshError.isEmpty()) {
            status.setText(refreshError);
        } else if (updatedAt > 0) {
            String date = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date(updatedAt));
            status.setText(getString(R.string.last_updated, date));
        } else {
            status.setText(R.string.latest_from_website);
        }
    }

    private void renderFonts(boolean preserveScroll) {
        if (destroyed || content == null) return;
        int position = preserveScroll ? scroll.getScrollY() : 0;
        rememberShelves();
        shelves.clear();
        clearImages(content);
        content.removeAllViews();
        updateHeader();
        String query = search.getText().toString().trim().toLowerCase(Locale.ROOT);
        List<FontItem> source = showingFavorites ? favorites : query.isEmpty() ? fonts : searchPager.fonts;
        int matched = 0;
        if (!showingFavorites && query.isEmpty()) {
            matched += section(getString(R.string.latest_commercial), "Commercial", source);
            matched += section(getString(R.string.latest_free), "Free", source);
        } else {
            TextView title = label(getString(showingFavorites ? R.string.favorites : R.string.search_results), 21, ink);
            title.setTypeface(Typeface.DEFAULT_BOLD);
            content.addView(title, matchWrap());
            for (FontItem font : source) {
                String haystack = (font.name + " " + font.title + " " + font.description + " " + font.availability).toLowerCase(Locale.ROOT);
                if (showingFavorites && !haystack.contains(query)) continue;
                content.addView(card(font), margin(matchWrap(), 12));
                matched++;
            }
            if (!showingFavorites && !query.isEmpty()) {
                TextView scope = label(getString(R.string.search_privacy), 12, muted);
                content.addView(scope, margin(matchWrap(), 8));
                if (!searchWaiting) content.addView(pageControl(searchPager, () -> loadSearchPage()), margin(matchWrap(), 12));
            }
        }
        if (matched == 0) {
            int message = !showingFavorites && !query.isEmpty()
                    ? searchWaiting || searchPager.loading ? R.string.searching_catalogue
                    : searchPager.failed ? R.string.search_failed : R.string.no_matches
                    : !query.isEmpty() ? R.string.no_matches : showingFavorites ? R.string.no_favorites
                    : loading ? R.string.loading_fonts : R.string.no_feed;
            TextView empty = label(getString(message), 16, muted);
            empty.setPadding(0, dp(18), 0, dp(20));
            content.addView(empty, matchWrap());
        }
        Button website = textButton(getString(R.string.visit_website));
        website.setOnClickListener(v -> openUrl("https://kreativfont.com"));
        content.addView(website, margin(matchWrap(), 24));
        Button privacy = textButton(getString(R.string.privacy_policy));
        privacy.setOnClickListener(v -> openUrl(PRIVACY));
        content.addView(privacy, margin(matchWrap(), 8));
        scroll.post(() -> { if (!destroyed) scroll.scrollTo(0, position); });
    }

    private void clearImages(View view) {
        if (view instanceof ImageView) Glide.with(this).clear(view);
        else if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) clearImages(group.getChildAt(i));
        }
    }

    private int section(String title, String availability, List<FontItem> source) {
        boolean free = availability.equals("Free");
        TextView eyebrow = label(getString(free ? R.string.free_eyebrow : R.string.commercial_eyebrow), 10, accent);
        eyebrow.setTypeface(Typeface.DEFAULT_BOLD);
        eyebrow.setLetterSpacing(.1f);
        content.addView(eyebrow, margin(matchWrap(), free ? 24 : 4));
        TextView heading = label(title, 21, ink);
        heading.setTypeface(Typeface.DEFAULT_BOLD);
        content.addView(heading, margin(matchWrap(), 4));
        int count = 0;
        for (FontItem font : source) if (font.availability.equals(availability)) count++;
        TextView hint = label(count > 0 ? getResources().getQuantityString(R.plurals.shelf_hint, count, count)
                : getString(loading ? R.string.loading_fonts : R.string.no_feed), 12, muted);
        content.addView(hint, margin(matchWrap(), 4));
        HorizontalScrollView shelf = new HorizontalScrollView(this);
        shelf.setHorizontalScrollBarEnabled(false);
        shelf.setContentDescription(title);
        LinearLayout cards = row();
        cards.setGravity(Gravity.TOP);
        // Leave the next preview visible as a cue that each section can be swiped.
        int width = Math.min(dp(252), Math.max(dp(180), getResources().getDisplayMetrics().widthPixels - dp(92)));
        for (FontItem font : source) {
            if (!font.availability.equals(availability)) continue;
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, -2);
            params.setMarginEnd(dp(12));
            cards.addView(card(font, true), params);
        }
        LinearLayout more = column();
        more.setBackgroundResource(R.drawable.card);
        more.setPadding(dp(16), dp(24), dp(16), dp(24));
        more.addView(pageControl(pager(availability), () -> loadFeedPage(availability)), matchWrap());
        TextView moreHint = label(getString(pager(availability).hasMore()
                ? R.string.more_fonts_hint : R.string.catalogue_end_hint), 13, muted);
        moreHint.setGravity(Gravity.CENTER);
        more.addView(moreHint, margin(matchWrap(), 12));
        cards.addView(more, new LinearLayout.LayoutParams(width, -2));
        shelf.addView(cards, new HorizontalScrollView.LayoutParams(-2, -2));
        content.addView(shelf, margin(matchWrap(), 12));
        shelves.put(availability, shelf);
        int position = shelfPositions.getOrDefault(availability, 0);
        shelf.post(() -> { if (!destroyed) shelf.scrollTo(position, 0); });
        return count;
    }

    private void rememberShelves() {
        for (Map.Entry<String, HorizontalScrollView> shelf : shelves.entrySet()) {
            shelfPositions.put(shelf.getKey(), shelf.getValue().getScrollX());
        }
    }

    private FontPager pager(String availability) {
        return availability.equals("Free") ? freePager : commercialPager;
    }

    private View pageControl(FontPager page, Runnable load) {
        if (!page.hasMore()) {
            TextView end = label(getString(R.string.all_fonts_loaded), 14, muted);
            end.setGravity(Gravity.CENTER);
            end.setPadding(dp(8), dp(12), dp(8), dp(12));
            return end;
        }
        Button more = button(getString(page.loading ? R.string.loading_more
                : page.failed ? R.string.retry_more : R.string.load_more), true);
        more.setEnabled(!page.loading && (page == searchPager || !loading));
        more.setOnClickListener(v -> load.run());
        return more;
    }

    private void onSearchChanged() {
        if (searchDebounce != null) handler.removeCallbacks(searchDebounce);
        searchDebounce = null;
        if (searchTask != null) searchTask.cancel(true);
        searchPager.reset();
        activeQuery = search.getText().toString().trim();
        searchWaiting = !showingFavorites && !activeQuery.isEmpty();
        renderFonts(false);
        if (searchWaiting) {
            searchDebounce = () -> {
                searchDebounce = null;
                if (destroyed || showingFavorites || activeQuery.isEmpty()) return;
                searchWaiting = false;
                loadSearchPage();
            };
            handler.postDelayed(searchDebounce, 450);
        }
    }

    private void restartSearch() {
        onSearchChanged();
        if (searchDebounce != null) {
            handler.removeCallbacks(searchDebounce);
            searchDebounce.run();
        }
    }

    private void loadSearchPage() {
        if (destroyed || showingFavorites || activeQuery.isEmpty()) return;
        FontPager.Request request = searchPager.begin();
        if (request == null) return;
        String query = activeQuery;
        renderFonts(true);
        searchTask = searchNetwork.submit(() -> {
            try {
                FontApi.Page page = FontApi.fetch(FontApi.searchUrl(query, request.page), null, request.page);
                runOnUiThread(() -> {
                    if (destroyed || isFinishing() || !searchPager.complete(request, page)) return;
                    updateFavorites(page.fonts);
                    renderFonts(true);
                });
            } catch (Exception failed) {
                // Do not include query-bearing URLs or exception messages in logs.
                runOnUiThread(() -> {
                    if (destroyed || isFinishing() || !searchPager.fail(request)) return;
                    renderFonts(true);
                });
            }
        });
    }

    private void loadFeedPage(String availability) {
        if (loading || destroyed) return;
        FontPager page = pager(availability);
        FontPager.Request request = page.begin();
        if (request == null) return;
        renderFonts(true);
        network.execute(() -> {
            try {
                FontApi.Page result = FontApi.fetch(FontApi.feedUrl(availability, request.page), availability, request.page);
                runOnUiThread(() -> {
                    if (destroyed || isFinishing() || !page.complete(request, result)) return;
                    syncFeed();
                    updateFavorites(result.fonts);
                    renderFonts(true);
                });
            } catch (Exception failed) {
                runOnUiThread(() -> {
                    if (destroyed || isFinishing() || !page.fail(request)) return;
                    renderFonts(true);
                });
            }
        });
    }

    private void syncFeed() {
        fonts = new ArrayList<>(commercialPager.fonts);
        fonts.addAll(freePager.fonts);
        store.saveFeed(fonts, updatedAt, commercialPager, freePager);
    }

    private void updateFavorites(List<FontItem> latest) {
        favorites = FontCatalog.refreshFavorites(favorites, latest);
        store.saveFavorites(favorites);
    }

    private View card(FontItem font) {
        return card(font, false);
    }

    private View card(FontItem font, boolean compact) {
        LinearLayout card = column();
        card.setBackgroundResource(R.drawable.card);
        card.setClipToOutline(true);
        if (FontCatalog.safeUrl(font.imageUrl, true)) {
            ImageView image = new ImageView(this) {
                @Override
                protected void onMeasure(int widthSpec, int heightSpec) {
                    int width = View.MeasureSpec.getSize(widthSpec);
                    double ratio = compact ? 16.0 / 9 : Math.max(.5, Math.min(3, font.imageRatio));
                    super.onMeasure(widthSpec, View.MeasureSpec.makeMeasureSpec((int) (width / ratio), View.MeasureSpec.EXACTLY));
                }
            };
            image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            image.setBackgroundColor(getColor(R.color.preview_background));
            image.setContentDescription(getString(R.string.font_preview, font.name));
            image.setOnClickListener(v -> showDetails(font));
            card.addView(image, matchWrap());
            Glide.with(this).load(font.imageUrl).fitCenter()
                    .placeholder(R.drawable.preview_placeholder).error(R.drawable.preview_placeholder).into(image);
        } else {
            TextView noPreview = label(getString(R.string.preview_online), 14, muted);
            noPreview.setPadding(0, dp(8), 0, dp(8));
            card.addView(noPreview, matchWrap());
        }
        LinearLayout body = column();
        body.setPadding(dp(12), dp(10), dp(12), dp(8));
        TextView name = label(font.name, 17, ink);
        name.setTypeface(Typeface.DEFAULT_BOLD);
        if (compact) {
            name.setMaxLines(2);
            name.setEllipsize(TextUtils.TruncateAt.END);
        }
        name.setOnClickListener(v -> showDetails(font));
        body.addView(name, matchWrap());
        if (!compact) {
            TextView badge = label(font.availability.toUpperCase(Locale.ROOT), 10, accent);
            badge.setTypeface(Typeface.DEFAULT_BOLD);
            body.addView(badge, margin(matchWrap(), 4));
        }
        TextView description = label(font.description, 13, muted);
        if (compact) {
            description.setLines(2);
            description.setEllipsize(TextUtils.TruncateAt.END);
            description.setOnClickListener(v -> showDetails(font));
        }
        body.addView(description, margin(matchWrap(), 4));
        LinearLayout actions = row();
        Button open = textButton(getString(R.string.open_article));
        open.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        open.setContentDescription(getString(R.string.open_font_article, font.name));
        open.setOnClickListener(v -> openUrl(font.articleUrl));
        actions.addView(open, weighted());
        gap(actions, 8);
        boolean saved = isFavorite(font);
        Button save = textButton("");
        save.setCompoundDrawablesRelativeWithIntrinsicBounds(saved ? R.drawable.ic_bookmark_saved : R.drawable.ic_bookmark, 0, 0, 0);
        save.setPadding(dp(12), dp(12), dp(12), dp(12));
        save.setMinWidth(0);
        save.setContentDescription(getString(saved ? R.string.remove_favorite : R.string.add_favorite, font.name));
        save.setOnClickListener(v -> toggleFavorite(font));
        actions.addView(save, new LinearLayout.LayoutParams(dp(48), dp(48)));
        body.addView(actions, margin(matchWrap(), 4));
        card.addView(body, matchWrap());
        return card;
    }

    private void showDetails(FontItem font) {
        ScrollView details = new ScrollView(this);
        LinearLayout body = column();
        body.setPadding(dp(24), dp(8), dp(24), dp(16));
        TextView text = label(font.description, 16, muted);
        text.setLineSpacing(dp(4), 1);
        body.addView(text, matchWrap());
        details.addView(body);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(font.name).setView(details)
                .setPositiveButton(R.string.open_article, (d, which) -> openUrl(font.articleUrl))
                .setNeutralButton(isFavorite(font) ? R.string.remove_saved : R.string.save, (d, which) -> toggleFavorite(font))
                .setNegativeButton(R.string.close, null).create();
        dialog.show();
    }

    private boolean isFavorite(FontItem font) {
        for (FontItem saved : favorites) if (saved.key().equals(font.key())) return true;
        return false;
    }

    private void toggleFavorite(FontItem font) {
        boolean saved = isFavorite(font);
        if (saved) favorites.removeIf(item -> item.key().equals(font.key()));
        else favorites.add(font);
        store.saveFavorites(favorites);
        Toast.makeText(this, saved ? R.string.favorite_removed : R.string.favorite_saved, Toast.LENGTH_SHORT).show();
        renderFonts(true);
    }

    private void refreshFeed() {
        if (loading || destroyed) return;
        loading = true;
        commercialPager.invalidate();
        freePager.invalidate();
        refreshError = "";
        if (showingFavorites) updateHeader();
        else renderFonts(true);
        network.execute(() -> {
            try {
                FontApi.Page commercial = FontApi.fetch(FontApi.feedUrl("Commercial", 1), "Commercial", 1);
                FontApi.Page free = FontApi.fetch(FontApi.feedUrl("Free", 1), "Free", 1);
                long time = System.currentTimeMillis();
                runOnUiThread(() -> {
                    if (destroyed || isFinishing()) return;
                    commercialPager.restore(commercial.fonts, 1, commercial.totalPages);
                    freePager.restore(free.fonts, 1, free.totalPages);
                    updatedAt = time;
                    syncFeed();
                    updateFavorites(fonts);
                    loading = false;
                    rememberShelves();
                    shelves.clear();
                    shelfPositions.clear();
                    if (showingFavorites) updateHeader();
                    else renderFonts(true);
                });
            } catch (Exception failed) {
                Log.w("FontFeed", "Could not refresh public catalogue", failed);
                runOnUiThread(() -> {
                    if (destroyed || isFinishing()) return;
                    loading = false;
                    refreshError = getString(fonts.isEmpty() ? R.string.refresh_failed_empty : R.string.refresh_failed_cached);
                    if (showingFavorites) updateHeader();
                    else renderFonts(true);
                });
            }
        });
    }

    private void openUrl(String url) {
        if (!FontCatalog.safeUrl(url, false)) {
            Toast.makeText(this, R.string.link_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
        catch (ActivityNotFoundException missing) { Toast.makeText(this, R.string.browser_unavailable, Toast.LENGTH_SHORT).show(); }
    }

    private Button button(String text, boolean primary) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(14);
        button.setAllCaps(false);
        button.setMinHeight(dp(48));
        button.setStateListAnimator(null);
        button.setTextColor(primary ? Color.WHITE : ink);
        button.setBackgroundResource(primary ? R.drawable.primary_button : R.drawable.secondary_button);
        return button;
    }

    private Button textButton(String text) {
        Button button = button(text, false);
        button.setTextColor(accent);
        button.setBackgroundResource(R.drawable.text_button);
        button.setPadding(dp(8), 0, dp(8), 0);
        button.setMinWidth(0);
        return button;
    }

    private TextView label(String text, int size, int color) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);
        return view;
    }

    private LinearLayout column() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private LinearLayout row() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER_VERTICAL);
        return layout;
    }

    private void gap(LinearLayout row, int width) { row.addView(new View(this), new LinearLayout.LayoutParams(dp(width), 1)); }
    private LinearLayout.LayoutParams weighted() { return new LinearLayout.LayoutParams(0, -2, 1); }
    private LinearLayout.LayoutParams matchWrap() { return new LinearLayout.LayoutParams(-1, -2); }
    private LinearLayout.LayoutParams wrap() { return new LinearLayout.LayoutParams(-2, -2); }
    private LinearLayout.LayoutParams margin(LinearLayout.LayoutParams params, int top) { params.topMargin = dp(top); return params; }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
