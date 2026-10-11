package com.kreativ.sharesafe;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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
        Ui.applyInsets(scroll);

        TextView badge = label("SHARESAFE", 12, Color.rgb(52, 116, 99));
        badge.setLetterSpacing(.12f);
        root.addView(badge);

        TextView title = label("Privacy & About", 30, Color.rgb(25, 44, 41));
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setPadding(0, dp(12), 0, dp(20));
        root.addView(title);

        TextView privacyTitle = label("Photos are processed on your device.", 20, Color.rgb(25, 44, 41));
        privacyTitle.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        root.addView(privacyTitle);

        TextView privacyCopy = label("ShareSafe never uploads your photos or metadata. If you choose Share, the receiving app handles those copies under its own privacy policy. "
                + (BuildConfig.PRO_AVAILABLE ? "Google Play handles Pro payments and purchase restoration using its network services. ShareSafe stores a verified purchase receipt privately on your phone; it does not receive your card details." : "This store edition has no in-app purchases."), 16, Color.rgb(96, 113, 109));
        privacyCopy.setLineSpacing(dp(3), 1f);
        privacyCopy.setPadding(0, dp(8), 0, dp(22));
        root.addView(privacyCopy);

        TextView storageTitle = label("Your control", 18, Color.rgb(25, 44, 41));
        storageTitle.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        root.addView(storageTitle);
        TextView storageCopy = label("Original photos are never changed. Gallery copies are saved to Pictures/ShareSafe only when you choose Save to gallery. Temporary copies are private; copies older than 24 hours are removed when a new session starts, or may be cleared earlier by Android. Exports are JPEGs with neutral filenames. Large images may be resized, transparency becomes white, and animated images export one frame. Cover is recommended for sensitive visible details; blur and pixelation are not guaranteed anonymisation.", 15, Color.rgb(96, 113, 109));
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

        if (BuildConfig.PRO_AVAILABLE) {
            Button reviewer = button(ReviewerAccess.enabled(this) ? "Disable reviewer access" : "Reviewer access", false);
            reviewer.setOnClickListener(v -> {
                if (ReviewerAccess.enabled(this)) {
                    ReviewerAccess.disable(this); reviewer.setText("Reviewer access");
                    android.widget.Toast.makeText(this, "Reviewer access disabled. Paid purchases are unchanged.", android.widget.Toast.LENGTH_LONG).show();
                } else showReviewerAccess(reviewer);
            });
            root.addView(reviewer, matchWrap());
            addSpacer(root, 10);
        }

        Button closeButton = button("Close", true);
        closeButton.setOnClickListener(v -> finish());
        root.addView(closeButton, matchWrap());
        return scroll;
    }

    private void showReviewerAccess(Button reviewer) {
        EditText code = new EditText(this);
        code.setSingleLine(true);
        code.setHint("Reviewer code");
        code.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        code.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Reviewer access")
                .setMessage("For authorised app reviewers only. Enter the code supplied in Play Console to use all Pro editing tools without payment. This does not create a Google Play purchase.")
                .setView(code).setNegativeButton("Cancel", null).setPositiveButton("Enable access", null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (!ReviewerAccess.activate(this, code.getText().toString())) {
                code.setError("Invalid reviewer code or access could not be saved."); return;
            }
            reviewer.setText("Disable reviewer access"); dialog.dismiss();
            android.widget.Toast.makeText(this, "Reviewer access enabled. All Pro editing tools are available.", android.widget.Toast.LENGTH_LONG).show();
        }));
        dialog.show();
    }

    private void openPrivacyPolicy() {
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL))); }
        catch (android.content.ActivityNotFoundException e) {
            android.widget.Toast.makeText(this, "No browser is installed. Privacy policy: " + PRIVACY_POLICY_URL, android.widget.Toast.LENGTH_LONG).show();
        }
    }

    private void contactSupport() {
        Intent email = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + SUPPORT_EMAIL));
        email.putExtra(Intent.EXTRA_SUBJECT, "ShareSafe support");
        try { startActivity(Intent.createChooser(email, "Contact ShareSafe support")); }
        catch (android.content.ActivityNotFoundException e) {
            android.widget.Toast.makeText(this, "Please email " + SUPPORT_EMAIL, android.widget.Toast.LENGTH_LONG).show();
        }
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
