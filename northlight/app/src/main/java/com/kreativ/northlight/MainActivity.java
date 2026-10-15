package com.kreativ.northlight;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.net.Uri;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Switch;
import android.widget.Toast;
import org.json.JSONException;
import java.text.NumberFormat;

/** A deliberately small, offline orientation and emergency-light utility. */
public class MainActivity extends Activity implements CompassSource.Listener, BillingManager.Listener, CompassSpeech.Listener, AltimeterSource.Listener {
    private static final int CAMERA_PERMISSION = 9;
    private static final int ALTITUDE_PERMISSION = 10;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private CompassSource compassSource;
    private FieldCompassView compass;
    private BillingManager billing;
    private CompassSpeech speech;
    private TextView speechStatus, speechResult;
    private Switch autoSpeech;
    private Button pauseSpeech;
    private boolean renderingSpeech;
    private AltimeterSource altimeter;
    private TextView altitudeValue, altitudeSource, altitudeAccuracy, altitudeStatus;
    private Button altitudeStart, altitudeUnits;
    private TextView bearingText;
    private TextView directionText;
    private TextView sensorStatus;
    private TextView torchStatus;
    private TextView brightnessLabel;
    private Button lanternButton;
    private Button redButton;
    private Button sosButton;
    private LinearLayout root;
    private LinearLayout savedBearings;
    private TextView proLabel, proHint;
    private Button proButton;
    private CameraManager cameraManager;
    private String torchCameraId;
    private boolean torchOn;
    private boolean sosOn;
    private int torchMaxStrength = 1;
    private int selectedStrength = 1;
    private SeekBar brightness;
    private boolean ownsTorch;
    private final SignalLoop signal = new SignalLoop();
    private Runnable pendingCameraAction;
    private final CameraManager.TorchCallback torchCallback = new CameraManager.TorchCallback() {
        @Override public void onTorchModeChanged(String cameraId, boolean enabled) {
            if (!cameraId.equals(torchCameraId)) return;
            torchOn = enabled;
            if (lanternButton != null) updateLightButtons();
            if (torchStatus != null) torchStatus.setText(sosOn ? getString(R.string.sos_active) : getString(enabled ? R.string.lantern_on : R.string.lantern_ready));
        }

        @Override public void onTorchStrengthLevelChanged(String cameraId, int level) {
            if (!cameraId.equals(torchCameraId) || sosOn || !torchOn || brightness == null) return;
            selectedStrength = Math.max(1, Math.min(level, torchMaxStrength));
            brightness.setProgress(selectedStrength - 1);
            updateBrightnessLabel();
        }

        @Override public void onTorchModeUnavailable(String cameraId) {
            if (!cameraId.equals(torchCameraId) || torchStatus == null) return;
            torchOn = false;
            ownsTorch = false;
            abortSos();
            torchStatus.setText(R.string.lantern_busy);
            lanternButton.setText(R.string.lantern_turn_on);
            updateLightButtons();
        }
    };

    private final Runnable sosPulse = new Runnable() {
        @Override public void run() {
            if (!sosOn) return;
            int delay = signal.pulse(MainActivity.this::setTorch);
            if (delay >= 0 && sosOn) handler.postDelayed(this, delay);
        }
    };

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        compassSource = new CompassSource(this, this);
        speech = new CompassSpeech(this, this);
        altimeter = new AltimeterSource(this, this);
        billing = BillingManager.get(this);
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        findTorch();
        setContentView(buildScreen());
        cameraManager.registerTorchCallback(torchCallback, handler);
        updateHardwareCopy();
    }

    private View buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(11, 19, 32));
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(12), dp(20), dp(24));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.ic_northlight);
        logo.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(dp(36), dp(36));
        logoParams.setMarginEnd(dp(10));
        top.addView(logo, logoParams);
        TextView brand = text(getString(R.string.app_name), 19, moon());
        brand.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
        top.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));
        Button about = smallButton(getString(R.string.main_about));
        about.setMinHeight(dp(48));
        about.setOnClickListener(v -> startActivity(new Intent(this, AboutActivity.class)));
        top.addView(about);
        root.addView(top, matchWrap());

        addSpacer(8);
        compass = new FieldCompassView(this);
        root.addView(compass, new LinearLayout.LayoutParams(-1, -2));
        bearingText = compass.headingText();
        directionText = compass.directionText();
        Button accuracy = smallButton(getString(R.string.compass_waiting_accuracy));
        accuracy.setId(R.id.field_accuracy);
        accuracy.setTextSize(12);
        accuracy.setMinHeight(dp(48));
        accuracy.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle(R.string.main_calibrate_compass)
                .setMessage(R.string.main_move_your_phone_in_a_slow_figure_eight_away_from_metal)
                .setPositiveButton(R.string.close, null).show());
        sensorStatus = accuracy;
        root.addView(accuracy, matchWrap());
        addSpacer(16);

        LinearLayout lightTools = new LinearLayout(this);
        boolean stacked = getResources().getConfiguration().fontScale >= 1.3f
                || getResources().getConfiguration().screenWidthDp < 340;
        lightTools.setOrientation(stacked ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        lanternButton = toolButton(R.string.field_lantern, R.drawable.ic_lantern);
        lanternButton.setId(R.id.field_lantern);
        lanternButton.setOnClickListener(v -> runWithCameraPermission(() -> toggleLantern()));
        redButton = toolButton(R.string.field_night_light, R.drawable.ic_night_light);
        redButton.setId(R.id.field_night_light);
        redButton.setOnClickListener(v -> openNightLight());
        sosButton = toolButton(R.string.field_sos, R.drawable.ic_signal);
        sosButton.setId(R.id.field_sos);
        sosButton.setOnClickListener(v -> runWithCameraPermission(() -> toggleSos()));
        Button[] tools = {lanternButton, redButton, sosButton};
        for (int i = 0; i < tools.length; i++) {
            LinearLayout.LayoutParams params = stacked ? matchWrap() : new LinearLayout.LayoutParams(0, -2, 1);
            if (i > 0) { if (stacked) params.topMargin = dp(8); else params.setMarginStart(dp(8)); }
            lightTools.addView(tools[i], params);
        }
        root.addView(lightTools, matchWrap());
        torchStatus = text(getString(R.string.lantern_ready), 13, mist());
        torchStatus.setGravity(Gravity.CENTER);
        torchStatus.setPadding(0, dp(7), 0, 0);
        root.addView(torchStatus, matchWrap());
        if (torchMaxStrength > 1) addBrightnessControl();
        updateLightButtons();

        addAltimeterPanel();
        addSpeechControls();
        addSpacer(24);
        proLabel = text(getString(R.string.pro_brand), 12, gold());
        proLabel.setLetterSpacing(.13f);
        root.addView(proLabel);
        savedBearings = new LinearLayout(this);
        savedBearings.setId(R.id.field_saved_bearings);
        savedBearings.setOrientation(LinearLayout.VERTICAL);
        root.addView(savedBearings, matchWrap());
        proHint = text(getString(R.string.main_saved_bearings_an_offline_night_light_palette_and_a_str), 14, mist());
        proHint.setLineSpacing(dp(2), 1f);
        proHint.setPadding(0, dp(6), 0, dp(12));
        root.addView(proHint);
        proButton = actionButton(getString(R.string.main_explore_northlight_pro), false);
        proButton.setId(R.id.field_pro_tools);
        proButton.setOnClickListener(v -> startActivity(new Intent(this, ProActivity.class)));
        root.addView(proButton, matchWrap());

        addSpacer(22);
        TextView footer = text(getString(R.string.main_no_account_no_ads_no_tracking), 13, mist());
        footer.setGravity(Gravity.CENTER);
        root.addView(footer, matchWrap());
        TextView credit = text(getString(R.string.field_version_credit, BuildConfig.VERSION_NAME), 12, mist());
        credit.setId(R.id.field_version_credit);
        credit.setGravity(Gravity.CENTER);
        credit.setPadding(0, dp(6), 0, 0);
        root.addView(credit, matchWrap());
        onBillingChanged();
        return Ui.screen(this, scroll);
    }

    @Override protected void onResume() {
        super.onResume();
        compass.reset();
        speech.resume();
        altimeter.resume();
        compassSource.start();
        billing.addListener(this);
    }

    @Override protected void onPause() {
        super.onPause();
        altimeter.pause();
        speech.pause();
        compassSource.stop();
        billing.removeListener(this);
        if (sosOn) stopSos();
        if (ownsTorch) setTorch(false);
    }

    @Override protected void onDestroy() {
        cameraManager.unregisterTorchCallback(torchCallback);
        speech.close();
        altimeter.close();
        super.onDestroy();
    }

    @Override public void onHeading(float heading) { updateHeading(heading); speech.heading(heading); }
    @Override public void onCompassStatus(int message) {
        speech.accuracy(message);
        sensorStatus.setText(message);
        sensorStatus.setContentDescription(getString(R.string.field_accuracy_action, getString(message)));
        if (message == R.string.compass_unavailable) {
            compass.reset(); directionText.setText(R.string.field_no_sensor);
        }
    }

    private void addAltimeterPanel() {
        addSpacer(20);
        LinearLayout panel = new LinearLayout(this);
        panel.setId(R.id.altimeter_panel);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(16), dp(16), dp(16), dp(16));
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.rgb(17, 28, 44)); background.setCornerRadius(dp(20));
        background.setStroke(dp(1), Color.rgb(45, 62, 83));
        panel.setBackground(background);
        TextView title = text(getString(R.string.altimeter_title), 12, gold());
        title.setLetterSpacing(.1f);
        androidx.core.view.ViewCompat.setAccessibilityHeading(title, true);
        panel.addView(title, matchWrap());
        altitudeValue = text("", 36, moon());
        altitudeValue.setId(R.id.altimeter_value);
        altitudeValue.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
        altitudeValue.setPadding(0, dp(10), 0, dp(4));
        panel.addView(altitudeValue, matchWrap());
        altitudeSource = text("", 13, mist()); altitudeSource.setId(R.id.altimeter_source);
        panel.addView(altitudeSource, matchWrap());
        altitudeAccuracy = text("", 13, mist()); altitudeAccuracy.setId(R.id.altimeter_accuracy);
        altitudeAccuracy.setPadding(0, dp(5), 0, 0);
        panel.addView(altitudeAccuracy, matchWrap());
        altitudeStatus = text("", 14, mist()); altitudeStatus.setId(R.id.altimeter_status);
        altitudeStatus.setPadding(0, dp(8), 0, dp(12));
        panel.addView(altitudeStatus, matchWrap());
        altitudeStart = actionButton(getString(R.string.altimeter_start), false);
        altitudeStart.setId(R.id.altimeter_start);
        altitudeStart.setOnClickListener(v -> altitudeAction());
        panel.addView(altitudeStart, matchWrap());
        LinearLayout actions = new LinearLayout(this);
        boolean stacked = getResources().getConfiguration().fontScale >= 1.3f
                || getResources().getConfiguration().screenWidthDp < 340;
        actions.setOrientation(stacked ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        altitudeUnits = smallButton(""); altitudeUnits.setId(R.id.altimeter_units);
        altitudeUnits.setOnClickListener(v -> {
            String[] choices = getResources().getStringArray(R.array.altimeter_units);
            new AlertDialog.Builder(this).setTitle(R.string.altimeter_units_title)
                    .setSingleChoiceItems(choices, altitudeFeet() ? 1 : 0, (dialog, which) -> {
                        getSharedPreferences("altimeter", MODE_PRIVATE).edit().putBoolean("feet", which == 1).apply();
                        onAltitudeChanged(); dialog.dismiss();
                    }).setNegativeButton(R.string.close, null).show();
        });
        LinearLayout.LayoutParams unitParams = stacked ? matchWrap() : new LinearLayout.LayoutParams(0, -2, 1);
        unitParams.topMargin = dp(8); actions.addView(altitudeUnits, unitParams);
        Button help = smallButton(getString(R.string.altimeter_help)); help.setId(R.id.altimeter_help);
        help.setOnClickListener(v -> new AlertDialog.Builder(this).setTitle(R.string.altimeter_help)
                .setMessage(R.string.altimeter_help_body).setPositiveButton(R.string.close, null).show());
        LinearLayout.LayoutParams helpParams = stacked ? matchWrap() : new LinearLayout.LayoutParams(0, -2, 1);
        helpParams.topMargin = dp(8); if (!stacked) helpParams.setMarginStart(dp(8));
        actions.addView(help, helpParams); panel.addView(actions, matchWrap());
        root.addView(panel, matchWrap());
        onAltitudeChanged();
    }

    private boolean altitudeFeet() { return getSharedPreferences("altimeter", MODE_PRIVATE).getBoolean("feet", false); }

    @Override public void onAltitudeChanged() {
        if (altitudeValue == null || altitudeUnits == null) return;
        boolean feet = altitudeFeet(), hasReading = altimeter.hasReading();
        double factor = feet ? 3.280839895013123 : 1;
        String unit = getString(feet ? R.string.altimeter_unit_ft : R.string.altimeter_unit_m);
        String unitName = getResources().getStringArray(R.array.altimeter_units)[feet ? 1 : 0];
        NumberFormat format = NumberFormat.getIntegerInstance(getResources().getConfiguration().getLocales().get(0));
        String value = hasReading ? format.format(Math.round(altimeter.meters() * factor)) : getString(R.string.altimeter_empty);
        setCopy(altitudeValue, getString(R.string.altimeter_reading, value, unit));
        altitudeValue.setContentDescription(hasReading ? getString(R.string.altimeter_accessible, value, unitName) : getString(R.string.altimeter_no_reading));
        setCopy(altitudeSource, getString(!hasReading ? R.string.altimeter_source_pending
                : altimeter.seaLevel() ? R.string.altimeter_source_msl : R.string.altimeter_source_wgs84));
        altitudeAccuracy.setVisibility(hasReading ? View.VISIBLE : View.GONE);
        setCopy(altitudeAccuracy, hasReading && Double.isFinite(altimeter.accuracy())
                ? getString(R.string.altimeter_accuracy, format.format(Math.max(1L, (long)Math.ceil(altimeter.accuracy() * factor))), unit)
                : getString(R.string.altimeter_accuracy_unknown));
        int status;
        switch (altimeter.state()) {
            case ALLOWED: status = R.string.altimeter_allowed; break;
            case WAITING: status = R.string.altimeter_waiting; break;
            case SLOW_FIX: status = R.string.altimeter_slow_fix; break;
            case READY: status = !Double.isFinite(altimeter.accuracy()) ? R.string.altimeter_ready_unknown
                    : altimeter.accuracy() > 30 ? R.string.altimeter_low_accuracy : R.string.altimeter_ready; break;
            case NO_ALTITUDE: status = R.string.altimeter_no_altitude; break;
            case STALE: status = R.string.altimeter_stale; break;
            case APPROXIMATE: status = R.string.altimeter_approximate; break;
            case DENIED: status = R.string.altimeter_denied; break;
            case GPS_OFF: status = R.string.altimeter_gps_off; break;
            case NO_GPS: status = R.string.altimeter_no_gps; break;
            case ERROR: status = R.string.altimeter_error; break;
            case PAUSED: status = R.string.altimeter_paused; break;
            case SIMULATED: status = R.string.altimeter_simulated; break;
            default: status = R.string.altimeter_intro;
        }
        setCopy(altitudeStatus, getString(status));
        setCopy(altitudeStart, getString(altimeter.active() ? R.string.altimeter_stop
                : altimeter.state() == AltimeterSource.State.GPS_OFF ? R.string.altimeter_location_settings : R.string.altimeter_start));
        setCopy(altitudeUnits, getString(R.string.altimeter_units_button, unit));
    }

    private void setCopy(TextView view, String value) { if (!value.contentEquals(view.getText())) view.setText(value); }

    private void altitudeAction() {
        if (altimeter.active()) { altimeter.stop(); return; }
        if (altimeter.state() == AltimeterSource.State.GPS_OFF) {
            openAltitudeSettings(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)); return;
        }
        if (!altimeter.hasGps() || checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            altimeter.start(); return;
        }
        boolean settingsRequired = getSharedPreferences("altimeter", MODE_PRIVATE).getBoolean("asked_location", false)
                && !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION);
        new AlertDialog.Builder(this).setTitle(R.string.altimeter_enable_title).setMessage(R.string.altimeter_permission_body)
                .setPositiveButton(settingsRequired ? R.string.altimeter_app_settings : R.string.altimeter_allow_location, (dialog, which) -> {
                    if (settingsRequired) openAltitudeSettings(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", getPackageName(), null)));
                    else {
                        getSharedPreferences("altimeter", MODE_PRIVATE).edit().putBoolean("asked_location", true).apply();
                        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, ALTITUDE_PERMISSION);
                    }
                }).setNegativeButton(R.string.cancel, null).show();
    }

    private void openAltitudeSettings(Intent intent) {
        try { startActivity(intent); }
        catch (ActivityNotFoundException | SecurityException unavailable) { Toast.makeText(this, R.string.altimeter_settings_unavailable, Toast.LENGTH_LONG).show(); }
    }

    private void addSpeechControls() {
        addSpacer(24);
        TextView title = text(getString(R.string.speech_title), 12, gold());
        title.setLetterSpacing(.1f);
        androidx.core.view.ViewCompat.setAccessibilityHeading(title, true);
        root.addView(title, matchWrap());
        speechStatus = text(getString(R.string.speech_intro), 14, mist());
        speechStatus.setId(R.id.speech_status);
        speechStatus.setPadding(0, dp(8), 0, dp(12));
        root.addView(speechStatus, matchWrap());
        Button speak = actionButton(getString(R.string.speech_speak_direction), false);
        speak.setId(R.id.speech_speak);
        speak.setOnClickListener(v -> speech.speakOnce());
        root.addView(speak, matchWrap());
        autoSpeech = new Switch(this);
        autoSpeech.setId(R.id.speech_auto);
        autoSpeech.setText(R.string.speech_auto);
        autoSpeech.setTextColor(moon()); autoSpeech.setTextSize(16);
        autoSpeech.setMinHeight(dp(56)); autoSpeech.setSwitchPadding(dp(12));
        autoSpeech.setOnCheckedChangeListener((button, checked) -> { if (!renderingSpeech) speech.automatic(checked); });
        root.addView(autoSpeech, matchWrap());
        LinearLayout actions = new LinearLayout(this);
        boolean stacked = getResources().getConfiguration().fontScale >= 1.3f
                || getResources().getConfiguration().screenWidthDp < 340;
        actions.setOrientation(stacked ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        pauseSpeech = smallButton(getString(R.string.speech_pause));
        pauseSpeech.setId(R.id.speech_pause);
        pauseSpeech.setOnClickListener(v -> speech.pauseSpeech());
        actions.addView(pauseSpeech, stacked ? matchWrap() : new LinearLayout.LayoutParams(0, -2, 1));
        Button settings = smallButton(getString(R.string.speech_settings));
        settings.setId(R.id.speech_settings);
        settings.setOnClickListener(v -> showSpeechSettings());
        LinearLayout.LayoutParams params = stacked ? matchWrap() : new LinearLayout.LayoutParams(0, -2, 1);
        if (stacked) params.topMargin = dp(8); else params.setMarginStart(dp(8));
        actions.addView(settings, params);
        root.addView(actions, matchWrap());
        speechResult = text("", 15, moon());
        speechResult.setId(R.id.speech_result);
        speechResult.setPadding(0, dp(8), 0, 0);
        root.addView(speechResult, matchWrap());
        TextView scope = text(getString(R.string.speech_scope), 12, mist());
        scope.setPadding(0, dp(6), 0, 0);
        root.addView(scope, matchWrap());
        onSpeechChanged();
    }

    @Override public void onSpeechChanged() {
        if (speechStatus == null || autoSpeech == null || pauseSpeech == null) return;
        speechStatus.setText(speech.status());
        renderingSpeech = true;
        autoSpeech.setChecked(speech.automatic());
        autoSpeech.setEnabled(!speech.screenReader());
        renderingSpeech = false;
        pauseSpeech.setEnabled(speech.canPause());
        pauseSpeech.setAlpha(speech.canPause() ? 1f : .45f);
    }

    @Override public void onSpeechReading(String value, boolean throughScreenReader) {
        // Only explicit TalkBack requests update a polite live region. Sensor ticks never announce.
        speechResult.setAccessibilityLiveRegion(throughScreenReader ? View.ACCESSIBILITY_LIVE_REGION_POLITE : View.ACCESSIBILITY_LIVE_REGION_NONE);
        speechResult.setText("");
        speechResult.setText(value);
    }

    private void showSpeechSettings() {
        speech.pauseSpeech();
        String[] frequency = getResources().getStringArray(R.array.speech_frequencies);
        String[] rates = getResources().getStringArray(R.array.speech_rates);
        String[] choices = {
                getString(R.string.speech_frequency, frequency[speech.intervalIndex()]),
                getString(R.string.speech_speed, rates[speech.rateIndex()]),
                getString(R.string.speech_voice_data), getString(R.string.speech_retry), getString(R.string.speech_limits)};
        new AlertDialog.Builder(this).setTitle(R.string.speech_settings).setItems(choices, (dialog, which) -> {
            if (which == 0) new AlertDialog.Builder(this).setTitle(R.string.speech_frequency_title)
                    .setSingleChoiceItems(frequency, speech.intervalIndex(), (picker, index) -> { speech.intervalIndex(index); picker.dismiss(); })
                    .setNegativeButton(R.string.close, null).show();
            else if (which == 1) new AlertDialog.Builder(this).setTitle(R.string.speech_speed_title)
                    .setSingleChoiceItems(rates, speech.rateIndex(), (picker, index) -> { speech.rateIndex(index); picker.dismiss(); })
                    .setNegativeButton(R.string.close, null).show();
            else if (which == 2) openVoiceData();
            else if (which == 3) speech.retryVoice();
            else new AlertDialog.Builder(this).setTitle(R.string.speech_limits).setMessage(R.string.speech_limits_body)
                    .setPositiveButton(R.string.close, null).show();
        }).setNegativeButton(R.string.close, null).show();
    }

    private void openVoiceData() {
        // No downloads in Northlight. The user chooses voice installation in their engine's settings.
        try { startActivity(new Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)); }
        catch (ActivityNotFoundException | SecurityException unavailable) {
            try { startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)); }
            catch (ActivityNotFoundException | SecurityException missing) { Toast.makeText(this, R.string.speech_setup_unavailable, Toast.LENGTH_LONG).show(); }
        }
    }

    @Override public void onBillingChanged() {
        if (savedBearings == null) return;
        boolean owned = billing.entitled();
        proLabel.setText(owned ? R.string.saved_bearings : R.string.pro_brand);
        proHint.setVisibility(owned ? View.GONE : View.VISIBLE);
        proButton.setText(owned ? R.string.field_all_pro_tools : R.string.main_explore_northlight_pro);
        updateLightButtons();
        savedBearings.removeAllViews();
        savedBearings.setVisibility(owned ? View.VISIBLE : View.GONE);
        if (!owned) return;
        try {
            BearingData data = new BearingData(getSharedPreferences(BillingManager.PREFS, MODE_PRIVATE).getString("saved_bearings", "[]"));
            if (data.size() == 0) {
                TextView empty = text(getString(R.string.field_bearings_empty), 14, mist());
                empty.setPadding(0, dp(10), 0, dp(12)); savedBearings.addView(empty, matchWrap());
            }
            for (int i = data.size() - 1; i >= 0; i--) {
                final int index = i;
                Button bearing = actionButton(getString(R.string.bearing_row, data.degrees(i), data.name(i)), false);
                bearing.setContentDescription(getString(R.string.field_follow_bearing, data.name(i), data.degrees(i)));
                bearing.setOnClickListener(v -> startActivity(new Intent(this, ProActivity.class).putExtra(ProActivity.EXTRA_BEARING_INDEX, index)));
                LinearLayout.LayoutParams params = matchWrap(); params.topMargin = dp(8); params.bottomMargin = dp(8);
                savedBearings.addView(bearing, params);
            }
        } catch (JSONException error) {
            savedBearings.addView(text(getString(R.string.bearings_read_error), 14, mist()), matchWrap());
        }
    }

    private void openNightLight() {
        if (!billing.entitled()) { startActivity(new Intent(this, RedLightActivity.class)); return; }
        new AlertDialog.Builder(this).setTitle(R.string.field_night_light)
                .setItems(new String[]{getString(R.string.red), getString(R.string.amber), getString(R.string.green)}, (dialog, which) -> {
                    String mode = new String[]{"red", "amber", "green"}[which];
                    startActivity(new Intent(this, RedLightActivity.class).putExtra(RedLightActivity.EXTRA_MODE, mode));
                }).setNegativeButton(R.string.cancel, null).show();
    }

    private void updateHeading(float heading) {
        if (compass == null) return;
        compass.setHeading(heading);
        int rounded = HeadingMath.rounded(heading);
        bearingText.setText(getString(R.string.bearing_degrees, rounded));
        directionText.setText(directionFor(rounded));
    }

    private String directionFor(int degrees) {
        String[] labels = getResources().getStringArray(R.array.compass_directions);
        return labels[((degrees + 22) % 360) / 45];
    }

    private void findTorch() {
        try {
            for (String id : cameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
                Boolean flash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                if (Boolean.TRUE.equals(flash)) {
                    torchCameraId = id;
                    if (android.os.Build.VERSION.SDK_INT >= 33) {
                        Integer max = characteristics.get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL);
                        if (max != null) torchMaxStrength = Math.max(1, max);
                        Integer initial = characteristics.get(CameraCharacteristics.FLASH_INFO_STRENGTH_DEFAULT_LEVEL);
                        selectedStrength = initial == null ? 1 : initial;
                    }
                    selectedStrength = Math.max(1, Math.min(torchMaxStrength, getPreferences(MODE_PRIVATE).getInt("torch_strength", selectedStrength)));
                    return;
                }
            }
        } catch (Exception ignored) { }
    }

    private void updateHardwareCopy() {
        if (!compassSource.available()) {
            sensorStatus.setText(getString(R.string.compass_unavailable));
            bearingText.setText(getString(R.string.main));
            directionText.setText(R.string.field_no_sensor);
        }
        if (torchCameraId == null) {
            torchStatus.setText(R.string.main_no_camera_flash_available);
            lanternButton.setEnabled(false);
            lanternButton.setText(getString(R.string.main_no_camera_flash_available));
            sosButton.setEnabled(false);
            sosButton.setText(getString(R.string.main_sos_unavailable_without_flash));
        }
        updateLightButtons();
    }

    private void runWithCameraPermission(Runnable action) {
        if (torchCameraId == null) return;
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            action.run();
        } else {
            pendingCameraAction = action;
            requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION);
        }
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == CAMERA_PERMISSION) {
            if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED && pendingCameraAction != null) pendingCameraAction.run();
            else Toast.makeText(this, getString(R.string.main_camera_access_is_needed_only_to_power_the_led_lantern), Toast.LENGTH_LONG).show();
            pendingCameraAction = null;
        }
        if (requestCode == ALTITUDE_PERMISSION) altimeter.permissionResult();
    }

    private void toggleLantern() {
        if (sosOn) stopSos();
        setTorch(!torchOn);
        lanternButton.setText(torchOn ? getString(R.string.main_turn_off_lantern) : getString(R.string.lantern_turn_on));
        updateLightButtons();
    }

    private boolean setTorch(boolean on) {
        if (torchCameraId == null) { abortSos(); return false; }
        try {
            if (on && android.os.Build.VERSION.SDK_INT >= 33 && torchMaxStrength > 1) cameraManager.turnOnTorchWithStrengthLevel(torchCameraId, selectedStrength);
            else cameraManager.setTorchMode(torchCameraId, on);
            ownsTorch = on;
            torchOn = on;
            if (!sosOn) lanternButton.setText(on ? getString(R.string.main_turn_off_lantern) : getString(R.string.lantern_turn_on));
            if (torchStatus != null) torchStatus.setText(sosOn ? getString(R.string.sos_active) : getString(on ? R.string.lantern_on : R.string.lantern_ready));
            updateLightButtons();
            return true;
        } catch (Exception e) {
            torchOn = false;
            ownsTorch = false;
            abortSos();
            lanternButton.setText(R.string.lantern_turn_on);
            if (torchStatus != null) torchStatus.setText(R.string.lantern_unavailable);
            Toast.makeText(this, R.string.lantern_error, Toast.LENGTH_SHORT).show();
            updateLightButtons();
            return false;
        }
    }

    private void addBrightnessControl() {
        brightnessLabel = text(getString(R.string.main_lantern_brightness), 13, mist());
        brightnessLabel.setPadding(0, dp(8), 0, 0);
        root.addView(brightnessLabel, matchWrap());
        brightness = new SeekBar(this);
        brightness.setMax(torchMaxStrength - 1);
        brightness.setProgress(selectedStrength - 1);
        brightness.setContentDescription(getString(R.string.main_lantern_brightness));
        brightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                if (fromUser && !sosOn) {
                    selectedStrength = progress + 1;
                    getPreferences(MODE_PRIVATE).edit().putInt("torch_strength", selectedStrength).apply();
                    if (torchOn) runWithCameraPermission(() -> setTorchStrength(selectedStrength));
                }
                updateBrightnessLabel();
            }
            @Override public void onStartTrackingTouch(SeekBar bar) { }
            @Override public void onStopTrackingTouch(SeekBar bar) { }
        });
        root.addView(brightness, matchWrap());
        updateBrightnessLabel();
    }

    private void setTorchStrength(int level) {
        if (android.os.Build.VERSION.SDK_INT < 33 || torchCameraId == null) return;
        try {
            cameraManager.turnOnTorchWithStrengthLevel(torchCameraId, Math.max(1, Math.min(level, torchMaxStrength)));
            torchOn = true;
            ownsTorch = true;
        } catch (Exception e) {
            try {
                selectedStrength = cameraManager.getTorchStrengthLevel(torchCameraId);
                brightness.setProgress(selectedStrength - 1);
                updateBrightnessLabel();
            } catch (Exception ignored) { }
            Toast.makeText(this, getString(R.string.main_this_phone_could_not_change_lantern_brightness), Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleSos() {
        if (sosOn) { stopSos(); return; }
        if (torchOn) setTorch(false);
        sosOn = true;
        signal.start();
        if (brightness != null) brightness.setEnabled(false);
        sosButton.setText(getString(R.string.main_stop_sos_signal));
        updateLightButtons();
        handler.post(sosPulse);
    }

    private void stopSos() {
        abortSos();
        setTorch(false);
    }

    private void abortSos() {
        sosOn = false;
        signal.stop();
        handler.removeCallbacks(sosPulse);
        if (sosButton != null) sosButton.setText(R.string.sos_signal);
        if (brightness != null) brightness.setEnabled(true);
        updateLightButtons();
    }

    private void updateBrightnessLabel() {
        if (brightnessLabel != null) brightnessLabel.setText(getString(R.string.lantern_brightness_level, selectedStrength, torchMaxStrength));
    }

    private TextView text(String value, float size, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        return view;
    }

    private Button toolButton(int label, int icon) {
        Button button = actionButton(getString(label), false);
        button.setTextSize(13); button.setMinWidth(0); button.setMinimumWidth(0);
        button.setMinHeight(dp(96));
        button.setPadding(dp(6), dp(14), dp(6), dp(14));
        Drawable drawable = getDrawable(icon).mutate(); drawable.setBounds(0, 0, dp(22), dp(22));
        button.setCompoundDrawables(null, drawable, null, null); button.setCompoundDrawablePadding(dp(8));
        return button;
    }

    private void updateLightButtons() {
        if (lanternButton == null || sosButton == null) return;
        styleTool(lanternButton, torchOn && !sosOn);
        styleTool(sosButton, sosOn);
        lanternButton.setText(torchOn && !sosOn ? R.string.field_lantern_on : R.string.field_lantern);
        sosButton.setText(sosOn ? R.string.field_stop_sos : R.string.field_sos);
        lanternButton.setContentDescription(getString(torchCameraId == null ? R.string.main_no_camera_flash_available
                : torchOn && !sosOn ? R.string.main_turn_off_lantern : R.string.lantern_turn_on));
        sosButton.setContentDescription(getString(torchCameraId == null ? R.string.main_sos_unavailable_without_flash
                : sosOn ? R.string.main_stop_sos_signal : R.string.sos_signal));
        redButton.setContentDescription(getString(billing.entitled() ? R.string.field_night_light : R.string.main_red_screen_light));
    }

    private void styleTool(Button button, boolean active) {
        if (button.isActivated() == active) return;
        button.setActivated(active);
        button.setBackgroundResource(active ? R.drawable.button_gold : R.drawable.button_dark);
        button.setPadding(dp(6), dp(14), dp(6), dp(14));
        button.setTextColor(active ? getColor(R.color.night) : moon());
        Drawable icon = button.getCompoundDrawables()[1];
        if (icon != null) icon.setTint(active ? getColor(R.color.night) : gold());
    }

    private Button actionButton(String label, boolean gold) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(16);
        button.setAllCaps(false);
        button.setMinHeight(dp(54));
        button.setTextColor(gold ? Color.rgb(11, 19, 32) : moon());
        button.setBackgroundResource(gold ? R.drawable.button_gold : R.drawable.button_dark);
        return button;
    }

    private Button smallButton(String label) {
        Button button = actionButton(label, false);
        button.setTextSize(13);
        button.setMinHeight(dp(48));
        return button;
    }

    private LinearLayout.LayoutParams matchWrap() { return new LinearLayout.LayoutParams(-1, -2); }
    private LinearLayout.LayoutParams centerWrap() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, -2);
        params.gravity = Gravity.CENTER_HORIZONTAL;
        return params;
    }
    private void addSpacer(int height) { root.addView(new View(this), new LinearLayout.LayoutParams(1, dp(height))); }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private int moon() { return Color.rgb(238, 243, 247); }
    private int mist() { return getColor(R.color.mist); }
    private int gold() { return Color.rgb(240, 182, 91); }

}
