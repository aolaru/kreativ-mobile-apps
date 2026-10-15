package com.kreativ.northlight;

/** Pure, monotonic-clock gating. Audio never treats an old/low-confidence heading as valid. */
final class SpeechPolicy {
    enum Accuracy { WAITING, OK, LOW, UNRELIABLE, UNAVAILABLE }
    enum Reading { HEADING, WAITING, LOW, CALIBRATE, UNAVAILABLE, STALE }
    static final long FRESH_MS = 3_000, STABLE_MS = 500;
    static final float CHANGE_DEGREES = 15f;
    private float heading = Float.NaN, anchor = Float.NaN, announcedHeading = Float.NaN;
    private long measuredAt, stableSince, announcedAt, warnedAt;
    private boolean announced, warned;
    private Reading lastReading, lastWarning;
    private Accuracy accuracy = Accuracy.WAITING;

    void reset() {
        heading = anchor = announcedHeading = Float.NaN;
        accuracy = Accuracy.WAITING; measuredAt = stableSince = 0;
        resetAnnouncements();
    }
    void resetAnnouncements() {
        announced = warned = false; lastReading = lastWarning = null;
        announcedHeading = Float.NaN;
    }
    void heading(float value, long now) {
        if (!Float.isFinite(value)) { heading = anchor = Float.NaN; return; }
        heading = HeadingMath.normalize(value); measuredAt = now;
        if (Float.isNaN(anchor) || Math.abs(HeadingMath.difference(heading, anchor)) > 3f) {
            anchor = heading; stableSince = now;
        }
    }
    void accuracy(Accuracy value) {
        accuracy = value;
        if (value == Accuracy.UNAVAILABLE || value == Accuracy.WAITING) heading = anchor = Float.NaN;
    }
    Reading reading(long now) {
        if (accuracy == Accuracy.UNAVAILABLE) return Reading.UNAVAILABLE;
        if (accuracy == Accuracy.UNRELIABLE) return Reading.CALIBRATE;
        if (accuracy == Accuracy.LOW) return Reading.LOW;
        if (accuracy == Accuracy.WAITING || Float.isNaN(heading)) return Reading.WAITING;
        if (now - measuredAt > FRESH_MS || now < measuredAt) return Reading.STALE;
        return Reading.HEADING;
    }
    int degrees() { return HeadingMath.rounded(heading); }
    float heading() { return heading; }
    boolean stable(long now) { return reading(now) == Reading.HEADING && now - stableSince >= STABLE_MS; }
    Reading automatic(long now, long intervalMs) {
        Reading next = reading(now);
        if (next != Reading.HEADING) {
            // Warn once per condition, with a separate anti-flapping cooldown.
            return next != lastWarning && (!warned || now - warnedAt >= 3_000) ? next : null;
        }
        if (now - stableSince < STABLE_MS) return null;
        if (announced && now - announcedAt < intervalMs) return null;
        if (lastReading == Reading.HEADING && Float.isFinite(announcedHeading)
                && Math.abs(HeadingMath.difference(heading, announcedHeading)) < CHANGE_DEGREES) return null;
        return next;
    }
    void delivered(Reading reading, long now) {
        announced = true; announcedAt = now; lastReading = reading;
        if (reading == Reading.HEADING) {
            announcedHeading = heading; lastWarning = null;
        } else {
            warned = true; warnedAt = now; lastWarning = reading;
        }
    }
}
