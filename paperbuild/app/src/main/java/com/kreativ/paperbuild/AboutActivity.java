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
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(Ui.SURFACE);
        LinearLayout root = Ui.column(this); Ui.applyInsets(scroll, root); scroll.addView(root, new ScrollView.LayoutParams(-1, -1));
        root.addView(Ui.brand(this)); Ui.gap(root, 22);
        root.addView(Ui.hero(this, "PRIVACY & ABOUT", "Your pages.\nYour business.", "Built to keep documents local.")); Ui.gap(root, 20);
        root.addView(Ui.feature(this, R.drawable.ic_shield, "Private by design", "Your pages and PDFs are processed on your phone. They aren’t uploaded.")); Ui.gap(root, 10);
        root.addView(Ui.feature(this, R.drawable.ic_images, "You choose access", "Use Android’s picker or your camera app. No broad photo access.")); Ui.gap(root, 10);
        root.addView(Ui.feature(this, R.drawable.ic_stack, "Your draft stays ready", "Private page copies keep your draft safe. New document clears them.")); Ui.gap(root, 10);
        root.addView(Ui.feature(this, R.drawable.ic_extract, "You control sharing", "Open, share or save when you choose. Your originals stay untouched.")); Ui.gap(root, 20);
        LinearLayout links = Ui.card(this, R.drawable.card);
        links.addView(Ui.text(this, "A little peace of mind.", 20, Ui.INK, true)); Ui.gap(links, 8);
        links.addView(Ui.text(this, "No account. No ads. Google Play handles optional Pro purchases.", 14, Ui.MUTED, false)); Ui.gap(links, 16);
        Button privacy = Ui.button(this, "Read Privacy Policy", false);
        privacy.setOnClickListener(v -> openExternal(new Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL)))); links.addView(privacy, matchWrap());
        Ui.gap(links, 10); Button support = Ui.button(this, "Contact support", false);
        support.setOnClickListener(v -> {
            Intent email = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + SUPPORT_EMAIL));
            email.putExtra(Intent.EXTRA_SUBJECT, "PaperBuild support"); openExternal(Intent.createChooser(email, "Contact PaperBuild support"));
        }); links.addView(support, matchWrap()); root.addView(links); Ui.gap(root, 18);
        Button close = Ui.button(this, "Back to PaperBuild", true); close.setOnClickListener(v -> finish()); root.addView(close, matchWrap());
        TextView version = Ui.text(this, "PaperBuild " + BuildConfig.VERSION_NAME + " · Made by Kreativ", 11, Ui.MUTED, false);
        version.setGravity(Gravity.CENTER); version.setPadding(0, dp(20), 0, 0); root.addView(version); return scroll;
    }

    private void openExternal(Intent intent) {
        try { startActivity(intent); }
        catch (android.content.ActivityNotFoundException error) {
            android.widget.Toast.makeText(this, "No app is available to open this link.", android.widget.Toast.LENGTH_LONG).show();
        }
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
