package com.kreativ.photofeather;

import android.app.*;
import android.graphics.Bitmap;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.core.view.*;
import com.kreativ.imageengine.ImageEngine;
import java.io.File;
import java.util.concurrent.*;

/** Both views use the same normalised zoom and centre; switching preserves the comparison area. */
public class PreviewActivity extends Activity {
  Button originalButton, outputButton;
  ZoomPreviewView image;
  Bitmap before, after;
  TextView label;
  boolean original;
  PhotoItem item;
  final ExecutorService loader = Executors.newSingleThreadExecutor();
  float restoredZoom = 1, restoredX = .5f, restoredY = .5f;

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    if (state != null) {
      original = state.getBoolean("original");
      restoredZoom = state.getFloat("zoom", 1);
      restoredX = state.getFloat("cx", .5f);
      restoredY = state.getFloat("cy", .5f);
    }
    Session session = ((FeatherApp) getApplication()).session;
    String id = getIntent().getStringExtra("id");
    for (PhotoItem p : session.items) if (p.id.equals(id)) item = p;
    if (session.busy && session.items.isEmpty()) {
      TextView loading = Ui.text(this, "Restoring your comparison…", 18, R.color.ink, false);
      setContentView(loading);
      session.main.postDelayed(
          () -> {
            if (!isDestroyed() && !isFinishing()) recreate();
          },
          150);
      return;
    }
    if (item == null || !item.hasOutput()) {
      finish();
      return;
    }
    LinearLayout page = Ui.column(this);
    page.setPadding(Ui.dp(this, 16), Ui.dp(this, 12), Ui.dp(this, 16), Ui.dp(this, 12));
    page.setBackgroundColor(Ui.color(this, R.color.surface));
    LinearLayout top = Ui.row(this);
    Button back = Ui.button(this, "Back", false);
    back.setOnClickListener(v -> finish());
    top.addView(back);
    TextView title = Ui.text(this, "  Compare", 22, R.color.ink, true);
    top.addView(title);
    page.addView(top);
    Ui.gap(page, 12);
    label = Ui.text(this, "Loading comparison…", 15, R.color.ink, true);
    label.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
    page.addView(label);
    Ui.gap(page, 12);
    image = new ZoomPreviewView(this);
    image.setBackgroundColor(Ui.color(this, R.color.mint));
    image.setContentDescription(
        "Photo comparison. Pinch to zoom, drag to pan, or use the zoom buttons below.");
    page.addView(
        image,
        new LinearLayout.LayoutParams(
            -1,
            Ui.dp(
                this,
                Math.max(
                    180, Math.min(440, getResources().getConfiguration().screenHeightDp - 330)))));
    Ui.gap(page, 12);
    LinearLayout toggles = Ui.row(this);
    Button orig = Ui.button(this, "Original", false), output = Ui.button(this, "Output", true);
    originalButton = orig;
    outputButton = output;
    toggles.addView(orig, new LinearLayout.LayoutParams(0, -2, 1));
    LinearLayout.LayoutParams second = new LinearLayout.LayoutParams(0, -2, 1);
    second.setMarginStart(Ui.dp(this, 8));
    toggles.addView(output, second);
    page.addView(toggles);
    orig.setOnClickListener(v -> switchImage(true));
    output.setOnClickListener(v -> switchImage(false));
    Ui.gap(page, 8);
    LinearLayout zoom = Ui.row(this);
    String[] names = {"Zoom out", "Fit", "Zoom in"};
    for (int i = 0; i < 3; i++) {
      final int action = i;
      Button b = Ui.button(this, names[i], false);
      LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1);
      if (i > 0) lp.setMarginStart(Ui.dp(this, 6));
      zoom.addView(b, lp);
      b.setOnClickListener(
          v -> {
            if (action == 0) image.zoomOut();
            else if (action == 1) image.fit();
            else image.zoomIn();
          });
    }
    page.addView(zoom);
    Ui.gap(page, 8);
    page.addView(
        Ui.text(
            this,
            "Same zoom when switching. Preview decoding is limited to 2,048 px; exported dimensions"
                + " are shown above.",
            12,
            R.color.muted,
            false));
    ScrollView scroll = new ScrollView(this);
    scroll.setFillViewport(true);
    scroll.addView(page);
    scroll.setBackgroundColor(Ui.color(this, R.color.surface));
    ViewCompat.setOnApplyWindowInsetsListener(
        scroll,
        (v, insets) -> {
          androidx.core.graphics.Insets bars =
              insets.getInsets(
                  WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
          v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
          return insets;
        });
    setContentView(scroll);
    loader.execute(
        () -> {
          Bitmap a = null, b = null;
          try {
            a = ImageEngine.preview(new File(item.source), item.orientation, 2048);
            b = ImageEngine.preview(new File(item.output), 1, 2048);
            Bitmap aa = a, bb = b;
            runOnUiThread(
                () -> {
                  if (isDestroyed()) {
                    aa.recycle();
                    bb.recycle();
                    return;
                  }
                  before = aa;
                  after = bb;
                  switchImage(original);
                  image.restoreZoom(restoredZoom, restoredX, restoredY);
                });
          } catch (Exception | OutOfMemoryError e) {
            if (a != null) a.recycle();
            if (b != null) b.recycle();
            runOnUiThread(() -> label.setText(R.string.preview_unavailable));
          }
        });
  }

  void switchImage(boolean showOriginal) {
    if (before == null || after == null) return;
    float z = image.transform.zoom, cx = image.transform.centerX(), cy = image.transform.centerY();
    original = showOriginal;
    Ui.styleButton(originalButton, original);
    Ui.styleButton(outputButton, !original);
    originalButton.setSelected(original);
    outputButton.setSelected(!original);
    ViewCompat.setStateDescription(originalButton, original ? "Selected" : null);
    ViewCompat.setStateDescription(outputButton, !original ? "Selected" : null);
    image.setImageBitmap(original ? before : after);
    image.restoreZoom(z, cx, cy);
    label.setText(
        original
            ? "Original · " + Ui.size(item.before) + " · " + item.ow + " × " + item.oh + " px"
            : "Output · " + Ui.size(item.after) + " · " + item.w + " × " + item.h + " px");
    image.setContentDescription(
        (original ? "Original" : "JPEG output") + " photo. Pinch to zoom or use the zoom buttons.");
  }

  @Override
  protected void onSaveInstanceState(Bundle out) {
    out.putBoolean("original", original);
    if (image != null) {
      out.putFloat("zoom", image.transform.zoom);
      out.putFloat("cx", image.transform.centerX());
      out.putFloat("cy", image.transform.centerY());
    }
    super.onSaveInstanceState(out);
  }

  @Override
  protected void onDestroy() {
    loader.shutdownNow();
    if (image != null) image.setImageDrawable(null);
    if (before != null) before.recycle();
    if (after != null) after.recycle();
    super.onDestroy();
  }
}
