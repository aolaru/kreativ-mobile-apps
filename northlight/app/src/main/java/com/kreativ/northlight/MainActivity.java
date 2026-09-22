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
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

/** A deliberately small, offline orientation and emergency-light utility. */
public class MainActivity extends Activity implements SensorEventListener {
    private static final int CAMERA_PERMISSION = 9;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private SensorManager sensors;
    private Sensor rotationSensor;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private final float[] gravity = new float[3];
    private final float[] geomagnetic = new float[3];
    private boolean hasGravity;
    private boolean hasGeomagnetic;
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
    private Runnable pendingCameraAction;
    private int sosIndex;
    private final int[] sosPattern = {220, 220, 220, 220, 220, 220, 700, 220, 700, 220, 700, 220, 220, 220, 220, 220, 220, 1100};
    private final CameraManager.TorchCallback torchCallback = new CameraManager.TorchCallback() {
        @Override public void onTorchModeChanged(String cameraId, boolean enabled) {
            if (!cameraId.equals(torchCameraId)) return;
            torchOn = enabled;
            if (!sosOn && lanternButton != null) lanternButton.setText(enabled ? "Turn off lantern" : "Turn on lantern");
            if (torchStatus != null) torchStatus.setText(enabled ? "Lantern on" : "Lantern ready");
        }

        @Override public void onTorchModeUnavailable(String cameraId) {
            if (!cameraId.equals(torchCameraId) || torchStatus == null) return;
            torchOn = false;
            torchStatus.setText("Lantern is in use by another camera app");
        }
    };

    private final Runnable sosPulse = new Runnable() {
        @Override public void run() {
            if (!sosOn) return;
            boolean lightOn = sosIndex % 2 == 0;
            setTorch(lightOn);
            int delay = sosPattern[sosIndex];
            sosIndex = (sosIndex + 1) % sosPattern.length;
            handler.postDelayed(this, delay);
        }
    };

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        sensors = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        rotationSensor = sensors.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        accelerometer = sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensors.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
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
        scroll.addView(root, new ScrollView.LayoutParams(-1, -1));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand = text("NORTHLIGHT", 12, gold());
        brand.setLetterSpacing(.16f);
        top.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));
        Button about = smallButton("About");
        about.setOnClickListener(v -> startActivity(new Intent(this, AboutActivity.class)));
        top.addView(about);
        root.addView(top, matchWrap());

        TextView title = text("Find north.\nMake light.", 32, moon());
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setPadding(0, dp(15), 0, dp(5));
        root.addView(title);
        TextView subtitle = text("A private, offline compass and lantern.", 16, mist());
        root.addView(subtitle);

        addSpacer(18);
        compass = new CompassView(this);
        root.addView(compass, new LinearLayout.LayoutParams(-1, dp(282)));
        bearingText = text("—°", 44, moon());
        bearingText.setGravity(Gravity.CENTER);
        bearingText.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        root.addView(bearingText, matchWrap());
        directionText = text("Waiting for compass", 16, gold());
        directionText.setGravity(Gravity.CENTER);
        directionText.setPadding(0, 0, 0, dp(8));
        root.addView(directionText, matchWrap());
        sensorStatus = text("Magnetic north · works without internet", 13, mist());
        sensorStatus.setGravity(Gravity.CENTER);
        sensorStatus.setBackgroundResource(R.drawable.pill);
        root.addView(sensorStatus, centerWrap());
        Button calibrate = smallButton("Calibrate compass");
        calibrate.setTextSize(14);
        calibrate.setOnClickListener(v -> Toast.makeText(this, "Move your phone in a slow figure-eight, away from metal or magnets.", Toast.LENGTH_LONG).show());
        LinearLayout.LayoutParams calibrateParams = centerWrap();
        calibrateParams.topMargin = dp(8);
        root.addView(calibrate, calibrateParams);

        addSpacer(24);
        TextView lightLabel = text("LIGHT", 12, gold());
        lightLabel.setLetterSpacing(.13f);
        root.addView(lightLabel);
        TextView lightHint = text("Use your camera flash, a night-safe red screen, or a repeating SOS signal.", 15, mist());
        lightHint.setLineSpacing(dp(2), 1f);
        lightHint.setPadding(0, dp(6), 0, dp(12));
        root.addView(lightHint);

        lanternButton = actionButton("Turn on lantern", true);
        lanternButton.setOnClickListener(v -> runWithCameraPermission(() -> toggleLantern()));
        root.addView(lanternButton, matchWrap());
        torchStatus = text("Lantern ready", 13, mist());
        torchStatus.setGravity(Gravity.CENTER);
        torchStatus.setPadding(0, dp(7), 0, 0);
        root.addView(torchStatus, matchWrap());
        if (torchMaxStrength > 1) addBrightnessControl();
        addSpacer(9);
        redButton = actionButton("Red screen light", false);
        redButton.setOnClickListener(v -> startActivity(new Intent(this, RedLightActivity.class)));
        root.addView(redButton, matchWrap());
        addSpacer(9);
        sosButton = actionButton("SOS signal", false);
        sosButton.setOnClickListener(v -> runWithCameraPermission(() -> toggleSos()));
        root.addView(sosButton, matchWrap());

        addSpacer(22);
        TextView footer = text("No account · No tracking · No internet permission", 13, mist());
        footer.setGravity(Gravity.CENTER);
        root.addView(footer, matchWrap());
        return scroll;
    }

    @Override protected void onResume() {
        super.onResume();
        if (rotationSensor != null) sensors.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_UI);
        else {
            if (accelerometer != null) sensors.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
            if (magnetometer != null) sensors.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override protected void onPause() {
        super.onPause();
        sensors.unregisterListener(this);
        if (sosOn) stopSos();
        if (torchOn) setTorch(false);
    }

    @Override protected void onDestroy() {
        cameraManager.unregisterTorchCallback(torchCallback);
        super.onDestroy();
    }

    @Override public void onSensorChanged(SensorEvent event) {
        float heading = Float.NaN;
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            float[] rotation = new float[9];
            SensorManager.getRotationMatrixFromVector(rotation, event.values);
            heading = headingFromRotation(rotation);
        } else {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                System.arraycopy(event.values, 0, gravity, 0, gravity.length);
                hasGravity = true;
            } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                System.arraycopy(event.values, 0, geomagnetic, 0, geomagnetic.length);
                hasGeomagnetic = true;
            }
            if (hasGravity && hasGeomagnetic) {
                float[] rotation = new float[9];
                if (SensorManager.getRotationMatrix(rotation, null, gravity, geomagnetic)) {
                    heading = headingFromRotation(rotation);
                }
            }
        }
        if (!Float.isNaN(heading)) updateHeading((heading + 360f) % 360f);
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {
        if (sensorStatus == null || (sensor != rotationSensor && sensor != magnetometer)) return;
        if (accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) sensorStatus.setText("Compass needs calibration · move in a figure-eight");
        else if (accuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW) sensorStatus.setText("Compass accuracy is low · calibrate before relying on it");
        else sensorStatus.setText("Magnetic north · compass calibrated");
    }

    /** Align the sensor coordinate system with the physical screen in portrait or landscape. */
    private float headingFromRotation(float[] rotation) {
        @SuppressWarnings("deprecation")
        int displayRotation = getWindowManager().getDefaultDisplay().getRotation();
        int axisX = SensorManager.AXIS_X;
        int axisY = SensorManager.AXIS_Y;
        if (displayRotation == Surface.ROTATION_90) {
            axisX = SensorManager.AXIS_Y;
            axisY = SensorManager.AXIS_MINUS_X;
        } else if (displayRotation == Surface.ROTATION_180) {
            axisX = SensorManager.AXIS_MINUS_X;
            axisY = SensorManager.AXIS_MINUS_Y;
        } else if (displayRotation == Surface.ROTATION_270) {
            axisX = SensorManager.AXIS_MINUS_Y;
            axisY = SensorManager.AXIS_X;
        }
        float[] adjusted = new float[9];
        SensorManager.remapCoordinateSystem(rotation, axisX, axisY, adjusted);
        float[] orientation = new float[3];
        SensorManager.getOrientation(adjusted, orientation);
        return (float) Math.toDegrees(orientation[0]);
    }

    private void updateHeading(float heading) {
        if (compass == null) return;
        compass.setHeading(heading);
        int rounded = Math.round(heading) % 360;
        bearingText.setText(String.format(Locale.US, "%03d°", rounded));
        directionText.setText(directionFor(rounded));
    }

    private String directionFor(int degrees) {
        String[] labels = {"North", "North-east", "East", "South-east", "South", "South-west", "West", "North-west"};
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
                        if (max != null) torchMaxStrength = max;
                    }
                    return;
                }
            }
        } catch (Exception ignored) { }
    }

    private void updateHardwareCopy() {
        if (rotationSensor == null && (accelerometer == null || magnetometer == null)) {
            sensorStatus.setText("Compass sensor unavailable on this phone");
            bearingText.setText("—°");
            directionText.setText("Compass unavailable");
        }
        if (torchCameraId == null) {
            lanternButton.setEnabled(false);
            lanternButton.setText("No camera flash available");
            sosButton.setEnabled(false);
            sosButton.setText("SOS unavailable without flash");
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
            else Toast.makeText(this, "Camera access is needed only to power the LED lantern.", Toast.LENGTH_LONG).show();
            pendingCameraAction = null;
        }
    }

    private void toggleLantern() {
        if (sosOn) stopSos();
        setTorch(!torchOn);
        lanternButton.setText(torchOn ? "Turn off lantern" : "Turn on lantern");
    }

    private void setTorch(boolean on) {
        if (torchCameraId == null) return;
        try {
            cameraManager.setTorchMode(torchCameraId, on);
            torchOn = on;
            if (!sosOn) lanternButton.setText(on ? "Turn off lantern" : "Turn on lantern");
            if (torchStatus != null) torchStatus.setText(on ? "Lantern on" : "Lantern ready");
        } catch (Exception e) {
            torchOn = false;
            if (torchStatus != null) torchStatus.setText("Lantern unavailable right now");
            Toast.makeText(this, "The lantern is unavailable right now.", Toast.LENGTH_SHORT).show();
        }
    }

    private void addBrightnessControl() {
        brightnessLabel = text("Lantern brightness", 13, mist());
        brightnessLabel.setPadding(0, dp(8), 0, 0);
        root.addView(brightnessLabel, matchWrap());
        SeekBar brightness = new SeekBar(this);
        brightness.setMax(torchMaxStrength - 1);
        brightness.setProgress(torchMaxStrength - 1);
        brightness.setContentDescription("Lantern brightness");
        brightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                int level = progress + 1;
                brightnessLabel.setText("Lantern brightness · " + level + " of " + torchMaxStrength);
                if (fromUser) runWithCameraPermission(() -> setTorchStrength(level));
            }
            @Override public void onStartTrackingTouch(SeekBar bar) { }
            @Override public void onStopTrackingTouch(SeekBar bar) { }
        });
        root.addView(brightness, matchWrap());
    }

    private void setTorchStrength(int level) {
        if (android.os.Build.VERSION.SDK_INT < 33 || torchCameraId == null) return;
        try {
            cameraManager.turnOnTorchWithStrengthLevel(torchCameraId, Math.max(1, Math.min(level, torchMaxStrength)));
            torchOn = true;
        } catch (Exception e) {
            Toast.makeText(this, "This phone could not change lantern brightness.", Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleSos() {
        if (sosOn) { stopSos(); return; }
        if (torchOn) setTorch(false);
        sosOn = true;
        sosIndex = 0;
        sosButton.setText("Stop SOS signal");
        handler.post(sosPulse);
    }

    private void stopSos() {
        sosOn = false;
        handler.removeCallbacks(sosPulse);
        setTorch(false);
        sosButton.setText("SOS signal");
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
    private int mist() { return Color.rgb(174, 187, 203); }
    private int gold() { return Color.rgb(240, 182, 91); }

    private static class CompassView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
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
            String[] marks = {"N", "E", "S", "W"};
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
            Path needle = new Path();
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
