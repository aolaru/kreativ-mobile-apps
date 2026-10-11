package com.kreativ.sharesafe;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.view.ViewCompat;

/** Manual on-device privacy editor. Cover is the default; edits survive recreation. */
public class RedactActivity extends Activity {
    public static final String EXTRA_REDACTED_PATH = "com.kreativ.sharesafe.REDACTED_PATH";
    private EditorSession session;
    private PhotoEditor editor;
    private TextView status;
    private ProgressBar progress;
    private Button save, undo, reset, cancel, move, addArea;
    private final Button[] tools = new Button[3];
    private final EditorSession.Listener observer = this::renderState;

    @Override protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        Uri source = getIntent().getData();
        if (source == null || !BuildConfig.PRO_AVAILABLE) { finish(); return; }
        session = (EditorSession) getLastNonConfigurationInstance();
        if (session == null) session = new EditorSession(this, source, saved);
        setContentView(buildScreen()); session.listener = observer; renderState();
        if (Build.VERSION.SDK_INT >= 33) getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT, this::requestClose);
    }
    @Override public Object onRetainNonConfigurationInstance() { return session; }
    @Override protected void onSaveInstanceState(Bundle out) { if (session != null) session.save(out); super.onSaveInstanceState(out); }
    @Override protected void onDestroy() {
        if (session != null) {
            if (session.listener == observer) session.listener = null;
            if (!isChangingConfigurations()) session.close();
        }
        super.onDestroy();
    }
    @Override public void onBackPressed() { requestClose(); }
    private void requestClose() {
        if (session == null) { finish(); return; }
        if (session.busy) { Toast.makeText(this, "Please wait while your copy is created.", Toast.LENGTH_SHORT).show(); return; }
        if (!session.masks.isEmpty()) new AlertDialog.Builder(this).setTitle("Discard these edits?")
                .setMessage("Your original photo has not been changed.").setNegativeButton("Keep editing", null)
                .setPositiveButton("Discard", (dialog, which) -> finish()).show();
        else finish();
    }

    private View buildScreen() {
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); Ui.applyInsets(scroll);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(16), dp(18), dp(18)); root.setBackgroundColor(Color.rgb(247, 248, 243)); scroll.addView(root);
        root.addView(label("SHARESAFE PRO", 12)); root.addView(label("Advanced edit", 26));
        root.addView(label("Cover replaces sensitive details with solid black. Blur and pixelation may still reveal information. Use Cover for text, IDs and faces.", 14));
        root.addView(label("Drag to mark an area. Pinch to zoom; use Move to pan. Large exports may be resized; transparency becomes white and animation exports one frame.", 13));
        LinearLayout toolRow = new LinearLayout(this);
        for (int i = 0; i < tools.length; i++) {
            final int tool = i; tools[i] = button(toolName(i));
            tools[i].setOnClickListener(v -> {
                if (session.busy) return;
                session.tool = tool; session.moveMode = false; session.status = ""; renderState();
            });
            toolRow.addView(tools[i], weighted());
        }
        root.addView(toolRow);
        editor = new PhotoEditor();
        // A scrollable screen keeps tools reachable in landscape and with large fonts.
        root.addView(editor, new LinearLayout.LayoutParams(-1, dp(360)));
        status = label("Opening photo…", 14); status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE); root.addView(status);
        progress = new ProgressBar(this); root.addView(progress, new LinearLayout.LayoutParams(-2, -2));
        LinearLayout nav = new LinearLayout(this);
        move = button("Move photo"); move.setOnClickListener(v -> { session.moveMode = !session.moveMode; renderState(); }); nav.addView(move, weighted());
        addArea = button("Add area…"); addArea.setOnClickListener(v -> addAccessibleArea()); nav.addView(addArea, weighted()); root.addView(nav);
        LinearLayout edits = new LinearLayout(this);
        undo = button("Undo"); undo.setOnClickListener(v -> session.undo()); edits.addView(undo, weighted());
        reset = button("Reset"); reset.setOnClickListener(v -> {
            new AlertDialog.Builder(this).setTitle("Remove all redactions?").setNegativeButton("Keep edits", null)
                    .setPositiveButton("Reset", (dialog, which) -> session.reset()).show();
        }); edits.addView(reset, weighted()); root.addView(edits);
        save = button("Create redacted copy"); save.setBackgroundResource(R.drawable.button_primary); save.setTextColor(Color.WHITE);
        save.setOnClickListener(v -> session.export()); root.addView(save, new LinearLayout.LayoutParams(-1, -2));
        cancel = button("Cancel"); cancel.setOnClickListener(v -> requestClose()); root.addView(cancel, new LinearLayout.LayoutParams(-1, -2));
        return scroll;
    }

    private void renderState() {
        if (session.outputPath != null) {
            Intent result = new Intent().putExtra(EXTRA_REDACTED_PATH, session.outputPath);
            setResult(RESULT_OK, result); finish(); return;
        }
        boolean editable = session.ready && !session.busy;
        for (int i = 0; i < tools.length; i++) {
            tools[i].setEnabled(editable); tools[i].setSelected(session.tool == i);
            tools[i].setBackgroundResource(session.tool == i ? R.drawable.button_primary : R.drawable.button_secondary);
            tools[i].setTextColor(session.tool == i ? Color.WHITE : Color.rgb(25, 44, 41));
            ViewCompat.setStateDescription(tools[i], session.tool == i ? "Selected" : "Not selected");
        }
        move.setEnabled(editable); move.setText(session.moveMode ? "Draw areas" : "Move photo");
        addArea.setEnabled(editable && session.masks.size() < 200);
        undo.setEnabled(editable && !session.masks.isEmpty()); reset.setEnabled(editable && !session.masks.isEmpty());
        save.setEnabled(editable && !session.previewPending && session.previewValid && !session.masks.isEmpty());
        cancel.setEnabled(!session.busy); editor.setEnabled(editable);
        progress.setVisibility(session.busy || (!session.ready && !session.loadFailed) || session.previewPending ? View.VISIBLE : View.GONE);
        status.setText(session.status.isEmpty() ? toolName(session.tool) + " selected • " + session.masks.size() + " area(s) • "
                + (session.moveMode ? "Drag to move the photo" : "Drag to redact") : session.status);
        editor.setContentDescription("Photo editor. " + session.masks.size() + " redaction areas. Use Add area to enter bounds without dragging.");
        editor.invalidate();
    }

    private void addAccessibleArea() {
        LinearLayout layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(18), 0, dp(18), 0);
        layout.addView(label("Enter the area's edges as percentages of the photo, from 0 to 100.", 14));
        String[] names = {"Left (%)", "Top (%)", "Right (%)", "Bottom (%)"};
        String[] defaults = {"25", "25", "75", "75"}; EditText[] fields = new EditText[4];
        for (int i = 0; i < 4; i++) {
            fields[i] = new EditText(this); fields[i].setHint(names[i]); fields[i].setContentDescription(names[i]);
            fields[i].setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL); fields[i].setText(defaults[i]); layout.addView(fields[i]);
        }
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Add " + toolName(session.tool) + " area")
                .setView(layout).setNegativeButton("Cancel", null).setPositiveButton("Add area", null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (session.busy) { dialog.dismiss(); return; }
            try {
                float[] bounds = new float[4];
                for (int i = 0; i < 4; i++) {
                    bounds[i] = Float.parseFloat(fields[i].getText().toString()) / 100f;
                    if (!Float.isFinite(bounds[i]) || bounds[i] < 0 || bounds[i] > 1) throw new IllegalArgumentException();
                }
                if (bounds[0] >= bounds[2] || bounds[1] >= bounds[3]) throw new IllegalArgumentException();
                session.add(new RectF(bounds[0], bounds[1], bounds[2], bounds[3])); dialog.dismiss();
            } catch (IllegalArgumentException e) { fields[0].setError("Use 0–100; left must be before right and top before bottom."); }
        })); dialog.show();
    }

    private final class PhotoEditor extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final Paint outline = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF imageBounds = new RectF();
        private final ScaleGestureDetector scaler;
        private RectF draft;
        private float lastX, lastY;
        private boolean drawing, multitouch;

        PhotoEditor() {
            super(RedactActivity.this); setBackgroundColor(Color.rgb(223, 229, 225));
            outline.setStyle(Paint.Style.STROKE); outline.setColor(Color.rgb(25, 44, 41)); outline.setStrokeWidth(dp(2));
            scaler = new ScaleGestureDetector(RedactActivity.this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                @Override public boolean onScale(ScaleGestureDetector detector) {
                    updateBounds();
                    float nx = imageBounds.width() == 0 ? .5f : (detector.getFocusX() - imageBounds.left) / imageBounds.width();
                    float ny = imageBounds.height() == 0 ? .5f : (detector.getFocusY() - imageBounds.top) / imageBounds.height();
                    session.zoom = Math.max(1, Math.min(6, session.zoom * detector.getScaleFactor()));
                    updateBounds();
                    session.panX += (detector.getFocusX() - imageBounds.left - nx * imageBounds.width()) / Math.max(1, getWidth());
                    session.panY += (detector.getFocusY() - imageBounds.top - ny * imageBounds.height()) / Math.max(1, getHeight());
                    updateBounds(); invalidate(); return true;
                }
            });
        }
        private void updateBounds() {
            if (session.preview == null) return;
            float scale = Math.min(getWidth() / (float) session.preview.getWidth(), getHeight() / (float) session.preview.getHeight()) * session.zoom;
            float width = session.preview.getWidth() * scale, height = session.preview.getHeight() * scale;
            float left = (getWidth() - width) / 2 + session.panX * getWidth();
            float top = (getHeight() - height) / 2 + session.panY * getHeight();
            left = width <= getWidth() ? (getWidth() - width) / 2 : Math.max(getWidth() - width, Math.min(0, left));
            top = height <= getHeight() ? (getHeight() - height) / 2 : Math.max(getHeight() - height, Math.min(0, top));
            session.panX = getWidth() == 0 ? 0 : (left - (getWidth() - width) / 2) / getWidth();
            session.panY = getHeight() == 0 ? 0 : (top - (getHeight() - height) / 2) / getHeight();
            imageBounds.set(left, top, left + width, top + height);
        }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas); updateBounds();
            if (session.preview != null) canvas.drawBitmap(session.preview, null, imageBounds, paint);
            if (draft != null) {
                RectF normal = normalized(draft);
                canvas.drawRect(imageBounds.left + normal.left * imageBounds.width(), imageBounds.top + normal.top * imageBounds.height(),
                        imageBounds.left + normal.right * imageBounds.width(), imageBounds.top + normal.bottom * imageBounds.height(), outline);
            }
        }
        private float x(float value) { return Math.max(0, Math.min(1, (value - imageBounds.left) / Math.max(1, imageBounds.width()))); }
        private float y(float value) { return Math.max(0, Math.min(1, (value - imageBounds.top) / Math.max(1, imageBounds.height()))); }
        private RectF normalized(RectF rect) { return new RectF(Math.min(rect.left, rect.right), Math.min(rect.top, rect.bottom), Math.max(rect.left, rect.right), Math.max(rect.top, rect.bottom)); }
        @Override public boolean onTouchEvent(MotionEvent event) {
            if (!isEnabled() || session.preview == null) return false;
            getParent().requestDisallowInterceptTouchEvent(true); updateBounds(); scaler.onTouchEvent(event);
            if (event.getPointerCount() > 1 || scaler.isInProgress()) { multitouch = true; drawing = false; draft = null; invalidate(); return true; }
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    multitouch = false; lastX = event.getX(); lastY = event.getY();
                    drawing = !session.moveMode && imageBounds.contains(lastX, lastY);
                    if (drawing) draft = new RectF(x(lastX), y(lastY), x(lastX), y(lastY)); return true;
                case MotionEvent.ACTION_MOVE:
                    if (multitouch) return true;
                    if (session.moveMode) {
                        session.panX += (event.getX() - lastX) / Math.max(1, getWidth());
                        session.panY += (event.getY() - lastY) / Math.max(1, getHeight()); updateBounds();
                    } else if (drawing && draft != null) { draft.right = x(event.getX()); draft.bottom = y(event.getY()); }
                    lastX = event.getX(); lastY = event.getY(); invalidate(); return true;
                case MotionEvent.ACTION_UP:
                    if (!multitouch && drawing && draft != null) {
                        draft.right = x(event.getX()); draft.bottom = y(event.getY()); RectF area = normalized(draft);
                        if (area.width() >= .002f && area.height() >= .002f) session.add(area);
                    }
                    drawing = false; draft = null; multitouch = false; performClick(); invalidate(); return true;
                case MotionEvent.ACTION_CANCEL:
                    drawing = false; draft = null; multitouch = false; invalidate(); return true;
                default: return true;
            }
        }
        @Override public boolean performClick() { super.performClick(); return true; }
    }
    private static String toolName(int tool) { return tool == RedactionRenderer.COVER ? "Cover" : tool == RedactionRenderer.BLUR ? "Blur" : "Pixelate"; }
    private TextView label(String text, int size) {
        TextView view = new TextView(this); view.setText(text); view.setTextSize(size); view.setTextColor(Color.rgb(25, 44, 41));
        view.setPadding(0, dp(6), 0, dp(6)); return view;
    }
    private Button button(String text) {
        Button button = new Button(this); button.setText(text); button.setTextSize(14); button.setAllCaps(false);
        button.setMinHeight(dp(48)); button.setTextColor(Color.rgb(25, 44, 41)); button.setBackgroundResource(R.drawable.button_secondary); return button;
    }
    private LinearLayout.LayoutParams weighted() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1); params.setMargins(dp(3), dp(5), dp(3), dp(5)); return params;
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
