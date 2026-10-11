package com.kreativ.paperbuild;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

final class Ui {
    static final int INK = Color.rgb(24, 43, 67);
    static final int MUTED = Color.rgb(94, 109, 131);
    static final int ACCENT = Color.rgb(76, 95, 223);
    static final int SURFACE = Color.rgb(245, 247, 251);
    private Ui() { }

    static void applyInsets(View scroll, View content) {
        float density = content.getResources().getDisplayMetrics().density;
        int side = Math.round(20 * density);
        int top = Math.round(20 * density);
        int bottom = Math.round(64 * density);
        content.setPadding(side, top, side, bottom);
        if (scroll instanceof ViewGroup) ((ViewGroup) scroll).setClipToPadding(true);
        ViewCompat.setOnApplyWindowInsetsListener(scroll, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            Insets keyboard = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
            // Keep the light surface behind system icons while the cards scroll.
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            content.setPadding(side, top, side,
                    bottom + Math.max(0, keyboard.bottom - bars.bottom));
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(scroll);
    }

    static int dp(Context context, int size) { return Math.round(size * context.getResources().getDisplayMetrics().density); }
    static TextView text(Context context, String copy, float size, int color, boolean bold) {
        TextView text = new TextView(context);
        text.setText(copy); text.setTextSize(size); text.setTextColor(color); text.setIncludeFontPadding(false);
        text.setLineSpacing(dp(context, 3), 1f);
        if (bold) text.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        return text;
    }
    static LinearLayout column(Context context) {
        LinearLayout layout = new LinearLayout(context); layout.setOrientation(LinearLayout.VERTICAL); return layout;
    }
    static LinearLayout card(Context context, int background) {
        LinearLayout card = column(context); card.setBackgroundResource(background);
        int padding = dp(context, 20); card.setPadding(padding, padding, padding, padding);
        return card;
    }
    static void gap(LinearLayout layout, int size) {
        layout.addView(new View(layout.getContext()), new LinearLayout.LayoutParams(1, dp(layout.getContext(), size)));
    }
    static Button button(Context context, String text, boolean primary) {
        Button button = new Button(context); button.setText(text); button.setAllCaps(false);
        button.setTextSize(15); button.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        button.setMinWidth(0); button.setMinimumWidth(0); button.setMinHeight(dp(context, 54));
        button.setPadding(dp(context, 14), dp(context, 12), dp(context, 14), dp(context, 12));
        button.setTextColor(context.getColorStateList(primary ? R.color.button_text_primary : R.color.button_text_secondary));
        button.setBackgroundResource(primary ? R.drawable.button_primary : R.drawable.button_secondary);
        button.setStateListAnimator(null); return button;
    }
    static void icon(Button button, int resource, int color) {
        Drawable icon = button.getContext().getDrawable(resource).mutate(); icon.setTint(color);
        int size = dp(button.getContext(), 20); icon.setBounds(0, 0, size, size);
        button.setCompoundDrawablesRelative(icon, null, null, null); button.setCompoundDrawablePadding(dp(button.getContext(), 8));
    }
    static ImageView image(Context context, int resource, int tint) {
        ImageView view = new ImageView(context); view.setImageResource(resource); view.setColorFilter(tint);
        view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO); return view;
    }
    static TextView pill(Context context, String copy, int background, int color) {
        TextView view = text(context, copy, 11, color, true);
        view.setPadding(dp(context, 10), dp(context, 7), dp(context, 10), dp(context, 7));
        GradientDrawable shape = new GradientDrawable(); shape.setColor(background); shape.setCornerRadius(dp(context, 30));
        view.setBackground(shape); return view;
    }
    static View brand(Context context) {
        LinearLayout row = new LinearLayout(context); row.setGravity(Gravity.CENTER_VERTICAL);
        ImageView logo = new ImageView(context); logo.setImageResource(R.drawable.ic_paperbuild);
        logo.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        row.addView(logo, new LinearLayout.LayoutParams(dp(context, 34), dp(context, 34)));
        TextView name = text(context, "PaperBuild", 21, INK, true); name.setPadding(dp(context, 10), 0, 0, 0);
        row.addView(name, new LinearLayout.LayoutParams(0, -2, 1));
        if (context.getResources().getConfiguration().screenWidthDp >= 360
                && context.getResources().getConfiguration().fontScale <= 1.15f) {
            row.addView(pill(context, "ON DEVICE", Color.rgb(231, 244, 238), Color.rgb(35, 104, 78)));
        }
        return row;
    }
    static View hero(Context context, String eyebrow, String title, String subtitle) {
        LinearLayout hero = card(context, R.drawable.hero_panel);
        TextView kicker = text(context, eyebrow, 11, Color.rgb(176, 196, 246), true);
        kicker.setLetterSpacing(.10f); hero.addView(kicker); gap(hero, 14);
        LinearLayout row = new LinearLayout(context); row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout copy = column(context);
        copy.addView(text(context, title, 30, Color.WHITE, true)); gap(copy, 10);
        copy.addView(text(context, subtitle, 14, Color.rgb(213, 223, 246), false));
        row.addView(copy, new LinearLayout.LayoutParams(0, -2, 1));
        ImageView illustration = new ImageView(context); illustration.setImageResource(R.drawable.ic_document_hero);
        illustration.setScaleType(ImageView.ScaleType.FIT_CENTER);
        illustration.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        boolean narrow = context.getResources().getConfiguration().screenWidthDp < 360
                || context.getResources().getConfiguration().fontScale > 1.15f;
        // Keep the headline readable when space or larger text takes priority.
        if (!narrow) row.addView(illustration, new LinearLayout.LayoutParams(dp(context, 100), dp(context, 120)));
        hero.addView(row); return hero;
    }
    static void step(LinearLayout layout, String number, String title) {
        Context context = layout.getContext(); LinearLayout row = new LinearLayout(context); row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(pill(context, number, Color.rgb(237, 240, 255), ACCENT));
        TextView text = text(context, title, 18, INK, true); text.setPadding(dp(context, 10), 0, 0, 0);
        row.addView(text, new LinearLayout.LayoutParams(0, -2, 1)); layout.addView(row);
    }
    static View feature(Context context, int icon, String heading, String body) {
        LinearLayout row = new LinearLayout(context); row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout card = card(context, R.drawable.card);
        ImageView image = image(context, icon, ACCENT);
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(dp(context, 30), dp(context, 30));
        imageParams.setMarginEnd(dp(context, 16)); row.addView(image, imageParams);
        LinearLayout copy = column(context); copy.addView(text(context, heading, 17, INK, true)); gap(copy, 5);
        copy.addView(text(context, body, 14, MUTED, false)); row.addView(copy, new LinearLayout.LayoutParams(0, -2, 1));
        card.addView(row); return card;
    }
}
