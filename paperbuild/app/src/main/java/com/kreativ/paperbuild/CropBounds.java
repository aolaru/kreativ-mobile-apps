package com.kreativ.paperbuild;

/** Normalised coordinates in the EXIF-corrected, user-rotated image. */
final class CropBounds {
    static final CropBounds FULL = new CropBounds(0, 0, 1, 1);
    final float left, top, right, bottom;
    CropBounds(float left, float top, float right, float bottom) {
        if (!Float.isFinite(left) || !Float.isFinite(top) || !Float.isFinite(right) || !Float.isFinite(bottom)
                || left < 0 || top < 0 || right > 1 || bottom > 1 || right - left < .009999f || bottom - top < .009999f) {
            throw new IllegalArgumentException("Choose a larger crop inside the image.");
        }
        this.left = left; this.top = top; this.right = right; this.bottom = bottom;
    }
    CropBounds clockwise() { return new CropBounds(1 - bottom, left, 1 - top, right); }
    boolean full() { return left == 0 && top == 0 && right == 1 && bottom == 1; }
    String key() { return left + ":" + top + ":" + right + ":" + bottom; }
}
