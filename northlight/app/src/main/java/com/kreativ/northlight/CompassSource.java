package com.kreativ.northlight;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/** One lifecycle-aware magnetic-heading implementation for every screen. */
final class CompassSource implements SensorEventListener {
    interface Listener {
        void onHeading(float heading);
        void onCompassStatus(int message);
    }
    private final Activity activity;
    private final Listener listener;
    private final SensorManager manager;
    private final Sensor rotation, accelerometer, magnetometer;
    private final float[] gravity = new float[3], magnetic = new float[3];
    private final float[] matrix = new float[9], adjusted = new float[9], orientation = new float[3];
    private boolean hasGravity, hasMagnetic;

    CompassSource(Activity activity, Listener listener) {
        this.activity = activity;
        this.listener = listener;
        manager = activity.getSystemService(SensorManager.class);
        rotation = manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        accelerometer = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }
    boolean available() { return rotation != null || (accelerometer != null && magnetometer != null); }
    void start() {
        hasGravity = hasMagnetic = false;
        if (!available()) { listener.onCompassStatus(R.string.compass_unavailable); return; }
        listener.onCompassStatus(R.string.compass_waiting_accuracy);
        if (rotation != null) manager.registerListener(this, rotation, SensorManager.SENSOR_DELAY_UI);
        else {
            manager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
            manager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
        }
    }
    void stop() { manager.unregisterListener(this); }
    @Override public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(matrix, event.values);
        } else {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                System.arraycopy(event.values, 0, gravity, 0, 3); hasGravity = true;
            } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                System.arraycopy(event.values, 0, magnetic, 0, 3); hasMagnetic = true;
            }
            if (!hasGravity || !hasMagnetic || !SensorManager.getRotationMatrix(matrix, null, gravity, magnetic)) return;
        }
        @SuppressWarnings("deprecation") int displayRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int[] axes = HeadingMath.axes(displayRotation);
        if (!SensorManager.remapCoordinateSystem(matrix, axes[0], axes[1], adjusted)) return;
        SensorManager.getOrientation(adjusted, orientation);
        listener.onHeading(HeadingMath.normalize((float) Math.toDegrees(orientation[0])));
        if (rotation != null || event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) onAccuracyChanged(event.sensor, event.accuracy);
    }
    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {
        if (sensor != rotation && sensor != magnetometer) return;
        int message = accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE ? R.string.compass_calibrate
                : accuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW ? R.string.compass_low_accuracy
                : R.string.compass_accuracy_ok;
        listener.onCompassStatus(message);
    }
}
