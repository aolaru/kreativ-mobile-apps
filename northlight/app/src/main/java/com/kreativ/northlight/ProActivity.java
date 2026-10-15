package com.kreativ.northlight;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Build;
import android.os.Vibrator;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.View;
import android.view.HapticFeedbackConstants;
import android.view.WindowManager;
import android.widget.*;
import org.json.JSONException;
import java.util.ArrayList;
import java.util.List;

/** Offline tools share the free compass's sensors and application-scoped billing. */
public class ProActivity extends Activity implements CompassSource.Listener, BillingManager.Listener, CompassSpeech.Listener {
    static final String EXTRA_BEARING_INDEX = "bearing_index";
    private static final String SAVED_BEARINGS = "saved_bearings";
    private CompassSource compass;
    private BillingManager billing;
    private CompassSpeech speech;
    private BearingGuidance spokenTarget;
    private TextView speechStatus, speechReading;
    private LinearLayout speechControls;
    private Button speakGuidance, startSpeech, pauseSpeech;
    private Switch alignmentHaptics;
    private boolean hapticsAvailable, resumed;
    private int compassMessage = R.string.compass_waiting_accuracy;
    private TextView heading, accuracy, entitlement, guidance;
    private TextView title, subtitle;
    private ScrollView scroll;
    private LinearLayout upgradeBenefits, freeOverview, ownerTools, purchaseTerms;
    private Button continueFree;
    private Boolean displayingOwned;
    private LinearLayout savedBearings;
    private Button unlock, restore, retry, saveBearing, clearTarget;
    private final List<Button> proControls = new ArrayList<>();
    private float currentHeading = Float.NaN;
    private int targetDegrees = -1;
    private String targetName;
    private int requestedBearing = -1;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        compass = new CompassSource(this, this);
        billing = BillingManager.get(this);
        speech = new CompassSpeech(this, this);
        try {
            Vibrator vibrator = getSystemService(Vibrator.class);
            hapticsAvailable = vibrator != null && vibrator.hasVibrator();
        } catch (RuntimeException unavailable) { hapticsAvailable = false; }
        if (state != null) { targetDegrees = state.getInt("target", -1); targetName = state.getString("targetName"); }
        else requestedBearing = getIntent().getIntExtra(EXTRA_BEARING_INDEX, -1);
        setContentView(buildScreen());
        onBillingChanged();
    }
    @Override protected void onResume() {
        super.onResume(); resumed = true; currentHeading = Float.NaN;
        speechReading.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_NONE); speechReading.setText("");
        speech.resume(); compass.start(); billing.addListener(this); updateBearingControls(); updateGuidance();
    }
    @Override protected void onPause() { resumed = false; speech.pause(); compass.stop(); billing.removeListener(this); super.onPause(); }
    @Override protected void onDestroy() { speech.close(); super.onDestroy(); }
    @Override protected void onSaveInstanceState(Bundle state) {
        state.putInt("target", targetDegrees); state.putString("targetName", targetName); super.onSaveInstanceState(state);
    }
    private View buildScreen() {
        scroll = new ScrollView(this);
        scroll.setFillViewport(true); scroll.setBackgroundColor(navy());
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(20), dp(16), dp(20), dp(28));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));
        LinearLayout top = new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL);
        TextView badge = text(getString(R.string.pro_brand), 12, gold()); badge.setLetterSpacing(.13f);
        top.addView(badge, new LinearLayout.LayoutParams(0, -2, 1));
        Button dismiss = button(getString(R.string.close), false); dismiss.setTextSize(13); dismiss.setMinHeight(dp(48));
        dismiss.setOnClickListener(v -> finish()); top.addView(dismiss, new LinearLayout.LayoutParams(-2, -2));
        root.addView(top, matchWrap());
        title = text(getString(R.string.pro_title), 31, moon()); title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        androidx.core.view.ViewCompat.setAccessibilityHeading(title, true);
        title.setPadding(0, dp(13), 0, dp(7)); root.addView(title);
        subtitle = text(getString(R.string.pro_subtitle), 16, mist()); subtitle.setLineSpacing(dp(2), 1f); root.addView(subtitle);

        upgradeBenefits = new LinearLayout(this); upgradeBenefits.setId(R.id.pro_benefits);
        upgradeBenefits.setOrientation(LinearLayout.VERTICAL); spacer(upgradeBenefits, 20);
        LinearLayout proCard = benefitsCard(R.string.pro_benefits_heading, true);
        addBenefit(proCard, R.string.pro_benefit_bearings, R.string.pro_benefit_bearings_body);
        addBenefit(proCard, R.string.pro_benefit_voice, R.string.pro_benefit_voice_body);
        addBenefit(proCard, R.string.pro_benefit_lights, R.string.pro_benefit_lights_body);
        addBenefit(proCard, R.string.pro_benefit_strobe, R.string.pro_benefit_strobe_body);
        upgradeBenefits.addView(proCard, matchWrap()); root.addView(upgradeBenefits, matchWrap());

        purchaseTerms = new LinearLayout(this); purchaseTerms.setId(R.id.pro_purchase_terms);
        purchaseTerms.setOrientation(LinearLayout.VERTICAL); purchaseTerms.setPadding(0, dp(18), 0, dp(4));
        TextView payment = text(getString(R.string.pro_purchase_terms), 15, gold());
        payment.setTypeface(android.graphics.Typeface.DEFAULT_BOLD); purchaseTerms.addView(payment, matchWrap());
        TextView restoreHint = text(getString(R.string.pro_restore_hint), 13, mist());
        restoreHint.setPadding(0, dp(5), 0, dp(7)); purchaseTerms.addView(restoreHint, matchWrap()); root.addView(purchaseTerms, matchWrap());
        entitlement = text(getString(R.string.billing_checking), 14, mist()); entitlement.setGravity(Gravity.CENTER); root.addView(entitlement, matchWrap());
        entitlement.setPadding(0, dp(8), 0, dp(10));
        unlock = button(getString(R.string.unlock_pro), true); unlock.setId(R.id.pro_unlock);
        unlock.setOnClickListener(v -> billing.buy(this)); root.addView(unlock, matchWrap());
        restore = button(getString(R.string.restore_purchase), false); restore.setId(R.id.pro_restore);
        restore.setOnClickListener(v -> billing.refresh(true));
        LinearLayout.LayoutParams restoreParams = matchWrap(); restoreParams.topMargin = dp(8); root.addView(restore, restoreParams);
        retry = button(getString(R.string.retry_play), false); retry.setOnClickListener(v -> billing.refresh(false)); root.addView(retry, matchWrap());
        continueFree = button(getString(R.string.pro_continue_free), false); continueFree.setId(R.id.pro_continue_free);
        continueFree.setOnClickListener(v -> finish());
        LinearLayout.LayoutParams continueParams = matchWrap(); continueParams.topMargin = dp(8); root.addView(continueFree, continueParams);

        freeOverview = new LinearLayout(this); freeOverview.setId(R.id.pro_free_features); freeOverview.setOrientation(LinearLayout.VERTICAL);
        spacer(freeOverview, 22);
        LinearLayout freeCard = benefitsCard(R.string.pro_free_heading, false);
        TextView freeIntro = text(getString(R.string.pro_free_intro), 14, mist()); freeIntro.setPadding(0, dp(8), 0, 0); freeCard.addView(freeIntro, matchWrap());
        addBenefit(freeCard, R.string.pro_free_compass, R.string.pro_free_compass_body);
        addBenefit(freeCard, R.string.pro_free_lights, R.string.pro_free_lights_body);
        addBenefit(freeCard, R.string.pro_free_voice, R.string.pro_free_voice_body);
        addBenefit(freeCard, R.string.pro_free_altitude, R.string.pro_free_altitude_body);
        freeOverview.addView(freeCard, matchWrap());
        TextView shared = text(getString(R.string.pro_shared_privacy), 14, moon()); shared.setPadding(0, dp(16), 0, dp(8)); freeOverview.addView(shared, matchWrap());
        TextView limits = text(getString(R.string.pro_feature_requirements), 13, mist()); limits.setLineSpacing(dp(2), 1f); freeOverview.addView(limits, matchWrap());
        root.addView(freeOverview, matchWrap());

        ownerTools = new LinearLayout(this); ownerTools.setId(R.id.pro_owner_tools); ownerTools.setOrientation(LinearLayout.VERTICAL);
        root.addView(ownerTools, matchWrap()); spacer(ownerTools, 14);
        heading = text(getString(R.string.bearing_waiting), 15, gold()); heading.setGravity(Gravity.CENTER);
        heading.setPadding(dp(12), dp(12), dp(12), dp(12)); heading.setBackgroundResource(R.drawable.pill); ownerTools.addView(heading, matchWrap());
        accuracy = text(getString(R.string.compass_waiting_accuracy), 13, mist()); accuracy.setGravity(Gravity.CENTER); ownerTools.addView(accuracy, matchWrap());
        spacer(ownerTools, 22); label(ownerTools, R.string.saved_bearings);
        ownerTools.addView(text(getString(R.string.bearing_hint), 15, mist()));
        saveBearing = button(getString(R.string.save_bearing), false); saveBearing.setOnClickListener(v -> saveCurrentBearing()); addProControl(ownerTools, saveBearing);
        guidance = text(getString(R.string.guidance_hint), 16, gold()); guidance.setGravity(Gravity.CENTER); ownerTools.addView(guidance, matchWrap());
        clearTarget = button(getString(R.string.clear_target), false); clearTarget.setOnClickListener(v -> { targetDegrees = -1; targetName = null; updateGuidance(); }); ownerTools.addView(clearTarget, matchWrap());
        addSpokenGuidance(ownerTools);
        savedBearings = new LinearLayout(this); savedBearings.setOrientation(LinearLayout.VERTICAL); ownerTools.addView(savedBearings, matchWrap());
        spacer(ownerTools, 24); label(ownerTools, R.string.night_palette);
        ownerTools.addView(text(getString(R.string.palette_hint), 15, mist()));
        LinearLayout palette = new LinearLayout(this);
        boolean stackedPalette = getResources().getConfiguration().fontScale >= 1.3f || getResources().getConfiguration().screenWidthDp < 340;
        palette.setOrientation(stackedPalette ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        Button amber = button(getString(R.string.amber), false); amber.setOnClickListener(v -> openNightLight("amber")); palette.addView(amber, stackedPalette ? matchWrap() : new LinearLayout.LayoutParams(0, -2, 1));
        Button green = button(getString(R.string.green), false); green.setOnClickListener(v -> openNightLight("green"));
        LinearLayout.LayoutParams greenParams = stackedPalette ? matchWrap() : new LinearLayout.LayoutParams(0, -2, 1);
        if (stackedPalette) greenParams.topMargin = dp(8); else greenParams.setMarginStart(dp(8)); palette.addView(green, greenParams);
        ownerTools.addView(palette, matchWrap()); proControls.add(amber); proControls.add(green);
        spacer(ownerTools, 24); label(ownerTools, R.string.flash_signal);
        ownerTools.addView(text(getString(R.string.strobe_hint), 15, mist()));
        Button strobe = button(getString(R.string.open_strobe), false); strobe.setOnClickListener(v -> startActivity(new Intent(this, StrobeActivity.class))); addProControl(ownerTools, strobe);
        spacer(ownerTools, 20);
        Button close = button(getString(R.string.close), false); close.setOnClickListener(v -> finish()); ownerTools.addView(close, matchWrap());
        spacer(root, 25); root.addView(text(getString(R.string.pro_footer), 13, mist()));
        return Ui.screen(this, scroll);
    }

    private LinearLayout benefitsCard(int headingResource, boolean pro) {
        LinearLayout card = new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        GradientDrawable background = new GradientDrawable(); background.setColor(Color.rgb(17, 28, 44));
        background.setCornerRadius(dp(20)); background.setStroke(dp(1), pro ? gold() : Color.rgb(45, 62, 83)); card.setBackground(background);
        TextView heading = text(getString(headingResource), 12, pro ? gold() : moon()); heading.setLetterSpacing(.1f);
        androidx.core.view.ViewCompat.setAccessibilityHeading(heading, true); card.addView(heading, matchWrap());
        return card;
    }
    private void addBenefit(LinearLayout card, int titleResource, int descriptionResource) {
        TextView title = text(getString(titleResource), 16, moon()); title.setPadding(0, dp(14), 0, dp(3));
        title.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)); card.addView(title, matchWrap());
        TextView description = text(getString(descriptionResource), 14, mist()); description.setLineSpacing(dp(2), 1f); card.addView(description, matchWrap());
    }
    @Override public void onBillingChanged() {
        if (unlock == null) return;
        boolean owned = billing.entitled();
        upgradeBenefits.setVisibility(owned ? View.GONE : View.VISIBLE);
        freeOverview.setVisibility(owned ? View.GONE : View.VISIBLE);
        purchaseTerms.setVisibility(owned ? View.GONE : View.VISIBLE);
        continueFree.setVisibility(owned ? View.GONE : View.VISIBLE);
        ownerTools.setVisibility(owned ? View.VISIBLE : View.GONE);
        if (displayingOwned != null && displayingOwned != owned) scroll.post(() -> scroll.scrollTo(0, 0));
        displayingOwned = owned;
        title.setText(owned ? R.string.field_pro_owner_title : R.string.pro_title);
        subtitle.setText(owned ? R.string.field_pro_owner_subtitle : R.string.pro_subtitle);
        if (owned && requestedBearing >= 0) {
            try {
                BearingData data = bearings();
                if (requestedBearing < data.size()) {
                    targetDegrees = data.degrees(requestedBearing); targetName = data.name(requestedBearing);
                }
            } catch (JSONException error) { toast(R.string.bearings_read_error); }
            requestedBearing = -1;
        }
        for (Button button : proControls) { button.setEnabled(owned); button.setAlpha(owned ? 1f : .45f); }
        unlock.setVisibility(owned ? View.GONE : View.VISIBLE); unlock.setEnabled(billing.canBuy());
        unlock.setText(billing.price().isEmpty() ? getString(R.string.unlock_pro) : getString(R.string.unlock_price, billing.price()));
        restore.setVisibility(owned ? View.GONE : View.VISIBLE);
        entitlement.setText(owned && billing.status() != R.string.billing_unlocked ? getString(R.string.billing_offline_access) : getString(billing.status()));
        retry.setVisibility(billing.needsRetry() ? View.VISIBLE : View.GONE);
        loadSavedBearings(); updateBearingControls(); updateGuidance(); onSpeechChanged();
    }
    @Override public void onHeading(float value) {
        currentHeading = Float.isFinite(value) ? HeadingMath.normalize(value) : Float.NaN;
        speech.heading(value);
        heading.setText(Float.isNaN(currentHeading) ? getString(R.string.bearing_waiting) : getString(R.string.current_bearing, HeadingMath.rounded(value)));
        updateBearingControls(); updateGuidance();
    }
    @Override public void onCompassStatus(int message) {
        compassMessage = message; accuracy.setText(message); speech.accuracy(message);
        if (message == R.string.compass_unavailable) { currentHeading = Float.NaN; heading.setText(R.string.bearing_waiting); }
        updateBearingControls(); updateGuidance();
    }

    private void addSpokenGuidance(LinearLayout root) {
        spacer(root, 16);
        TextView label = text(getString(R.string.bearing_audio_title), 12, gold());
        label.setLetterSpacing(.1f); androidx.core.view.ViewCompat.setAccessibilityHeading(label, true);
        root.addView(label, matchWrap());
        speechStatus = text("", 14, mist()); speechStatus.setId(R.id.bearing_audio_status);
        speechStatus.setPadding(0, dp(8), 0, dp(10)); root.addView(speechStatus, matchWrap());
        speechControls = new LinearLayout(this); speechControls.setOrientation(LinearLayout.VERTICAL);
        root.addView(speechControls, matchWrap());
        speakGuidance = button(getString(R.string.bearing_audio_speak), false); speakGuidance.setId(R.id.bearing_audio_speak);
        speakGuidance.setOnClickListener(v -> { if (canGuide()) speech.speakOnce(); }); speechControls.addView(speakGuidance, matchWrap());
        startSpeech = button(getString(R.string.bearing_audio_start), false); startSpeech.setId(R.id.bearing_audio_start);
        startSpeech.setOnClickListener(v -> { if (canGuide()) speech.automatic(true); });
        LinearLayout.LayoutParams startParams = matchWrap(); startParams.topMargin = dp(8); speechControls.addView(startSpeech, startParams);
        alignmentHaptics = new Switch(this); alignmentHaptics.setId(R.id.bearing_audio_haptics);
        alignmentHaptics.setText(hapticsAvailable ? R.string.bearing_audio_haptics : R.string.bearing_audio_no_haptics);
        alignmentHaptics.setTextColor(moon()); alignmentHaptics.setTextSize(15); alignmentHaptics.setMinHeight(dp(56)); alignmentHaptics.setSwitchPadding(dp(12));
        alignmentHaptics.setChecked(getSharedPreferences(CompassSpeech.PREFS, MODE_PRIVATE).getBoolean("bearing_haptics", false));
        alignmentHaptics.setOnCheckedChangeListener((view, checked) -> {
            getSharedPreferences(CompassSpeech.PREFS, MODE_PRIVATE).edit().putBoolean("bearing_haptics", checked).apply();
        });
        speechControls.addView(alignmentHaptics, matchWrap());
        LinearLayout actions = new LinearLayout(this);
        boolean stacked = getResources().getConfiguration().fontScale >= 1.3f || getResources().getConfiguration().screenWidthDp < 340;
        actions.setOrientation(stacked ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        pauseSpeech = button(getString(R.string.speech_pause), false); pauseSpeech.setId(R.id.bearing_audio_pause);
        pauseSpeech.setOnClickListener(v -> speech.pauseSpeech()); actions.addView(pauseSpeech, stacked ? matchWrap() : new LinearLayout.LayoutParams(0, -2, 1));
        Button settings = button(getString(R.string.speech_settings), false); settings.setId(R.id.bearing_audio_settings);
        settings.setOnClickListener(v -> { if (canGuide()) showGuidanceSettings(); });
        LinearLayout.LayoutParams settingsParams = stacked ? matchWrap() : new LinearLayout.LayoutParams(0, -2, 1);
        if (stacked) settingsParams.topMargin = dp(8); else settingsParams.setMarginStart(dp(8));
        actions.addView(settings, settingsParams); speechControls.addView(actions, matchWrap());
        speechReading = text("", 15, moon()); speechReading.setId(R.id.bearing_audio_reading);
        speechReading.setPadding(0, dp(8), 0, dp(8)); speechControls.addView(speechReading, matchWrap());
        root.addView(text(getString(R.string.speech_scope), 13, mist()), matchWrap());
    }

    private boolean canGuide() { return billing.entitled() && targetDegrees >= 0 && targetDegrees < 360; }

    private void syncSpokenTarget() {
        boolean selected = canGuide();
        if ((selected && (spokenTarget == null || spokenTarget.target() != targetDegrees)) || (!selected && spokenTarget != null)) {
            spokenTarget = selected ? new BearingGuidance(this, targetDegrees, this::alignmentFeedback) : null;
            speech.guidance(spokenTarget);
            speechReading.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_NONE); speechReading.setText("");
        }
    }

    @Override public void onSpeechChanged() {
        if (speechStatus == null || startSpeech == null) return;
        boolean selected = canGuide();
        speechControls.setVisibility(selected ? View.VISIBLE : View.GONE);
        speakGuidance.setEnabled(selected);
        startSpeech.setEnabled(selected && !speech.screenReader() && !speech.automatic());
        startSpeech.setAlpha(startSpeech.isEnabled() ? 1f : .45f);
        pauseSpeech.setEnabled(selected && speech.canPause()); pauseSpeech.setAlpha(pauseSpeech.isEnabled() ? 1f : .45f);
        alignmentHaptics.setEnabled(selected && hapticsAvailable);
        int status = speech.status();
        if (!billing.entitled()) status = R.string.bearing_audio_locked;
        else if (!selected) status = R.string.bearing_audio_choose;
        else if (speech.screenReader()) status = R.string.bearing_audio_reader;
        else if (speech.automatic() && status != R.string.speech_preparing) status = R.string.bearing_audio_active;
        else if (status == R.string.speech_intro || status == R.string.speech_ready) status = R.string.bearing_audio_intro;
        else if (status == R.string.speech_paused) status = R.string.bearing_audio_paused;
        else if (status == R.string.speech_audio_busy) status = R.string.bearing_audio_interrupted;
        speechStatus.setText(status);
    }

    @Override public void onSpeechReading(String text, boolean throughScreenReader) {
        if (!canGuide() || !resumed) return;
        speechReading.setAccessibilityLiveRegion(throughScreenReader ? View.ACCESSIBILITY_LIVE_REGION_POLITE : View.ACCESSIBILITY_LIVE_REGION_NONE);
        speechReading.setText(""); speechReading.setText(getString(R.string.bearing_audio_last, text));
    }

    private void alignmentFeedback() {
        if (!resumed || !canGuide() || !hapticsAvailable || !alignmentHaptics.isChecked() || !hasWindowFocus()) return;
        // Native feedback honors Android's haptic settings; no VIBRATE permission or repeating effect.
        guidance.performHapticFeedback(Build.VERSION.SDK_INT >= 30 ? HapticFeedbackConstants.CONFIRM : HapticFeedbackConstants.CLOCK_TICK);
    }

    private void showGuidanceSettings() {
        speech.pauseSpeech();
        String[] intervals = getResources().getStringArray(R.array.speech_frequencies), rates = getResources().getStringArray(R.array.speech_rates);
        String[] choices = { getString(R.string.speech_frequency, intervals[speech.intervalIndex()]),
                getString(R.string.speech_speed, rates[speech.rateIndex()]), getString(R.string.speech_voice_data),
                getString(R.string.bearing_audio_retry), getString(R.string.bearing_audio_help) };
        new AlertDialog.Builder(this).setTitle(R.string.speech_settings).setItems(choices, (dialog, which) -> {
            if (which == 0) new AlertDialog.Builder(this).setTitle(R.string.speech_frequency_title)
                    .setSingleChoiceItems(intervals, speech.intervalIndex(), (picker, index) -> { speech.intervalIndex(index); picker.dismiss(); })
                    .setNegativeButton(R.string.close, null).show();
            else if (which == 1) new AlertDialog.Builder(this).setTitle(R.string.speech_speed_title)
                    .setSingleChoiceItems(rates, speech.rateIndex(), (picker, index) -> { speech.rateIndex(index); picker.dismiss(); })
                    .setNegativeButton(R.string.close, null).show();
            else if (which == 2) {
                try { startActivity(new Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)); }
                catch (ActivityNotFoundException | SecurityException unavailable) {
                    try { startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)); }
                    catch (ActivityNotFoundException | SecurityException missing) { toast(R.string.speech_setup_unavailable); }
                }
            } else if (which == 3 && canGuide()) speech.retryVoice();
            else new AlertDialog.Builder(this).setTitle(R.string.bearing_audio_help).setMessage(R.string.bearing_audio_help_body)
                    .setPositiveButton(R.string.close, null).show();
        }).setNegativeButton(R.string.close, null).show();
    }
    private void updateBearingControls() {
        saveBearing.setEnabled(billing.entitled() && compass.available() && !Float.isNaN(currentHeading));
        saveBearing.setAlpha(saveBearing.isEnabled() ? 1f : .45f);
    }
    private SharedPreferences preferences() { return getSharedPreferences(BillingManager.PREFS, MODE_PRIVATE); }
    private BearingData bearings() throws JSONException { return new BearingData(preferences().getString(SAVED_BEARINGS, "[]")); }
    private void persist(BearingData data) { preferences().edit().putString(SAVED_BEARINGS, data.toString()).apply(); loadSavedBearings(); }
    private void saveCurrentBearing() {
        if (!billing.entitled() || Float.isNaN(currentHeading)) return;
        speech.pauseSpeech();
        final int captured = HeadingMath.rounded(currentHeading);
        try {
            if (bearings().size() >= BearingData.LIMIT) { toast(R.string.bearings_full); return; }
        } catch (JSONException error) { toast(R.string.bearings_read_error); return; }
        EditText input = nameInput("");
        new AlertDialog.Builder(this).setTitle(R.string.save_bearing).setMessage(getString(R.string.bearing_degrees, captured))
                .setView(input).setNegativeButton(R.string.cancel, null).setPositiveButton(R.string.save, (dialog, which) -> {
                    try {
                        BearingData data = bearings();
                        if (data.add(cleanName(input), captured)) persist(data); else toast(R.string.bearings_full);
                    } catch (JSONException error) { toast(R.string.bearing_save_error); }
                }).show();
    }
    private void loadSavedBearings() {
        savedBearings.removeAllViews();
        if (!billing.entitled()) { savedBearings.addView(text(getString(R.string.bearings_locked), 14, mist())); return; }
        try {
            BearingData data = bearings();
            if (data.size() == 0) savedBearings.addView(text(getString(R.string.bearings_empty), 14, mist()));
            for (int i = data.size() - 1; i >= 0; i--) {
                final int index = i;
                Button row = button(getString(R.string.bearing_row, data.degrees(i), data.name(i)), false);
                row.setOnClickListener(v -> manageBearing(index)); savedBearings.addView(row, matchWrap());
            }
        } catch (JSONException error) { savedBearings.addView(text(getString(R.string.bearings_read_error), 14, mist())); }
    }
    private void manageBearing(int index) {
        speech.pauseSpeech();
        try {
            BearingData data = bearings();
            new AlertDialog.Builder(this).setTitle(getString(R.string.bearing_row, data.degrees(index), data.name(index)))
                    .setItems(R.array.bearing_actions, (dialog, action) -> {
                        try {
                            if (action == 0) {
                                speech.pauseSpeech(); spokenTarget = null;
                                targetDegrees = data.degrees(index); targetName = data.name(index); updateGuidance();
                            }
                            else if (action == 1) {
                                EditText input = nameInput(data.name(index));
                                new AlertDialog.Builder(this).setTitle(R.string.rename_bearing).setView(input).setNegativeButton(R.string.cancel, null)
                                        .setPositiveButton(R.string.save, (d, w) -> {
                                            try { data.rename(index, cleanName(input)); persist(data); targetDegrees = -1; updateGuidance(); }
                                            catch (JSONException error) { toast(R.string.bearing_save_error); }
                                        }).show();
                            } else new AlertDialog.Builder(this).setTitle(R.string.delete_bearing).setMessage(R.string.delete_bearing_confirm)
                                    .setNegativeButton(R.string.cancel, null).setPositiveButton(R.string.delete, (d, w) -> {
                                        data.delete(index); persist(data); targetDegrees = -1; updateGuidance();
                                    }).show();
                        } catch (JSONException error) { toast(R.string.bearings_read_error); }
                    }).show();
        } catch (JSONException error) { toast(R.string.bearings_read_error); }
    }
    private void updateGuidance() {
        if (guidance == null) return;
        syncSpokenTarget();
        boolean selected = canGuide();
        clearTarget.setVisibility(selected ? View.VISIBLE : View.GONE);
        if (!selected) { guidance.setText(R.string.guidance_hint); return; }
        if (compassMessage != R.string.compass_accuracy_ok) { guidance.setText(compassMessage); return; }
        if (Float.isNaN(currentHeading)) { guidance.setText(R.string.bearing_waiting); return; }
        String instruction = spokenTarget.visualInstruction(currentHeading);
        guidance.setText(getString(R.string.guidance_target, targetName == null ? getString(R.string.default_bearing) : targetName, targetDegrees, instruction));
    }
    private EditText nameInput(String value) {
        EditText input = new EditText(this); input.setHint(R.string.bearing_name_hint); input.setSingleLine(true);
        input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(60)}); input.setText(value); return input;
    }
    private String cleanName(EditText input) { String name = input.getText().toString().trim(); return name.isEmpty() ? getString(R.string.default_bearing) : name; }
    private void openNightLight(String mode) { startActivity(new Intent(this, RedLightActivity.class).putExtra(RedLightActivity.EXTRA_MODE, mode)); }
    private void toast(int message) { Toast.makeText(this, message, Toast.LENGTH_LONG).show(); }
    private void addProControl(LinearLayout root, Button button) { root.addView(button, matchWrap()); proControls.add(button); }
    private void label(LinearLayout root, int message) { TextView view = text(getString(message), 12, gold()); view.setLetterSpacing(.13f); root.addView(view); }
    private TextView text(String value, float size, int color) { TextView view = new TextView(this); view.setText(value); view.setTextSize(size); view.setTextColor(color); return view; }
    private Button button(String label, boolean gold) { Button button = new Button(this); button.setText(label); button.setTextSize(16); button.setAllCaps(false); button.setMinHeight(dp(54)); button.setTextColor(gold ? navy() : moon()); button.setBackgroundResource(gold ? R.drawable.button_gold : R.drawable.button_dark); return button; }
    private LinearLayout.LayoutParams matchWrap() { return new LinearLayout.LayoutParams(-1, -2); }
    private void spacer(LinearLayout root, int height) { root.addView(new View(this), new LinearLayout.LayoutParams(1, dp(height))); }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private int navy() { return Color.rgb(11, 19, 32); }
    private int moon() { return Color.rgb(238, 243, 247); }
    private int mist() { return Color.rgb(174, 187, 203); }
    private int gold() { return Color.rgb(240, 182, 91); }
}
