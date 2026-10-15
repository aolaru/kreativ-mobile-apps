package com.kreativ.sharesafe;

import android.graphics.RectF;

final class MaskGeometry {
    private MaskGeometry() { }
    static boolean finite(RectF area) {
        return Float.isFinite(area.left) && Float.isFinite(area.top) && Float.isFinite(area.right) && Float.isFinite(area.bottom);
    }
    static boolean valid(RectF area) {
        return area != null && finite(area) && area.left >= 0 && area.top >= 0 && area.right <= 1 && area.bottom <= 1
                && area.width() >= .002f && area.height() >= .002f;
    }
    static RectF move(RectF area, float dx, float dy) {
        float left = Math.max(0, Math.min(1 - area.width(), area.left + dx));
        float top = Math.max(0, Math.min(1 - area.height(), area.top + dy));
        return new RectF(left, top, left + area.width(), top + area.height());
    }
    static RectF resize(RectF area, int corner, float x, float y) {
        RectF result = new RectF(area);
        // Leave room above the validation threshold to avoid floating-point edge failures.
        if (corner == 0 || corner == 3) result.left = Math.max(0, Math.min(area.right - .005f, x));
        else result.right = Math.min(1, Math.max(area.left + .005f, x));
        if (corner == 0 || corner == 1) result.top = Math.max(0, Math.min(area.bottom - .005f, y));
        else result.bottom = Math.min(1, Math.max(area.top + .005f, y));
        return result;
    }
}
