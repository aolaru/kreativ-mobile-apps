package com.kreativ.fontboard;

import org.junit.Test;
import static org.junit.Assert.*;

public class PreviewTransformTest {
    @Test public void fitsWideArtworkAndCentersIt() {
        PreviewTransform image = new PreviewTransform();
        image.setGeometry(1000, 1000, 2000, 1000);
        assertTrue(image.ready());
        assertEquals(.5f, image.scale(), .001f);
        assertEquals(0, image.left, .001f);
        assertEquals(250, image.top, .001f);
    }

    @Test public void zoomKeepsTheFocusPointInPlace() {
        PreviewTransform image = new PreviewTransform();
        image.setGeometry(1000, 1000, 1000, 1000);
        image.zoomBy(2, 250, 250);
        assertEquals(2, image.zoom, .001f);
        assertEquals(-250, image.left, .001f);
        assertEquals(-250, image.top, .001f);
    }

    @Test public void clampsPanningAndZoomToSafeBounds() {
        PreviewTransform image = new PreviewTransform();
        image.setGeometry(1000, 1000, 1000, 1000);
        image.zoomBy(100, 500, 500);
        assertEquals(5, image.zoom, .001f);
        image.pan(100000, 100000);
        assertEquals(0, image.left, .001f);
        assertEquals(0, image.top, .001f);
        image.pan(-100000, -100000);
        assertEquals(-4000, image.left, .001f);
        assertEquals(-4000, image.top, .001f);
        image.zoomBy(.001f, 500, 500);
        assertEquals(1, image.zoom, .001f);
        assertEquals(0, image.left, .001f);
        assertEquals(0, image.top, .001f);
    }

    @Test public void shortAxisStaysCenteredWhileZoomed() {
        PreviewTransform image = new PreviewTransform();
        image.setGeometry(1000, 1000, 4000, 1000);
        image.zoomBy(2, 500, 500);
        image.pan(-500, -500);
        assertEquals(250, image.top, .001f);
        assertEquals(-1000, image.left, .001f);
    }

    @Test public void restoresNormalizedCenterAcrossRotation() {
        PreviewTransform image = new PreviewTransform();
        image.setGeometry(1000, 1000, 1000, 1000);
        image.zoomBy(3, 500, 500);
        image.pan(-250, -200);
        float x = image.centerX(), y = image.centerY();
        image.setGeometry(1600, 900, 1000, 1000);
        image.restore(3, x, y);
        assertEquals(x, image.centerX(), .001f);
        assertEquals(y, image.centerY(), .001f);
        assertEquals(3, image.zoom, .001f);
    }

    @Test public void missingGeometryAndInvalidInputDoNotCorruptTransform() {
        PreviewTransform image = new PreviewTransform();
        image.zoomBy(2, 50, 50);
        assertFalse(image.ready());
        image.setGeometry(100, 100, 100, 100);
        image.zoomBy(Float.NaN, 50, 50);
        image.zoomBy(-2, 50, 50);
        image.pan(Float.NaN, 0);
        image.restore(Float.NaN, Float.NaN, Float.NaN);
        assertEquals(1, image.zoom, .001f);
        assertEquals(0, image.left, .001f);
        assertEquals(0, image.top, .001f);
    }
}
