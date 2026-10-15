package com.kreativ.northlight;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import androidx.core.location.LocationCompat;

/** Explicitly enabled, foreground-only GNSS altitude. No coordinates, routes or fixes are retained. */
final class AltimeterSource {
    interface Listener { void onAltitudeChanged(); }
    enum State { OFF, ALLOWED, WAITING, SLOW_FIX, READY, NO_ALTITUDE, STALE, DENIED, APPROXIMATE, GPS_OFF, NO_GPS, ERROR, PAUSED, SIMULATED }
    private static final long FRESH_NANOS = 15_000_000_000L;
    private final Context context;
    private final LocationManager manager;
    private final Listener listener;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private LocationListener updates;
    private boolean foreground, closed, seaLevel;
    private int generation;
    private State state = State.OFF;
    private double meters = Double.NaN, verticalAccuracy = Double.NaN;
    private long startedAt, latestFix;

    private final Runnable watch = new Runnable() {
        @Override public void run() {
            if (!active() || !foreground || closed) return;
            if (!finePermission()) { finish(permissionState()); return; }
            try {
                if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) { finish(State.GPS_OFF); return; }
            } catch (RuntimeException unavailable) { finish(State.ERROR); return; }
            long now = SystemClock.elapsedRealtimeNanos();
            if (state == State.READY && now - latestFix > FRESH_NANOS) {
                clearReading(); state = State.STALE; listener.onAltitudeChanged();
            } else if (state == State.WAITING && now - startedAt >= 60_000_000_000L) {
                state = State.SLOW_FIX; listener.onAltitudeChanged();
            }
            handler.postDelayed(this, 1_000);
        }
    };

    AltimeterSource(Context context, Listener listener) {
        this.context = context; this.listener = listener;
        manager = context.getSystemService(LocationManager.class);
    }

    void resume() {
        if (closed) return;
        foreground = true;
        // Returning from Settings never enables collection. It only refreshes the available action.
        if ((state == State.DENIED || state == State.APPROXIMATE) && finePermission()) state = State.ALLOWED;
        if (state == State.GPS_OFF) {
            try { if (manager != null && manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) state = State.PAUSED; }
            catch (RuntimeException ignored) { }
        }
        listener.onAltitudeChanged();
    }

    void pause() {
        foreground = false;
        if (active()) finish(State.PAUSED);
    }

    void close() {
        if (closed) return;
        closed = true; foreground = false; removeUpdates(); clearReading();
        handler.removeCallbacksAndMessages(null);
    }

    boolean hasGps() {
        try { return manager != null && manager.getAllProviders().contains(LocationManager.GPS_PROVIDER); }
        catch (RuntimeException unavailable) { return false; }
    }
    private boolean finePermission() {
        return context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
    private State permissionState() {
        return context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                ? State.APPROXIMATE : State.DENIED;
    }
    void permissionResult() {
        // Permission alone is not an instruction to resume location after a system dialog/background trip.
        finish(finePermission() ? State.ALLOWED : permissionState());
    }

    void start() {
        if (!foreground || closed) return;
        removeUpdates(); clearReading(); latestFix = 0;
        if (!hasGps()) { finish(State.NO_GPS); return; }
        if (!finePermission()) { finish(permissionState()); return; }
        try {
            if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) { finish(State.GPS_OFF); return; }
            startedAt = SystemClock.elapsedRealtimeNanos();
            final int token = generation;
            updates = new LocationListener() {
                @Override public void onLocationChanged(Location location) { accept(location, token); }
                @Override public void onProviderDisabled(String provider) {
                    if (current(token) && LocationManager.GPS_PROVIDER.equals(provider)) finish(State.GPS_OFF);
                }
                @Override public void onProviderEnabled(String provider) { /* Explicit restart required. */ }
                @Override public void onStatusChanged(String provider, int status, Bundle extras) { /* Legacy callback. */ }
            };
            state = State.WAITING;
            // Direct GNSS, not a network/fused provider. Never consult a cached last-known location.
            manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2_000L, 0f, updates, Looper.getMainLooper());
            handler.postDelayed(watch, 1_000);
            listener.onAltitudeChanged();
        } catch (SecurityException denied) { finish(permissionState()); }
        catch (RuntimeException unavailable) { finish(State.ERROR); }
    }

    void stop() { if (!closed) finish(State.OFF); }

    private boolean current(int token) { return !closed && foreground && active() && token == generation; }

    private void accept(Location location, int token) {
        if (!current(token) || location == null || !LocationManager.GPS_PROVIDER.equals(location.getProvider())) return;
        if (!finePermission()) { finish(permissionState()); return; }
        long measured = location.getElapsedRealtimeNanos(), now = SystemClock.elapsedRealtimeNanos();
        // Reject old sessions, delayed/out-of-order fixes and future timestamps using the monotonic clock.
        if (measured < startedAt || measured <= latestFix || measured > now || now - measured > FRESH_NANOS) return;
        latestFix = measured;
        clearReading();
        if (LocationCompat.isMock(location)) {
            state = State.SIMULATED; listener.onAltitudeChanged(); return;
        }
        if (Build.VERSION.SDK_INT >= 34 && Api34.hasUsableMsl(location)) {
            seaLevel = true; meters = Api34.altitude(location); verticalAccuracy = Api34.accuracy(location);
        } else if (location.hasAltitude() && Double.isFinite(location.getAltitude())) {
            meters = location.getAltitude();
            if (location.hasVerticalAccuracy()) verticalAccuracy = location.getVerticalAccuracyMeters();
        }
        if (!Double.isFinite(verticalAccuracy) || verticalAccuracy < 0) verticalAccuracy = Double.NaN;
        state = Double.isFinite(meters) ? State.READY : State.NO_ALTITUDE;
        listener.onAltitudeChanged();
    }

    private void clearReading() { meters = verticalAccuracy = Double.NaN; seaLevel = false; }
    private void finish(State next) {
        removeUpdates(); clearReading(); state = next; listener.onAltitudeChanged();
    }
    private void removeUpdates() {
        ++generation; handler.removeCallbacks(watch);
        LocationListener old = updates; updates = null;
        if (manager != null && old != null) try { manager.removeUpdates(old); } catch (RuntimeException ignored) { }
    }

    boolean active() { return updates != null; }
    State state() { return state; }
    boolean hasReading() { return state == State.READY && Double.isFinite(meters); }
    double meters() { return meters; }
    double accuracy() { return verticalAccuracy; }
    boolean seaLevel() { return seaLevel; }

    @androidx.annotation.RequiresApi(34)
    private static final class Api34 {
        static boolean hasUsableMsl(Location location) { return location.hasMslAltitude() && Double.isFinite(location.getMslAltitudeMeters()); }
        static double altitude(Location location) { return location.getMslAltitudeMeters(); }
        static double accuracy(Location location) { return location.hasMslAltitudeAccuracy() ? location.getMslAltitudeAccuracyMeters() : Double.NaN; }
    }
}
