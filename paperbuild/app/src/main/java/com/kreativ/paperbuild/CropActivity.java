package com.kreativ.paperbuild;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.text.InputType;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Free, non-destructive crop: the original private image is never overwritten. */
public class CropActivity extends Activity {
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private DocumentSession session;
    private PageItem original;
    private int index;
    private CropView preview;
    private Button apply;
    private boolean closed;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state); session = DocumentSession.get(this); index = getIntent().getIntExtra("index", -1);
        if (index < 0 || index >= session.pages.size() || session.busy) { finish(); return; }
        original = session.pages.get(index);
        ScrollView scroll = new ScrollView(this); scroll.setBackgroundColor(Ui.SURFACE);
        LinearLayout root = Ui.column(this); Ui.applyInsets(scroll, root); scroll.addView(root);
        root.addView(Ui.brand(this)); Ui.gap(root, 20);
        root.addView(Ui.text(this, "Crop page " + (index + 1), 26, Ui.INK, true)); Ui.gap(root, 8);
        root.addView(Ui.text(this, "Drag the corners. Your original stays untouched.", 14, Ui.MUTED, false)); Ui.gap(root, 16);
        preview = new CropView(); preview.bounds = original.crop;
        if (state != null) {
            float[] saved = state.getFloatArray("crop");
            if (saved != null) try { preview.bounds = new CropBounds(saved[0], saved[1], saved[2], saved[3]); }
            catch (IllegalArgumentException ignored) { }
        }
        root.addView(preview, new LinearLayout.LayoutParams(-1, Ui.dp(this, 340))); Ui.gap(root, 12);
        Button precise = Ui.button(this, "Adjust crop bounds", false); precise.setOnClickListener(v -> preciseBounds()); root.addView(precise);
        Ui.gap(root, 10); Button reset = Ui.button(this, "Restore full page", false);
        reset.setId(R.id.crop_restore);
        reset.setOnClickListener(v -> { preview.bounds = CropBounds.FULL; preview.invalidate(); }); root.addView(reset); Ui.gap(root, 16);
        apply = Ui.button(this, "Apply crop", true); apply.setId(R.id.crop_apply); apply.setEnabled(false); apply.setOnClickListener(v -> {
            if (session.busy || index >= session.pages.size() || session.pages.get(index) != original) {
                Toast.makeText(this, "The draft changed. Open Crop again.", Toast.LENGTH_LONG).show(); finish(); return;
            }
            session.pages.set(index, new PageItem(original.uri, original.name, original.rotation, preview.bounds)); session.changed(); finish();
        }); root.addView(apply); Ui.gap(root, 10);
        Button cancel = Ui.button(this, "Cancel", false); cancel.setOnClickListener(v -> finish()); root.addView(cancel); setContentView(scroll);
        worker.execute(() -> {
            Bitmap image = null;
            try { image = session.processor.loadImage(new PageItem(original.uri, original.name, original.rotation), 1600, 0); }
            catch (Exception | OutOfMemoryError ignored) { }
            Bitmap result = image;
            runOnUiThread(() -> {
                if (closed) { if (result != null) result.recycle(); return; }
                if (result == null) { Toast.makeText(this, "Couldn’t read this page. Add it again.", Toast.LENGTH_LONG).show(); finish(); return; }
                preview.image = result; preview.invalidate(); apply.setEnabled(true);
            });
        });
    }
    private void preciseBounds() {
        LinearLayout fields = Ui.column(this); fields.setPadding(Ui.dp(this, 20), 0, Ui.dp(this, 20), 0);
        String[] names = {"Left %", "Top %", "Right %", "Bottom %"};
        float[] values = {preview.bounds.left, preview.bounds.top, preview.bounds.right, preview.bounds.bottom};
        EditText[] inputs = new EditText[4];
        for (int i = 0; i < 4; i++) {
            fields.addView(Ui.text(this, names[i], 13, Ui.MUTED, false));
            inputs[i] = new EditText(this); inputs[i].setContentDescription(names[i]);
            inputs[i].setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            inputs[i].setText(String.format(Locale.US, "%.1f", values[i] * 100)); fields.addView(inputs[i]);
        }
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Crop bounds · 0–100%")
                .setView(fields).setNegativeButton("Cancel", null).setPositiveButton("Apply", null).create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            try {
                preview.bounds = new CropBounds(Float.parseFloat(inputs[0].getText().toString()) / 100,
                        Float.parseFloat(inputs[1].getText().toString()) / 100, Float.parseFloat(inputs[2].getText().toString()) / 100,
                        Float.parseFloat(inputs[3].getText().toString()) / 100);
                preview.invalidate(); dialog.dismiss();
            } catch (IllegalArgumentException invalid) { inputs[0].setError("Use 0–100%, with left < right and top < bottom."); }
        })); dialog.show();
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        if (preview != null) state.putFloatArray("crop", new float[]{preview.bounds.left, preview.bounds.top, preview.bounds.right, preview.bounds.bottom});
        super.onSaveInstanceState(state);
    }
    @Override protected void onDestroy() {
        closed = true; worker.shutdownNow(); if (preview != null && preview.image != null) preview.image.recycle(); super.onDestroy();
    }
    private final class CropView extends View {
        Bitmap image;
        CropBounds bounds = CropBounds.FULL;
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        final RectF imageRect = new RectF();
        int corner = -1;
        float previousX, previousY;
        CropView() { super(CropActivity.this); setContentDescription("Crop preview. Drag corners or use Adjust crop bounds for precise, accessible controls."); }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas); canvas.drawColor(Color.rgb(226, 231, 240));
            if (image == null) return;
            float scale = Math.min((getWidth() - 32f) / image.getWidth(), (getHeight() - 32f) / image.getHeight());
            float w = image.getWidth() * scale, h = image.getHeight() * scale;
            imageRect.set((getWidth() - w) / 2, (getHeight() - h) / 2, (getWidth() + w) / 2, (getHeight() + h) / 2);
            paint.setStyle(Paint.Style.FILL); paint.setColor(Color.WHITE); canvas.drawBitmap(image, null, imageRect, paint);
            RectF crop = rect(); paint.setColor(0x880E192D);
            canvas.drawRect(imageRect.left, imageRect.top, imageRect.right, crop.top, paint);
            canvas.drawRect(imageRect.left, crop.bottom, imageRect.right, imageRect.bottom, paint);
            canvas.drawRect(imageRect.left, crop.top, crop.left, crop.bottom, paint);
            canvas.drawRect(crop.right, crop.top, imageRect.right, crop.bottom, paint);
            paint.setColor(Ui.ACCENT); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(Ui.dp(CropActivity.this, 2)); canvas.drawRect(crop, paint);
            paint.setStyle(Paint.Style.FILL);
            for (float x : new float[]{crop.left, crop.right}) for (float y : new float[]{crop.top, crop.bottom}) canvas.drawCircle(x, y, Ui.dp(CropActivity.this, 7), paint);
        }
        RectF rect() { return new RectF(imageRect.left + bounds.left * imageRect.width(), imageRect.top + bounds.top * imageRect.height(),
                imageRect.left + bounds.right * imageRect.width(), imageRect.top + bounds.bottom * imageRect.height()); }
        @Override public boolean onTouchEvent(MotionEvent event) {
            if (image == null || imageRect.isEmpty()) return false;
            float x = event.getX(), y = event.getY();
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                RectF r = rect(); float distance = Ui.dp(CropActivity.this, 36); corner = -1;
                float[] xs = {r.left, r.right, r.right, r.left}, ys = {r.top, r.top, r.bottom, r.bottom};
                for (int i = 0; i < 4; i++) {
                    float d = (float) Math.hypot(x - xs[i], y - ys[i]); if (d < distance) { distance = d; corner = i; }
                }
                if (corner == -1 && r.contains(x, y)) corner = 4;
                if (corner < 0) return false;
                previousX = x; previousY = y; getParent().requestDisallowInterceptTouchEvent(true); return true;
            }
            if (event.getActionMasked() == MotionEvent.ACTION_MOVE && corner >= 0) {
                float l = bounds.left, t = bounds.top, r = bounds.right, b = bounds.bottom;
                float nx = Math.max(0, Math.min(1, (x - imageRect.left) / imageRect.width()));
                float ny = Math.max(0, Math.min(1, (y - imageRect.top) / imageRect.height()));
                if (corner == 4) {
                    float dx = Math.max(-l, Math.min(1 - r, (x - previousX) / imageRect.width()));
                    float dy = Math.max(-t, Math.min(1 - b, (y - previousY) / imageRect.height()));
                    l += dx; r += dx; t += dy; b += dy;
                } else {
                    if (corner == 0 || corner == 3) l = Math.min(nx, r - .02f); else r = Math.max(nx, l + .02f);
                    if (corner == 0 || corner == 1) t = Math.min(ny, b - .02f); else b = Math.max(ny, t + .02f);
                }
                try { bounds = new CropBounds(l, t, r, b); } catch (IllegalArgumentException ignored) { }
                previousX = x; previousY = y; invalidate(); return true;
            }
            if (event.getActionMasked() == MotionEvent.ACTION_UP || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                corner = -1; getParent().requestDisallowInterceptTouchEvent(false); performClick(); return true;
            }
            return true;
        }
        @Override public boolean performClick() { super.performClick(); return true; }
    }
}
