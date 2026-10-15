package com.kreativ.northlight;

import android.content.Context;

/** Target-relative prompts. Bearings describe phone orientation, never a route or an arrival. */
final class BearingGuidance {
    private static final int LEFT = -1, ALIGNED = 0, RIGHT = 1;
    private final Context context;
    private final int target;
    private final Runnable alignmentFeedback;
    private boolean aligned, announced, hapticLatched, hasPulsed;
    private int lastKind;
    private float lastError;
    private long announcedAt, pulsedAt;

    BearingGuidance(Context context, int target, Runnable alignmentFeedback) {
        if (target < 0 || target >= 360) throw new IllegalArgumentException("Invalid bearing");
        this.context = context; this.target = target; this.alignmentFeedback = alignmentFeedback;
    }
    int target() { return target; }
    private float error(float heading) { return HeadingMath.difference(target, heading); }
    private int kind(float heading) {
        float delta = error(heading);
        // Enter within 3 degrees; leave beyond 6 to prevent chatter from small sensor fluctuations.
        if (aligned && Math.abs(delta) > 6) aligned = false;
        else if (!aligned && Math.abs(delta) <= 3) aligned = true;
        return aligned ? ALIGNED : delta < 0 ? LEFT : RIGHT;
    }
    boolean automatic(float heading, long now, long interval) {
        int next = kind(heading);
        if (!announced) return true;
        if (now - announcedAt < interval) return false;
        return next != lastKind || (next != ALIGNED && Math.abs(error(heading) - lastError) >= 10);
    }
    boolean obsolete(float heading) { return announced && kind(heading) != lastKind; }
    void delivered(float heading, long now) {
        lastKind = kind(heading); lastError = error(heading); announcedAt = now; announced = true;
    }
    void feedback(float heading, boolean stable, long now) {
        float amount = Math.abs(error(heading));
        if (amount > 8) hapticLatched = false;
        if (stable && kind(heading) == ALIGNED && !hapticLatched && (!hasPulsed || now - pulsedAt >= 5_000)) {
            hapticLatched = true; hasPulsed = true; pulsedAt = now; alignmentFeedback.run();
        }
    }
    void reset() { aligned = announced = hapticLatched = false; /* Preserve haptic cooldown across pause/recovery. */ }
    String visualInstruction(float heading) {
        float delta = error(heading);
        // Rendering must not arm the automatic alignment cue from a fleeting sensor sample.
        int next = Math.abs(delta) <= (aligned ? 6 : 3) ? ALIGNED : delta < 0 ? LEFT : RIGHT;
        return next == ALIGNED ? context.getString(R.string.guidance_aligned)
                : context.getString(next == LEFT ? R.string.guidance_left : R.string.guidance_right, Math.max(1, Math.round(Math.abs(error(heading)))));
    }
    String text(float heading) {
        int next = kind(heading), turn = Math.max(1, Math.round(Math.abs(error(heading))));
        String instruction = next == ALIGNED ? context.getString(R.string.bearing_audio_aligned)
                : context.getResources().getQuantityString(next == LEFT ? R.plurals.bearing_audio_left : R.plurals.bearing_audio_right, turn, turn);
        String direction = context.getResources().getStringArray(R.array.compass_directions)[((target + 22) % 360) / 45];
        return context.getResources().getQuantityString(R.plurals.bearing_audio_prompt, target, direction, target, instruction);
    }
}
