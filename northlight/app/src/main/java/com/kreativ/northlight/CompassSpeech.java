package com.kreativ.northlight;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.os.Handler;
import android.os.Build;
import android.os.Looper;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityManager;
import java.util.List;
import java.util.function.BooleanSupplier;

/** Foreground-only, opt-in compass speech. Independent of purchases and Pro preferences. */
final class CompassSpeech {
    enum VoiceState { IDLE, STARTING, READY, MISSING, FAILED, INTERRUPTED }
    interface Output {
        void listener(Runnable changed);
        VoiceState state();
        void prepare();
        boolean speak(String text, float rate);
        boolean busy();
        void stop();
        void close();
    }
    interface Listener {
        void onSpeechChanged();
        void onSpeechReading(String text, boolean throughScreenReader);
    }
    static final String PREFS = "compass_speech";
    private static final int[] INTERVALS = {3, 5, 10};
    private static final float[] RATES = {.8f, 1f, 1.2f};
    private final Activity activity;
    private final Listener listener;
    private final Output output;
    private final BooleanSupplier reader;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final SpeechPolicy policy = new SpeechPolicy();
    private final AccessibilityManager accessibility;
    private final AccessibilityManager.AccessibilityStateChangeListener accessibilityChanged = enabled -> refreshReader();
    private final AccessibilityManager.TouchExplorationStateChangeListener touchChanged = enabled -> refreshReader();
    private Object servicesChanged;
    private boolean foreground, closed, automatic, pending, readerActive;
    private int status = R.string.speech_intro;
    private SpeechPolicy.Reading speakingReading;
    private BearingGuidance guidance;
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (!foreground || closed || !automatic) return;
            if (refreshReader()) return;
            if (output.state() != VoiceState.READY) { outputChanged(); return; }
            long now = SystemClock.elapsedRealtime();
            SpeechPolicy.Reading current = policy.reading(now);
            if (guidance != null) {
                if (current == SpeechPolicy.Reading.HEADING) {
                    guidance.feedback(policy.heading(), policy.stable(now), now);
                    if (speakingReading == SpeechPolicy.Reading.HEADING && output.busy()
                            && policy.stable(now) && guidance.obsolete(policy.heading())) output.stop();
                } else guidance.reset();
            }
            if (speakingReading == SpeechPolicy.Reading.HEADING && current != SpeechPolicy.Reading.HEADING) output.stop();
            if (!output.busy() && output.state() == VoiceState.READY) {
                SpeechPolicy.Reading next = guidance != null && current == SpeechPolicy.Reading.HEADING
                        ? policy.stable(now) && guidance.automatic(policy.heading(), now, intervalSeconds() * 1_000L) ? current : null
                        : policy.automatic(now, intervalSeconds() * 1_000L);
                if (next != null) deliver(next, false);
            }
            if (automatic) handler.postDelayed(this, 250);
        }
    };

    CompassSpeech(Activity activity, Listener listener) {
        this(activity, listener, new OfflineSpeechOutput(activity), () -> screenReaderEnabled(activity));
    }
    CompassSpeech(Activity activity, Listener listener, Output output, BooleanSupplier reader) {
        this.activity = activity; this.listener = listener; this.output = output; this.reader = reader;
        accessibility = activity.getSystemService(AccessibilityManager.class);
        output.listener(() -> handler.post(this::outputChanged));
    }
    void resume() {
        if (closed || foreground) return;
        foreground = true; automatic = pending = false; policy.reset(); speakingReading = null;
        if (guidance != null) guidance.reset();
        accessibility.addAccessibilityStateChangeListener(accessibilityChanged);
        accessibility.addTouchExplorationStateChangeListener(touchChanged);
        if (Build.VERSION.SDK_INT >= 33) {
            servicesChanged = Api33.observe(activity, accessibility, this::refreshReader);
        }
        readerActive = reader.getAsBoolean();
        status = readerActive ? R.string.speech_reader_mode : voiceStatus();
        listener.onSpeechChanged();
    }
    void pause() {
        foreground = false; automatic = pending = false; speakingReading = null;
        handler.removeCallbacks(tick); output.stop(); policy.reset();
        if (guidance != null) guidance.reset();
        accessibility.removeAccessibilityStateChangeListener(accessibilityChanged);
        accessibility.removeTouchExplorationStateChangeListener(touchChanged);
        if (Build.VERSION.SDK_INT >= 33 && servicesChanged != null) {
            Api33.remove(accessibility, servicesChanged); servicesChanged = null;
        }
        status = R.string.speech_paused; listener.onSpeechChanged();
    }
    void close() {
        if (closed) return;
        pause(); closed = true; handler.removeCallbacksAndMessages(null); output.close();
    }
    void heading(float value) { policy.heading(value, SystemClock.elapsedRealtime()); }
    void accuracy(int message) {
        SpeechPolicy.Accuracy accuracy = message == R.string.compass_accuracy_ok ? SpeechPolicy.Accuracy.OK
                : message == R.string.compass_low_accuracy ? SpeechPolicy.Accuracy.LOW
                : message == R.string.compass_calibrate ? SpeechPolicy.Accuracy.UNRELIABLE
                : message == R.string.compass_unavailable ? SpeechPolicy.Accuracy.UNAVAILABLE : SpeechPolicy.Accuracy.WAITING;
        policy.accuracy(accuracy);
        if (accuracy != SpeechPolicy.Accuracy.OK && guidance != null) guidance.reset();
        if (accuracy != SpeechPolicy.Accuracy.OK && speakingReading == SpeechPolicy.Reading.HEADING) {
            output.stop(); speakingReading = null;
            listener.onSpeechChanged();
        }
    }
    void speakOnce() {
        if (!foreground || closed) return;
        if (refreshReader()) {
            pending = false;
            long now = SystemClock.elapsedRealtime();
            SpeechPolicy.Reading reading = policy.reading(now);
            listener.onSpeechReading(text(reading), true);
            if (guidance != null && reading == SpeechPolicy.Reading.HEADING) guidance.feedback(policy.heading(), policy.stable(now), now);
            return;
        }
        pending = true;
        if (output.state() == VoiceState.IDLE || output.state() == VoiceState.INTERRUPTED) output.prepare();
        if (output.state() == VoiceState.READY) { pending = false; deliver(policy.reading(SystemClock.elapsedRealtime()), true); }
        else if (output.state() == VoiceState.MISSING || output.state() == VoiceState.FAILED) {
            pending = false; status = voiceStatus(); listener.onSpeechChanged();
        } else { status = R.string.speech_preparing; listener.onSpeechChanged(); }
    }
    void automatic(boolean enabled) {
        if (!foreground || closed) return;
        pauseSpeech();
        if (!enabled) return;
        if (refreshReader()) { status = R.string.speech_reader_mode; listener.onSpeechChanged(); return; }
        automatic = true; policy.resetAnnouncements();
        if (output.state() == VoiceState.IDLE || output.state() == VoiceState.INTERRUPTED) output.prepare();
        outputChanged();
    }
    void pauseSpeech() {
        automatic = pending = false; speakingReading = null;
        handler.removeCallbacks(tick); output.stop(); status = R.string.speech_paused;
        if (guidance != null) guidance.reset();
        listener.onSpeechChanged();
    }
    void retryVoice() {
        if (!foreground || closed || refreshReader()) return;
        pauseSpeech(); pending = true; output.prepare(); status = R.string.speech_preparing;
        listener.onSpeechChanged();
    }
    private void outputChanged() {
        if (!foreground || closed) return;
        if (refreshReader()) return;
        VoiceState state = output.state();
        status = voiceStatus();
        if (state == VoiceState.MISSING || state == VoiceState.FAILED || state == VoiceState.INTERRUPTED) {
            automatic = pending = false; handler.removeCallbacks(tick); output.stop();
            if (guidance != null) guidance.reset();
        } else if (state == VoiceState.READY) {
            if (pending) { pending = false; deliver(policy.reading(SystemClock.elapsedRealtime()), true); }
            handler.removeCallbacks(tick);
            if (automatic) handler.post(tick);
        }
        listener.onSpeechChanged();
    }
    private boolean refreshReader() {
        if (!foreground || closed) return readerActive;
        boolean current = reader.getAsBoolean();
        if (current != readerActive) {
            readerActive = current; output.stop(); speakingReading = null;
            pending = automatic = false; handler.removeCallbacks(tick);
            if (guidance != null) guidance.reset();
            status = current ? R.string.speech_reader_mode : voiceStatus(); listener.onSpeechChanged();
        }
        return current;
    }
    private void deliver(SpeechPolicy.Reading reading, boolean manual) {
        if (!foreground || closed || refreshReader()) return;
        if (manual) output.stop();
        String text = text(reading);
        speakingReading = reading;
        if (output.speak(text, RATES[rateIndex()])) {
            long now = SystemClock.elapsedRealtime();
            policy.delivered(reading, now);
            if (guidance != null && reading == SpeechPolicy.Reading.HEADING) {
                guidance.delivered(policy.heading(), now); guidance.feedback(policy.heading(), policy.stable(now), now);
            }
            status = automatic ? R.string.speech_auto_active : R.string.speech_ready;
            listener.onSpeechReading(text, false);
        } else {
            speakingReading = null; automatic = pending = false; handler.removeCallbacks(tick);
            if (guidance != null) guidance.reset();
            status = output.state() == VoiceState.FAILED ? R.string.speech_failed : R.string.speech_audio_busy;
        }
        listener.onSpeechChanged();
    }
    private String text(SpeechPolicy.Reading reading) {
        if (reading != SpeechPolicy.Reading.HEADING) {
            int message = reading == SpeechPolicy.Reading.UNAVAILABLE ? R.string.speech_no_sensor
                    : reading == SpeechPolicy.Reading.LOW ? R.string.speech_low_accuracy
                    : reading == SpeechPolicy.Reading.CALIBRATE ? R.string.speech_calibrate
                    : reading == SpeechPolicy.Reading.STALE ? R.string.speech_stale : R.string.speech_waiting;
            return activity.getString(message);
        }
        if (guidance != null) return guidance.text(policy.heading());
        int degrees = policy.degrees();
        String direction = activity.getResources().getStringArray(R.array.compass_directions)[((degrees + 22) % 360) / 45];
        return activity.getResources().getQuantityString(R.plurals.speech_heading, degrees, direction, degrees);
    }
    private int voiceStatus() {
        if (output.state() == VoiceState.STARTING) return R.string.speech_preparing;
        if (output.state() == VoiceState.MISSING) return R.string.speech_missing_voice;
        if (output.state() == VoiceState.FAILED) return R.string.speech_failed;
        if (output.state() == VoiceState.INTERRUPTED) return R.string.speech_audio_busy;
        if (automatic) return R.string.speech_auto_active;
        return output.state() == VoiceState.READY ? R.string.speech_ready : R.string.speech_intro;
    }
    boolean automatic() { return automatic; }
    void guidance(BearingGuidance value) {
        if (guidance == value) return;
        pauseSpeech(); guidance = value;
        if (guidance != null) guidance.reset();
        listener.onSpeechChanged();
    }
    boolean screenReader() { return readerActive; }
    boolean canPause() { return automatic || pending || output.busy(); }
    int status() { return status; }
    int intervalIndex() { return Math.max(0, Math.min(2, activity.getSharedPreferences(PREFS, 0).getInt("interval", 1))); }
    int rateIndex() { return Math.max(0, Math.min(2, activity.getSharedPreferences(PREFS, 0).getInt("rate", 1))); }
    int intervalSeconds() { return INTERVALS[intervalIndex()]; }
    void intervalIndex(int index) { activity.getSharedPreferences(PREFS, 0).edit().putInt("interval", Math.max(0, Math.min(2, index))).apply(); }
    void rateIndex(int index) { activity.getSharedPreferences(PREFS, 0).edit().putInt("rate", Math.max(0, Math.min(2, index))).apply(); }
    static boolean screenReaderEnabled(Activity activity) {
        AccessibilityManager manager = activity.getSystemService(AccessibilityManager.class);
        List<AccessibilityServiceInfo> services = manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_SPOKEN);
        return manager.isTouchExplorationEnabled() || (services != null && !services.isEmpty());
    }
    @androidx.annotation.RequiresApi(33)
    private static final class Api33 {
        static Object observe(Activity activity, AccessibilityManager manager, Runnable changed) {
            AccessibilityManager.AccessibilityServicesStateChangeListener listener = ignored -> changed.run();
            manager.addAccessibilityServicesStateChangeListener(activity.getMainExecutor(), listener);
            return listener;
        }
        static void remove(AccessibilityManager manager, Object listener) {
            manager.removeAccessibilityServicesStateChangeListener((AccessibilityManager.AccessibilityServicesStateChangeListener)listener);
        }
    }
}
