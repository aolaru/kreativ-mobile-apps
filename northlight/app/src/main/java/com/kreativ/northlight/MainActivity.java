package com.kreativ.northlight;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

/** A deliberately small, offline orientation and emergency-light utility. */
public class MainActivity extends Activity implements CompassSource.Listener {
    private static final int CAMERA_PERMISSION = 9;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private CompassSource compassSource;
    private CompassView compass;
    private TextView bearingText;
    private TextView directionText;
    private TextView sensorStatus;
    private TextView torchStatus;
    private TextView brightnessLabel;
    private Button lanternButton;
    private Button redButton;
    private Button sosButton;
    private LinearLayout root;
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
            if (!sosOn && lanternButton != null) lanternButton.setText(enabled ? getString(R.string.main_turn_off_lantern) : getString(R.string.lantern_turn_on));
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
        root.setPadding(dp(24), dp(26), dp(24), dp(28));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand = text(getString(R.string.main_northlight), 12, gold());
        brand.setLetterSpacing(.16f);
        top.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));
        Button about = smallButton(getString(R.string.main_about));
        about.setOnClickListener(v -> startActivity(new Intent(this, AboutActivity.class)));
        top.addView(about);
        root.addView(top, matchWrap());

        TextView title = text(getString(R.string.main_find_north_make_light), 32, moon());
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setPadding(0, dp(15), 0, dp(5));
        root.addView(title);
        TextView subtitle = text(getString(R.string.main_a_private_offline_compass_and_lantern), 16, mist());
        root.addView(subtitle);

        addSpacer(18);
        compass = new CompassView(this);
        compass.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        root.addView(compass, new LinearLayout.LayoutParams(-1, dp(282)));
        bearingText = text(getString(R.string.main), 44, moon());
        bearingText.setGravity(Gravity.CENTER);
        bearingText.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        root.addView(bearingText, matchWrap());
        directionText = text(getString(R.string.main_waiting_for_compass), 16, gold());
        directionText.setGravity(Gravity.CENTER);
        directionText.setPadding(0, 0, 0, dp(8));
        root.addView(directionText, matchWrap());
        sensorStatus = text(getString(R.string.main_magnetic_north_works_offline), 13, mist());
        sensorStatus.setGravity(Gravity.CENTER);
        sensorStatus.setBackgroundResource(R.drawable.pill);
        root.addView(sensorStatus, centerWrap());
        Button calibrate = smallButton(getString(R.string.main_calibrate_compass));
        calibrate.setTextSize(14);
        calibrate.setOnClickListener(v -> Toast.makeText(this, getString(R.string.main_move_your_phone_in_a_slow_figure_eight_away_from_metal), Toast.LENGTH_LONG).show());
        LinearLayout.LayoutParams calibrateParams = centerWrap();
        calibrateParams.topMargin = dp(8);
        root.addView(calibrate, calibrateParams);

        addSpacer(24);
        TextView lightLabel = text(getString(R.string.main_light), 12, gold());
        lightLabel.setLetterSpacing(.13f);
        root.addView(lightLabel);
        TextView lightHint = text(getString(R.string.main_use_your_camera_flash_an_adjustable_red_screen_light_or), 15, mist());
        lightHint.setLineSpacing(dp(2), 1f);
        lightHint.setPadding(0, dp(6), 0, dp(12));
        root.addView(lightHint);

        lanternButton = actionButton(getString(R.string.lantern_turn_on), true);
        lanternButton.setOnClickListener(v -> runWithCameraPermission(() -> toggleLantern()));
        root.addView(lanternButton, matchWrap());
        torchStatus = text(getString(R.string.lantern_ready), 13, mist());
        torchStatus.setGravity(Gravity.CENTER);
        torchStatus.setPadding(0, dp(7), 0, 0);
        root.addView(torchStatus, matchWrap());
        if (torchMaxStrength > 1) addBrightnessControl();
        addSpacer(9);
        redButton = actionButton(getString(R.string.main_red_screen_light), false);
        redButton.setOnClickListener(v -> startActivity(new Intent(this, RedLightActivity.class)));
        root.addView(redButton, matchWrap());
        addSpacer(9);
        sosButton = actionButton(getString(R.string.sos_signal), false);
        sosButton.setOnClickListener(v -> runWithCameraPermission(() -> toggleSos()));
        root.addView(sosButton, matchWrap());

        addSpacer(24);
        TextView proLabel = text(getString(R.string.pro_brand), 12, gold());
        proLabel.setLetterSpacing(.13f);
        root.addView(proLabel);
        TextView proHint = text(getString(R.string.main_saved_bearings_an_offline_night_light_palette_and_a_str), 15, mist());
        proHint.setLineSpacing(dp(2), 1f);
        proHint.setPadding(0, dp(6), 0, dp(12));
        root.addView(proHint);
        Button proButton = actionButton(getString(R.string.main_explore_northlight_pro), false);
        proButton.setOnClickListener(v -> startActivity(new Intent(this, ProActivity.class)));
        root.addView(proButton, matchWrap());

        addSpacer(22);
        TextView footer = text(getString(R.string.main_no_account_no_ads_no_tracking), 13, mist());
        footer.setGravity(Gravity.CENTER);
        root.addView(footer, matchWrap());
        return Ui.screen(this, scroll);
    }

    @Override protected void onResume() {
        super.onResume();
        compassSource.start();
    }

    @Override protected void onPause() {
        super.onPause();
        compassSource.stop();
        if (sosOn) stopSos();
        if (ownsTorch) setTorch(false);
    }

    @Override protected void onDestroy() {
        cameraManager.unregisterTorchCallback(torchCallback);
        super.onDestroy();
    }

    @Override public void onHeading(float heading) { updateHeading(heading); }
    @Override public void onCompassStatus(int message) { sensorStatus.setText(message); }

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
            directionText.setText(getString(R.string.main_compass_unavailable));
        }
        if (torchCameraId == null) {
            lanternButton.setEnabled(false);
            lanternButton.setText(getString(R.string.main_no_camera_flash_available));
            sosButton.setEnabled(false);
            sosButton.setText(getString(R.string.main_sos_unavailable_without_flash));
        }
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
    }

    private void toggleLantern() {
        if (sosOn) stopSos();
        setTorch(!torchOn);
        lanternButton.setText(torchOn ? getString(R.string.main_turn_off_lantern) : getString(R.string.lantern_turn_on));
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
            return true;
        } catch (Exception e) {
            torchOn = false;
            ownsTorch = false;
            abortSos();
            lanternButton.setText(R.string.lantern_turn_on);
            if (torchStatus != null) torchStatus.setText(R.string.lantern_unavailable);
            Toast.makeText(this, R.string.lantern_error, Toast.LENGTH_SHORT).show();
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
        button.setMinHeight(dp(38));
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

    private static class CompassView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path needle = new Path();
        private final String[] marks = {"N", "E", "S", "W"};
        private float heading;
        CompassView(Context context) { super(context); paint.setTypeface(android.graphics.Typeface.create("sans", android.graphics.Typeface.BOLD)); }
        void setHeading(float value) { heading = value; invalidate(); }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float radius = Math.min(getWidth(), getHeight()) / 2f - dp(16);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(22, 34, 56));
            canvas.drawCircle(cx, cy, radius, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(1));
            paint.setColor(Color.rgb(45, 60, 83));
            canvas.drawCircle(cx, cy, radius, paint);
            canvas.save();
            canvas.rotate(-heading, cx, cy);
            for (int i = 0; i < 72; i++) {
                float angle = i * 5f;
                double radians = Math.toRadians(angle - 90);
                boolean cardinal = i % 18 == 0;
                boolean major = i % 9 == 0;
                float outerX = cx + (float) Math.cos(radians) * (radius - dp(9));
                float outerY = cy + (float) Math.sin(radians) * (radius - dp(9));
                float tick = cardinal ? dp(17) : (major ? dp(11) : dp(6));
                float innerX = cx + (float) Math.cos(radians) * (radius - dp(9) - tick);
                float innerY = cy + (float) Math.sin(radians) * (radius - dp(9) - tick);
                paint.setStrokeWidth(cardinal ? dp(2) : dp(1));
                paint.setColor(cardinal ? Color.rgb(238, 243, 247) : Color.rgb(104, 122, 147));
                canvas.drawLine(innerX, innerY, outerX, outerY, paint);
            }
            for (int i = 0; i < 4; i++) {
                double radians = Math.toRadians(i * 90 - 90);
                float x = cx + (float) Math.cos(radians) * (radius - dp(39));
                float y = cy + (float) Math.sin(radians) * (radius - dp(39)) + dp(6);
                paint.setTextSize(dp(17));
                paint.setTextAlign(Paint.Align.CENTER);
                paint.setColor(i == 0 ? Color.rgb(240, 182, 91) : Color.rgb(238, 243, 247));
                canvas.drawText(marks[i], x, y, paint);
            }
            canvas.restore();
            needle.reset();
            needle.moveTo(cx, cy - radius + dp(50));
            needle.lineTo(cx - dp(10), cy + dp(13));
            needle.lineTo(cx + dp(10), cy + dp(13));
            needle.close();
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(240, 182, 91));
            canvas.drawPath(needle, paint);
            paint.setColor(Color.rgb(238, 243, 247));
            canvas.drawCircle(cx, cy, dp(6), paint);
        }
        private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    }
}
