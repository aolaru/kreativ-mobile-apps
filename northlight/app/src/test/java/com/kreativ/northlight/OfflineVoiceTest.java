package com.kreativ.northlight;

import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import java.util.Locale;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36, application = FieldUiTest.OfflineApplication.class)
public class OfflineVoiceTest {
    private Voice voice(String name, Locale locale, boolean network, int quality, Set<String> features) {
        return new Voice(name, locale, quality, Voice.LATENCY_NORMAL, network, features);
    }
    @Test public void excludesNetworkAndUninstalledVoices() {
        Voice network = voice("network", Locale.ENGLISH, true, 500, Set.of());
        Voice missing = voice("missing", Locale.ENGLISH, false, 500, Set.of(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED));
        assertNull(OfflineSpeechOutput.chooseVoice(Set.of(network, missing), Locale.ENGLISH));
    }
    @Test public void neverFallsBackToWrongLanguage() {
        Voice english = voice("en", Locale.US, false, 500, Set.of());
        assertNull(OfflineSpeechOutput.chooseVoice(Set.of(english), Locale.forLanguageTag("ro-RO")));
    }
    @Test public void prefersExactLocaleThenQuality() {
        Voice exact = voice("exact", Locale.UK, false, 100, Set.of());
        Voice other = voice("other", Locale.US, false, 500, Set.of());
        assertEquals(exact, OfflineSpeechOutput.chooseVoice(Set.of(other, exact), Locale.UK));
        Voice better = voice("better", Locale.UK, false, 500, Set.of());
        assertEquals(better, OfflineSpeechOutput.chooseVoice(Set.of(better, exact), Locale.UK));
    }
    @Test public void acceptsInstalledSameLanguageCountryVariant() {
        Voice english = voice("en-us-local", Locale.US, false, 500, Set.of());
        assertEquals(english, OfflineSpeechOutput.chooseVoice(Set.of(english), Locale.ENGLISH));
        assertNull(OfflineSpeechOutput.chooseVoice(null, Locale.ENGLISH));
    }
}
