package com.kreativ.northlight;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.GeomagneticField;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.Settings;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONException;

/** A private foreground-only point-back tool: straight-line estimates, not navigation. */
public class PlaceActivity extends Activity implements CompassSource.Listener, CompassSpeech.Listener,
        PlaceLocationSource.Listener, BillingManager.Listener {
    private static final String KEY = "saved_places";
    private static final int LOCATION_REQUEST = 410;
    private BillingManager billing;
    private PlaceLocationSource location;
    private CompassSource compass;
    private CompassSpeech speech;
    private BearingGuidance direction;
    private TextView status, target, speechStatus, reading;
    private Button gps, save, speak, auto, pause;
    private Switch haptics;
    private LinearLayout places;
    private boolean foreground, hapticsAvailable;
    private int selected = -1, compassStatus = R.string.compass_waiting_accuracy;
    private float heading = Float.NaN;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        billing = BillingManager.get(this);
        location = new PlaceLocationSource(this, this);
        compass = new CompassSource(this, this);
        speech = new CompassSpeech(this, this);
        try { Vibrator vibrator = getSystemService(Vibrator.class); hapticsAvailable = vibrator != null && vibrator.hasVibrator(); }
        catch (RuntimeException unavailable) { hapticsAvailable = false; }
        setContentView(buildScreen());
        loadPlaces();
        update();
    }
    @Override protected void onResume() {
        super.onResume(); foreground = true; heading = Float.NaN;
        speech.resume(); compass.start(); billing.addListener(this); update();
    }
    @Override protected void onPause() {
        foreground = false; location.stop(); compass.stop(); speech.pause(); billing.removeListener(this);
        heading = Float.NaN; super.onPause();
    }
    @Override protected void onDestroy() { speech.close(); super.onDestroy(); }
    @Override public void onBillingChanged() {
        if (!billing.entitled()) {
            if (location != null && location.active()) location.stop();
            if (speech != null) speech.pauseSpeech();
            finish(); return;
        }
        update();
    }
    @Override public void onHeading(float value) { heading = HeadingMath.normalize(value); speech.heading(value); updateTarget(); }
    @Override public void onCompassStatus(int message) { compassStatus = message; speech.accuracy(message); updateTarget(); }
    @Override public void onPlaceLocationChanged() { update(); }
    @Override public void onSpeechChanged() {
        if (speechStatus == null) return;
        speechStatus.setText(speech.screenReader() ? R.string.place_reader : speech.status());
        boolean ready = direction != null && foreground && billing.entitled();
        speak.setEnabled(ready); auto.setEnabled(ready && !speech.screenReader() && !speech.automatic());
        pause.setEnabled(ready && speech.canPause()); haptics.setEnabled(ready && hapticsAvailable);
    }
    @Override public void onSpeechReading(String value, boolean reader) {
        reading.setAccessibilityLiveRegion(reader ? View.ACCESSIBILITY_LIVE_REGION_POLITE : View.ACCESSIBILITY_LIVE_REGION_NONE);
        reading.setText(""); reading.setText(getString(R.string.bearing_audio_last, value));
    }
    private View buildScreen() {
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(navy());
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(20), dp(20), dp(20), dp(28));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));
        TextView badge = text(getString(R.string.pro_brand), 12, gold()); badge.setLetterSpacing(.13f); root.addView(badge);
        TextView title = text(getString(R.string.places_title), 30, moon()); title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setPadding(0, dp(12), 0, dp(8)); androidx.core.view.ViewCompat.setAccessibilityHeading(title, true); root.addView(title);
        root.addView(text(getString(R.string.places_intro), 15, mist()));
        status = text("", 15, mist()); status.setPadding(0, dp(14), 0, dp(8)); root.addView(status);
        gps = button(""); gps.setId(R.id.place_gps); gps.setOnClickListener(v -> locationAction()); root.addView(gps, match());
        save = button(getString(R.string.place_save)); save.setId(R.id.place_save); save.setOnClickListener(v -> savePlace()); root.addView(save, match());
        target = text(getString(R.string.place_choose), 18, gold()); target.setId(R.id.place_target);
        target.setGravity(Gravity.CENTER); target.setPadding(dp(8), dp(18), dp(8), dp(12)); root.addView(target, match());
        root.addView(text(getString(R.string.place_limitations), 14, mist()));
        speechStatus = text("", 14, mist()); speechStatus.setPadding(0, dp(12), 0, dp(8)); root.addView(speechStatus);
        speak = button(getString(R.string.bearing_audio_speak)); speak.setId(R.id.place_speak); speak.setOnClickListener(v -> { if (direction != null) speech.speakOnce(); }); root.addView(speak, match());
        auto = button(getString(R.string.bearing_audio_start)); auto.setId(R.id.place_auto); auto.setOnClickListener(v -> { if (direction != null) speech.automatic(true); }); root.addView(auto, match());
        haptics = new Switch(this); haptics.setText(hapticsAvailable ? R.string.bearing_audio_haptics : R.string.bearing_audio_no_haptics); haptics.setTextColor(moon()); haptics.setTextSize(15); haptics.setMinHeight(dp(56)); root.addView(haptics, match());
        pause = button(getString(R.string.speech_pause)); pause.setOnClickListener(v -> speech.pauseSpeech()); root.addView(pause, match());
        Button speechSettings = button(getString(R.string.speech_settings)); speechSettings.setOnClickListener(v -> speechSettings()); root.addView(speechSettings, match());
        reading = text("", 15, moon()); reading.setPadding(0, dp(8), 0, dp(12)); root.addView(reading);
        TextView listTitle = text(getString(R.string.places_saved), 13, gold()); listTitle.setLetterSpacing(.1f); root.addView(listTitle);
        places = new LinearLayout(this); places.setOrientation(LinearLayout.VERTICAL); root.addView(places, match());
        Button close = button(getString(R.string.close)); close.setOnClickListener(v -> finish()); root.addView(close, match());
        return Ui.screen(this, scroll);
    }
    private void locationAction() {
        if (!billing.entitled() || !foreground) return;
        if (location.active()) { location.stop(); return; }
        if (location.state() == PlaceLocationSource.State.GPS_OFF) {
            try { startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)); }
            catch (RuntimeException unavailable) { toast(R.string.altimeter_settings_unavailable); }
            return;
        }
        if (location.permitted()) { location.start(); return; }
        boolean asked = prefs().getBoolean("place_asked_location", false);
        boolean settings = asked && !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION);
        new AlertDialog.Builder(this).setTitle(R.string.place_location_title).setMessage(R.string.place_location_permission)
                .setNegativeButton(R.string.cancel, null).setPositiveButton(settings ? R.string.altimeter_app_settings : R.string.altimeter_allow_location, (dialog, which) -> {
                    if (settings) {
                        try { startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()))); }
                        catch (RuntimeException unavailable) { toast(R.string.altimeter_settings_unavailable); }
                    } else {
                        prefs().edit().putBoolean("place_asked_location", true).apply();
                        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_REQUEST);
                    }
                }).show();
    }
    @Override public void onRequestPermissionsResult(int request, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(request, permissions, results);
        if (request == LOCATION_REQUEST) { toast(location.permitted() ? R.string.place_permission_ready : R.string.place_permission_denied); update(); }
    }
    private SharedPreferences prefs() { return getSharedPreferences(BillingManager.PREFS, MODE_PRIVATE); }
    private PlaceData data() throws JSONException { return new PlaceData(prefs().getString(KEY, "[]")); }
    private boolean persist(PlaceData data) {
        if (!prefs().edit().putString(KEY, data.toString()).commit()) { toast(R.string.place_store_error); return false; }
        loadPlaces(); update(); return true;
    }
    private void savePlace() {
        Location captured = location.fix();
        if (!foreground || !billing.entitled() || captured == null) return;
        try { if (data().size() >= PlaceData.LIMIT) { toast(R.string.place_full); return; } }
        catch (JSONException invalid) { toast(R.string.place_store_error); return; }
        EditText name = nameInput("");
        new AlertDialog.Builder(this).setTitle(R.string.place_save).setMessage(getString(R.string.place_capture_accuracy, Math.round(captured.getAccuracy())))
                .setView(name).setNegativeButton(R.string.cancel, null).setPositiveButton(R.string.save, (dialog, which) -> {
                    if (!billing.entitled()) return;
                    try {
                        PlaceData data = data();
                        if (!data.add(clean(name), captured.getLatitude(), captured.getLongitude(), captured.getAccuracy())) toast(R.string.place_full);
                        else persist(data);
                    } catch (JSONException invalid) { toast(R.string.place_store_error); }
                }).show();
    }
    private void manage(int index) {
        if (!billing.entitled()) return;
        try {
            PlaceData current = data();
            new AlertDialog.Builder(this).setTitle(current.name(index)).setItems(R.array.place_actions, (dialog, action) -> {
                try {
                    PlaceData fresh = data(); if (index >= fresh.size()) { update(); return; }
                    if (action == 0) { speech.pauseSpeech(); selected = index; clearDirection(); update(); }
                    else if (action == 1) {
                        EditText name = nameInput(fresh.name(index));
                        new AlertDialog.Builder(this).setTitle(R.string.place_rename).setView(name).setNegativeButton(R.string.cancel, null)
                                .setPositiveButton(R.string.save, (d, w) -> {
                                    try { PlaceData latest = data(); if (index < latest.size()) { latest.rename(index, clean(name)); persist(latest); } }
                                    catch (JSONException invalid) { toast(R.string.place_store_error); }
                                }).show();
                    } else new AlertDialog.Builder(this).setTitle(R.string.place_delete).setMessage(R.string.place_delete_confirm)
                            .setNegativeButton(R.string.cancel, null).setPositiveButton(R.string.delete, (d, w) -> {
                                try { PlaceData latest = data(); if (index < latest.size()) { latest.delete(index); selected = -1; clearDirection(); persist(latest); } }
                                catch (JSONException invalid) { toast(R.string.place_store_error); }
                            }).show();
                } catch (JSONException invalid) { toast(R.string.place_store_error); }
            }).setNegativeButton(R.string.cancel, null).show();
        } catch (JSONException invalid) { toast(R.string.place_store_error); }
    }
    private void update() {
        if (status == null || location == null) return;
        int message;
        switch (location.state()) {
            case WAITING: message = R.string.place_waiting; break;
            case READY: message = R.string.place_ready; break;
            case LOW: message = R.string.place_low; break;
            case STALE: message = R.string.place_stale; break;
            case DENIED: message = R.string.place_permission_denied; break;
            case GPS_OFF: message = R.string.place_gps_off; break;
            case UNAVAILABLE: message = R.string.place_no_gps; break;
            case SIMULATED: message = R.string.place_simulated; break;
            case ERROR: message = R.string.place_error; break;
            default: message = R.string.place_off;
        }
        status.setText(message); gps.setText(location.active() ? R.string.place_stop_gps : location.state() == PlaceLocationSource.State.GPS_OFF ? R.string.altimeter_location_settings : R.string.place_start_gps);
        save.setEnabled(foreground && billing.entitled() && location.fix() != null);
        updateTarget();
    }
    private void loadPlaces() {
        if (places == null) return;
        places.removeAllViews();
        try {
            PlaceData data = data();
            if (data.size() == 0) places.addView(text(getString(R.string.place_empty), 14, mist()));
            for (int i = 0; i < data.size(); i++) {
                final int index = i; Button row = button(data.name(i)); row.setOnClickListener(v -> manage(index)); places.addView(row, match());
            }
        } catch (JSONException invalid) { places.addView(text(getString(R.string.place_store_error), 14, mist())); }
    }
    private void speechSettings() {
        speech.pauseSpeech();
        String[] intervals = getResources().getStringArray(R.array.speech_frequencies), rates = getResources().getStringArray(R.array.speech_rates);
        String[] choices = { getString(R.string.speech_frequency, intervals[speech.intervalIndex()]),
                getString(R.string.speech_speed, rates[speech.rateIndex()]), getString(R.string.speech_voice_data), getString(R.string.bearing_audio_retry) };
        new AlertDialog.Builder(this).setTitle(R.string.speech_settings).setItems(choices, (dialog, which) -> {
            if (which == 0) new AlertDialog.Builder(this).setTitle(R.string.speech_frequency_title)
                    .setSingleChoiceItems(intervals, speech.intervalIndex(), (picker, index) -> { speech.intervalIndex(index); picker.dismiss(); })
                    .setNegativeButton(R.string.close, null).show();
            else if (which == 1) new AlertDialog.Builder(this).setTitle(R.string.speech_speed_title)
                    .setSingleChoiceItems(rates, speech.rateIndex(), (picker, index) -> { speech.rateIndex(index); picker.dismiss(); })
                    .setNegativeButton(R.string.close, null).show();
            else if (which == 2) {
                try { startActivity(new Intent(android.speech.tts.TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)); }
                catch (RuntimeException unavailable) { toast(R.string.speech_setup_unavailable); }
            } else if (direction != null) speech.retryVoice();
        }).setNegativeButton(R.string.close, null).show();
    }
    private void updateTarget() {
        if (target == null || speech == null) return;
        Location fix = location.fix();
        if (!foreground || !billing.entitled() || selected < 0) { clearDirection(); target.setText(R.string.place_choose); return; }
        try {
            PlaceData data = data();
            if (selected >= data.size()) { selected = -1; clearDirection(); target.setText(R.string.place_choose); return; }
            String name = data.name(selected);
            if (fix == null) { clearDirection(); target.setText(getString(R.string.place_target_waiting, name)); return; }
            double distance = PlaceMath.distance(fix.getLatitude(), fix.getLongitude(), data.latitude(selected), data.longitude(selected));
            String distanceText = distance >= 1000 ? getString(R.string.place_distance_km, distance / 1000.0) : getString(R.string.place_distance_m, Math.round(distance));
            if (!PlaceMath.reliableDirection(distance, fix.getAccuracy(), data.accuracy(selected))) {
                clearDirection(); target.setText(getString(R.string.place_near, name, distanceText)); return;
            }
            if (compassStatus != R.string.compass_accuracy_ok || !Float.isFinite(heading)) {
                clearDirection(); target.setText(getString(R.string.place_compass_waiting, name, distanceText, getString(compassStatus))); return;
            }
            float altitude = fix.hasAltitude() && Double.isFinite(fix.getAltitude()) ? (float) fix.getAltitude() : 0f;
            GeomagneticField field = new GeomagneticField((float) fix.getLatitude(), (float) fix.getLongitude(), altitude, System.currentTimeMillis());
            int bearing = PlaceMath.magneticBearing(PlaceMath.trueBearing(fix.getLatitude(), fix.getLongitude(), data.latitude(selected), data.longitude(selected)), field.getDeclination());
            if (direction == null) {
                direction = new BearingGuidance(this, bearing, () -> {
                    if (foreground && haptics.isChecked() && hasWindowFocus()) target.performHapticFeedback(Build.VERSION.SDK_INT >= 30 ? HapticFeedbackConstants.CONFIRM : HapticFeedbackConstants.CLOCK_TICK);
                });
                speech.guidance(direction);
            } else direction.target(bearing);
            target.setText(getString(R.string.place_guidance, name, distanceText, bearing, direction.visualInstruction(heading), Math.round(fix.getAccuracy())));
            onSpeechChanged();
        } catch (JSONException invalid) { clearDirection(); target.setText(R.string.place_store_error); }
    }
    private void clearDirection() {
        if (direction == null) return;
        direction = null; speech.guidance(null);
        reading.setText("");
    }
    private EditText nameInput(String value) {
        EditText input = new EditText(this); input.setHint(R.string.place_name_hint); input.setSingleLine(true);
        input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(60)}); input.setText(value); return input;
    }
    private String clean(EditText input) { String value = input.getText().toString().trim(); return value.isEmpty() ? getString(R.string.place_default_name) : value; }
    private void toast(int id) { Toast.makeText(this, id, Toast.LENGTH_LONG).show(); }
    private TextView text(String value, float size, int color) { TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color); return t; }
    private Button button(String value) { Button b = new Button(this); b.setText(value); b.setTextSize(16); b.setAllCaps(false); b.setMinHeight(dp(54)); b.setTextColor(moon()); b.setBackgroundResource(R.drawable.button_dark); return b; }
    private LinearLayout.LayoutParams match() { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.topMargin = dp(8); return p; }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private int navy() { return Color.rgb(11, 19, 32); }
    private int moon() { return Color.rgb(238, 243, 247); }
    private int mist() { return Color.rgb(174, 187, 203); }
    private int gold() { return Color.rgb(240, 182, 91); }
}
