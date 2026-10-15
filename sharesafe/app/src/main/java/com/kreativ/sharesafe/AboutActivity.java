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
        LinearLayout root = Ui.pageContent(this); Ui.addPage(scroll, root);
        root.addView(Ui.badge(this, "SHARESAFE"), new LinearLayout.LayoutParams(-2, -2));
        addSpacer(root, 14); root.addView(Ui.heading(this, "Privacy & About", 28));
        addSpacer(root, 8); root.addView(label("Small tools. More control over what you share.", 15, Ui.MUTED));
        addSpacer(root, 24);

        LinearLayout privateCard = Ui.card(this, Ui.MINT);
        privateCard.addView(Ui.heading(this, "Private by design", 21)); addSpacer(privateCard, 10);
        privateCard.addView(label("Photos and face detection results stay on your device; they are not uploaded by ShareSafe. Privacy Check reads EXIF fields locally. When you choose Share, the receiving app handles those copies under its own privacy policy.", 15, Ui.INK));
        root.addView(privateCard, matchWrap()); addSpacer(root, 16);

        LinearLayout originalsCard = Ui.card(this, Color.WHITE);
        originalsCard.addView(Ui.heading(this, "Your originals stay yours", 19)); addSpacer(originalsCard, 10);
        originalsCard.addView(label("Original photos are never changed. Gallery copies are saved to Pictures/ShareSafe only when you choose Save to gallery. Temporary copies are private; copies older than 24 hours are removed when a new session starts, or may be cleared earlier by Android.", 15, Ui.MUTED));
        root.addView(originalsCard, matchWrap()); addSpacer(root, 16);

        LinearLayout exportCard = Ui.card(this, Color.WHITE);
        exportCard.addView(Ui.heading(this, "Know what you're exporting", 19)); addSpacer(exportCard, 10);
        exportCard.addView(label("Exports are fresh JPEGs with neutral filenames. Large images may be resized, transparency becomes white and animated images export one frame. Metadata removal does not hide visible details. Cover is recommended for sensitive text, IDs and faces; blur and pixelation are not guaranteed anonymisation.", 15, Ui.MUTED));
        root.addView(exportCard, matchWrap()); addSpacer(root, 16);

        LinearLayout checkCard = Ui.card(this, Color.WHITE);
        checkCard.addView(Ui.heading(this, "Privacy Check is free", 19)); addSpacer(checkCard, 10);
        checkCard.addView(label("Checks show the presence of selected EXIF location, date/time and camera/device fields, without displaying or retaining their values. Unreadable or unsupported images are marked unknown. " + PrivacyCheck.LIMITS, 15, Ui.MUTED));
        root.addView(checkCard, matchWrap()); addSpacer(root, 16);
        if (BuildConfig.PRO_AVAILABLE) {
            LinearLayout faceCard = Ui.card(this, Color.WHITE);
            faceCard.addView(Ui.heading(this, "Face Assist stays on-device", 19)); addSpacer(faceCard, 10);
            faceCard.addView(label("The face detection model is bundled in the Play edition for Pro. Google ML Kit processes photos on-device and does not send images or detection results to Google. Its SDK collects device/app information, per-installation identifiers, performance/error and usage/configuration metrics (such as image format and resolution), and may contact Google for maintenance information. Face suggestions can miss people; review them and apply solid covers yourself. Detection identifies possible faces, not who a person is.", 15, Ui.MUTED));
            root.addView(faceCard, matchWrap()); addSpacer(root, 16);
        }

        LinearLayout purchaseCard = Ui.card(this, Color.WHITE);
        purchaseCard.addView(Ui.heading(this, BuildConfig.PRO_AVAILABLE ? "Optional Pro, no subscription" : "No in-app purchases", 19)); addSpacer(purchaseCard, 10);
        purchaseCard.addView(label(BuildConfig.PRO_AVAILABLE
                ? "Metadata cleaning stays free. Google Play handles Pro payments and purchase restoration using its network services. ShareSafe stores a verified purchase receipt privately on your phone; it does not receive your card details."
                : "This store edition has no in-app purchases.", 15, Ui.MUTED));
        root.addView(purchaseCard, matchWrap()); addSpacer(root, 20);

        Button privacyButton = button("Read Privacy Policy", false);
        privacyButton.setOnClickListener(v -> openPrivacyPolicy());
        root.addView(privacyButton, matchWrap());

        addSpacer(root, 10);
        Button supportButton = button("Contact support", false);
        supportButton.setOnClickListener(v -> contactSupport());
        root.addView(supportButton, matchWrap());

        TextView version = label("Made by Kreativ · Version " + BuildConfig.VERSION_NAME, 13, Ui.MUTED);
        version.setGravity(Gravity.CENTER);
        version.setPadding(0, dp(24), 0, dp(12));
        root.addView(version, matchWrap());

        if (BuildConfig.PRO_AVAILABLE) {
            Button reviewer = Ui.textButton(this, ReviewerAccess.enabled(this) ? "Disable reviewer access" : "Reviewer access");
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
        dialog.show(); Ui.styleDialog(dialog);
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
        return Ui.text(this, text, size, color);
    }

    private Button button(String text, boolean primary) {
        return Ui.button(this, text, primary);
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
