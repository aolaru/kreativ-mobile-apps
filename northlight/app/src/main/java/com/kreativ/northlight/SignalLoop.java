package com.kreativ.northlight;

/** Scheduling is supplied by Android; the failure/stop semantics are unit-testable. */
final class SignalLoop {
    interface Torch { boolean set(boolean on); }
    static final int[] SOS = {220, 220, 220, 220, 220, 220, 700, 220, 700, 220, 700, 220, 220, 220, 220, 220, 220, 1100};
    private boolean running;
    private int index;
    void start() { running = true; index = 0; }
    void stop() { running = false; }
    boolean running() { return running; }
    int pulse(Torch torch) {
        if (!running) return -1;
        if (!torch.set(index % 2 == 0)) { stop(); return -1; }
        if (!running) return -1;
        int delay = SOS[index]; index = (index + 1) % SOS.length; return delay;
    }
}
