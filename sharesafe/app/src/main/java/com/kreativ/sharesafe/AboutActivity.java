package com.kreativ.sharesafe;

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

/** Plain-language privacy and support information for ShareSafe. */
public class AboutActivity extends Activity {
    private static final String PRIVACY_POLICY_URL = "https://madebykreativ.com/privacy/sharesafe/";
    private static final String SUPPORT_EMAIL = "info@madebykreativ.com";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildScreen());
    }

    private View buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(247, 248, 243));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(30), dp(24), dp(30));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -1));

        TextView badge = label("SHARESAFE", 12, Color.rgb(52, 116, 99));
        badge.setLetterSpacing(.12f);
        root.addView(badge);

        TextView title = label("Privacy & About", 30, Color.rgb(25, 44, 41));
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setPadding(0, dp(12), 0, dp(20));
        root.addView(title);

        TextView privacyTitle = label("Photos never leave your device.", 20, Color.rgb(25, 44, 41));
        privacyTitle.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        root.addView(privacyTitle);

        TextView privacyCopy = label("ShareSafe processes selected photos locally on your phone. It does not upload your photos, photo metadata, or location information to Kreativ or any other server.", 16, Color.rgb(96, 113, 109));
        privacyCopy.setLineSpacing(dp(3), 1f);
        privacyCopy.setPadding(0, dp(8), 0, dp(22));
        root.addView(privacyCopy);

        TextView storageTitle = label("Your control", 18, Color.rgb(25, 44, 41));
        storageTitle.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        root.addView(storageTitle);
        TextView storageCopy = label("Original photos are never changed. Clean copies are only saved to your gallery when you choose Save to gallery, and are placed in Pictures/ShareSafe.", 15, Color.rgb(96, 113, 109));
        storageCopy.setLineSpacing(dp(3), 1f);
        storageCopy.setPadding(0, dp(8), 0, dp(22));
        root.addView(storageCopy);

        Button privacyButton = button("Read Privacy Policy", false);
        privacyButton.setOnClickListener(v -> openPrivacyPolicy());
        root.addView(privacyButton, matchWrap());

        addSpacer(root, 10);
        Button supportButton = button("Contact support", false);
        supportButton.setOnClickListener(v -> contactSupport());
        root.addView(supportButton, matchWrap());

        TextView version = label("Version " + BuildConfig.VERSION_NAME, 14, Color.rgb(96, 113, 109));
        version.setGravity(Gravity.CENTER);
        version.setPadding(0, dp(24), 0, dp(12));
        root.addView(version, matchWrap());

        Button closeButton = button("Close", true);
        closeButton.setOnClickListener(v -> finish());
        root.addView(closeButton, matchWrap());
        return scroll;
    }

    private void openPrivacyPolicy() {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL)));
    }

    private void contactSupport() {
        Intent email = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + SUPPORT_EMAIL));
        email.putExtra(Intent.EXTRA_SUBJECT, "ShareSafe support");
        startActivity(Intent.createChooser(email, "Contact ShareSafe support"));
    }

    private TextView label(String text, float size, int color) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);
        return view;
    }

    private Button button(String text, boolean primary) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(16);
        button.setAllCaps(false);
        button.setMinHeight(dp(54));
        button.setTextColor(primary ? Color.WHITE : Color.rgb(25, 44, 41));
        button.setBackgroundResource(primary ? R.drawable.button_primary : R.drawable.button_secondary);
        return button;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(-1, -2);
    }

    private void addSpacer(LinearLayout root, int heightDp) {
        View spacer = new View(this);
        root.addView(spacer, new LinearLayout.LayoutParams(1, dp(heightDp)));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
