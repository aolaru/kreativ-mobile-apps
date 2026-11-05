package com.kreativ.northlight;

import org.junit.Test;
import static org.junit.Assert.*;

public class PlaceMathTest {
    @Test public void distanceAndTrueBearingHandleCardinalsAndDateLine() {
        assertEquals(111195, PlaceMath.distance(0, 0, 0, 1), 20);
        assertEquals(90, PlaceMath.trueBearing(0, 0, 0, 1), .01);
        assertEquals(0, PlaceMath.trueBearing(0, 0, 1, 0), .01);
        assertEquals(90, PlaceMath.trueBearing(0, 179.9, 0, -179.9), .01);
        assertEquals(22239, PlaceMath.distance(0, 179.9, 0, -179.9), 20);
    }
    @Test public void declinationConvertsTrueToMagneticAndWraps() {
        assertEquals(350, PlaceMath.magneticBearing(5, 15));
        assertEquals(10, PlaceMath.magneticBearing(355, -15));
    }
    @Test public void nearbyOrUncertainFixNeverClaimsDirection() {
        assertFalse(PlaceMath.reliableDirection(10, 5, 5));
        assertFalse(PlaceMath.reliableDirection(15, 10, 7));
        assertFalse(PlaceMath.reliableDirection(100, 51, 3));
        assertFalse(PlaceMath.reliableDirection(100, Double.NaN, 3));
        assertTrue(PlaceMath.reliableDirection(30, 10, 7));
    }
}
