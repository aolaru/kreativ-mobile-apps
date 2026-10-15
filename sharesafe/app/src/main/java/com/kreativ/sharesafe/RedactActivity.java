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
    private Button save, undo, redo, reset, cancel, move, addArea, selectArea, chooseArea, editArea, deleteArea, findFaces, reviewFaces;
    private AlertDialog faceDialog, areaDialog, chooseDialog;
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
        if (faceDialog != null) faceDialog.dismiss();
        if (areaDialog != null) areaDialog.dismiss();
        if (chooseDialog != null) chooseDialog.dismiss();
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
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = Ui.pageContent(this); Ui.addPage(scroll, root);
        root.addView(Ui.badge(this, "SHARESAFE PRO"), new LinearLayout.LayoutParams(-2, -2));
        spacer(root, 12); root.addView(Ui.heading(this, "Advanced edit", 28));
        root.addView(label("A safer copy. Your original stays untouched.", 14)); spacer(root, 12);
        LinearLayout warning = Ui.card(this, Color.rgb(255, 243, 217)); warning.setPadding(dp(16), dp(10), dp(16), dp(10));
        warning.addView(label("Use Cover for sensitive text, IDs and faces. Blur and pixelation may still reveal information.", 13));
        root.addView(warning, new LinearLayout.LayoutParams(-1, -2)); spacer(root, 18);
        LinearLayout canvasCard = Ui.card(this, Color.WHITE); canvasCard.setPadding(dp(12), dp(12), dp(12), dp(12));
        LinearLayout toolRow = new LinearLayout(this);
        for (int i = 0; i < tools.length; i++) {
            final int tool = i; tools[i] = button(toolName(i));
            tools[i].setOnClickListener(v -> {
                if (!session.editable()) return;
                session.tool = tool; session.moveMode = false; session.selectMode = false; session.selectedMask = -1; session.status = ""; renderState();
            });
            toolRow.addView(tools[i], weighted());
        }
        canvasCard.addView(toolRow); spacer(canvasCard, 8);
        editor = new PhotoEditor();
        // A scrollable screen keeps tools reachable in landscape and with large fonts.
        canvasCard.addView(editor, new LinearLayout.LayoutParams(-1, dp(360)));
        canvasCard.addView(label("Draw to mark · Select areas to move or resize · Pinch to zoom", 12)); root.addView(canvasCard, new LinearLayout.LayoutParams(-1, -2));
        spacer(root, 12);
        findFaces = button("Find faces · Pro"); findFaces.setOnClickListener(v -> session.findFaces()); root.addView(findFaces, new LinearLayout.LayoutParams(-1, -2));
        reviewFaces = button("Review face suggestions"); reviewFaces.setOnClickListener(v -> reviewFaceSuggestions()); root.addView(reviewFaces, new LinearLayout.LayoutParams(-1, -2));
        root.addView(label("Numbered outlines are suggestions, not covers. Review and apply or dismiss them before exporting. Faces can be missed; review the entire photo and use solid Cover for privacy.", 12));
        spacer(root, 12);
        status = label("Opening photo…", 14); status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE); root.addView(status);
        progress = new ProgressBar(this); root.addView(progress, new LinearLayout.LayoutParams(-2, -2));
        LinearLayout nav = new LinearLayout(this);
        move = button("Move photo"); move.setOnClickListener(v -> { session.moveMode = !session.moveMode; session.selectMode = false; renderState(); }); nav.addView(move, weighted());
        addArea = button("Add area…"); addArea.setOnClickListener(v -> addAccessibleArea()); nav.addView(addArea, weighted()); root.addView(nav);
        LinearLayout selectionTools = new LinearLayout(this);
        selectArea = button("Select areas"); selectArea.setOnClickListener(v -> {
            session.selectMode = !session.selectMode; session.moveMode = false; renderState();
        }); selectionTools.addView(selectArea, weighted());
        chooseArea = button("Choose area…"); chooseArea.setOnClickListener(v -> {
            String[] names = new String[session.masks.size()];
            for (int i = 0; i < names.length; i++) names[i] = "Area " + (i + 1) + " · " + toolName(session.masks.get(i).tool);
            chooseDialog = new AlertDialog.Builder(this).setTitle("Choose a redaction area")
                    .setItems(names, (d, which) -> { session.selectedMask = which; session.selectMode = true; session.moveMode = false; renderState(); })
                    .setNegativeButton("Cancel", null).show(); Ui.styleDialog(chooseDialog);
        }); selectionTools.addView(chooseArea, weighted()); root.addView(selectionTools);
        LinearLayout selected = new LinearLayout(this);
        editArea = button("Edit selected…"); editArea.setOnClickListener(v -> showAreaDialog(true)); selected.addView(editArea, weighted());
        deleteArea = button("Delete selected"); deleteArea.setOnClickListener(v -> session.deleteSelected()); selected.addView(deleteArea, weighted()); root.addView(selected);
        LinearLayout edits = new LinearLayout(this);
        undo = button("Undo"); undo.setOnClickListener(v -> session.undo()); edits.addView(undo, weighted());
        redo = button("Redo"); redo.setOnClickListener(v -> session.redo()); edits.addView(redo, weighted());
        reset = button("Reset"); reset.setOnClickListener(v -> {
            new AlertDialog.Builder(this).setTitle("Remove all redactions?").setNegativeButton("Keep edits", null)
                    .setPositiveButton("Reset", (dialog, which) -> session.reset()).show();
        }); edits.addView(reset, weighted()); root.addView(edits);
        spacer(root, 12); save = Ui.button(this, "Create redacted copy", true);
        save.setOnClickListener(v -> session.export()); root.addView(save, new LinearLayout.LayoutParams(-1, -2));
        spacer(root, 8); cancel = Ui.textButton(this, "Cancel"); cancel.setOnClickListener(v -> requestClose()); root.addView(cancel, new LinearLayout.LayoutParams(-1, -2));
        root.addView(label("Undo/redo keeps the last 20 changes. JPEG copies may be resized. Transparency becomes white; animations export one frame.", 12));
        return scroll;
    }

    private void renderState() {
        if (session.outputPath != null) {
            Intent result = new Intent().putExtra(EXTRA_REDACTED_PATH, session.outputPath);
            setResult(RESULT_OK, result); finish(); return;
        }
        boolean editable = session.editable();
        for (int i = 0; i < tools.length; i++) {
            tools[i].setEnabled(editable); tools[i].setSelected(session.tool == i);
            Ui.styleButton(tools[i], session.tool == i);
            ViewCompat.setStateDescription(tools[i], session.tool == i ? "Selected" : "Not selected");
        }
        move.setEnabled(editable); move.setText(session.moveMode ? "Draw areas" : "Move photo");
        addArea.setEnabled(editable && session.masks.size() < 200);
        selectArea.setEnabled(editable && !session.masks.isEmpty()); selectArea.setText(session.selectMode ? "Draw new areas" : "Select areas");
        chooseArea.setEnabled(editable && !session.masks.isEmpty());
        ViewCompat.setStateDescription(selectArea, session.selectMode ? "Selecting areas" : "Drawing areas");
        editArea.setEnabled(editable && session.hasSelection()); deleteArea.setEnabled(editable && session.hasSelection());
        undo.setEnabled(editable && !session.undoHistory.isEmpty()); redo.setEnabled(editable && !session.redoHistory.isEmpty());
        reset.setEnabled(editable && (!session.masks.isEmpty() || !session.faceSuggestions.isEmpty()));
        findFaces.setEnabled(editable && !session.previewPending);
        findFaces.setText(session.detecting ? "Finding faces…" : "Find faces · Pro");
        reviewFaces.setVisibility(session.faceSuggestions.isEmpty() ? View.GONE : View.VISIBLE);
        reviewFaces.setText("Review " + session.faceSuggestions.size() + " face suggestion(s)"); reviewFaces.setEnabled(editable);
        save.setEnabled(editable && !session.previewPending && session.previewValid && !session.masks.isEmpty() && session.faceSuggestions.isEmpty());
        cancel.setEnabled(!session.busy); editor.setEnabled(editable);
        progress.setVisibility(session.busy || session.detecting || (!session.ready && !session.loadFailed) || session.previewPending ? View.VISIBLE : View.GONE);
        status.setText(session.status.isEmpty() ? toolName(session.tool) + " selected • " + session.masks.size() + " area(s) • "
                + (session.moveMode ? "Drag to move the photo" : session.selectMode ? "Tap an area, then drag it or its corner handles" : "Drag to redact")
                + (session.hasSelection() ? " · Area " + (session.selectedMask + 1) + " selected" : "") : session.status);
        editor.setContentDescription("Photo editor. " + session.masks.size() + " redaction areas. Use Add area for numeric bounds. Tap Select areas to adjust an existing area.");
        editor.invalidate();
    }

    private void reviewFaceSuggestions() {
        if (!session.editable() || session.faceSuggestions.isEmpty()) return;
        int count = session.faceSuggestions.size(); String[] labels = new String[count];
        for (int i = 0; i < count; i++) labels[i] = "Face suggestion " + (i + 1);
        faceDialog = new AlertDialog.Builder(this).setTitle("Review possible faces")
                .setMultiChoiceItems(labels, session.faceChoices.clone(), (dialog, which, checked) -> {
                    session.faceChoices[which] = checked; editor.invalidate();
                })
                .setPositiveButton("Apply selected covers", null).setNegativeButton("Keep reviewing", null)
                .setNeutralButton("Dismiss suggestions", (dialog, which) -> session.discardFaceSuggestions()).create();
        faceDialog.setOnShowListener(ignored -> faceDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            session.applyFaceSuggestions();
            if (session.faceSuggestions.isEmpty()) faceDialog.dismiss();
            else Toast.makeText(this, session.status, Toast.LENGTH_LONG).show();
        }));
        faceDialog.show(); Ui.styleDialog(faceDialog);
    }

    private void addAccessibleArea() { showAreaDialog(false); }
    private void showAreaDialog(boolean editing) {
        if (!session.editable() || (editing && !session.hasSelection())) return;
        final int index = editing ? session.selectedMask : -1;
        LinearLayout layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(18), 0, dp(18), 0);
        layout.addView(label("Enter the area's edges as percentages of the photo, from 0 to 100.", 14));
        String[] names = {"Left (%)", "Top (%)", "Right (%)", "Bottom (%)"};
        String[] defaults = {"25", "25", "75", "75"}; EditText[] fields = new EditText[4];
        if (editing) {
            RectF area = session.masks.get(index).area;
            float[] values = {area.left, area.top, area.right, area.bottom};
            for (int i = 0; i < 4; i++) defaults[i] = String.format(java.util.Locale.US, "%.2f", values[i] * 100);
        }
        for (int i = 0; i < 4; i++) {
            fields[i] = new EditText(this); fields[i].setHint(names[i]); fields[i].setContentDescription(names[i]);
            fields[i].setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL); fields[i].setText(defaults[i]); layout.addView(fields[i]);
        }
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(editing ? "Edit selected area" : "Add " + toolName(session.tool) + " area")
                .setView(layout).setNegativeButton("Cancel", null).setPositiveButton(editing ? "Save area" : "Add area", null).create();
        areaDialog = dialog;
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (!session.editable()) { dialog.dismiss(); return; }
            try {
                float[] bounds = new float[4];
                for (int i = 0; i < 4; i++) {
                    bounds[i] = Float.parseFloat(fields[i].getText().toString().trim().replace(',', '.')) / 100f;
                    if (!Float.isFinite(bounds[i]) || bounds[i] < 0 || bounds[i] > 1) throw new IllegalArgumentException();
                }
                if (bounds[0] >= bounds[2] || bounds[1] >= bounds[3]) throw new IllegalArgumentException();
                RectF area = new RectF(bounds[0], bounds[1], bounds[2], bounds[3]);
                if (!MaskGeometry.valid(area)) throw new IllegalArgumentException();
                if (editing) session.updateMask(index, area); else session.add(area); dialog.dismiss();
            } catch (IllegalArgumentException e) { fields[0].setError("Use 0–100; left must be before right and top before bottom."); }
        })); dialog.show(); Ui.styleDialog(dialog);
    }

    private final class PhotoEditor extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final Paint outline = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF imageBounds = new RectF();
        private final ScaleGestureDetector scaler;
        private RectF draft;
        private RectF gestureArea;
        private int gestureIndex = -1, corner = -1;
        private float startX, startY;
        private float lastX, lastY;
        private boolean drawing, multitouch;

        PhotoEditor() {
            super(RedactActivity.this); setBackground(Ui.rounded(RedactActivity.this, Ui.SURFACE, 16, 0)); setClipToOutline(true);
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
            outline.setColor(Color.rgb(166, 92, 0)); outline.setPathEffect(new android.graphics.DashPathEffect(new float[] {dp(5), dp(4)}, 0));
            for (int i = 0; i < session.faceSuggestions.size(); i++) {
                RectF area = screen(session.faceSuggestions.get(i));
                canvas.drawRect(area, outline);
                paint.setColor(Color.rgb(166, 92, 0)); paint.setTextSize(dp(14));
                canvas.drawText(Integer.toString(i + 1), area.left + dp(4), area.top + dp(17), paint);
            }
            outline.setPathEffect(null); outline.setColor(Color.rgb(34, 130, 104));
            if (session.hasSelection() && session.selectMode) {
                RectF area = screen(gestureIndex >= 0 && draft != null ? draft : session.masks.get(session.selectedMask).area);
                canvas.drawRect(area, outline); paint.setColor(Color.WHITE);
                for (int i = 0; i < 4; i++) {
                    float cx = (i == 0 || i == 3) ? area.left : area.right, cy = (i == 0 || i == 1) ? area.top : area.bottom;
                    canvas.drawCircle(cx, cy, dp(6), paint); canvas.drawCircle(cx, cy, dp(6), outline);
                }
            }
            if (draft != null) {
                canvas.drawRect(screen(normalized(draft)), outline);
            }
        }
        private RectF screen(RectF area) { return new RectF(imageBounds.left + area.left * imageBounds.width(), imageBounds.top + area.top * imageBounds.height(), imageBounds.left + area.right * imageBounds.width(), imageBounds.top + area.bottom * imageBounds.height()); }
        private int hitArea(float px, float py) {
            for (int i = session.masks.size() - 1; i >= 0; i--) if (session.masks.get(i).area.contains(x(px), y(py))) return i;
            return -1;
        }
        private int hitCorner(float px, float py) {
            if (!session.hasSelection()) return -1;
            RectF area = screen(session.masks.get(session.selectedMask).area);
            for (int i = 0; i < 4; i++) {
                float cx = (i == 0 || i == 3) ? area.left : area.right, cy = (i == 0 || i == 1) ? area.top : area.bottom;
                if (Math.hypot(px - cx, py - cy) <= dp(20)) return i;
            }
            return -1;
        }
        private float x(float value) { return Math.max(0, Math.min(1, (value - imageBounds.left) / Math.max(1, imageBounds.width()))); }
        private float y(float value) { return Math.max(0, Math.min(1, (value - imageBounds.top) / Math.max(1, imageBounds.height()))); }
        private RectF normalized(RectF rect) { return new RectF(Math.min(rect.left, rect.right), Math.min(rect.top, rect.bottom), Math.max(rect.left, rect.right), Math.max(rect.top, rect.bottom)); }
        @Override public boolean onTouchEvent(MotionEvent event) {
            if (!isEnabled() || session.preview == null) return false;
            getParent().requestDisallowInterceptTouchEvent(true); updateBounds(); scaler.onTouchEvent(event);
            if (event.getPointerCount() > 1 || scaler.isInProgress()) { multitouch = true; drawing = false; draft = null; gestureIndex = -1; invalidate(); return true; }
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    multitouch = false; lastX = event.getX(); lastY = event.getY();
                    gestureIndex = -1; corner = -1;
                    if (session.selectMode) {
                        corner = hitCorner(lastX, lastY);
                        if (corner < 0) session.selectedMask = imageBounds.contains(lastX, lastY) ? hitArea(lastX, lastY) : -1;
                        if (session.hasSelection()) {
                            gestureIndex = session.selectedMask; gestureArea = new RectF(session.masks.get(gestureIndex).area);
                            draft = new RectF(gestureArea); startX = x(lastX); startY = y(lastY);
                        }
                        renderState(); return true;
                    }
                    drawing = !session.moveMode && imageBounds.contains(lastX, lastY);
                    if (drawing) draft = new RectF(x(lastX), y(lastY), x(lastX), y(lastY)); return true;
                case MotionEvent.ACTION_MOVE:
                    if (multitouch) return true;
                    if (gestureIndex >= 0 && draft != null) {
                        draft = corner >= 0 ? MaskGeometry.resize(gestureArea, corner, x(event.getX()), y(event.getY()))
                                : MaskGeometry.move(gestureArea, x(event.getX()) - startX, y(event.getY()) - startY);
                    } else if (session.moveMode) {
                        session.panX += (event.getX() - lastX) / Math.max(1, getWidth());
                        session.panY += (event.getY() - lastY) / Math.max(1, getHeight()); updateBounds();
                    } else if (drawing && draft != null) { draft.right = x(event.getX()); draft.bottom = y(event.getY()); }
                    lastX = event.getX(); lastY = event.getY(); invalidate(); return true;
                case MotionEvent.ACTION_UP:
                    if (!multitouch && gestureIndex >= 0 && draft != null) {
                        draft = corner >= 0 ? MaskGeometry.resize(gestureArea, corner, x(event.getX()), y(event.getY()))
                                : MaskGeometry.move(gestureArea, x(event.getX()) - startX, y(event.getY()) - startY);
                        session.updateMask(gestureIndex, draft);
                    } else if (!multitouch && drawing && draft != null) {
                        draft.right = x(event.getX()); draft.bottom = y(event.getY()); RectF area = normalized(draft);
                        if (area.width() >= .002f && area.height() >= .002f) session.add(area);
                    }
                    drawing = false; draft = null; gestureIndex = -1; multitouch = false; performClick(); invalidate(); return true;
                case MotionEvent.ACTION_CANCEL:
                    drawing = false; draft = null; gestureIndex = -1; multitouch = false; invalidate(); return true;
                default: return true;
            }
        }
        @Override public boolean performClick() { super.performClick(); return true; }
    }
    private static String toolName(int tool) { return tool == RedactionRenderer.COVER ? "Cover" : tool == RedactionRenderer.BLUR ? "Blur" : "Pixelate"; }
    private TextView label(String text, int size) {
        TextView view = Ui.text(this, text, size, Ui.MUTED);
        view.setPadding(0, dp(6), 0, dp(6)); return view;
    }
    private Button button(String text) {
        Button button = Ui.button(this, text, false); button.setTextSize(14);
        button.setPadding(dp(10), dp(12), dp(10), dp(12)); button.setMinHeight(dp(48)); return button;
    }
    private LinearLayout.LayoutParams weighted() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1); params.setMargins(dp(3), dp(5), dp(3), dp(5)); return params;
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private void spacer(LinearLayout root, int height) { root.addView(new View(this), new LinearLayout.LayoutParams(1, dp(height))); }
}
