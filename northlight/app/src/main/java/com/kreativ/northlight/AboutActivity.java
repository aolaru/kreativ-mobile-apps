package com.kreativ.northlight;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Plain-language explanation of the app's intentionally minimal data practices. */
public class AboutActivity extends Activity {
    private static final String PRIVACY_POLICY_URL = "https://madebykreativ.com/privacy/northlight/";
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildScreen());
    }

    private View buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(11, 19, 32));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(30), dp(24), dp(30));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));

        TextView badge = text(getString(R.string.main_northlight), 12, gold());
        badge.setLetterSpacing(.16f);
        root.addView(badge);
        TextView title = text(getString(R.string.about_built_for_the_quiet_moments), 31, moon());
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setPadding(0, dp(14), 0, dp(22));
        root.addView(title);
        section(root, getString(R.string.about_private_by_design), getString(R.string.about_northlight_s_compass_and_light_tools_work_on_your_phone));
        section(root, getString(R.string.about_camera_permission), getString(R.string.about_northlight_asks_for_camera_access_only_when_you_choose));
        section(root, getString(R.string.about_compass_accuracy), getString(R.string.about_the_compass_uses_your_phone_s_orientation_sensors_and_s));
        section(root, getString(R.string.altimeter_about_title), getString(R.string.altimeter_help_body));
        section(root, getString(R.string.speech_speak_direction), getString(R.string.speech_limits_body));
        Button privacy = new Button(this);
        privacy.setText(getString(R.string.about_read_privacy_policy)); privacy.setTextSize(16); privacy.setAllCaps(false); privacy.setTextColor(moon());
        privacy.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL))));
        root.addView(privacy, new LinearLayout.LayoutParams(-1, -2));
        TextView version = text(getString(R.string.app_version, BuildConfig.VERSION_NAME), 14, mist());
        version.setGravity(Gravity.CENTER);
        version.setPadding(0, dp(16), 0, dp(14));
        root.addView(version, new LinearLayout.LayoutParams(-1, -2));
        Button close = new Button(this);
        close.setText(getString(R.string.close)); close.setTextSize(16); close.setAllCaps(false); close.setTextColor(Color.rgb(11, 19, 32));
        close.setBackgroundResource(R.drawable.button_gold); close.setMinHeight(dp(54)); close.setOnClickListener(v -> finish());
        root.addView(close, new LinearLayout.LayoutParams(-1, -2));
        return Ui.screen(this, scroll);
    }

    private void section(LinearLayout root, String heading, String body) {
        TextView title = text(heading, 19, moon()); title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD); root.addView(title);
        TextView copy = text(body, 16, mist()); copy.setLineSpacing(dp(3), 1f); copy.setPadding(0, dp(7), 0, dp(23)); root.addView(copy);
    }
    private TextView text(String value, float size, int color) { TextView view = new TextView(this); view.setText(value); view.setTextSize(size); view.setTextColor(color); return view; }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private int moon() { return Color.rgb(238, 243, 247); }
    private int mist() { return Color.rgb(174, 187, 203); }
    private int gold() { return Color.rgb(240, 182, 91); }
}
