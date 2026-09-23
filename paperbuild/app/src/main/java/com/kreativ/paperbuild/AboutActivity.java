package com.kreativ.paperbuild;

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

/** Plain-language privacy and support information for PaperBuild. */
public class AboutActivity extends Activity {
    private static final String PRIVACY_POLICY_URL = "https://github.com/aolaru/kreativ-mobile-apps/blob/main/paperbuild/PRIVACY_POLICY.md";
    private static final String SUPPORT_EMAIL = "info@madebykreativ.com";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildScreen());
    }

    private View buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(247, 248, 252));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(30), dp(24), dp(132));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -1));

        TextView badge = label("PAPERBUILD", 12, Color.rgb(49, 94, 170));
        badge.setLetterSpacing(.12f);
        root.addView(badge);

        TextView title = label("Privacy & About", 30, Color.rgb(30, 41, 53));
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setPadding(0, dp(12), 0, dp(20));
        root.addView(title);

        addSection(root, "Your pages stay private.", "PaperBuild creates PDFs on your phone. It does not upload your pages or PDFs.");
        addSection(root, "You choose access.", "You select images with Android's picker. Scanning uses your camera app. No broad photo or camera access is requested.");
        addSection(root, "You control exports.", "PDFs stay temporary until you share, open, or save them. Originals are never changed.");

        Button privacyButton = button("Read Privacy Policy", false);
        privacyButton.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL))));
        root.addView(privacyButton, matchWrap());
        addSpacer(root, 10);
        Button supportButton = button("Contact support", false);
        supportButton.setOnClickListener(v -> {
            Intent email = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + SUPPORT_EMAIL));
            email.putExtra(Intent.EXTRA_SUBJECT, "PaperBuild support");
            startActivity(Intent.createChooser(email, "Contact PaperBuild support"));
        });
        root.addView(supportButton, matchWrap());

        TextView version = label("Version " + BuildConfig.VERSION_NAME, 14, Color.rgb(91, 104, 120));
        version.setGravity(Gravity.CENTER);
        version.setPadding(0, dp(24), 0, dp(12));
        root.addView(version, matchWrap());
        Button close = button("Close", true);
        close.setOnClickListener(v -> finish());
        root.addView(close, matchWrap());
        return scroll;
    }

    private void addSection(LinearLayout root, String heading, String copy) {
        TextView title = label(heading, 19, Color.rgb(30, 41, 53));
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        root.addView(title);
        TextView body = label(copy, 16, Color.rgb(91, 104, 120));
        body.setLineSpacing(dp(3), 1f);
        body.setPadding(0, dp(8), 0, dp(22));
        root.addView(body);
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
        button.setTextColor(primary ? Color.WHITE : Color.rgb(30, 41, 53));
        button.setBackgroundResource(primary ? R.drawable.button_primary : R.drawable.button_secondary);
        return button;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(-1, -2);
    }

    private void addSpacer(LinearLayout layout, int heightDp) {
        View spacer = new View(this);
        layout.addView(spacer, new LinearLayout.LayoutParams(1, dp(heightDp)));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
