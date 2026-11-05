package com.kreativ.northlight;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import androidx.core.location.LocationCompat;

/** Foreground-only, explicitly started GPS. Cached, mock, stale and low-quality fixes are never used. */
final class PlaceLocationSource {
    interface Listener { void onPlaceLocationChanged(); }
    enum State { OFF, WAITING, READY, LOW, STALE, DENIED, GPS_OFF, UNAVAILABLE, SIMULATED, ERROR }
    private static final long FRESH_NANOS = 15_000_000_000L;
    private final Context context;
    private final LocationManager manager;
    private final Listener listener;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private LocationListener updates;
    private Location fix;
    private State state = State.OFF;
    private long started, latest;
    private int generation;
    private final Runnable watch = new Runnable() {
        @Override public void run() {
            if (!active()) return;
            if (!permitted()) { finish(State.DENIED); return; }
            try { if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) { finish(State.GPS_OFF); return; } }
            catch (RuntimeException failure) { finish(State.ERROR); return; }
            if (fix != null && SystemClock.elapsedRealtimeNanos() - latest > FRESH_NANOS) {
                fix = null; state = State.STALE; listener.onPlaceLocationChanged();
            }
            handler.postDelayed(this, 1_000);
        }
    };
    PlaceLocationSource(Context context, Listener listener) {
        this.context = context; this.listener = listener; manager = context.getSystemService(LocationManager.class);
    }
    boolean permitted() { return context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED; }
    boolean active() { return updates != null; }
    State state() { return state; }
    Location fix() { return fix == null ? null : new Location(fix); }
    void start() {
        remove(); fix = null; latest = 0;
        if (!permitted()) { finish(State.DENIED); return; }
        try {
            if (manager == null || !manager.getAllProviders().contains(LocationManager.GPS_PROVIDER)) { finish(State.UNAVAILABLE); return; }
            if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) { finish(State.GPS_OFF); return; }
            started = SystemClock.elapsedRealtimeNanos();
            final int token = generation;
            updates = new LocationListener() {
                @Override public void onLocationChanged(Location location) { accept(location, token); }
                @Override public void onProviderDisabled(String provider) { if (token == generation) finish(State.GPS_OFF); }
                @Override public void onProviderEnabled(String provider) { }
                @Override public void onStatusChanged(String provider, int status, Bundle extras) { }
            };
            state = State.WAITING;
            manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2_000L, 0f, updates, Looper.getMainLooper());
            handler.postDelayed(watch, 1_000); listener.onPlaceLocationChanged();
        } catch (SecurityException denied) { finish(State.DENIED); }
        catch (RuntimeException failure) { finish(State.ERROR); }
    }
    private void accept(Location value, int token) {
        if (!active() || token != generation || value == null || !LocationManager.GPS_PROVIDER.equals(value.getProvider())) return;
        if (!permitted()) { finish(State.DENIED); return; }
        long time = value.getElapsedRealtimeNanos(), now = SystemClock.elapsedRealtimeNanos();
        if (time < started || time <= latest || time > now || now - time > FRESH_NANOS) return;
        latest = time; fix = null;
        if (LocationCompat.isMock(value)) state = State.SIMULATED;
        else if (!PlaceMath.valid(value.getLatitude(), value.getLongitude()) || !value.hasAccuracy()
                || !Float.isFinite(value.getAccuracy()) || value.getAccuracy() < 0 || value.getAccuracy() > 50) state = State.LOW;
        else { fix = new Location(value); state = State.READY; }
        listener.onPlaceLocationChanged();
    }
    void stop() { finish(State.OFF); }
    private void finish(State next) { remove(); fix = null; state = next; listener.onPlaceLocationChanged(); }
    private void remove() {
        generation++; handler.removeCallbacks(watch);
        LocationListener old = updates; updates = null;
        if (manager != null && old != null) try { manager.removeUpdates(old); } catch (RuntimeException ignored) { }
    }
}
