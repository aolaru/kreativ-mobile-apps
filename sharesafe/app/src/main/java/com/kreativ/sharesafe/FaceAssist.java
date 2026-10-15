package com.kreativ.sharesafe;

import android.graphics.Bitmap;
import android.graphics.RectF;
import java.util.List;

/** Returns suggestions only, in the same orientation/normalized space as editor masks. */
interface FaceAssist {
    List<RectF> find(Bitmap uprightImage) throws Exception;

    static RectF padded(RectF face, int width, int height) {
        if (width <= 0 || height <= 0 || face == null || !MaskGeometry.finite(face) || face.width() <= 0 || face.height() <= 0) return null;
        float dx = face.width() * .18f, dy = face.height() * .22f;
        RectF area = new RectF(Math.max(0, (face.left - dx) / width), Math.max(0, (face.top - dy) / height),
                Math.min(1, (face.right + dx) / width), Math.min(1, (face.bottom + dy) / height));
        return MaskGeometry.valid(area) ? area : null;
    }
}
