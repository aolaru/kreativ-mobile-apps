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
import org.json.JSONObject;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MainActivity extends Activity {
    private static final String PRIVACY = "https://kreativfont.com/blog/terms-of-use-privacy-policy";
    private static final String KREATIV_APPS = "https://madebykreativ.com/apps";
    private final ExecutorService network = Executors.newSingleThreadExecutor();
    private final ExecutorService searchNetwork = Executors.newFixedThreadPool(2);
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final FontPager commercialPager = new FontPager(), freePager = new FontPager(), searchPager = new FontPager();
    private Future<?> searchTask;
    private FontSearch searchSession;
    private Runnable searchDebounce;
    private String activeQuery = "";
    private String searchFilter = "All";
    private LinearLayout searchFilters;
    private final Map<String, Button> filterButtons = new HashMap<>();
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
            String savedFilter = state.getString("search_filter", "All");
            if (savedFilter.equals("Commercial") || savedFilter.equals("Free")) searchFilter = savedFilter;
        }
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
        state.putString("search_filter", searchFilter);
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
        searchFilters = row();
        searchFilters.setTag("search_filters");
        for (String filter : new String[]{"All", "Commercial", "Free"}) {
            if (!filterButtons.isEmpty()) gap(searchFilters, 8);
            Button choice = button(filter, false);
            choice.setTag("search_filter." + filter);
            choice.setPadding(dp(4), 0, dp(4), 0);
            choice.setOnClickListener(v -> {
                if (searchFilter.equals(filter)) return;
                searchFilter = filter;
                onSearchChanged();
            });
            filterButtons.put(filter, choice);
            searchFilters.addView(choice, weighted());
        }
        header.addView(searchFilters, margin(matchWrap(), 8));
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
        searchFilters.setVisibility(searching ? View.VISIBLE : View.GONE);
        for (Map.Entry<String, Button> filter : filterButtons.entrySet()) {
            boolean selected = filter.getKey().equals(searchFilter);
            filter.getValue().setSelected(selected);
            filter.getValue().setBackgroundResource(selected ? R.drawable.primary_button : R.drawable.secondary_button);
            filter.getValue().setTextColor(selected ? Color.WHITE : ink);
            filter.getValue().setContentDescription(getString(selected ? R.string.filter_selected : R.string.filter_search, filter.getKey()));
        }
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
                    : searchPager.busy ? R.string.service_busy : searchPager.failed ? R.string.search_failed : R.string.search_scope);
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
            if (showingFavorites) {
                Button projects = button(getString(R.string.projects), false);
                projects.setOnClickListener(v -> showProjects());
                content.addView(projects, margin(matchWrap(), 8));
            }
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
        LinearLayout credit = row();
        credit.setTag("version_credit");
        credit.setGravity(Gravity.CENTER);
        TextView version = label(getString(R.string.app_version, BuildConfig.VERSION_NAME), 12, muted);
        credit.addView(version, wrap());
        Button maker = textButton(getString(R.string.maker_credit));
        maker.setTag("maker_credit");
        maker.setTextSize(12);
        maker.setOnClickListener(v -> openUrl(KREATIV_APPS));
        credit.addView(maker, wrap());
        content.addView(credit, margin(matchWrap(), 8));
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
        FontPager page = pager(availability);
        LinearLayout more = column();
        more.setTag("load_more_card." + availability);
        more.setBackgroundResource(R.drawable.load_more_card);
        more.setPadding(dp(16), dp(16), dp(16), dp(16));
        more.setGravity(Gravity.CENTER);
        TextView arrow = label("→", 32, accent);
        arrow.setGravity(Gravity.CENTER);
        arrow.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        more.addView(arrow, matchWrap());
        TextView moreTitle = label(getString(page.hasMore() ? R.string.explore_more_fonts : R.string.all_caught_up), 18, ink);
        moreTitle.setTypeface(Typeface.DEFAULT_BOLD);
        moreTitle.setGravity(Gravity.CENTER);
        more.addView(moreTitle, margin(matchWrap(), 4));
        TextView moreHint = label(getString(page.hasMore()
                ? R.string.more_fonts_hint : R.string.catalogue_end_hint), 13, muted);
        moreHint.setGravity(Gravity.CENTER);
        more.addView(moreHint, margin(matchWrap(), 8));
        View endControl = sectionPageControl(availability, false);
        endControl.setTag("load_more_end." + availability);
        more.addView(endControl, margin(matchWrap(), 16));
        // A wrap-content end card floated above the font-card actions. Match the
        // row's measured height and centre the call to action within that card.
        cards.addView(more, new LinearLayout.LayoutParams(width, ViewGroup.LayoutParams.MATCH_PARENT));
        shelf.addView(cards, new HorizontalScrollView.LayoutParams(-2, -2));
        content.addView(shelf, margin(matchWrap(), 12));
        // The same action is reachable without swiping past every preview.
        View footerControl = sectionPageControl(availability, true);
        footerControl.setTag("load_more_footer." + availability);
        content.addView(footerControl, margin(matchWrap(), 12));
        if (page.busy) {
            TextView busy = label(getString(R.string.service_busy), 14, ink);
            busy.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
            content.addView(busy, margin(matchWrap(), 8));
        }
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

    private View sectionPageControl(String availability, boolean footer) {
        FontPager page = pager(availability);
        View control = pageControl(page, () -> loadFeedPage(availability));
        if (control instanceof Button) {
            Button button = (Button) control;
            if (footer && !page.loading && !page.failed) {
                button.setText(availability.equals("Free") ? R.string.load_more_free : R.string.load_more_commercial);
            }
            String title = getString(availability.equals("Free") ? R.string.latest_free : R.string.latest_commercial);
            button.setContentDescription(getString(R.string.section_action, title, button.getText()));
        }
        return control;
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
        if (page.busy) more.setContentDescription(getString(R.string.service_busy) + " " + getString(R.string.retry_more));
        more.setMinHeight(dp(56));
        more.setGravity(Gravity.CENTER);
        more.setEnabled(!page.loading && (page == searchPager || !loading));
        more.setAlpha(more.isEnabled() ? 1f : .55f);
        more.setOnClickListener(v -> load.run());
        return more;
    }

    private void onSearchChanged() {
        if (searchDebounce != null) handler.removeCallbacks(searchDebounce);
        searchDebounce = null;
        if (searchTask != null) searchTask.cancel(true);
        searchPager.reset();
        activeQuery = search.getText().toString().trim();
        searchSession = new FontSearch(activeQuery, searchFilter);
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
        FontSearch session = searchSession;
        renderFonts(true);
        searchTask = searchNetwork.submit(() -> {
            try {
                FontApi.Page page = session.load(request.page);
                runOnUiThread(() -> {
                    if (destroyed || isFinishing() || !searchPager.complete(request, page)) return;
                    updateFavorites(page.fonts);
                    renderFonts(true);
                });
            } catch (Exception failed) {
                // Do not include query-bearing URLs or exception messages in logs.
                Log.w("FontSearch", failed instanceof FontApi.RequestFailure
                        ? "Catalogue request failed (HTTP " + ((FontApi.RequestFailure) failed).status + ")"
                        : "Catalogue request failed (" + failed.getClass().getSimpleName() + ")");
                runOnUiThread(() -> {
                    if (destroyed || isFinishing() || !searchPager.fail(request, isRateLimited(failed))) return;
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
                    if (destroyed || isFinishing() || !page.fail(request, isRateLimited(failed))) return;
                    if (page.busy) Toast.makeText(this, R.string.service_busy, Toast.LENGTH_LONG).show();
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
            image.setContentDescription(getString(R.string.open_font_preview, font.name));
            image.setOnClickListener(v -> openPreview(font));
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
        if (FontCatalog.safeUrl(font.fullImageUrl, true) || FontCatalog.safeUrl(font.imageUrl, true)) {
            Button preview = button(getString(R.string.open_preview), true);
            preview.setOnClickListener(v -> openPreview(font));
            body.addView(preview, margin(matchWrap(), 8));
        }
        TextView text = label(font.description, 16, muted);
        text.setLineSpacing(dp(4), 1);
        body.addView(text, matchWrap());
        TextView licence = label(getString(R.string.license_reminder), 14, ink);
        body.addView(licence, margin(matchWrap(), 12));
        Button share = button(getString(R.string.share_font), false);
        share.setContentDescription(getString(R.string.share_font_description, font.name));
        share.setOnClickListener(v -> shareFont(font));
        body.addView(share, margin(matchWrap(), 12));
        Button project = button(getString(R.string.add_to_project), false);
        project.setOnClickListener(v -> chooseProject(font));
        body.addView(project, margin(matchWrap(), 8));
        details.addView(body);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(font.name).setView(details)
                .setPositiveButton(R.string.open_article, (d, which) -> openUrl(font.articleUrl))
                .setNeutralButton(isFavorite(font) ? R.string.remove_saved : R.string.save, (d, which) -> toggleFavorite(font))
                .setNegativeButton(R.string.close, null).create();
        dialog.show();
    }

    private void openPreview(FontItem font) {
        String imageUrl = FontCatalog.safeUrl(font.fullImageUrl, true) ? font.fullImageUrl : font.imageUrl;
        if (!FontCatalog.safeUrl(imageUrl, true)) {
            Toast.makeText(this, R.string.preview_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, FontPreviewActivity.class);
        intent.putExtra("image", imageUrl);
        intent.putExtra("name", font.name);
        startActivity(intent);
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
                    refreshError = getString(isRateLimited(failed) ? (fonts.isEmpty() ? R.string.service_busy_empty : R.string.service_busy)
                            : (fonts.isEmpty() ? R.string.refresh_failed_empty : R.string.refresh_failed_cached));
                    if (showingFavorites) updateHeader();
                    else renderFonts(true);
                });
            }
        });
    }

    private static boolean isRateLimited(Exception failure) {
        return failure instanceof FontApi.RequestFailure && ((FontApi.RequestFailure) failure).status == 429;
    }

    private void shareFont(FontItem font) {
        if (font.id <= 0 || !FontCatalog.safeUrl(font.articleUrl, false)) {
            Toast.makeText(this, R.string.link_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent share = new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, font.articleUrl);
        try { startActivity(Intent.createChooser(share, getString(R.string.share_font))); }
        catch (ActivityNotFoundException missing) { Toast.makeText(this, R.string.share_unavailable, Toast.LENGTH_SHORT).show(); }
    }

    private List<String> projectNames() {
        List<String> names = new ArrayList<>();
        java.util.Iterator<String> keys = store.projects().keys();
        while (keys.hasNext()) names.add(keys.next());
        Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    private void createProject(FontItem add) {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint(R.string.project_name);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(R.string.new_project).setView(input)
                .setPositiveButton(R.string.save, null).setNegativeButton(R.string.close, null).create();
        dialog.setOnShowListener(unused -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = input.getText().toString().trim();
            List<String> names = projectNames();
            boolean exists = false;
            for (String previous : names) if (previous.equalsIgnoreCase(name)) exists = true;
            if (name.isEmpty() || exists || name.length() > 80) {
                input.setError(getString(R.string.project_exists));
                return;
            }
            try {
                JSONObject projects = store.projects();
                projects.put(name, new org.json.JSONArray());
                if (!store.saveProjects(projects)) throw new IllegalStateException();
                dialog.dismiss();
                if (add != null) addToProject(name, add);
                else showProject(name);
            } catch (Exception failed) { Toast.makeText(this, R.string.project_error, Toast.LENGTH_LONG).show(); }
        }));
        dialog.show();
    }

    private void chooseProject(FontItem font) {
        List<String> names = projectNames();
        List<String> choices = new ArrayList<>(names);
        choices.add(getString(R.string.new_project));
        new AlertDialog.Builder(this).setTitle(R.string.add_to_project)
                .setItems(choices.toArray(new String[0]), (d, which) -> {
                    if (which == names.size()) createProject(font);
                    else addToProject(names.get(which), font);
                }).setNegativeButton(R.string.close, null).show();
    }

    private void addToProject(String name, FontItem font) {
        try {
            List<FontItem> items = store.projectFonts(name);
            boolean exists = false;
            for (FontItem item : items) if (item.key().equals(font.key())) exists = true;
            if (!exists) items.add(font);
            JSONObject projects = store.projects();
            projects.put(name, new org.json.JSONArray(FontCatalog.encode(items)));
            if (!store.saveProjects(projects)) throw new IllegalStateException();
            Toast.makeText(this, R.string.added_to_project, Toast.LENGTH_SHORT).show();
        } catch (Exception failed) { Toast.makeText(this, R.string.project_error, Toast.LENGTH_LONG).show(); }
    }

    private void showProjects() {
        List<String> names = projectNames();
        List<String> choices = new ArrayList<>(names);
        choices.add(getString(R.string.new_project));
        new AlertDialog.Builder(this).setTitle(R.string.projects)
                .setItems(choices.toArray(new String[0]), (d, which) -> {
                    if (which == names.size()) createProject(null); else showProject(names.get(which));
                }).setNegativeButton(R.string.close, null).show();
    }

    private void showProject(String name) {
        List<FontItem> items = store.projectFonts(name);
        List<String> choices = new ArrayList<>();
        for (FontItem item : items) choices.add(item.name);
        if (items.isEmpty()) choices.add(getString(R.string.project_empty));
        new AlertDialog.Builder(this).setTitle(name).setItems(choices.toArray(new String[0]), (d, which) -> {
            if (items.isEmpty()) { showProject(name); return; }
            FontItem selected = items.get(which);
            new AlertDialog.Builder(this).setTitle(selected.name)
                    .setItems(new String[]{getString(R.string.open_article), getString(R.string.share_font), getString(R.string.remove_from_project)}, (choice, action) -> {
                        if (action == 0) openUrl(selected.articleUrl);
                        else if (action == 1) shareFont(selected);
                        else {
                            try {
                                items.remove(which);
                                JSONObject projects = store.projects();
                                projects.put(name, new org.json.JSONArray(FontCatalog.encode(items)));
                                if (!store.saveProjects(projects)) throw new IllegalStateException();
                                showProject(name);
                            } catch (Exception failed) { Toast.makeText(this, R.string.project_error, Toast.LENGTH_LONG).show(); }
                        }
                    }).setNegativeButton(R.string.close, null).show();
        }).setPositiveButton(R.string.close, null).setNeutralButton(R.string.delete_project, (d, which) ->
                new AlertDialog.Builder(this).setMessage(R.string.delete_project_confirm)
                        .setPositiveButton(R.string.delete_project, (confirm, action) -> {
                            JSONObject projects = store.projects();
                            projects.remove(name);
                            if (!store.saveProjects(projects)) Toast.makeText(this, R.string.project_error, Toast.LENGTH_LONG).show();
                            else showProjects();
                        }).setNegativeButton(R.string.close, null).show()).show();
    }

    private void openUrl(String url) {
        if (!KREATIV_APPS.equals(url) && !FontCatalog.safeUrl(url, false)) {
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
