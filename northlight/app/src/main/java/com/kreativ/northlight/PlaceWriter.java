package com.kreativ.northlight;

import java.util.ArrayList;
import java.util.concurrent.Executor;

/** Coordinates one durable saved-place write at a time across activity recreation. */
final class PlaceWriter {
    interface Write { boolean commit(String value); }
    interface Listener { void onPlaceWriteChanged(boolean failed); }

    private final Executor worker, main;
    private final Write write;
    private final ArrayList<Listener> listeners = new ArrayList<>();
    private boolean pending;

    PlaceWriter(Executor worker, Executor main, Write write) {
        this.worker = worker; this.main = main; this.write = write;
    }
    boolean pending() { return pending; }
    void addListener(Listener listener) { listeners.add(listener); }
    void removeListener(Listener listener) { listeners.remove(listener); }
    boolean save(String value) {
        if (pending) return false;
        pending = true;
        notifyListeners(false);
        worker.execute(() -> {
            boolean success;
            try { success = write.commit(value); }
            catch (RuntimeException failure) { success = false; }
            final boolean failed = !success;
            main.execute(() -> {
                pending = false;
                notifyListeners(failed);
            });
        });
        return true;
    }
    private void notifyListeners(boolean failed) {
        for (Listener listener : new ArrayList<>(listeners)) listener.onPlaceWriteChanged(failed);
    }
}
