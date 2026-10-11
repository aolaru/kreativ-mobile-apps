package com.kreativ.sharesafe;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

final class Ui {
    static final int INK = Color.rgb(24, 48, 43), MUTED = Color.rgb(92, 110, 103);
    static final int SURFACE = Color.rgb(244, 247, 245), ACCENT = Color.rgb(24, 105, 80);
    static final int MINT = Color.rgb(229, 244, 235), LINE = Color.rgb(222, 232, 225);
    private Ui() { }

    static int dp(Context context, int value) { return Math.round(value * context.getResources().getDisplayMetrics().density); }

    static GradientDrawable rounded(Context context, int color, int radius, int border) {
        GradientDrawable shape = new GradientDrawable(); shape.setColor(color);
        shape.setCornerRadius(dp(context, radius));
        if (border != 0) shape.setStroke(dp(context, 1), border);
        return shape;
    }

    static LinearLayout card(Context context, int color) {
        LinearLayout card = new LinearLayout(context); card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(context, 20), dp(context, 20), dp(context, 20), dp(context, 20));
        card.setBackground(rounded(context, color, 24, color == Color.WHITE ? LINE : 0));
        return card;
    }

    static TextView text(Context context, String text, float size, int color) {
        TextView view = new TextView(context); view.setText(text); view.setTextSize(size); view.setTextColor(color);
        view.setFontFeatureSettings("kern"); view.setLineSpacing(dp(context, 2), 1f); return view;
    }

    static TextView heading(Context context, String text, float size) {
        TextView view = text(context, text, size, INK);
        view.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL)); return view;
    }

    static TextView badge(Context context, String text) {
        TextView view = text(context, text, 12, ACCENT);
        view.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        view.setPadding(dp(context, 12), dp(context, 7), dp(context, 12), dp(context, 7));
        view.setBackground(rounded(context, MINT, 50, 0)); return view;
    }

    static ImageView icon(Context context, int drawable, int size) {
        ImageView icon = new ImageView(context); icon.setImageResource(drawable);
        icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        icon.setLayoutParams(new LinearLayout.LayoutParams(dp(context, size), dp(context, size))); return icon;
    }

    static Button button(Context context, String text, boolean primary) {
        Button button = new Button(context); button.setText(text); button.setTextSize(16); button.setAllCaps(false);
        button.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        button.setMinHeight(dp(context, 54)); button.setMinimumWidth(0);
        button.setStateListAnimator(null);
        styleButton(button, primary); return button;
    }

    static void styleButton(Button button, boolean primary) {
        button.setTextColor(new ColorStateList(new int[][] {new int[] {-android.R.attr.state_enabled}, new int[] {}},
                new int[] {Color.rgb(105, 122, 113), primary ? Color.WHITE : INK}));
        button.setBackgroundResource(primary ? R.drawable.button_primary : R.drawable.button_secondary);
    }

    static Button textButton(Context context, String text) {
        Button button = button(context, text, false);
        android.graphics.drawable.RippleDrawable ripple = new android.graphics.drawable.RippleDrawable(
                ColorStateList.valueOf(LINE), rounded(context, Color.TRANSPARENT, 14, 0), rounded(context, Color.WHITE, 14, 0));
        button.setBackground(ripple); button.setMinHeight(dp(context, 48));
        button.setPadding(dp(context, 8), dp(context, 12), dp(context, 8), dp(context, 12)); return button;
    }

    static void styleDialog(AlertDialog dialog) {
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(
                rounded(dialog.getContext(), Color.WHITE, 24, 0));
    }

    static LinearLayout pageContent(Context context) {
        LinearLayout root = new LinearLayout(context) {
            @Override protected void onMeasure(int width, int height) {
                super.onMeasure(View.MeasureSpec.makeMeasureSpec(Math.min(View.MeasureSpec.getSize(width), dp(getContext(), 640)), View.MeasureSpec.EXACTLY), height);
            }
        };
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(context, 20), dp(context, 20), dp(context, 20), dp(context, 24));
        return root;
    }

    static void addPage(android.widget.ScrollView scroll, LinearLayout root) {
        scroll.setFillViewport(true); scroll.setBackgroundColor(SURFACE); applyInsets(scroll);
        // Avoid stretching cards across a tablet or landscape window.
        LinearLayout wrap = new LinearLayout(scroll.getContext()); wrap.setGravity(Gravity.CENTER_HORIZONTAL);
        wrap.addView(root, new LinearLayout.LayoutParams(-1, -2));
        scroll.addView(wrap);
    }

    static void applyInsets(View view) {
        int left = view.getPaddingLeft(), top = view.getPaddingTop();
        int right = view.getPaddingRight(), bottom = view.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            androidx.core.graphics.Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(left + bars.left, top + bars.top, right + bars.right, bottom + bars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(view);
    }
}
