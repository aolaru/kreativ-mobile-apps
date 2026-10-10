package com.kreativ.northlight;
import org.junit.Test;
import static org.junit.Assert.*;

public class HeadingMathTest {
    @Test public void wrapsNegativeAndMultipleTurns() {
        assertEquals(350f, HeadingMath.normalize(-10), .001f);
        assertEquals(10f, HeadingMath.normalize(730), .001f);
        assertEquals(0, HeadingMath.rounded(359.9f));
    }
    @Test public void takesShortestTurnAcrossNorth() {
        assertEquals(20f, HeadingMath.difference(10, 350), .001f);
        assertEquals(-20f, HeadingMath.difference(350, 10), .001f);
        assertEquals(-180f, HeadingMath.difference(180, 0), .001f);
    }
    @Test public void remapsAllFourDisplayRotations() {
        assertArrayEquals(new int[]{1, 2}, HeadingMath.axes(0));
        assertArrayEquals(new int[]{2, 129}, HeadingMath.axes(1));
        assertArrayEquals(new int[]{129, 130}, HeadingMath.axes(2));
        assertArrayEquals(new int[]{130, 1}, HeadingMath.axes(3));
    }
}
