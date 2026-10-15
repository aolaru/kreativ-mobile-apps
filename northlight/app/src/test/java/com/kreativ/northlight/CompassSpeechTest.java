package com.kreativ.northlight;

import android.app.Activity;
import android.os.Looper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

/** Fake speech output: no engine, microphone, network, billing or audible playback. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36, application = FieldUiTest.OfflineApplication.class)
public class CompassSpeechTest {
    private ActivityController<Activity> activity;
    private CompassSpeech speech;
    private FakeOutput output;
    private final List<String> readings = new ArrayList<>();
    private boolean reader, throughReader;
    static class FakeOutput implements CompassSpeech.Output {
        Runnable changed = () -> { };
        CompassSpeech.VoiceState state = CompassSpeech.VoiceState.IDLE;
        final List<String> said = new ArrayList<>();
        int prepares, stops; boolean busy, closed, accept = true; float rate;
        public void listener(Runnable r) { changed = r; }
        public CompassSpeech.VoiceState state() { return state; }
        public void prepare() { prepares++; state = CompassSpeech.VoiceState.STARTING; }
        public boolean speak(String text, float speed) { if (!accept) return false; said.add(text); rate = speed; busy = true; return true; }
        public boolean busy() { return busy; }
        public void stop() { stops++; busy = false; }
        public void close() { closed = true; stop(); }
        void state(CompassSpeech.VoiceState next) { state = next; changed.run(); }
        void ready() { state(CompassSpeech.VoiceState.READY); }
        void done() { busy = false; changed.run(); }
    }
    @Before public void setup() {
        activity = Robolectric.buildActivity(Activity.class).setup(); output = new FakeOutput();
        speech = new CompassSpeech(activity.get(), new CompassSpeech.Listener() {
            public void onSpeechChanged() { }
            public void onSpeechReading(String text, boolean viaReader) { readings.add(text); throughReader = viaReader; }
        }, output, () -> reader);
        speech.resume();
    }
    @After public void cleanup() { speech.close(); activity.close(); }
    private void idle(long ms) { shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms)); }
    private void heading(float degrees) { speech.heading(degrees); speech.accuracy(R.string.compass_accuracy_ok); }
    private void ready() { output.ready(); idle(0); }
    @Test public void staysSilentAndLazyUntilRequested() {
        heading(315); idle(10000); assertEquals(0, output.prepares); assertTrue(output.said.isEmpty()); assertFalse(speech.automatic());
    }
    @Test public void delayedInitUsesLatestReadingNotTappedHeading() {
        heading(45); speech.speakOnce(); assertEquals(1, output.prepares);
        idle(5000); heading(315); ready(); assertEquals(1, output.said.size());
        assertTrue(output.said.get(0).contains("315")); assertFalse(throughReader);
    }
    @Test public void delayedInitNeverSpeaksStaleDegrees() {
        heading(315); speech.speakOnce(); idle(5000); ready();
        assertEquals(activity.get().getString(R.string.speech_stale), output.said.get(0));
    }
    @Test public void pauseCancelsPendingAndLateCallbacks() {
        heading(315); speech.speakOnce(); speech.pause(); ready();
        assertTrue(output.said.isEmpty()); speech.resume(); idle(1000);
        assertFalse(speech.automatic()); assertTrue(output.said.isEmpty());
        speech.speakOnce(); assertEquals(activity.get().getString(R.string.speech_waiting), output.said.get(0));
    }
    @Test public void explicitPauseCancelsInitializationRequest() {
        heading(315); speech.speakOnce(); speech.pauseSpeech(); ready(); assertTrue(output.said.isEmpty());
    }
    @Test public void closeIgnoresLateCallbacks() {
        speech.speakOnce(); speech.close(); ready(); assertTrue(output.closed); assertTrue(output.said.isEmpty());
    }
    @Test public void accuracyWarningsReplaceHeadingAndStopCurrentSpeech() {
        heading(315); speech.speakOnce(); ready(); assertTrue(output.busy);
        speech.accuracy(R.string.compass_low_accuracy); assertFalse(output.busy);
        speech.speakOnce(); assertEquals(activity.get().getString(R.string.speech_low_accuracy), output.said.get(1));
        speech.accuracy(R.string.compass_calibrate); speech.speakOnce();
        assertEquals(activity.get().getString(R.string.speech_calibrate), output.said.get(2));
    }
    @Test public void noSensorNeverSaysDegrees() {
        heading(315); speech.accuracy(R.string.compass_unavailable); speech.speakOnce(); ready();
        assertEquals(activity.get().getString(R.string.speech_no_sensor), output.said.get(0));
    }
    @Test public void automaticNeedsStableChangeAndDoesNotQueueBehindSpeech() {
        heading(90); speech.automatic(true); ready(); idle(500); assertEquals(1, output.said.size());
        heading(120); idle(1000); assertEquals(1, output.said.size());
        output.done(); idle(0);
        for (int i=0;i<5;i++) { heading(120); idle(1000); }
        assertEquals(2, output.said.size()); assertTrue(output.said.get(1).contains("120"));
        speech.pause(); speech.resume(); assertFalse(speech.automatic());
    }
    @Test public void screenReaderUsesPoliteUiReadingNotSecondVoice() {
        reader = true; heading(315); speech.speakOnce();
        assertTrue(throughReader); assertTrue(readings.get(0).contains("315"));
        assertEquals(0, output.prepares); speech.automatic(true); assertFalse(speech.automatic());
        assertEquals(R.string.speech_reader_mode, speech.status());
    }
    @Test public void readerEnabledDuringAutomaticStopsOwnSpeech() {
        heading(90); speech.automatic(true); ready(); idle(500); assertTrue(output.busy);
        reader = true; idle(250); assertFalse(output.busy); assertFalse(speech.automatic());
        assertEquals(R.string.speech_reader_mode, speech.status());
    }
    @Test public void missingVoicePausesAndExplicitRetryCanRecover() {
        speech.automatic(true); output.state(CompassSpeech.VoiceState.MISSING); idle(0);
        assertFalse(speech.automatic()); assertEquals(R.string.speech_missing_voice, speech.status());
        heading(315); speech.retryVoice(); ready(); assertEquals(2, output.prepares); assertEquals(1, output.said.size());
    }
    @Test public void audioDenialPausesAutomaticWithoutLoopingRetries() {
        output.accept = false; heading(90); speech.automatic(true); ready(); idle(1000);
        assertFalse(speech.automatic()); assertEquals(R.string.speech_audio_busy, speech.status());
        idle(10000); assertEquals(1, output.prepares);
    }
    @Test public void audioInterruptionPausesWithoutClaimingVoiceIsMissing() {
        heading(90); speech.automatic(true); ready(); idle(500);
        output.state(CompassSpeech.VoiceState.INTERRUPTED); idle(0);
        assertFalse(speech.automatic()); assertEquals(R.string.speech_audio_busy, speech.status());
        speech.speakOnce(); assertEquals(2, output.prepares);
    }
    @Test public void preferencesPersistAndSpeedIsUsed() {
        speech.rateIndex(2); speech.intervalIndex(0); heading(1); speech.speakOnce(); ready();
        assertEquals(1.2f, output.rate, .001f); assertEquals(3, speech.intervalSeconds());
        assertTrue(output.said.get(0).contains("1 degree,"));
        speech.pause(); speech.resume(); assertEquals(2, speech.rateIndex()); assertFalse(speech.automatic());
    }
    @Test @Config(qualifiers = "ro-rRO") public void romanianHeadingAndWarningsAreLocalized() {
        heading(315); speech.speakOnce(); ready(); assertTrue(output.said.get(0).contains("Nord-vest, 315 grade"));
        speech.accuracy(R.string.compass_low_accuracy); speech.speakOnce();
        assertEquals(activity.get().getString(R.string.speech_low_accuracy), output.said.get(1));
    }
    @Test @Config(sdk = 26) public void lifecycleWorksOnOldestSupportedAndroid() {
        heading(315); speech.speakOnce(); ready(); assertEquals(1, output.said.size());
        speech.pause(); speech.resume(); assertFalse(speech.automatic());
    }
}
