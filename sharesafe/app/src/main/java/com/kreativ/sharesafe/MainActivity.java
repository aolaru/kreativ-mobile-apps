package com.kreativ.sharesafe;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/** Photos are processed locally. Play Billing and Face Assist's SDK may use network services. */
public class MainActivity extends Activity {
    private static final String KREATIV_APPS_URL = "https://madebykreativ.com/apps/";
    private static final int PICK_PHOTOS = 44, WRITE_GALLERY_PERMISSION = 45, REDACT_PHOTO = 46;
    private ExportSession session;
    private BillingManager billing;
    private TextView selectionText, statusText, resultSummary, proHint;
    private Button pickButton, cleanButton, redactButton, shareButton, saveButton, optionsToggle;
    private LinearLayout optionsBody, resultCard;
    private LinearLayout privacyCard;
    private TextView privacySummary;
    private Button privacyToggle;
    private LinearLayout privacyDetails;
    private boolean privacyExpanded;
    private boolean optionsExpanded;
    private ProgressBar progress;
    private RadioGroup qualityGroup, formatGroup;
    private AlertDialog upgradeDialog;
    private final ExportSession.Listener sessionListener = this::renderState;

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        session = (ExportSession) getLastNonConfigurationInstance();
        if (session == null) session = new ExportSession(this, saved);
        optionsExpanded = saved != null && saved.getBoolean("export_options_expanded", false);
        privacyExpanded = saved != null && saved.getBoolean("privacy_expanded", false);
        setContentView(buildScreen());
        session.listener = sessionListener;
        billing = BillingManager.create(this, new BillingManager.Listener() {
            public void onProStatusChanged(boolean enabled) {
                if (isFinishing() || isDestroyed()) return;
                renderState();
                if (enabled && upgradeDialog != null) {
                    upgradeDialog.dismiss();
                    Toast.makeText(MainActivity.this, "ShareSafe Pro unlocked. Advanced edit is ready.", Toast.LENGTH_LONG).show();
                }
            }
            public void onBillingMessage(String message) {
                if (!isFinishing() && !isDestroyed()) Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
            }
            public void onProductChanged() { updateUpgradeDialog(); }
        });
        billing.connect();
        renderState();
    }

    @Override protected void onResume() { super.onResume(); if (billing != null) billing.refresh(); }
    @Override public Object onRetainNonConfigurationInstance() {
        session.quality = chosenQuality(); session.zip = isZipSelected(); return session;
    }
    @Override protected void onSaveInstanceState(Bundle out) {
        session.quality = chosenQuality(); session.zip = isZipSelected(); session.save(out);
        out.putBoolean("export_options_expanded", optionsExpanded);
        out.putBoolean("privacy_expanded", privacyExpanded);
        super.onSaveInstanceState(out);
    }
    @Override protected void onDestroy() {
        if (session.listener == sessionListener) session.listener = null;
        if (!isChangingConfigurations()) session.close();
        if (billing != null) billing.destroy();
        if (upgradeDialog != null) upgradeDialog.dismiss();
        super.onDestroy();
    }

    private View buildScreen() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = Ui.pageContent(this); Ui.addPage(scroll, root);
        LinearLayout brand = new LinearLayout(this); brand.setGravity(Gravity.CENTER_VERTICAL);
        brand.addView(Ui.icon(this, R.drawable.ic_sharesafe, 34));
        TextView name = Ui.heading(this, "ShareSafe", 23); name.setPadding(dp(10), 0, 0, 0);
        brand.addView(name, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(brand); addSpacer(root, 22);
        root.addView(Ui.heading(this, "A little more privacy.\nEvery time you share.", 28)); addSpacer(root, 8);
        root.addView(label("Remove hidden location and camera details. Keep the moment, not the metadata.", 15, Ui.MUTED));
        addSpacer(root, 14); root.addView(Ui.badge(this, "On-device photo processing"), new LinearLayout.LayoutParams(-2, -2));
        addSpacer(root, 24);

        LinearLayout selection = Ui.card(this, Color.WHITE);
        LinearLayout selectionHeader = new LinearLayout(this); selectionHeader.setGravity(Gravity.CENTER_VERTICAL);
        selectionHeader.addView(Ui.icon(this, R.drawable.ic_photo_add, 56));
        LinearLayout selectionLabels = new LinearLayout(this); selectionLabels.setOrientation(LinearLayout.VERTICAL);
        selectionLabels.setPadding(dp(14), 0, 0, 0);
        selectionLabels.addView(Ui.heading(this, "Start with your photos", 18));
        selectionText = label("No photos selected", 14, Ui.MUTED); selectionText.setPadding(0, dp(5), 0, 0);
        selectionLabels.addView(selectionText); selectionHeader.addView(selectionLabels, new LinearLayout.LayoutParams(0, -2, 1));
        selection.addView(selectionHeader); addSpacer(selection, 18);
        pickButton = button("Choose photos", true);
        pickButton.setOnClickListener(v -> choosePhotos()); selection.addView(pickButton, matchWrap());
        addSpacer(selection, 12); TextView originals = label("Your originals stay untouched.", 13, Ui.MUTED);
        originals.setGravity(Gravity.CENTER); selection.addView(originals); root.addView(selection, matchWrap());
        addSpacer(root, 14);
        privacyCard = Ui.card(this, Color.WHITE);
        privacyToggle = Ui.textButton(this, "Privacy Check · Free"); privacyToggle.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        privacyToggle.setOnClickListener(v -> { privacyExpanded = !privacyExpanded; renderState(); }); privacyCard.addView(privacyToggle, matchWrap());
        privacyDetails = new LinearLayout(this); privacyDetails.setOrientation(LinearLayout.VERTICAL);
        privacySummary = label("", 14, Ui.INK); privacySummary.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        privacyDetails.addView(privacySummary); addSpacer(privacyDetails, 10);
        privacyDetails.addView(label(PrivacyCheck.LIMITS, 12, Ui.MUTED)); privacyCard.addView(privacyDetails, matchWrap());
        cleanButton = button("Clean photos", true);
        cleanButton.setOnClickListener(v -> session.clean(chosenQuality(), isZipSelected())); root.addView(cleanButton, matchWrap());
        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setProgressTintList(android.content.res.ColorStateList.valueOf(Ui.ACCENT));
        progress.setMax(100); root.addView(progress, matchWrap());
        statusText = label("", 14, Ui.MUTED);
        statusText.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        statusText.setPadding(0, dp(12), 0, dp(8)); root.addView(statusText);
        addSpacer(root, 14);

        LinearLayout.LayoutParams privacyParams = matchWrap(); privacyParams.bottomMargin = dp(16); root.addView(privacyCard, privacyParams);

        resultCard = Ui.card(this, Color.WHITE);
        resultCard.addView(Ui.heading(this, "Ready to share", 21)); addSpacer(resultCard, 12);
        resultSummary = label("", 14, Ui.INK);
        resultSummary.setPadding(dp(16), dp(14), dp(16), dp(14)); resultSummary.setBackgroundResource(R.drawable.result_card);
        resultCard.addView(resultSummary, matchWrap());
        addSpacer(resultCard, 14); shareButton = button("Share cleaned photos", true);
        shareButton.setOnClickListener(v -> shareLatestExport()); resultCard.addView(shareButton, matchWrap());
        addSpacer(resultCard, 10); saveButton = button("Save to gallery", false);
        saveButton.setOnClickListener(v -> saveToGallery()); resultCard.addView(saveButton, matchWrap());
        LinearLayout.LayoutParams resultParams = matchWrap(); resultParams.bottomMargin = dp(16); root.addView(resultCard, resultParams);

        LinearLayout optionsCard = Ui.card(this, Color.WHITE);
        optionsCard.setPadding(dp(14), dp(6), dp(14), dp(6));
        optionsToggle = Ui.textButton(this, "Export options"); optionsToggle.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        optionsToggle.setOnClickListener(v -> { optionsExpanded = !optionsExpanded; renderOptions(); });
        optionsCard.addView(optionsToggle, matchWrap());
        optionsBody = new LinearLayout(this); optionsBody.setOrientation(LinearLayout.VERTICAL);
        optionsBody.setPadding(dp(6), dp(12), dp(6), dp(14));
        optionsBody.addView(Ui.heading(this, "JPEG quality", 16)); addSpacer(optionsBody, 10);
        qualityGroup = new RadioGroup(this);
        qualityGroup.addView(radio("Balanced — smaller files", 76, session.quality == 76));
        qualityGroup.addView(radio("High — sharper photos", 88, session.quality == 88));
        qualityGroup.addView(radio("Maximum — larger files", 95, session.quality == 95)); optionsBody.addView(qualityGroup);
        addSpacer(optionsBody, 18); optionsBody.addView(Ui.heading(this, "Share format", 16)); addSpacer(optionsBody, 10);
        formatGroup = new RadioGroup(this);
        formatGroup.addView(radio("Individual photos — recommended", 0, !session.zip));
        formatGroup.addView(radio("One ZIP file", 1, session.zip)); optionsBody.addView(formatGroup);
        addSpacer(optionsBody, 12); optionsBody.addView(label("Fresh JPEG copies, up to 4096 pixels per side / 8 megapixels. Transparency becomes white; animations export one frame. ZIP is useful for email or archives.", 13, Ui.MUTED));
        optionsCard.addView(optionsBody); root.addView(optionsCard, matchWrap());
        qualityGroup.setOnCheckedChangeListener((group, checked) -> renderOptions());
        formatGroup.setOnCheckedChangeListener((group, checked) -> renderOptions()); renderOptions();

        LinearLayout proCard = Ui.card(this, Ui.MINT);
        LinearLayout.LayoutParams proParams = matchWrap(); proParams.topMargin = dp(16);
        proCard.setVisibility(BuildConfig.PRO_AVAILABLE ? View.VISIBLE : View.GONE);
        proCard.addView(Ui.badge(this, "SHARESAFE PRO"), new LinearLayout.LayoutParams(-2, -2)); addSpacer(proCard, 10);
        proCard.addView(Ui.heading(this, "Keep private details private.", 20));
        proHint = label("Select one photo for Face Assist and precise redaction tools.", 14, Ui.MUTED);
        proHint.setPadding(0, dp(8), 0, dp(16)); proCard.addView(proHint);
        redactButton = button("Advanced edit • Pro", false);
        redactButton.setOnClickListener(v -> redactSelectedPhoto()); proCard.addView(redactButton, matchWrap()); root.addView(proCard, proParams);
        addSpacer(root, 16);
        Button about = Ui.textButton(this, "Privacy & About");
        about.setOnClickListener(v -> startActivity(new Intent(this, AboutActivity.class))); root.addView(about, matchWrap());
        if (BuildConfig.PRO_AVAILABLE) {
            Button restore = Ui.textButton(this, "Restore Pro purchase");
            restore.setOnClickListener(v -> billing.restore()); root.addView(restore, matchWrap());
        }
        if (BuildConfig.DEBUG && BuildConfig.PRO_AVAILABLE) {
            addSpacer(root, 10); Button demo = button("Try editor with demo image • Test build", false);
            demo.setOnClickListener(v -> { if (!session.busy) startActivityForResult(new Intent().setClassName(this, "com.kreativ.sharesafe.DemoActivity"), REDACT_PHOTO); });
            root.addView(demo, matchWrap());
        }
        addSpacer(root, 20);
        root.addView(label("Metadata cleaning is always free. It does not hide details visible in a photo. Review your copies before sharing.", 13, Ui.MUTED));
        LinearLayout footer = new LinearLayout(this);
        footer.setId(R.id.main_footer);
        footer.setOrientation(LinearLayout.VERTICAL);
        footer.setGravity(Gravity.CENTER_HORIZONTAL);
        TextView credit = label(getString(R.string.main_version_credit, BuildConfig.VERSION_NAME), 12, Ui.MUTED);
        credit.setId(R.id.main_version_credit);
        credit.setGravity(Gravity.CENTER);
        credit.setPadding(0, dp(6), 0, 0);
        footer.addView(credit, matchWrap());
        Button creatorLink = Ui.textButton(this, getString(R.string.main_creator_credit));
        creatorLink.setId(R.id.main_creator_link);
        creatorLink.setTextSize(12);
        creatorLink.setTextColor(Ui.ACCENT);
        creatorLink.setContentDescription(getString(R.string.main_creator_link_description));
        creatorLink.setOnClickListener(v -> {
            try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(KREATIV_APPS_URL)).addCategory(Intent.CATEGORY_BROWSABLE)); }
            catch (android.content.ActivityNotFoundException e) {
                Toast.makeText(this, getString(R.string.main_no_browser, KREATIV_APPS_URL), Toast.LENGTH_LONG).show();
            }
        });
        footer.addView(creatorLink, new LinearLayout.LayoutParams(-2, -2));
        root.addView(footer, matchWrap());
        return scroll;
    }

    private void renderOptions() {
        if (optionsToggle == null || formatGroup == null) return;
        String quality = chosenQuality() == 95 ? "Maximum" : chosenQuality() == 88 ? "High" : "Balanced";
        optionsToggle.setText("Export options · " + (optionsExpanded ? "Hide" : "Change") + "\n" + quality + " · " + (isZipSelected() ? "One ZIP file" : "Individual photos"));
        androidx.core.view.ViewCompat.setStateDescription(optionsToggle, optionsExpanded ? "Expanded" : "Collapsed");
        optionsBody.setVisibility(optionsExpanded ? View.VISIBLE : View.GONE);
    }

    private void renderState() {
        boolean idle = !session.busy;
        pickButton.setEnabled(idle);
        Ui.styleButton(pickButton, session.selected.isEmpty());
        cleanButton.setEnabled(idle && !session.selected.isEmpty());
        redactButton.setEnabled(idle && session.selected.size() == 1);
        for (int i = 0; i < qualityGroup.getChildCount(); i++) qualityGroup.getChildAt(i).setEnabled(idle);
        for (int i = 0; i < formatGroup.getChildCount(); i++) formatGroup.getChildAt(i).setEnabled(idle);
        optionsToggle.setEnabled(idle);
        selectionText.setText(session.selected.isEmpty() ? "No photos selected" : session.selected.size() + " photo(s) selected");
        statusText.setText(session.status);
        statusText.setVisibility(session.status.isEmpty() ? View.GONE : View.VISIBLE);
        proHint.setText(session.selected.size() == 1 ? "Find faces, adjust individual areas and cover visible details." : "Select one photo for Face Assist and precise redaction tools.");
        privacyCard.setVisibility(session.selected.isEmpty() ? View.GONE : View.VISIBLE);
        privacyToggle.setText("Privacy Check · Free\n" + (session.checkingPrivacy ? "Checking readable metadata…"
                : privacyExpanded ? "Hide report" : session.result == null ? "View original metadata report" : "View before / after report"));
        androidx.core.view.ViewCompat.setStateDescription(privacyToggle, privacyExpanded ? "Expanded" : "Collapsed");
        privacyDetails.setVisibility(privacyExpanded ? View.VISIBLE : View.GONE);
        String check = session.checkingPrivacy ? "Checking readable metadata on your device…"
                : session.privacyBefore == null ? "Original check unavailable. Select the photos again to retry."
                : session.privacyBefore.summary(false);
        if (session.result != null) check += "\n\n" + (session.result.privacyAfter == null
                ? "Exported copies: not checked in this session. Clean again to verify."
                : session.result.privacyAfter.summary(true));
        privacySummary.setText(check);
        progress.setVisibility(session.busy ? View.VISIBLE : View.GONE); progress.setProgress(session.progress);
        boolean ready = session.result != null;
        resultCard.setVisibility(ready ? View.VISIBLE : View.GONE);
        resultSummary.setVisibility(ready ? View.VISIBLE : View.GONE);
        if (ready) resultSummary.setText(session.result.summary());
        shareButton.setVisibility(ready ? View.VISIBLE : View.GONE); shareButton.setEnabled(ready && idle);
        shareButton.setText(ready && session.result.zipFile != null ? "Share ZIP" : "Share cleaned photos");
        saveButton.setVisibility(ready ? View.VISIBLE : View.GONE); saveButton.setEnabled(ready && idle);
    }

    private void choosePhotos() {
        if (session.busy) return;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(Intent.createChooser(intent, "Choose photos to clean"), PICK_PHOTOS);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (session.busy || resultCode != RESULT_OK || data == null) return;
        if (requestCode == REDACT_PHOTO) {
            String path = data.getStringExtra(RedactActivity.EXTRA_REDACTED_PATH);
            if (path == null || !new File(path).isFile()) return;
            session.select(Collections.singletonList(Uri.fromFile(new File(path))));
            session.status = "Redacted copy ready. Tap Clean photos to prepare it for sharing.";
            renderState(); return;
        }
        if (requestCode != PICK_PHOTOS) return;
        Set<Uri> unique = new LinkedHashSet<>();
        if (data.getData() != null) unique.add(data.getData());
        ClipData clips = data.getClipData();
        if (clips != null) for (int i = 0; i < clips.getItemCount(); i++) unique.add(clips.getItemAt(i).getUri());
        for (Uri uri : unique) {
            try { getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); }
            catch (SecurityException ignored) { /* Some providers grant only temporary access. */ }
        }
        session.select(new ArrayList<>(unique));
    }

    private void redactSelectedPhoto() {
        if (session.busy || session.selected.size() != 1 || !BuildConfig.PRO_AVAILABLE) return;
        if (!billing.isProEnabled() && !ReviewerAccess.enabled(this)) { showProUpgrade(); return; }
        startActivityForResult(new Intent(this, RedactActivity.class).setData(session.selected.get(0)), REDACT_PHOTO);
    }

    private void showProUpgrade() {
        upgradeDialog = new AlertDialog.Builder(this).setTitle("Unlock ShareSafe Pro")
                .setMessage(upgradeMessage()).setNegativeButton("Not now", null)
                .setNeutralButton("Restore purchase", (dialog, which) -> billing.restore())
                .setPositiveButton("Unlock Pro", (dialog, which) -> billing.launchPurchase(this)).create();
        upgradeDialog.show(); Ui.styleDialog(upgradeDialog); updateUpgradeDialog(); billing.refresh();
    }

    private String upgradeMessage() {
        String price = billing.price();
        return "Privacy tools for one photo at a time:\n\n• Face Assist: find faces on-device, review suggestions and apply covers\n• Move, resize or delete individual redactions\n• Undo, redo, zoom and flattened JPEG copies\n• Solid Cover plus optional blur and pixelation\n\nDetection can miss faces. Review every photo. Blur and pixelation may still reveal information; use Cover for stronger privacy.\n\n"
                + (price == null ? billing.purchaseStatus()
                    : "One-time purchase: " + price + ". No subscription.");
    }

    private void updateUpgradeDialog() {
        if (isFinishing() || isDestroyed() || upgradeDialog == null || !upgradeDialog.isShowing() || billing == null) return;
        upgradeDialog.setMessage(upgradeMessage());
        upgradeDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(billing.canPurchase());
    }

    private void shareLatestExport() {
        if (session.busy || session.result == null) return;
        ArrayList<Uri> uris = new ArrayList<>();
        boolean zip = session.result.zipFile != null;
        ArrayList<File> files = zip ? new ArrayList<>(Collections.singletonList(session.result.zipFile)) : session.result.photos;
        for (File file : files) if (file.isFile()) uris.add(FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file));
        if (uris.isEmpty()) { session.status = "Temporary copies are no longer available. Please clean the photos again."; renderState(); return; }
        Intent send = new Intent(uris.size() == 1 ? Intent.ACTION_SEND : Intent.ACTION_SEND_MULTIPLE).setType(zip ? "application/zip" : "image/jpeg");
        if (uris.size() == 1) send.putExtra(Intent.EXTRA_STREAM, uris.get(0)); else send.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        ClipData clips = ClipData.newUri(getContentResolver(), "ShareSafe clean copies", uris.get(0));
        for (int i = 1; i < uris.size(); i++) clips.addItem(new ClipData.Item(uris.get(i)));
        send.setClipData(clips); send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(send, zip ? "Share cleaned photo ZIP" : "Share cleaned photos"));
    }

    private void saveToGallery() {
        if (session.busy || session.result == null) return;
        if (Build.VERSION.SDK_INT <= 28 && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_GALLERY_PERMISSION); return;
        }
        session.saveToGallery();
    }
    @Override public void onRequestPermissionsResult(int code, String[] permissions, int[] grants) {
        super.onRequestPermissionsResult(code, permissions, grants);
        if (code == WRITE_GALLERY_PERMISSION) {
            if (grants.length > 0 && grants[0] == PackageManager.PERMISSION_GRANTED) saveToGallery();
            else Toast.makeText(this, "Gallery permission is needed to save on this Android version.", Toast.LENGTH_LONG).show();
        }
    }
    private int chosenQuality() {
        View selected = qualityGroup.findViewById(qualityGroup.getCheckedRadioButtonId());
        return selected == null ? 76 : (Integer) selected.getTag();
    }
    private boolean isZipSelected() {
        View selected = formatGroup.findViewById(formatGroup.getCheckedRadioButtonId());
        return selected != null && (Integer) selected.getTag() == 1;
    }
    private RadioButton radio(String text, int tag, boolean checked) {
        RadioButton radio = new RadioButton(this); radio.setId(View.generateViewId()); radio.setText(text);
        radio.setTag(tag); radio.setChecked(checked); radio.setTextSize(14); radio.setTextColor(Ui.INK);
        radio.setMinHeight(dp(50)); radio.setButtonTintList(android.content.res.ColorStateList.valueOf(Ui.ACCENT));
        radio.setPadding(dp(12), dp(10), dp(12), dp(10)); radio.setBackgroundResource(R.drawable.option_background);
        RadioGroup.LayoutParams params = new RadioGroup.LayoutParams(-1, -2); params.bottomMargin = dp(8); radio.setLayoutParams(params);
        return radio;
    }
    private Button button(String text, boolean primary) {
        return Ui.button(this, text, primary);
    }
    private TextView label(String text, float size, int color) {
        return Ui.text(this, text, size, color);
    }
    private LinearLayout.LayoutParams matchWrap() { return new LinearLayout.LayoutParams(-1, -2); }
    private void addSpacer(LinearLayout root, int height) { root.addView(new View(this), new LinearLayout.LayoutParams(1, dp(height))); }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
