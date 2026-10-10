package com.kreativ.northlight;
import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

public class SignalLoopTest {
    @Test public void playsAndRepeatsThreeShortThreeLongThreeShort() {
        SignalLoop signal = new SignalLoop(); signal.start();
        List<Boolean> states = new ArrayList<>();
        int[] durations = {220,220,220,220,220,220,700,220,700,220,700,220,220,220,220,220,220,1100};
        for (int i = 0; i < 18; i++) {
            assertEquals(durations[i], signal.pulse(on -> { states.add(on); return true; }));
            assertEquals(i % 2 == 0, states.get(i));
        }
        assertEquals(220, signal.pulse(on -> true));
    }
    @Test public void hardwareFailureStopsWithoutSchedulingAnotherPulse() {
        SignalLoop signal = new SignalLoop(); signal.start();
        assertEquals(-1, signal.pulse(on -> false)); assertFalse(signal.running());
        assertEquals(-1, signal.pulse(on -> { fail("Stopped signal must not touch hardware"); return true; }));
    }
    @Test public void manualStopCancelsFurtherHardwareAccess() {
        SignalLoop signal = new SignalLoop(); signal.start(); signal.stop();
        assertEquals(-1, signal.pulse(on -> { fail("Stopped signal must not touch hardware"); return true; }));
    }
}
