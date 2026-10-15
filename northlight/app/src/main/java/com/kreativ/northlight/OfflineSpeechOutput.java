package com.kreativ.northlight;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import java.util.Comparator;
import java.util.Locale;
import java.util.Set;

/** Lazy TTS: only an installed, non-network voice matching the UI language is accepted. */
final class OfflineSpeechOutput implements CompassSpeech.Output {
    private final Context context;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final AudioManager audio;
    private final AudioAttributes attributes = new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build();
    private final AudioFocusRequest focus;
    private TextToSpeech tts;
    private Runnable changed = () -> { };
    private CompassSpeech.VoiceState state = CompassSpeech.VoiceState.IDLE;
    private int generation, utterance;
    private String current;
    private boolean closed, focused, busy;
    private final Runnable timeout = () -> fail(CompassSpeech.VoiceState.FAILED);
    private final Runnable playbackTimeout = () -> fail(CompassSpeech.VoiceState.FAILED);

    OfflineSpeechOutput(Context context) {
        this.context = context.getApplicationContext(); audio = context.getSystemService(AudioManager.class);
        focus = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(attributes).setAcceptsDelayedFocusGain(false)
                .setOnAudioFocusChangeListener(change -> {
                    if (change < 0 && focused) { stop(); state = CompassSpeech.VoiceState.INTERRUPTED; changed.run(); }
                }, handler).build();
    }
    @Override public void listener(Runnable listener) { changed = listener; }
    @Override public CompassSpeech.VoiceState state() { return state; }
    @Override public boolean busy() { return busy; }
    @Override public void prepare() {
        if (closed) return;
        release(); state = CompassSpeech.VoiceState.STARTING;
        int token = generation;
        try {
            tts = new TextToSpeech(context, result -> handler.post(() -> initialized(token, result)));
            handler.postDelayed(timeout, 10_000);
        } catch (RuntimeException error) { fail(CompassSpeech.VoiceState.FAILED); }
        changed.run();
    }
    private void initialized(int token, int result) {
        if (closed || token != generation || state != CompassSpeech.VoiceState.STARTING) return;
        handler.removeCallbacks(timeout);
        if (result != TextToSpeech.SUCCESS || tts == null) { fail(CompassSpeech.VoiceState.FAILED); return; }
        try {
            Locale appLocale = context.getResources().getConfiguration().getLocales().get(0);
            Locale requested = "ro".equals(appLocale.getLanguage()) ? Locale.forLanguageTag("ro-RO") : Locale.ENGLISH;
            Set<Voice> voices = tts.getVoices();
            Voice voice = chooseVoice(voices, requested);
            if (voice == null || tts.setVoice(voice) != TextToSpeech.SUCCESS) { fail(CompassSpeech.VoiceState.MISSING); return; }
            if (tts.setAudioAttributes(attributes) != TextToSpeech.SUCCESS) { fail(CompassSpeech.VoiceState.FAILED); return; }
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override public void onStart(String id) { }
                @Override public void onDone(String id) { handler.post(() -> finished(token, id, false)); }
                @Override public void onStop(String id, boolean interrupted) { handler.post(() -> finished(token, id, true)); }
                @Override public void onError(String id) { handler.post(() -> finished(token, id, true)); }
                @Override public void onError(String id, int code) { handler.post(() -> finished(token, id, true)); }
            });
            state = CompassSpeech.VoiceState.READY; changed.run();
        } catch (RuntimeException error) { fail(CompassSpeech.VoiceState.FAILED); }
    }
    static Voice chooseVoice(Set<Voice> voices, Locale locale) {
        if (voices == null) return null;
        return voices.stream().filter(v -> !v.isNetworkConnectionRequired()
                && locale.getLanguage().equals(v.getLocale().getLanguage())
                && (v.getFeatures() == null || !v.getFeatures().contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)))
                .sorted(Comparator.<Voice>comparingInt(v -> v.getLocale().equals(locale) ? 0 : 1)
                        .thenComparing(Comparator.comparingInt(Voice::getQuality).reversed()).thenComparing(Voice::getName))
                .findFirst().orElse(null);
    }
    @Override public boolean speak(String text, float rate) {
        if (closed || state != CompassSpeech.VoiceState.READY || tts == null) return false;
        try {
            Voice voice = tts.getVoice();
            if (voice == null || voice.isNetworkConnectionRequired()
                    || (voice.getFeatures() != null && voice.getFeatures().contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED))) {
                fail(CompassSpeech.VoiceState.MISSING); return false;
            }
            if (audio.requestAudioFocus(focus) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) return false;
            focused = true;
            if (tts.setSpeechRate(rate) != TextToSpeech.SUCCESS) { fail(CompassSpeech.VoiceState.FAILED); return false; }
            current = "northlight-" + generation + "-" + (++utterance); busy = true;
            if (tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, current) != TextToSpeech.SUCCESS) {
                fail(CompassSpeech.VoiceState.FAILED); return false;
            }
            handler.postDelayed(playbackTimeout, 15_000);
            return true;
        } catch (RuntimeException error) { fail(CompassSpeech.VoiceState.FAILED); return false; }
    }
    private void finished(int token, String id, boolean error) {
        if (closed || token != generation || current == null || !current.equals(id)) return;
        handler.removeCallbacks(playbackTimeout); current = null; busy = false; abandonFocus();
        if (error) state = CompassSpeech.VoiceState.FAILED;
        changed.run();
    }
    private void fail(CompassSpeech.VoiceState failure) { release(); state = failure; changed.run(); }
    @Override public void stop() {
        handler.removeCallbacks(playbackTimeout); current = null; busy = false;
        if (tts != null) try { tts.stop(); } catch (RuntimeException ignored) { }
        abandonFocus();
    }
    private void abandonFocus() {
        if (focused) { focused = false; audio.abandonAudioFocusRequest(focus); }
    }
    private void release() {
        ++generation; handler.removeCallbacks(timeout); stop();
        if (tts != null) { TextToSpeech old = tts; tts = null; try { old.shutdown(); } catch (RuntimeException ignored) { } }
    }
    @Override public void close() { closed = true; release(); changed = () -> { }; handler.removeCallbacksAndMessages(null); }
}
