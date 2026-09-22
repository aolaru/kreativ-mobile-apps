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

/** A distraction-free red screen light for preserving night vision. */
public class RedLightActivity extends Activity {
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.setStatusBarColor(Color.rgb(106, 16, 26));
        window.setNavigationBarColor(Color.rgb(106, 16, 26));
        WindowManager.LayoutParams attributes = window.getAttributes();
        attributes.screenBrightness = 1f;
        window.setAttributes(attributes);
        hideSystemBars();
        setContentView(buildScreen());
    }

    @Override public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemBars();
    }

    private View buildScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(28), dp(28), dp(28), dp(34));
        root.setBackgroundColor(Color.rgb(106, 16, 26));
        View spacer = new View(this);
        root.addView(spacer, new LinearLayout.LayoutParams(1, 0, 1));
        TextView label = new TextView(this);
        label.setText("RED LIGHT");
        label.setTextColor(Color.rgb(255, 215, 215));
        label.setTextSize(13);
        label.setLetterSpacing(.18f);
        label.setGravity(Gravity.CENTER);
        root.addView(label, new LinearLayout.LayoutParams(-1, -2));
        TextView hint = new TextView(this);
        hint.setText("Night vision mode");
        hint.setTextColor(Color.rgb(255, 215, 215));
        hint.setTextSize(18);
        hint.setGravity(Gravity.CENTER);
        hint.setPadding(0, dp(9), 0, 0);
        root.addView(hint, new LinearLayout.LayoutParams(-1, -2));
        View lower = new View(this);
        root.addView(lower, new LinearLayout.LayoutParams(1, 0, 1));
        Button close = new Button(this);
        close.setText("Close red light");
        close.setAllCaps(false);
        close.setTextSize(16);
        close.setTextColor(Color.rgb(106, 16, 26));
        close.setBackgroundResource(R.drawable.button_gold);
        close.setMinHeight(dp(54));
        close.setOnClickListener(v -> finish());
        root.addView(close, new LinearLayout.LayoutParams(-1, -2));
        return root;
    }

    private void hideSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().getDecorView().getWindowInsetsController().hide(WindowInsets.Type.systemBars());
        } else {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
