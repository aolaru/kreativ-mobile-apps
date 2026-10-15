package com.kreativ.northlight;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Rotating magnetic scale with upright labels and accessible, native centre text. */
final class FieldCompassView extends FrameLayout {
    private final Dial dial;
    private final LinearLayout centre;
    private final TextView heading, direction, reference;

    FieldCompassView(Context context) {
        super(context);
        setId(R.id.field_compass);
        dial = new Dial(context);
        dial.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        addView(dial, new LayoutParams(-1, -1));
        centre = new LinearLayout(context);
        centre.setOrientation(LinearLayout.VERTICAL); centre.setGravity(Gravity.CENTER);
        heading = label(context, R.string.main, 44, R.color.moon);
        heading.setId(R.id.field_heading);
        heading.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        heading.setAutoSizeTextTypeUniformWithConfiguration(20, 44, 1, TypedValue.COMPLEX_UNIT_SP);
        heading.setMaxLines(1);
        centre.addView(heading, new LinearLayout.LayoutParams(-1, dp(68)));
        direction = label(context, R.string.field_waiting, 17, R.color.gold);
        direction.setId(R.id.field_direction);
        direction.setMaxLines(2);
        direction.setAutoSizeTextTypeUniformWithConfiguration(12, 17, 1, TypedValue.COMPLEX_UNIT_SP);
        centre.addView(direction, new LinearLayout.LayoutParams(-1, dp(44)));
        reference = label(context, R.string.field_magnetic_north, 11, R.color.mist);
        reference.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        reference.setMaxLines(1);
        reference.setAutoSizeTextTypeUniformWithConfiguration(10, 11, 1, TypedValue.COMPLEX_UNIT_SP);
        centre.addView(reference, new LinearLayout.LayoutParams(-1, dp(24)));
        addView(centre, new LayoutParams(dp(160), dp(140), Gravity.CENTER));
    }
    TextView headingText() { return heading; }
    TextView directionText() { return direction; }
    void setHeading(float value) { dial.heading = HeadingMath.normalize(value); dial.invalidate(); }
    void reset() {
        dial.heading = Float.NaN; dial.invalidate();
        heading.setText(R.string.main); direction.setText(R.string.field_waiting);
    }
    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int width = MeasureSpec.getSize(widthSpec);
        int size = Math.min(width, dp(360));
        // Prioritize heading and direction when the annulus has less room.
        reference.setVisibility(size < dp(320) || getResources().getConfiguration().fontScale >= 1.3f ? GONE : VISIBLE);
        LayoutParams params = (LayoutParams) centre.getLayoutParams();
        params.width = Math.round(size * .49f); params.height = dp(140);
        super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY));
    }
    private TextView label(Context context, int text, int size, int color) {
        TextView view = new TextView(context); view.setText(text); view.setTextSize(size);
        view.setTextColor(context.getColor(color)); view.setGravity(Gravity.CENTER);
        view.setIncludeFontPadding(false); return view;
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private final class Dial extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path marker = new Path();
        private final String[] cardinals = {"N", "E", "S", "W"};
        private final String[] degrees = {"", "30", "60", "", "120", "150", "", "210", "240", "", "300", "330"};
        private final Typeface medium = Typeface.create("sans-serif-medium", Typeface.NORMAL);
        private float heading = Float.NaN;
        Dial(Context context) { super(context); }
        @Override protected void onDraw(Canvas canvas) {
            float cx = getWidth() / 2f, cy = getHeight() / 2f;
            float radius = Math.min(getWidth(), getHeight()) / 2f - dp(14);
            float angle = Float.isNaN(heading) ? 0 : heading;
            paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(dp(1));
            paint.setColor(getContext().getColor(R.color.line));
            canvas.drawCircle(cx, cy, radius, paint);
            canvas.drawCircle(cx, cy, radius - dp(62), paint);
            // Degree ticks rotate with the scale; the top index always means phone-forward.
            for (int degree = 0; degree < 360; degree += 5) {
                double radians = Math.toRadians(degree - angle - 90);
                boolean major = degree % 30 == 0;
                paint.setColor(getContext().getColor(major ? R.color.moon : R.color.mist));
                paint.setAlpha(Float.isNaN(heading) ? 70 : major ? 220 : 130);
                paint.setStrokeWidth(dp(major ? 2 : 1));
                float outer = radius - dp(7), inner = outer - dp(major ? 14 : 7);
                canvas.drawLine(cx + (float)Math.cos(radians) * inner, cy + (float)Math.sin(radians) * inner,
                        cx + (float)Math.cos(radians) * outer, cy + (float)Math.sin(radians) * outer, paint);
            }
            paint.setStyle(Paint.Style.FILL); paint.setTextAlign(Paint.Align.CENTER);
            for (int degree = 0; degree < 360; degree += 30) {
                double radians = Math.toRadians(degree - angle - 90);
                boolean cardinal = degree % 90 == 0;
                String label = cardinal ? cardinals[degree / 90] : degrees[degree / 30];
                paint.setTypeface(medium);
                paint.setTextSize(dp(cardinal ? 19 : 11));
                paint.setColor(getContext().getColor(degree == 0 ? R.color.gold : cardinal ? R.color.moon : R.color.mist));
                paint.setAlpha(Float.isNaN(heading) ? 70 : 255);
                float distance = radius - dp(39);
                float baseline = -(paint.ascent() + paint.descent()) / 2;
                canvas.drawText(label, cx + (float)Math.cos(radians) * distance,
                        cy + (float)Math.sin(radians) * distance + baseline, paint);
            }
            paint.setColor(getContext().getColor(R.color.gold));
            paint.setAlpha(Float.isNaN(heading) ? 70 : 255);
            marker.reset(); marker.moveTo(cx, cy - radius + dp(8));
            marker.lineTo(cx - dp(6), cy - radius - dp(3));
            marker.lineTo(cx + dp(6), cy - radius - dp(3)); marker.close();
            canvas.drawPath(marker, paint);
            paint.setAlpha(255);
        }
    }
}
