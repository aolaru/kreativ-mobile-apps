package com.kreativ.paperbuild;

import org.junit.Test;
import static org.junit.Assert.*;

public class CropBoundsTest {
    @Test public void rotationKeepsTheSameImageRegion() {
        CropBounds start = new CropBounds(.1f, .2f, .7f, .9f), rotated = start.clockwise();
        assertEquals(.1f, rotated.left, .0001f); assertEquals(.1f, rotated.top, .0001f);
        assertEquals(.8f, rotated.right, .0001f); assertEquals(.7f, rotated.bottom, .0001f);
        CropBounds restored = rotated.clockwise().clockwise().clockwise();
        assertEquals(start.left, restored.left, .0001f); assertEquals(start.top, restored.top, .0001f);
        assertEquals(start.right, restored.right, .0001f); assertEquals(start.bottom, restored.bottom, .0001f);
        assertTrue(CropBounds.FULL.clockwise().full());
        CropBounds narrow = new CropBounds(.99f, 0, 1, 1);
        narrow.clockwise().clockwise().clockwise().clockwise(); // Floating-point rounding must not invalidate a 1% crop.
    }
    @Test public void rejectsInvalidBounds() {
        float[][] invalid = {{-.1f, 0, 1, 1}, {0, 0, 2, 1}, {.8f, 0, .2f, 1}, {0, 0, Float.NaN, 1}, {0, 0, .001f, 1}};
        for (float[] v : invalid) {
            try { new CropBounds(v[0], v[1], v[2], v[3]); fail(); } catch (IllegalArgumentException expected) { }
        }
    }
}
