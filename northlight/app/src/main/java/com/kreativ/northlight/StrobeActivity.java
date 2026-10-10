package com.kreativ.northlight;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
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
import android.widget.SeekBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** Pro-only adjustable visual strobe. It never changes the free SOS signal. */
public class StrobeActivity extends Activity {
    private static final int CAMERA_PERMISSION = 31;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private CameraManager camera;
    private String torchId;
    private boolean strobing;
    private boolean lightOn;
    private int interval = 300;
    private String speedLabel() { return getResources().getStringArray(R.array.strobe_speeds)[speedIndex]; }
    private int speedIndex = 1;
    private Button toggle;
    private TextView status;
    private final Runnable pulse = new Runnable() {
        @Override public void run() {
            if (!strobing) return;
            setTorch(!lightOn);
            if (strobing) handler.postDelayed(this, interval);
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        camera = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        findTorch();
        setContentView(buildScreen());
    }

    @Override protected void onPause() { stop(); super.onPause(); }

    private View buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(11, 19, 32));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(30), dp(24), dp(30));
        root.setBackgroundColor(Color.rgb(11, 19, 32));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));

        TextView badge = text(getString(R.string.pro_brand), 12, gold());
        badge.setLetterSpacing(.16f);
        root.addView(badge);
        TextView title = text(getString(R.string.strobe_flash_signal), 31, moon());
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setPadding(0, dp(14), 0, dp(8));
        root.addView(title);
        TextView hint = text(getString(R.string.strobe_hint), 16, mist());
        hint.setLineSpacing(dp(3), 1f);
        root.addView(hint);
        View space = new View(this);
        root.addView(space, new LinearLayout.LayoutParams(1, 0, 1));
        status = text(torchId == null ? getString(R.string.main_no_camera_flash_available) : getString(R.string.strobe_ready, speedLabel()), 15, mist());
        status.setGravity(Gravity.CENTER);
        root.addView(status, new LinearLayout.LayoutParams(-1, -2));
        SeekBar speed = new SeekBar(this);
        speed.setMax(3);
        speed.setProgress(1);
        speed.setContentDescription(getString(R.string.strobe_strobe_speed));
        speed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                int[] values = {420, 300, 220, 140};
                speedIndex = progress;
                interval = values[progress];
                if (!strobing) status.setText(getString(R.string.strobe_ready, speedLabel()));
            }
            @Override public void onStartTrackingTouch(SeekBar bar) { }
            @Override public void onStopTrackingTouch(SeekBar bar) { }
        });
        root.addView(speed, new LinearLayout.LayoutParams(-1, -2));
        toggle = button(getString(R.string.strobe_start_strobe), true);
        toggle.setEnabled(torchId != null);
        toggle.setOnClickListener(v -> runWithCameraPermission());
        root.addView(toggle, new LinearLayout.LayoutParams(-1, -2));
        View lower = new View(this);
        root.addView(lower, new LinearLayout.LayoutParams(1, dp(10)));
        Button close = button(getString(R.string.close), false);
        close.setOnClickListener(v -> finish());
        root.addView(close, new LinearLayout.LayoutParams(-1, -2));
        return Ui.screen(this, scroll);
    }

    private void runWithCameraPermission() {
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) toggle();
        else requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION);
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == CAMERA_PERMISSION && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) toggle();
        else if (requestCode == CAMERA_PERMISSION) Toast.makeText(this, getString(R.string.strobe_camera_access_is_needed_only_to_control_the_flash), Toast.LENGTH_LONG).show();
    }

    private void toggle() { if (strobing) stop(); else start(); }
    private void start() { strobing = true; toggle.setText(getString(R.string.strobe_stop_strobe)); status.setText(getString(R.string.strobe_strobe_active)); handler.post(pulse); }
    private void stop() { strobing = false; handler.removeCallbacks(pulse); if (lightOn) setTorch(false); if (toggle != null) toggle.setText(getString(R.string.strobe_start_strobe)); if (status != null && torchId != null) status.setText(getString(R.string.strobe_ready, speedLabel())); }
    private void setTorch(boolean on) { if (torchId == null) return; try { camera.setTorchMode(torchId, on); lightOn = on; } catch (Exception e) { strobing = false; lightOn = false; handler.removeCallbacks(pulse); if (toggle != null) toggle.setText(getString(R.string.strobe_start_strobe)); if (status != null) status.setText(getString(R.string.strobe_flash_unavailable)); Toast.makeText(this, getString(R.string.strobe_the_flash_is_unavailable_right_now), Toast.LENGTH_SHORT).show(); } }
    private void findTorch() { try { for (String id : camera.getCameraIdList()) { Boolean flash = camera.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE); if (Boolean.TRUE.equals(flash)) { torchId = id; return; } } } catch (Exception ignored) { } }
    private TextView text(String value, float size, int color) { TextView view = new TextView(this); view.setText(value); view.setTextSize(size); view.setTextColor(color); return view; }
    private Button button(String label, boolean gold) { Button button = new Button(this); button.setText(label); button.setAllCaps(false); button.setTextSize(16); button.setMinHeight(dp(54)); button.setTextColor(gold ? Color.rgb(11, 19, 32) : moon()); button.setBackgroundResource(gold ? R.drawable.button_gold : R.drawable.button_dark); return button; }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private int moon() { return Color.rgb(238, 243, 247); }
    private int mist() { return Color.rgb(174, 187, 203); }
    private int gold() { return Color.rgb(240, 182, 91); }
}
