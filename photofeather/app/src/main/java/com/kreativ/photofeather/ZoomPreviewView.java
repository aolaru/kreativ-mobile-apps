package com.kreativ.photofeather;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;

final class ZoomPreviewView extends ImageView {
  final PreviewTransform transform = new PreviewTransform();
  private final Matrix matrix = new Matrix();
  private final GestureDetector gestures;
  private final ScaleGestureDetector pinch;
  private float[] pendingRestore;
  private boolean tapped;

  ZoomPreviewView(Context context) {
    super(context);
    setScaleType(ScaleType.MATRIX);
    setClickable(true);
    pinch =
        new ScaleGestureDetector(
            context,
            new ScaleGestureDetector.SimpleOnScaleGestureListener() {
              @Override
              public boolean onScale(ScaleGestureDetector detector) {
                transform.zoomBy(
                    detector.getScaleFactor(), detector.getFocusX(), detector.getFocusY());
                applyTransform();
                return true;
              }
            });
    gestures =
        new GestureDetector(
            context,
            new GestureDetector.SimpleOnGestureListener() {
              @Override
              public boolean onDown(MotionEvent event) {
                return true;
              }

              @Override
              public boolean onDoubleTap(MotionEvent event) {
                transform.zoomBy(
                    transform.zoom > 1.01f ? 1 / transform.zoom : 2.5f, event.getX(), event.getY());
                applyTransform();
                return true;
              }

              @Override
              public boolean onSingleTapUp(MotionEvent event) {
                tapped = true;
                return true;
              }

              @Override
              public boolean onScroll(MotionEvent first, MotionEvent current, float dx, float dy) {
                if (pinch.isInProgress() || current.getPointerCount() > 1) return true;
                transform.pan(-dx, -dy);
                applyTransform();
                return true;
              }
            });
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    if (getDrawable() == null) return false;
    if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
    pinch.onTouchEvent(event);
    gestures.onTouchEvent(event);
    if (tapped) {
      tapped = false;
      performClick();
    }
    return true;
  }

  @Override
  public boolean performClick() {
    super.performClick();
    return true;
  }

  @Override
  public void setImageDrawable(Drawable drawable) {
    super.setImageDrawable(drawable);
    // ImageView's constructor may call this before this subclass is initialised.
    if (transform != null) fitDrawable();
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    fitDrawable();
  }

  void restoreZoom(float zoom, float centerX, float centerY) {
    pendingRestore = new float[] {zoom, centerX, centerY};
    fitDrawable();
  }

  void zoomIn() {
    transform.zoomBy(1.5f, getWidth() / 2f, getHeight() / 2f);
    applyTransform();
  }

  void zoomOut() {
    transform.zoomBy(1 / 1.5f, getWidth() / 2f, getHeight() / 2f);
    applyTransform();
  }

  void fit() {
    transform.restore(1, .5f, .5f);
    applyTransform();
  }

  private void fitDrawable() {
    Drawable drawable = getDrawable();
    if (drawable == null
        || drawable.getIntrinsicWidth() <= 0
        || drawable.getIntrinsicHeight() <= 0
        || getWidth() <= 0
        || getHeight() <= 0) return;
    float zoom = transform.zoom, centerX = transform.centerX(), centerY = transform.centerY();
    transform.setGeometry(
        getWidth(), getHeight(), drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
    if (pendingRestore != null) {
      transform.restore(pendingRestore[0], pendingRestore[1], pendingRestore[2]);
      pendingRestore = null;
    } else transform.restore(zoom, centerX, centerY);
    applyTransform();
  }

  private void applyTransform() {
    matrix.setValues(
        new float[] {
          transform.scale(), 0, transform.left, 0, transform.scale(), transform.top, 0, 0, 1
        });
    setImageMatrix(matrix);
  }
}
