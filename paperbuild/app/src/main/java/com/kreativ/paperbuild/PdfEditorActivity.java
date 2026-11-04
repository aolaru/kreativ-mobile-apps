package com.kreativ.paperbuild;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Edit an existing PDF without converting its pages to images. */
public class PdfEditorActivity extends Activity {
    private static final int PICK_PDF = 201, PICK_LOGO = 202;
    private PdfEditorSession editor;
    private DocumentSession documents;
    private final ExecutorService previewWorker = Executors.newSingleThreadExecutor();
    private final Runnable editorListener = this::render, documentListener = this::render;
    private Button choose, export, undo, options, clear, done, presets;
    private EditText title;
    private TextView status, previewLabel;
    private ImageView preview;
    private LinearLayout workspace;
    private RecyclerView pages;
    private PageRows adapter;
    private Bitmap previewBitmap;
    private int shownRevision = -1, shownPage = -1;
    private volatile int previewGeneration;
    private volatile boolean closed;
    private boolean logoPending, exportPending;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if (!ProBilling.isPro(this)) { startActivity(new Intent(this, ProActivity.class)); finish(); return; }
        editor = PdfEditorSession.get(this); documents = DocumentSession.get(this);
        logoPending = state != null && state.getBoolean("logoPending");
        exportPending = state != null && state.getBoolean("exportPending");
        ScrollView scroll = new ScrollView(this); scroll.setBackgroundColor(Ui.SURFACE);
        LinearLayout root = Ui.column(this); Ui.applyInsets(scroll, root); scroll.addView(root);
        Button back = Ui.button(this, "← Back to PaperBuild", false); back.setOnClickListener(v -> finish()); root.addView(back); Ui.gap(root, 18);
        root.addView(Ui.hero(this, "PRO PAGE EDITOR", "Every page.\nIn its place.", "Arrange. Rotate. Make a copy.")); Ui.gap(root, 16);
        choose = Ui.button(this, "Choose PDF", false); choose.setId(R.id.editor_choose); choose.setOnClickListener(v -> replace()); root.addView(choose); Ui.gap(root, 12);
        status = Ui.text(this, "", 14, Ui.MUTED, false); status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE); root.addView(status); Ui.gap(root, 14);
        workspace = Ui.card(this, R.drawable.card); root.addView(workspace);
        previewLabel = Ui.text(this, "", 16, Ui.INK, true); workspace.addView(previewLabel); Ui.gap(workspace, 10);
        preview = new ImageView(this); preview.setScaleType(ImageView.ScaleType.FIT_CENTER); preview.setBackgroundResource(R.drawable.page_preview);
        workspace.addView(preview, new LinearLayout.LayoutParams(-1, Ui.dp(this, 220))); Ui.gap(workspace, 14);
        workspace.addView(Ui.text(this, "Tap a page to preview. Use its menu to edit.", 13, Ui.MUTED, false)); Ui.gap(workspace, 10);
        pages = new RecyclerView(this); pages.setLayoutManager(new LinearLayoutManager(this)); pages.setNestedScrollingEnabled(true);
        adapter = new PageRows(); pages.setAdapter(adapter); workspace.addView(pages, new LinearLayout.LayoutParams(-1, Ui.dp(this, 300))); Ui.gap(workspace, 12);
        undo = Ui.button(this, "Undo last edit", false); undo.setId(R.id.editor_undo); undo.setOnClickListener(v -> {
            if (busy()) return; editor.plan.undo(); editor.exportedPdf = null; editor.changed();
        }); workspace.addView(undo); Ui.gap(workspace, 14);
        title = new EditText(this); title.setSingleLine(true); title.setText(editor.title); title.setContentDescription("Edited PDF name");
        title.setBackgroundResource(R.drawable.field_background); title.setPadding(Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 12));
        workspace.addView(title); title.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) { editor.title = s.toString(); editor.save(); }
            public void afterTextChanged(Editable e) { }
        }); Ui.gap(workspace, 10);
        options = Ui.button(this, "Pro export options", false); options.setOnClickListener(v -> exportOptions(false)); workspace.addView(options); Ui.gap(workspace, 10);
        presets = Ui.button(this, "Use saved preset", false); presets.setOnClickListener(v -> presets()); workspace.addView(presets); Ui.gap(workspace, 10);
        export = Ui.button(this, "Save edited PDF", true); export.setId(R.id.editor_export); export.setOnClickListener(v -> export()); workspace.addView(export); Ui.gap(workspace, 10);
        workspace.addView(Ui.text(this, "Saves a new copy in Recent PDFs. Your original stays untouched.", 12, Ui.MUTED, false)); Ui.gap(workspace, 14);
        clear = Ui.button(this, "Clear PDF draft", false); clear.setOnClickListener(v -> new AlertDialog.Builder(this).setTitle("Clear PDF draft?")
                .setMessage("Your original and exported copies stay available.").setNegativeButton("Cancel", null)
                .setPositiveButton("Clear", (d, w) -> { if (!busy()) editor.clear(); }).show()); workspace.addView(clear);
        Ui.gap(root, 12); done = Ui.button(this, "View saved PDFs", true); done.setOnClickListener(v -> {
            startActivity(new Intent(this, RecentPdfsActivity.class)); finish();
        }); root.addView(done);
        setContentView(scroll);
        if (state == null && getIntent().getData() != null) {
            Uri input = getIntent().getData();
            if (editor.plan == null) editor.open(input);
            else new AlertDialog.Builder(this).setTitle("Replace PDF draft?").setMessage("Your saved PDFs and photo draft stay available.")
                    .setNegativeButton("Keep draft", null).setPositiveButton("Edit this PDF", (d, w) -> { if (!busy() && requirePro()) editor.open(input); }).show();
        }
    }
    private boolean busy() { return editor.busy || documents.busy; }
    private boolean requirePro() {
        if (ProBilling.isPro(this)) return true;
        startActivity(new Intent(this, ProActivity.class)); return false;
    }
    @Override protected void onStart() { super.onStart(); if (editor != null) { editor.attach(editorListener); documents.attach(documentListener); } }
    @Override protected void onStop() { if (editor != null) { editor.detach(editorListener); documents.detach(documentListener); } super.onStop(); }
    @Override protected void onSaveInstanceState(Bundle state) {
        state.putBoolean("logoPending", logoPending); state.putBoolean("exportPending", exportPending); super.onSaveInstanceState(state);
    }
    private void replace() {
        if (busy() || !requirePro()) return;
        if (editor.plan == null) pick("application/pdf", PICK_PDF);
        else new AlertDialog.Builder(this).setTitle("Choose another PDF?").setMessage("This replaces the editor draft. Your photo draft and saved PDFs stay available.")
                .setNegativeButton("Cancel", null).setPositiveButton("Choose PDF", (d, w) -> pick("application/pdf", PICK_PDF)).show();
    }
    private void pick(String mime, int request) {
        try { startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType(mime)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), request); }
        catch (ActivityNotFoundException error) { Toast.makeText(this, "No file picker is available.", Toast.LENGTH_LONG).show(); }
    }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == PICK_LOGO && result != RESULT_OK) { exportOptions(exportPending); return; }
        if (result != RESULT_OK || data == null || data.getData() == null || busy() || !requirePro()) return;
        if (request == PICK_PDF) editor.open(data.getData());
        else if (request == PICK_LOGO) { logoPending = true; documents.importLogo(this, data.getData()); }
    }
    private void render() {
        if (closed || editor == null) return;
        boolean busy = busy(), available = editor.plan != null;
        choose.setEnabled(!busy); workspace.setVisibility(available ? View.VISIBLE : View.GONE);
        export.setEnabled(available && !busy); options.setEnabled(!busy); presets.setEnabled(!busy); clear.setEnabled(!busy); title.setEnabled(!busy);
        undo.setEnabled(available && !busy && editor.plan.canUndo());
        status.setText(editor.busy ? editor.status : documents.busy || logoPending || exportPending ? documents.status : editor.status);
        if (editor.exporting && !documents.busy) {
            editor.exporting = false;
            editor.exportedPdf = "Edited PDF is ready.".equals(documents.status) ? documents.latestPdf : null;
            editor.status = editor.exportedPdf == null ? documents.status : "Edited PDF saved. Share or export it from PaperBuild.";
            exportPending = false;
        }
        done.setVisibility(editor.exportedPdf != null && editor.exportedPdf.isFile() ? View.VISIBLE : View.GONE);
        if (!busy() && !logoPending) status.setText(editor.status);
        adapter.notifyDataSetChanged();
        if (available) {
            int rowHeight = Math.max(72, Math.round(72 * getResources().getConfiguration().fontScale));
            pages.getLayoutParams().height = Ui.dp(this, (int) Math.min(300L, (long) editor.plan.size() * rowHeight));
            pages.requestLayout();
            if (!title.getText().toString().equals(editor.title)) title.setText(editor.title);
            boolean refresh = shownRevision != editor.revision || shownPage != editor.selected;
            if (refresh) { shownRevision = editor.revision; shownPage = editor.selected; loadPreview(); }
        } else { previewGeneration++; preview.setImageDrawable(null); recyclePreview(); shownRevision = -1; shownPage = -1; }
        if (logoPending && !documents.busy) {
            logoPending = false; status.post(() -> { if (!closed) exportOptions(exportPending); });
        }
    }
    private void presets() {
        if (busy() || !requirePro()) return;
        List<PresetStore.Preset> saved = new PresetStore(this).list();
        if (saved.isEmpty()) { Toast.makeText(this, "Save a preset from PaperBuild’s PDF tools first.", Toast.LENGTH_LONG).show(); return; }
        String[] names = saved.stream().map(p -> p.name).toArray(String[]::new);
        new AlertDialog.Builder(this).setTitle("Use saved Pro options")
                .setItems(names, (d, index) -> {
                    if (busy() || !requirePro()) return;
                    documents.options = saved.get(index).options.withoutPassword(); documents.save(); render();
                    Toast.makeText(this, "Preset applied. Original page sizes stay unchanged.", Toast.LENGTH_SHORT).show();
                }).setNegativeButton("Cancel", null).show();
    }
    private void exportOptions(boolean continueExport) {
        if (busy() || !requirePro()) return;
        ExportOptionsDialog.show(this, documents.options, applied -> {
            if (busy() || !requirePro()) return;
            documents.options = applied; documents.save(); if (continueExport) export();
        }, staged -> {
            if (busy() || !requirePro()) return;
            documents.options = staged; documents.save(); exportPending = continueExport; pick("image/*", PICK_LOGO);
        });
    }
    private void export() {
        if (busy() || editor.plan == null || !requirePro()) return;
        ExportOptions selected = documents.options;
        try { selected.validate(); } catch (IllegalArgumentException missingPassword) { exportOptions(true); return; }
        List<PdfPagePlan.Page> plan = editor.plan.snapshot(); File source = editor.source;
        String name = DocumentProcessor.safeName(editor.title);
        documents.options = selected.withoutPassword(); exportPending = true; editor.exporting = true; editor.exportedPdf = null;
        documents.run("Saving edited PDF…", progress -> {
            File pdf = editor.processor.editPdf(source, plan, name, selected, progress);
            return new DocumentSession.Result(pdf, name + ".pdf\n" + plan.size() + " pages\nSaved privately in Recent PDFs", "Edited PDF is ready.", null, name);
        });
    }
    private void loadPreview() {
        int generation = ++previewGeneration; File source = editor.source; PdfPagePlan.Page edit = editor.plan.get(editor.selected);
        previewLabel.setText("Page " + (editor.selected + 1) + " of " + editor.plan.size() + " · original " + (edit.source + 1));
        preview.setContentDescription("Preview of original page " + (edit.source + 1) + ", rotated " + edit.rotation + " degrees");
        preview.setImageDrawable(null); recyclePreview();
        previewWorker.execute(() -> {
            if (closed || generation != previewGeneration) return;
            Bitmap bitmap = null; String error = null;
            try (ParcelFileDescriptor fd = ParcelFileDescriptor.open(source, ParcelFileDescriptor.MODE_READ_ONLY); PdfRenderer pdf = new PdfRenderer(fd);
                 PdfRenderer.Page page = pdf.openPage(edit.source)) {
                float scale = 900f / Math.max(page.getWidth(), page.getHeight());
                bitmap = Bitmap.createBitmap(Math.max(1, Math.round(page.getWidth() * scale)), Math.max(1, Math.round(page.getHeight() * scale)), Bitmap.Config.ARGB_8888);
                bitmap.eraseColor(Color.WHITE); page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                if (edit.rotation != 0) {
                    Matrix rotation = new Matrix(); rotation.postRotate(edit.rotation);
                    Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), rotation, true);
                    if (rotated != bitmap) bitmap.recycle(); bitmap = rotated;
                }
            } catch (Exception | OutOfMemoryError failure) { if (bitmap != null) bitmap.recycle(); bitmap = null; error = "Preview unavailable. You can still edit and save this PDF."; }
            Bitmap ready = bitmap; String message = error;
            runOnUiThread(() -> {
                if (closed || generation != previewGeneration) { if (ready != null) ready.recycle(); return; }
                previewBitmap = ready; preview.setImageBitmap(ready); if (message != null) status.setText(message);
            });
        });
    }
    private void recyclePreview() { if (previewBitmap != null) { previewBitmap.recycle(); previewBitmap = null; } }
    private void menu(int index) {
        if (busy() || editor.plan == null || index >= editor.plan.size() || !requirePro()) return;
        java.util.ArrayList<String> actions = new java.util.ArrayList<>(); actions.add("Rotate clockwise"); actions.add("Duplicate page");
        if (index > 0) actions.add("Move earlier"); if (index < editor.plan.size() - 1) actions.add("Move later");
        actions.add("Move to position…"); if (editor.plan.size() > 1) actions.add("Remove page");
        new AlertDialog.Builder(this).setTitle("Page " + (index + 1)).setItems(actions.toArray(new String[0]), (d, which) -> {
            if (busy() || !requirePro()) return;
            String action = actions.get(which); editor.selected = index;
            if (action.equals("Rotate clockwise")) editor.plan.rotate(index);
            else if (action.equals("Duplicate page")) { editor.plan.duplicate(index); editor.selected = index + 1; }
            else if (action.equals("Move earlier")) { editor.plan.move(index, index - 1); editor.selected = index - 1; }
            else if (action.equals("Move later")) { editor.plan.move(index, index + 1); editor.selected = index + 1; }
            else if (action.equals("Remove page")) editor.plan.remove(index);
            else { moveTo(index); return; }
            exportPending = false; editor.exportedPdf = null; editor.status = "PDF draft saved."; editor.changed();
        }).show();
    }
    private void moveTo(int index) {
        EditText input = new EditText(this); input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER); input.setHint("1–" + editor.plan.size()); input.setContentDescription("New page position");
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Move page to position").setView(input)
                .setNegativeButton("Cancel", null).setPositiveButton("Move", null).create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (busy() || !requirePro()) return;
            try { int position = Integer.parseInt(input.getText().toString()) - 1;
                if (position < 0 || position >= editor.plan.size()) throw new NumberFormatException();
                editor.plan.move(index, position); editor.selected = position; exportPending = false; editor.exportedPdf = null; editor.changed(); dialog.dismiss();
            } catch (NumberFormatException invalid) { input.setError("Choose a position from 1 to " + editor.plan.size() + "."); }
        })); dialog.show();
    }
    private final class PageRows extends RecyclerView.Adapter<Row> {
        @Override public int getItemCount() { return editor.plan == null ? 0 : editor.plan.size(); }
        @Override public Row onCreateViewHolder(ViewGroup parent, int type) {
            LinearLayout row = new LinearLayout(PdfEditorActivity.this); row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            row.setPadding(0, Ui.dp(PdfEditorActivity.this, 4), 0, Ui.dp(PdfEditorActivity.this, 4));
            Button select = Ui.button(PdfEditorActivity.this, "", false); row.addView(select, new LinearLayout.LayoutParams(0, -2, 1));
            Button more = Ui.button(PdfEditorActivity.this, "•••", false); more.setMinHeight(Ui.dp(PdfEditorActivity.this, 54));
            LinearLayout.LayoutParams margin = new LinearLayout.LayoutParams(Ui.dp(PdfEditorActivity.this, 60), -2); margin.setMarginStart(Ui.dp(PdfEditorActivity.this, 6)); row.addView(more, margin);
            return new Row(row, select, more);
        }
        @Override public void onBindViewHolder(Row row, int index) {
            PdfPagePlan.Page page = editor.plan.get(index);
            row.select.setText((index == editor.selected ? "● " : "") + (index + 1) + " · Original " + (page.source + 1) + (page.rotation == 0 ? "" : " · " + page.rotation + "°"));
            row.select.setContentDescription("Preview page " + (index + 1)); row.more.setContentDescription("Edit page " + (index + 1));
            row.select.setOnClickListener(v -> {
                int position = row.getBindingAdapterPosition();
                if (busy() || position == RecyclerView.NO_POSITION) return; editor.selected = position; editor.changed();
            });
            row.more.setOnClickListener(v -> { int position = row.getBindingAdapterPosition(); if (position != RecyclerView.NO_POSITION) menu(position); });
            row.select.setEnabled(!busy()); row.more.setEnabled(!busy());
        }
    }
    private static final class Row extends RecyclerView.ViewHolder {
        final Button select, more;
        Row(View view, Button select, Button more) { super(view); this.select = select; this.more = more; }
    }
    @Override protected void onDestroy() {
        closed = true; previewGeneration++; previewWorker.shutdownNow(); if (preview != null) preview.setImageDrawable(null); recyclePreview(); super.onDestroy();
    }
}
