package com.kreativ.paperbuild;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/** Serial-callback acknowledgement retries with timeout recovery and token deduplication. */
final class AcknowledgementQueue {
    interface Sender { void send(String token, Consumer<Boolean> result); }
    interface Scheduler { void later(Runnable task, long milliseconds); }
    private final Sender sender;
    private final Scheduler scheduler;
    private final Consumer<Boolean> result;
    private final Map<String, Attempt> pending = new HashMap<>();
    private static final long[] RETRY_DELAYS = {3000, 15000, 60000};
    private static final class Attempt { int number, sequence; }
    AcknowledgementQueue(Sender sender, Scheduler scheduler, Consumer<Boolean> result) {
        this.sender = sender; this.scheduler = scheduler; this.result = result;
    }
    void add(String token) {
        if (pending.containsKey(token)) return;
        Attempt attempt = new Attempt(); pending.put(token, attempt); send(token, attempt);
    }
    private void send(String token, Attempt attempt) {
        int sequence = ++attempt.sequence;
        scheduler.later(() -> finish(token, attempt, sequence, false), 15000);
        sender.send(token, success -> finish(token, attempt, sequence, success));
    }
    private void finish(String token, Attempt attempt, int sequence, boolean success) {
        if (pending.get(token) != attempt || sequence != attempt.sequence) return;
        attempt.sequence++; // Ignore a duplicate callback or a late response from a timed-out request.
        if (success) { pending.remove(token); result.accept(true); return; }
        result.accept(false);
        if (attempt.number >= RETRY_DELAYS.length) { pending.remove(token); return; }
        long delay = RETRY_DELAYS[attempt.number++];
        scheduler.later(() -> { if (pending.get(token) == attempt) send(token, attempt); }, delay);
    }
}
