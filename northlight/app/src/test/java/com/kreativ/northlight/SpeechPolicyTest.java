package com.kreativ.northlight;

import org.junit.Test;
import static org.junit.Assert.*;
import static com.kreativ.northlight.SpeechPolicy.Accuracy.*;
import static com.kreativ.northlight.SpeechPolicy.Reading.*;

public class SpeechPolicyTest {
    private SpeechPolicy valid(float heading, long time) {
        SpeechPolicy p = new SpeechPolicy(); p.accuracy(OK); p.heading(heading, time); return p;
    }
    @Test public void needsBothHeadingAndAccuracy() {
        SpeechPolicy p = new SpeechPolicy(); assertEquals(SpeechPolicy.Reading.WAITING, p.reading(0));
        p.heading(315, 0); assertEquals(SpeechPolicy.Reading.WAITING, p.reading(0));
        p.accuracy(OK); assertEquals(HEADING, p.reading(0));
    }
    @Test public void badAccuracyReplacesNumericHeading() {
        SpeechPolicy p = valid(315, 0);
        p.accuracy(SpeechPolicy.Accuracy.LOW); assertEquals(SpeechPolicy.Reading.LOW, p.reading(0));
        p.accuracy(UNRELIABLE); assertEquals(CALIBRATE, p.reading(0));
        p.accuracy(SpeechPolicy.Accuracy.UNAVAILABLE); assertEquals(SpeechPolicy.Reading.UNAVAILABLE, p.reading(0));
        p.accuracy(OK); assertEquals(SpeechPolicy.Reading.WAITING, p.reading(0));
    }
    @Test public void staleAndBackwardsTimesNeverReadHeading() {
        SpeechPolicy p = valid(100, 100); assertEquals(HEADING, p.reading(3100));
        assertEquals(STALE, p.reading(3101)); assertEquals(STALE, p.reading(99));
    }
    @Test public void rejectsNonFiniteAndNormalizesDegrees() {
        SpeechPolicy p = valid(-1, 0); assertEquals(359, p.degrees());
        p.heading(359.9f, 0); assertEquals(0, p.degrees());
        p.heading(Float.NaN, 0); assertEquals(SpeechPolicy.Reading.WAITING, p.reading(0));
        p.heading(Float.POSITIVE_INFINITY, 0); assertEquals(SpeechPolicy.Reading.WAITING, p.reading(0));
    }
    @Test public void mustSettleBeforeSpeakingAndRespectInterval() {
        SpeechPolicy p = valid(90, 0); assertNull(p.automatic(499, 5000));
        assertEquals(HEADING, p.automatic(500, 5000)); p.delivered(HEADING, 500);
        p.heading(120, 3000); assertNull(p.automatic(3500, 5000));
        p.heading(120, 5500); assertEquals(HEADING, p.automatic(5500, 5000));
    }
    @Test public void wrapAroundNorthIsSmallChange() {
        SpeechPolicy p = valid(355, 0); p.delivered(HEADING, 500);
        p.heading(5, 5500); assertNull(p.automatic(6000, 5000));
        p.heading(10, 6500); assertEquals(HEADING, p.automatic(7000, 5000));
    }
    @Test public void gradualRotationCannotFakeStability() {
        SpeechPolicy p = valid(0, 0);
        for (int i=1; i<=10; i++) { p.heading(i * 2f, i * 100); assertNull(p.automatic(i * 100, 3000)); }
        p.heading(20, 1600); assertEquals(HEADING, p.automatic(1600, 3000));
    }
    @Test public void warningsDoNotRepeatOrFlapRapidly() {
        SpeechPolicy p = valid(90, 0); p.accuracy(UNRELIABLE);
        assertEquals(CALIBRATE, p.automatic(0, 5000)); p.delivered(CALIBRATE, 0);
        assertNull(p.automatic(10000, 5000));
        p.accuracy(SpeechPolicy.Accuracy.LOW); assertNull(p.automatic(100, 5000));
        assertEquals(SpeechPolicy.Reading.LOW, p.automatic(3000, 5000));
    }
    @Test public void recoveryAnnouncesEvenUnchangedDirection() {
        SpeechPolicy p = valid(90, 0); p.delivered(HEADING, 500);
        p.accuracy(UNRELIABLE); p.delivered(CALIBRATE, 1000);
        p.accuracy(OK); p.heading(90, 6000); assertEquals(HEADING, p.automatic(6000, 5000));
    }
    @Test public void resetInvalidatesReadingsAndAnnouncementHistory() {
        SpeechPolicy p = valid(90, 0); p.delivered(HEADING, 500); p.reset();
        assertEquals(SpeechPolicy.Reading.WAITING, p.reading(6000));
        p.accuracy(OK); p.heading(90, 6000); assertEquals(HEADING, p.automatic(6500, 5000));
    }
}
