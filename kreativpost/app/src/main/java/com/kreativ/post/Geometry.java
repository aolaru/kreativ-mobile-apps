package com.kreativ.post;

/** Normalized geometry, independent of density and output dimensions. */
final class Geometry {
    static float clamp(float v, float min, float max) { return Math.max(min, Math.min(max, v)); }
    static float snap(float v, float size) {
        for (float anchor : new float[]{.05f, .5f - size / 2, .95f - size})
            if (Math.abs(v - anchor) < .015f) return anchor;
        return clamp(v, 0, 1 - size);
    }
    static float[] crop(int sourceW, int sourceH, float boxW, float boxH, float zoom, float panX, float panY) {
        float scale = Math.max(boxW / sourceW, boxH / sourceH) * clamp(zoom, 1, 3);
        float w = sourceW * scale, h = sourceH * scale;
        return new float[]{-(w - boxW) * clamp(panX, 0, 1), -(h - boxH) * clamp(panY, 0, 1), w, h};
    }
}
