package com.kreativ.northlight;
import org.junit.Test;
import static org.junit.Assert.*;

public class BearingDataTest {
    @Test public void preservesExistingFormatAndSupportsManagement() throws Exception {
        BearingData data = new BearingData("[{\"name\":\"Trail\",\"degrees\":45}]");
        assertEquals(45, data.degrees(0));
        data.rename(0, "Home");
        assertEquals("Home", new BearingData(data.toString()).name(0));
        data.delete(0); assertEquals(0, data.size());
    }
    @Test public void refusesNinthBearingWithoutEvicting() throws Exception {
        BearingData data = new BearingData("[]");
        for (int i = 0; i < 8; i++) assertTrue(data.add("Bearing " + i, i));
        String original = data.toString();
        assertFalse(data.add("Ninth", 99)); assertEquals(original, data.toString());
        data.delete(3); assertTrue(data.add("Replacement", 99)); assertEquals(8, data.size());
    }
    @Test public void savesTheCapturedValueNotLaterHeading() throws Exception {
        float current = 45f;
        final int captured = HeadingMath.rounded(current);
        current = 175f;
        BearingData data = new BearingData("[]"); data.add("Captured", captured);
        assertEquals(45, data.degrees(0)); assertNotEquals(HeadingMath.rounded(current), data.degrees(0));
    }
    @Test(expected = org.json.JSONException.class) public void corruptDataIsNotSilentlyReplaced() throws Exception {
        new BearingData("broken");
    }
}
