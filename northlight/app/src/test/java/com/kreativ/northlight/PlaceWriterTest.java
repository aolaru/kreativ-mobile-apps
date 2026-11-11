package com.kreativ.northlight;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Executor;
import org.junit.Test;
import static org.junit.Assert.*;

public class PlaceWriterTest {
    private static class Queue implements Executor {
        final ArrayDeque<Runnable> tasks = new ArrayDeque<>();
        @Override public void execute(Runnable task) { tasks.add(task); }
        void runNext() { tasks.remove().run(); }
    }

    @Test public void writeRunsInBackgroundAndBlocksOverlappingEditsUntilCompletion() {
        Queue worker = new Queue(), main = new Queue();
        ArrayList<String> writes = new ArrayList<>(), events = new ArrayList<>();
        PlaceWriter writer = new PlaceWriter(worker, main, value -> { writes.add(value); return true; });
        writer.addListener(failed -> events.add(writer.pending() + ":" + failed));

        assertTrue(writer.save("first"));
        assertTrue(writer.pending());
        assertFalse(writer.save("second"));
        assertTrue(writes.isEmpty());
        worker.runNext();
        assertEquals(Arrays.asList("first"), writes);
        assertTrue(writer.pending());
        main.runNext();
        assertFalse(writer.pending());
        assertEquals(Arrays.asList("true:false", "false:false"), events);
        assertTrue(writer.save("second"));
    }

    @Test public void completionReachesReplacementListenerAndReportsFailure() {
        Queue worker = new Queue(), main = new Queue();
        ArrayList<Boolean> oldEvents = new ArrayList<>(), newEvents = new ArrayList<>();
        PlaceWriter writer = new PlaceWriter(worker, main, value -> { throw new IllegalStateException(); });
        PlaceWriter.Listener old = oldEvents::add;
        writer.addListener(old);
        writer.save("place");
        writer.removeListener(old);
        writer.addListener(newEvents::add);
        worker.runNext();
        main.runNext();
        assertEquals(Arrays.asList(false), oldEvents);
        assertEquals(Arrays.asList(true), newEvents);
        assertFalse(writer.pending());
    }
}
