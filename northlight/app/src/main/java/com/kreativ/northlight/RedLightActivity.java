package com.kreativ.northlight;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.SeekBar;
import android.widget.ScrollView;

/** A distraction-free red screen light for preserving night vision. */
public class RedLightActivity extends Activity {
    public static final String EXTRA_MODE = "night_light_mode";
    private int background;
    private int foreground;
    private String modeLabel;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureMode();
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        applyBrightness(getPreferences(MODE_PRIVATE).getInt("night_brightness", 35));
        setContentView(buildScreen());
        hideSystemBars();
    }

    @Override public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemBars();
    }

    private View buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(background);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(28), dp(28), dp(28), dp(34));
        root.setBackgroundColor(background);
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));

        View spacer = new View(this);
        root.addView(spacer, new LinearLayout.LayoutParams(1, 0, 1));
        TextView label = new TextView(this);
        label.setText(getString(R.string.night_light_label, modeLabel));
        label.setTextColor(foreground);
        label.setTextSize(13);
        label.setLetterSpacing(.18f);
        label.setGravity(Gravity.CENTER);
        root.addView(label, new LinearLayout.LayoutParams(-1, -2));
        TextView hint = new TextView(this);
        hint.setText(R.string.night_light_hint);
        hint.setTextColor(foreground);
        hint.setTextSize(18);
        hint.setGravity(Gravity.CENTER);
        hint.setPadding(0, dp(9), 0, 0);
        root.addView(hint, new LinearLayout.LayoutParams(-1, -2));
        View lower = new View(this);
        root.addView(lower, new LinearLayout.LayoutParams(1, 0, 1));
        TextView brightnessLabel = new TextView(this);
        brightnessLabel.setText(R.string.screen_brightness);
        brightnessLabel.setTextColor(foreground);
        root.addView(brightnessLabel);
        SeekBar brightness = new SeekBar(this);
        brightness.setMax(95);
        brightness.setProgress(getPreferences(MODE_PRIVATE).getInt("night_brightness", 35) - 5);
        brightness.setContentDescription(getString(R.string.screen_brightness));
        brightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                if (fromUser) {
                    int percent = progress + 5;
                    applyBrightness(percent);
                    getPreferences(MODE_PRIVATE).edit().putInt("night_brightness", percent).apply();
                }
            }
            @Override public void onStartTrackingTouch(SeekBar bar) { }
            @Override public void onStopTrackingTouch(SeekBar bar) { }
        });
        root.addView(brightness, new LinearLayout.LayoutParams(-1, -2));
        Button close = new Button(this);
        close.setText(getString(R.string.close_night_light, modeLabel));
        close.setAllCaps(false);
        close.setTextSize(16);
        close.setTextColor(background);
        close.setBackgroundResource(R.drawable.button_gold);
        close.setMinHeight(dp(54));
        close.setOnClickListener(v -> finish());
        root.addView(close, new LinearLayout.LayoutParams(-1, -2));
        return Ui.screen(this, scroll);
    }

    private void configureMode() {
        String mode = getIntent().getStringExtra(EXTRA_MODE);
        if ("amber".equals(mode)) {
            background = Color.rgb(112, 65, 8);
            foreground = Color.rgb(255, 229, 183);
            modeLabel = getString(R.string.amber);
        } else if ("green".equals(mode)) {
            background = Color.rgb(8, 69, 43);
            foreground = Color.rgb(207, 255, 222);
            modeLabel = getString(R.string.green);
        } else {
            background = Color.rgb(106, 16, 26);
            foreground = Color.rgb(255, 215, 215);
            modeLabel = getString(R.string.red);
        }
    }

    private void hideSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().getDecorView().getWindowInsetsController().hide(WindowInsets.Type.systemBars());
        } else {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }
    private void applyBrightness(int percent) {
        WindowManager.LayoutParams attributes = getWindow().getAttributes();
        attributes.screenBrightness = Math.max(5, Math.min(100, percent)) / 100f;
        getWindow().setAttributes(attributes);
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
