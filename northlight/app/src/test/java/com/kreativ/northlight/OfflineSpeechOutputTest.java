package com.kreativ.northlight;

import android.media.AudioAttributes;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Implementation;
import org.robolectric.shadows.ShadowTextToSpeech;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

/** Exercises engine lifecycle and callbacks, with no real speech service. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36, application = FieldUiTest.OfflineApplication.class, shadows = OfflineSpeechOutputTest.TestTts.class)
public class OfflineSpeechOutputTest {
    @Implements(TextToSpeech.class)
    public static class TestTts extends ShadowTextToSpeech {
        String id;
        @Implementation protected int setAudioAttributes(AudioAttributes attributes) { return TextToSpeech.SUCCESS; }
        @Implementation protected int setSpeechRate(float rate) { return TextToSpeech.SUCCESS; }
        @Override @Implementation protected int speak(CharSequence text, int queue, Bundle parameters, String utteranceId) {
            id = utteranceId; return TextToSpeech.SUCCESS; // Deliberately wait for an explicit completion callback.
        }
    }
    private OfflineSpeechOutput output;
    private AudioManager audio;
    @Before public void setup() {
        output = new OfflineSpeechOutput(RuntimeEnvironment.getApplication());
        audio = RuntimeEnvironment.getApplication().getSystemService(AudioManager.class);
    }
    @After public void cleanup() { output.close(); }
    private void idle(long ms) { shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms)); }
    private TextToSpeech engine() { return ShadowTextToSpeech.getLastTextToSpeechInstance(); }
    private TestTts ready() {
        ShadowTextToSpeech.addVoice(new Voice("local", Locale.US, 300, 300, false, Set.of()));
        output.prepare(); TestTts shadow = (TestTts)shadowOf(engine());
        shadow.getOnInitListener().onInit(TextToSpeech.SUCCESS); idle(0);
        assertEquals(CompassSpeech.VoiceState.READY, output.state()); return shadow;
    }
    @Test public void initializationTimesOutAndShutsEngineDown() {
        output.prepare(); TextToSpeech old = engine(); idle(10001);
        assertEquals(CompassSpeech.VoiceState.FAILED, output.state()); assertTrue(shadowOf(old).isShutdown());
        shadowOf(old).getOnInitListener().onInit(TextToSpeech.SUCCESS); idle(0);
        assertEquals(CompassSpeech.VoiceState.FAILED, output.state());
    }
    @Test public void closingRejectsLateInitialization() {
        output.prepare(); TextToSpeech old = engine(); output.close();
        shadowOf(old).getOnInitListener().onInit(TextToSpeech.SUCCESS); idle(0);
        assertNotEquals(CompassSpeech.VoiceState.READY, output.state()); assertTrue(shadowOf(old).isShutdown());
    }
    @Test public void onlyNetworkVoiceIsMissingRatherThanUsed() {
        ShadowTextToSpeech.addVoice(new Voice("network", Locale.US, 300, 300, true, Set.of()));
        output.prepare(); shadowOf(engine()).getOnInitListener().onInit(TextToSpeech.SUCCESS); idle(0);
        assertEquals(CompassSpeech.VoiceState.MISSING, output.state());
    }
    @Test public void completionReleasesFocusAndIgnoresOldUtterance() {
        TestTts engine = ready(); assertTrue(output.speak("North", 1)); String first = engine.id;
        output.stop(); assertTrue(output.speak("East", 1));
        engine.getUtteranceProgressListener().onDone(first); idle(0); assertTrue(output.busy());
        engine.getUtteranceProgressListener().onDone(engine.id); idle(0); assertFalse(output.busy());
        assertNotNull(shadowOf(audio).getLastAbandonedAudioFocusRequest());
    }
    @Test public void stuckPlaybackHasBoundedTimeout() {
        ready(); assertTrue(output.speak("North", 1)); idle(15001);
        assertFalse(output.busy()); assertEquals(CompassSpeech.VoiceState.FAILED, output.state());
        assertTrue(shadowOf(engine()).isShutdown());
    }
    @Test public void deniedAudioFocusDoesNotStartSpeech() {
        ready(); shadowOf(audio).setNextFocusRequestResponse(AudioManager.AUDIOFOCUS_REQUEST_FAILED);
        assertFalse(output.speak("North", 1)); assertFalse(output.busy());
        assertEquals(CompassSpeech.VoiceState.READY, output.state());
    }
    @Test public void retryInvalidatesOldEngineCallbacks() {
        output.prepare(); TextToSpeech old = engine(); ready();
        shadowOf(old).getOnInitListener().onInit(TextToSpeech.ERROR); idle(0);
        assertEquals(CompassSpeech.VoiceState.READY, output.state()); assertTrue(shadowOf(old).isShutdown());
    }
}
