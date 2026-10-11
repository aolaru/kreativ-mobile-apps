package com.kreativ.sharesafe;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import java.util.List;

final class RedactionRenderer {
    static final int COVER = 0, BLUR = 1, PIXELATE = 2;
    static final class Mask {
        final RectF area; // Normalized coordinates: preview and export use the same selection.
        final int tool;
        Mask(RectF area, int tool) { this.area = new RectF(area); this.tool = tool; }
    }
    private RedactionRenderer() { }

    static Bitmap render(Bitmap source, List<Mask> masks) {
        Bitmap output = source.copy(Bitmap.Config.ARGB_8888, true);
        if (output == null) throw new IllegalStateException("Could not allocate image");
        try {
            Canvas canvas = new Canvas(output);
            for (Mask mask : masks) {
                RectF area = mask.area;
                Rect rect = new Rect(Math.max(0, (int) Math.floor(area.left * output.getWidth())),
                        Math.max(0, (int) Math.floor(area.top * output.getHeight())),
                        Math.min(output.getWidth(), (int) Math.ceil(area.right * output.getWidth())),
                        Math.min(output.getHeight(), (int) Math.ceil(area.bottom * output.getHeight())));
                if (rect.width() < 1 || rect.height() < 1) continue;
                Paint paint = new Paint();
                if (mask.tool == COVER) {
                    paint.setColor(Color.BLACK);
                    canvas.drawRect(rect, paint);
                } else {
                    // Bounded intermediate: no full-resolution crop is allocated.
                    int cells = mask.tool == BLUR ? 12 : 18;
                    int w = Math.min(rect.width(), cells);
                    int h = Math.max(1, Math.min(rect.height(), Math.round(w * rect.height() / (float) rect.width())));
                    Bitmap small = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                    try {
                        paint.setFilterBitmap(mask.tool == BLUR);
                        new Canvas(small).drawBitmap(output, rect, new Rect(0, 0, w, h), paint);
                        canvas.drawBitmap(small, null, rect, paint);
                    } finally { small.recycle(); }
                }
            }
            return output;
        } catch (RuntimeException | OutOfMemoryError e) {
            output.recycle();
            throw e;
        }
    }
}
