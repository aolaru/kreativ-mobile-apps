package com.kreativ.paperbuild;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import org.junit.Test;
import static org.junit.Assert.*;

public class AcknowledgementQueueTest {
    private static final class Clock implements AcknowledgementQueue.Scheduler {
        static class Event { long time; Runnable task; Event(long time, Runnable task) { this.time = time; this.task = task; } }
        long now; List<Event> events = new ArrayList<>();
        public void later(Runnable task, long delay) { events.add(new Event(now + delay, task)); }
        void next() { events.sort(Comparator.comparingLong(event -> event.time)); Event event = events.remove(0); now = event.time; event.task.run(); }
        void drain() { int count = 0; while (!events.isEmpty() && count++ < 100) next(); }
    }
    @Test public void deduplicatesAndIgnoresTimeoutAfterSuccess() {
        Clock clock = new Clock(); List<Consumer<Boolean>> replies = new ArrayList<>(); List<Boolean> outcomes = new ArrayList<>();
        AcknowledgementQueue queue = new AcknowledgementQueue((token, callback) -> replies.add(callback), clock, outcomes::add);
        queue.add("token"); queue.add("token"); assertEquals(1, replies.size());
        replies.get(0).accept(true); clock.drain(); assertEquals(1, replies.size());
        assertEquals(java.util.Collections.singletonList(true), outcomes);
    }
    @Test public void retriesFailuresThenAllowsForegroundRetry() {
        Clock clock = new Clock(); int[] sent = {0};
        AcknowledgementQueue queue = new AcknowledgementQueue((token, callback) -> { sent[0]++; callback.accept(false); }, clock, result -> { });
        queue.add("token"); clock.drain(); assertEquals(4, sent[0]);
        queue.add("token"); assertEquals(5, sent[0]);
    }
    @Test public void recoversFromMissingCallbackAndIgnoresLateSuccess() {
        Clock clock = new Clock(); List<Consumer<Boolean>> replies = new ArrayList<>(); List<Boolean> outcomes = new ArrayList<>();
        AcknowledgementQueue queue = new AcknowledgementQueue((token, callback) -> replies.add(callback), clock, outcomes::add);
        queue.add("token"); clock.next(); clock.next(); assertEquals(2, replies.size());
        replies.get(0).accept(true); assertEquals(java.util.Collections.singletonList(false), outcomes);
        replies.get(1).accept(true); clock.drain(); assertEquals(2, replies.size());
        assertEquals(java.util.Arrays.asList(false, true), outcomes);
    }
}
