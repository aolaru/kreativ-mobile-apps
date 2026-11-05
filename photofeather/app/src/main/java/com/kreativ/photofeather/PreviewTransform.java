package com.kreativ.photofeather;

/** Bounded zoom/pan geometry, independent of Android so gestures can be regression tested. */
final class PreviewTransform {
  static final float MAX_ZOOM = 5f;
  private float width, height, imageWidth, imageHeight, fit;
  float zoom = 1f, left, top;

  boolean ready() {
    return width > 0 && height > 0 && imageWidth > 0 && imageHeight > 0;
  }

  float scale() {
    return fit * zoom;
  }

  void setGeometry(float width, float height, float imageWidth, float imageHeight) {
    this.width = width;
    this.height = height;
    this.imageWidth = imageWidth;
    this.imageHeight = imageHeight;
    if (!ready()) return;
    fit = Math.min(width / imageWidth, height / imageHeight);
    restore(1, .5f, .5f);
  }

  void restore(float zoom, float centerX, float centerY) {
    if (!ready()) return;
    this.zoom = Float.isFinite(zoom) ? clamp(zoom, 1, MAX_ZOOM) : 1;
    centerX = Float.isFinite(centerX) ? clamp(centerX, 0, 1) : .5f;
    centerY = Float.isFinite(centerY) ? clamp(centerY, 0, 1) : .5f;
    left = width / 2 - centerX * imageWidth * scale();
    top = height / 2 - centerY * imageHeight * scale();
    constrain();
  }

  void zoomBy(float factor, float focusX, float focusY) {
    if (!ready() || !Float.isFinite(factor) || factor <= 0) return;
    float next = clamp(zoom * factor, 1, MAX_ZOOM);
    float ratio = next / zoom;
    left = focusX - (focusX - left) * ratio;
    top = focusY - (focusY - top) * ratio;
    zoom = next;
    constrain();
  }

  void pan(float dx, float dy) {
    if (!ready() || !Float.isFinite(dx) || !Float.isFinite(dy)) return;
    left += dx;
    top += dy;
    constrain();
  }

  float centerX() {
    return ready() ? (width / 2 - left) / (imageWidth * scale()) : .5f;
  }

  float centerY() {
    return ready() ? (height / 2 - top) / (imageHeight * scale()) : .5f;
  }

  private void constrain() {
    float scaledWidth = imageWidth * scale(), scaledHeight = imageHeight * scale();
    left = scaledWidth <= width ? (width - scaledWidth) / 2 : clamp(left, width - scaledWidth, 0);
    top =
        scaledHeight <= height ? (height - scaledHeight) / 2 : clamp(top, height - scaledHeight, 0);
  }

  private static float clamp(float value, float min, float max) {
    return Math.max(min, Math.min(max, value));
  }
}
