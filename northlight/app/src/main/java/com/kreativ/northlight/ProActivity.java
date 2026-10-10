package com.kreativ.northlight;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.*;
import org.json.JSONException;
import java.util.ArrayList;
import java.util.List;

/** Offline tools share the free compass's sensors and application-scoped billing. */
public class ProActivity extends Activity implements CompassSource.Listener, BillingManager.Listener {
    private static final String SAVED_BEARINGS = "saved_bearings";
    private CompassSource compass;
    private BillingManager billing;
    private TextView heading, accuracy, entitlement, guidance;
    private LinearLayout savedBearings;
    private Button unlock, restore, retry, saveBearing, clearTarget;
    private final List<Button> proControls = new ArrayList<>();
    private float currentHeading = Float.NaN;
    private int targetDegrees = -1;
    private String targetName;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        compass = new CompassSource(this, this);
        billing = BillingManager.get(this);
        if (state != null) { targetDegrees = state.getInt("target", -1); targetName = state.getString("targetName"); }
        setContentView(buildScreen());
        onBillingChanged();
    }
    @Override protected void onResume() {
        super.onResume(); currentHeading = Float.NaN; compass.start(); billing.addListener(this); updateBearingControls();
    }
    @Override protected void onPause() { compass.stop(); billing.removeListener(this); super.onPause(); }
    @Override protected void onSaveInstanceState(Bundle state) {
        state.putInt("target", targetDegrees); state.putString("targetName", targetName); super.onSaveInstanceState(state);
    }
    private View buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true); scroll.setBackgroundColor(navy());
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(24), dp(28), dp(24), dp(30));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));
        TextView badge = text(getString(R.string.pro_brand), 12, gold()); badge.setLetterSpacing(.16f); root.addView(badge);
        TextView title = text(getString(R.string.pro_title), 31, moon()); title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setPadding(0, dp(13), 0, dp(7)); root.addView(title);
        root.addView(text(getString(R.string.pro_subtitle), 16, mist()));
        spacer(root, 20);
        heading = text(getString(R.string.bearing_waiting), 15, gold()); heading.setGravity(Gravity.CENTER);
        heading.setPadding(dp(12), dp(12), dp(12), dp(12)); heading.setBackgroundResource(R.drawable.pill); root.addView(heading, matchWrap());
        accuracy = text(getString(R.string.compass_waiting_accuracy), 13, mist()); accuracy.setGravity(Gravity.CENTER); root.addView(accuracy, matchWrap());
        spacer(root, 18);
        entitlement = text(getString(R.string.billing_checking), 14, mist()); entitlement.setGravity(Gravity.CENTER); root.addView(entitlement, matchWrap());
        unlock = button(getString(R.string.unlock_pro), true); unlock.setOnClickListener(v -> billing.buy(this)); root.addView(unlock, matchWrap());
        spacer(root, 8);
        restore = button(getString(R.string.restore_purchase), false); restore.setOnClickListener(v -> billing.refresh(true)); root.addView(restore, matchWrap());
        retry = button(getString(R.string.retry_play), false); retry.setOnClickListener(v -> billing.refresh(false)); root.addView(retry, matchWrap());
        spacer(root, 26); label(root, R.string.saved_bearings);
        root.addView(text(getString(R.string.bearing_hint), 15, mist()));
        saveBearing = button(getString(R.string.save_bearing), false); saveBearing.setOnClickListener(v -> saveCurrentBearing()); addProControl(root, saveBearing);
        guidance = text(getString(R.string.guidance_hint), 16, gold()); guidance.setGravity(Gravity.CENTER); root.addView(guidance, matchWrap());
        clearTarget = button(getString(R.string.clear_target), false); clearTarget.setOnClickListener(v -> { targetDegrees = -1; updateGuidance(); }); root.addView(clearTarget, matchWrap());
        savedBearings = new LinearLayout(this); savedBearings.setOrientation(LinearLayout.VERTICAL); root.addView(savedBearings, matchWrap());
        spacer(root, 24); label(root, R.string.night_palette);
        root.addView(text(getString(R.string.palette_hint), 15, mist()));
        LinearLayout palette = new LinearLayout(this);
        Button amber = button(getString(R.string.amber), false); amber.setOnClickListener(v -> openNightLight("amber")); palette.addView(amber, new LinearLayout.LayoutParams(0, -2, 1));
        Button green = button(getString(R.string.green), false); green.setOnClickListener(v -> openNightLight("green"));
        LinearLayout.LayoutParams greenParams = new LinearLayout.LayoutParams(0, -2, 1); greenParams.leftMargin = dp(8); palette.addView(green, greenParams);
        root.addView(palette, matchWrap()); proControls.add(amber); proControls.add(green);
        spacer(root, 24); label(root, R.string.flash_signal);
        root.addView(text(getString(R.string.strobe_hint), 15, mist()));
        Button strobe = button(getString(R.string.open_strobe), false); strobe.setOnClickListener(v -> startActivity(new Intent(this, StrobeActivity.class))); addProControl(root, strobe);
        spacer(root, 25); root.addView(text(getString(R.string.pro_footer), 13, mist()));
        Button close = button(getString(R.string.close), false); close.setOnClickListener(v -> finish()); root.addView(close, matchWrap());
        return Ui.screen(this, scroll);
    }
    @Override public void onBillingChanged() {
        if (unlock == null) return;
        boolean owned = billing.entitled();
        for (Button button : proControls) { button.setEnabled(owned); button.setAlpha(owned ? 1f : .45f); }
        unlock.setVisibility(owned ? View.GONE : View.VISIBLE); unlock.setEnabled(billing.canBuy());
        unlock.setText(billing.price().isEmpty() ? getString(R.string.unlock_pro) : getString(R.string.unlock_price, billing.price()));
        restore.setVisibility(owned ? View.GONE : View.VISIBLE);
        entitlement.setText(owned && billing.status() != R.string.billing_unlocked ? getString(R.string.billing_offline_access) : getString(billing.status()));
        retry.setVisibility(billing.needsRetry() ? View.VISIBLE : View.GONE);
        loadSavedBearings(); updateBearingControls(); updateGuidance();
    }
    @Override public void onHeading(float value) {
        currentHeading = value; heading.setText(getString(R.string.current_bearing, HeadingMath.rounded(value))); updateBearingControls(); updateGuidance();
    }
    @Override public void onCompassStatus(int message) { accuracy.setText(message); }
    private void updateBearingControls() {
        saveBearing.setEnabled(billing.entitled() && compass.available() && !Float.isNaN(currentHeading));
        saveBearing.setAlpha(saveBearing.isEnabled() ? 1f : .45f);
    }
    private SharedPreferences preferences() { return getSharedPreferences(BillingManager.PREFS, MODE_PRIVATE); }
    private BearingData bearings() throws JSONException { return new BearingData(preferences().getString(SAVED_BEARINGS, "[]")); }
    private void persist(BearingData data) { preferences().edit().putString(SAVED_BEARINGS, data.toString()).apply(); loadSavedBearings(); }
    private void saveCurrentBearing() {
        if (!billing.entitled() || Float.isNaN(currentHeading)) return;
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
        try {
            BearingData data = bearings();
            new AlertDialog.Builder(this).setTitle(getString(R.string.bearing_row, data.degrees(index), data.name(index)))
                    .setItems(R.array.bearing_actions, (dialog, action) -> {
                        try {
                            if (action == 0) { targetDegrees = data.degrees(index); targetName = data.name(index); updateGuidance(); }
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
        boolean selected = billing.entitled() && targetDegrees >= 0;
        clearTarget.setVisibility(selected ? View.VISIBLE : View.GONE);
        if (!selected) { guidance.setText(R.string.guidance_hint); return; }
        if (Float.isNaN(currentHeading)) { guidance.setText(R.string.bearing_waiting); return; }
        float delta = HeadingMath.difference(targetDegrees, currentHeading);
        String instruction = Math.abs(delta) <= 3f ? getString(R.string.guidance_aligned)
                : getString(delta < 0 ? R.string.guidance_left : R.string.guidance_right, Math.round(Math.abs(delta)));
        guidance.setText(getString(R.string.guidance_target, targetName, targetDegrees, instruction));
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
