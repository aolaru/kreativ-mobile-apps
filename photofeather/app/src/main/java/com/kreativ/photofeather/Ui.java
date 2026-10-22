package com.kreativ.photofeather;

import android.content.*;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;
import androidx.core.view.*;

final class Ui {
  static int color(Context c, int id) {
    return c.getColor(id);
  }

  static int dp(Context c, int n) {
    return Math.round(n * c.getResources().getDisplayMetrics().density);
  }

  static LinearLayout column(Context c) {
    LinearLayout l = new LinearLayout(c);
    l.setOrientation(LinearLayout.VERTICAL);
    return l;
  }

  static LinearLayout row(Context c) {
    LinearLayout l = new LinearLayout(c);
    l.setGravity(Gravity.CENTER_VERTICAL);
    return l;
  }

  static GradientDrawable shape(Context c, int color, int radius, boolean border) {
    GradientDrawable d = new GradientDrawable();
    d.setColor(color);
    d.setCornerRadius(dp(c, radius));
    if (border) d.setStroke(dp(c, 1), color(c, R.color.line));
    return d;
  }

  static TextView text(Context c, String s, int sp, int color, boolean bold) {
    TextView t = new TextView(c);
    t.setText(s);
    t.setTextSize(sp);
    t.setTextColor(color(c, color));
    t.setFontFeatureSettings("kern");
    t.setLineSpacing(dp(c, 3), 1);
    if (bold) t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
    return t;
  }

  static void heading(TextView t) {
    ViewCompat.setAccessibilityHeading(t, true);
  }

  static void gap(LinearLayout l, int size) {
    l.addView(new View(l.getContext()), new LinearLayout.LayoutParams(1, dp(l.getContext(), size)));
  }

  static LinearLayout card(Context c) {
    LinearLayout l = column(c);
    l.setPadding(dp(c, 20), dp(c, 20), dp(c, 20), dp(c, 20));
    l.setBackground(shape(c, color(c, R.color.card), 24, true));
    return l;
  }

  static Button button(Context c, String s, boolean primary) {
    Button b = new Button(c);
    b.setText(s);
    b.setAllCaps(false);
    b.setTextSize(15);
    b.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
    b.setMinHeight(dp(c, 52));
    b.setMinimumWidth(0);
    b.setMinWidth(0);
    b.setPadding(dp(c, 16), dp(c, 12), dp(c, 16), dp(c, 12));
    styleButton(b, primary);
    b.setStateListAnimator(null);
    return b;
  }

  static void styleButton(Button b, boolean primary) {
    Context c = b.getContext();
    b.setTextColor(
        new ColorStateList(
            new int[][] {new int[] {-android.R.attr.state_enabled}, new int[] {}},
            new int[] {
              color(c, R.color.muted), color(c, primary ? R.color.on_accent : R.color.ink)
            }));
    b.setBackground(
        new RippleDrawable(
            ColorStateList.valueOf(color(c, R.color.line)),
            shape(c, color(c, primary ? R.color.accent : R.color.mint), 16, false),
            null));
  }

  static Button addButton(LinearLayout l, String s, boolean primary, Runnable run) {
    Button b = button(l.getContext(), s, primary);
    l.addView(b, new LinearLayout.LayoutParams(-1, -2));
    b.setOnClickListener(v -> run.run());
    return b;
  }

  static EditText number(Context c, String label, String value) {
    EditText e = new EditText(c);
    e.setSingleLine(true);
    e.setText(value);
    e.setTextSize(18);
    e.setTextColor(color(c, R.color.ink));
    e.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
    e.setSelectAllOnFocus(true);
    e.setContentDescription(label);
    e.setMinHeight(dp(c, 52));
    e.setPadding(dp(c, 12), dp(c, 8), dp(c, 12), dp(c, 8));
    e.setBackground(shape(c, color(c, R.color.surface), 12, true));
    return e;
  }

  static ScrollView page(android.app.Activity a, LinearLayout content) {
    LinearLayout outer = column(a);
    outer.setBackgroundColor(color(a, R.color.surface));
    ScrollView scroll = new ScrollView(a);
    scroll.setFillViewport(true);
    LinearLayout wrap = row(a);
    wrap.setGravity(Gravity.CENTER_HORIZONTAL);
    content.setPadding(dp(a, 20), dp(a, 20), dp(a, 20), dp(a, 28));
    wrap.addView(content, new LinearLayout.LayoutParams(-1, -2));
    scroll.addView(wrap);
    outer.addView(scroll, new LinearLayout.LayoutParams(-1, -1));
    ViewCompat.setOnApplyWindowInsetsListener(
        outer,
        (v, insets) -> {
          androidx.core.graphics.Insets bars =
              insets.getInsets(
                  WindowInsetsCompat.Type.systemBars()
                      | WindowInsetsCompat.Type.displayCutout()
                      | WindowInsetsCompat.Type.ime());
          v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
          return insets;
        });
    a.setContentView(outer);
    ViewCompat.requestApplyInsets(outer);
    return scroll;
  }

  static LinearLayout pageColumn(Context c) {
    return new LinearLayout(c) {
      {
        setOrientation(LinearLayout.VERTICAL);
      }

      @Override
      protected void onMeasure(int w, int h) {
        super.onMeasure(
            MeasureSpec.makeMeasureSpec(
                Math.min(MeasureSpec.getSize(w), dp(getContext(), 640)), MeasureSpec.EXACTLY),
            h);
      }
    };
  }

  static String size(long n) {
    return n < 1000
        ? n + " B"
        : n < 1_000_000
            ? String.format(java.util.Locale.US, "%.1f KB", n / 1000.0)
            : String.format(java.util.Locale.US, "%.2f MB", n / 1_000_000.0);
  }
}
