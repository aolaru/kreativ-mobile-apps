package com.kreativ.fontboard;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

/** Private viewer of website artwork, not a font installer or synthetic text specimen. */
public class FontPreviewActivity extends Activity {
    private ZoomPreviewView preview;
    private TextView status;
    private Button retry, zoomIn, zoomOut, fit;
    private String imageUrl;
    private CustomTarget<Drawable> imageTarget;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        imageUrl = getIntent().getStringExtra("image");
        if (imageUrl == null || !FontCatalog.safeUrl(imageUrl, true)) { finish(); return; }
        String name = getIntent().getStringExtra("name");
        if (name == null) name = getString(R.string.app_name);
        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setBackgroundColor(Color.WHITE);
        ViewCompat.setOnApplyWindowInsetsListener(shell, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(12), dp(8), dp(12), dp(8));
        Button close = action(getString(R.string.close));
        close.setOnClickListener(v -> finish());
        header.addView(close);
        TextView title = text(name, 18);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setMaxLines(2);
        title.setPadding(dp(12), 0, 0, 0);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        shell.addView(header);
        preview = new ZoomPreviewView(this);
        preview.setTag("full_screen_preview");
        preview.setContentDescription(getString(R.string.font_preview, name));
        preview.setBackgroundColor(getColor(R.color.preview_background));
        shell.addView(preview, new LinearLayout.LayoutParams(-1, 0, 1));
        status = text(getString(R.string.preview_loading), 13);
        status.setGravity(Gravity.CENTER);
        status.setPadding(dp(16), dp(8), dp(16), dp(8));
        status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        shell.addView(status);
        LinearLayout controls = new LinearLayout(this);
        controls.setGravity(Gravity.CENTER);
        controls.setPadding(dp(12), 0, dp(12), dp(8));
        zoomOut = action("−");
        zoomOut.setTag("preview_zoom_out");
        zoomOut.setContentDescription(getString(R.string.zoom_out));
        zoomOut.setOnClickListener(v -> preview.zoomOut());
        zoomIn = action("+");
        zoomIn.setTag("preview_zoom_in");
        zoomIn.setContentDescription(getString(R.string.zoom_in));
        zoomIn.setOnClickListener(v -> preview.zoomIn());
        fit = action(getString(R.string.zoom_fit));
        fit.setTag("preview_zoom_fit");
        fit.setContentDescription(getString(R.string.zoom_fit_description));
        fit.setOnClickListener(v -> preview.fit());
        retry = action(getString(R.string.preview_retry));
        retry.setOnClickListener(v -> loadImage());
        for (Button control : new Button[]{zoomOut, fit, zoomIn, retry}) {
            controls.addView(control, new LinearLayout.LayoutParams(0, -2, 1));
        }
        shell.addView(controls);
        setContentView(shell);
        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), shell);
        controller.setAppearanceLightStatusBars(true);
        controller.setAppearanceLightNavigationBars(true);
        ViewCompat.requestApplyInsets(shell);
        if (state != null) preview.restoreZoom(state.getFloat("zoom", 1), state.getFloat("center_x", .5f), state.getFloat("center_y", .5f));
        loadImage();
    }

    private void loadImage() {
        if (imageTarget != null) Glide.with(this).clear(imageTarget);
        setReady(false);
        retry.setVisibility(View.GONE);
        status.setText(R.string.preview_loading);
        // Bound decoded image memory, even when a publisher uploads extremely large artwork.
        imageTarget = new CustomTarget<Drawable>(2048, 2048) {
            @Override public void onResourceReady(Drawable resource, Transition<? super Drawable> transition) {
                if (isDestroyed() || isFinishing()) return;
                preview.setImageDrawable(resource);
                status.setText(R.string.preview_gestures);
                setReady(true);
            }
            @Override public void onLoadFailed(Drawable errorDrawable) {
                if (isDestroyed() || isFinishing()) return;
                status.setText(R.string.preview_failed);
                retry.setVisibility(View.VISIBLE);
            }
            @Override public void onLoadCleared(Drawable placeholder) { preview.setImageDrawable(null); }
        };
        Glide.with(this).load(imageUrl).fitCenter().into(imageTarget);
    }

    private void setReady(boolean ready) {
        for (Button control : new Button[]{zoomOut, fit, zoomIn}) { control.setEnabled(ready); control.setAlpha(ready ? 1 : .45f); }
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        if (preview != null) {
            state.putFloat("zoom", preview.transform.zoom);
            state.putFloat("center_x", preview.transform.centerX());
            state.putFloat("center_y", preview.transform.centerY());
        }
        super.onSaveInstanceState(state);
    }

    @Override protected void onDestroy() {
        if (imageTarget != null) Glide.with(this).clear(imageTarget);
        super.onDestroy();
    }

    private Button action(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(14);
        button.setMinWidth(dp(48));
        button.setMinHeight(dp(48));
        button.setTextColor(getColor(R.color.primary));
        button.setBackgroundResource(R.drawable.text_button);
        button.setPadding(dp(6), 0, dp(6), 0);
        return button;
    }

    private TextView text(String value, int size) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(getColor(R.color.ink));
        return view;
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
