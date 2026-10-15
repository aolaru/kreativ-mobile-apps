package com.kreativ.paperbuild;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.ClipData;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RecentPdfsActivity extends Activity {
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final List<Bitmap> previews = new ArrayList<>();
    private PdfLibrary library;
    private LinearLayout items;
    private Button toggle;
    private boolean removed, working;
    private volatile boolean closed;
    private volatile int generation;
    private int page;
    private PdfLibrary.Entry pendingSave;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state); library = new PdfLibrary(this);
        if (state != null) { removed = state.getBoolean("removed"); page = state.getInt("page", 0); }
        ScrollView scroll = new ScrollView(this); scroll.setBackgroundColor(Ui.SURFACE);
        LinearLayout root = Ui.column(this); Ui.applyInsets(scroll, root); scroll.addView(root);
        root.addView(Ui.brand(this)); Ui.gap(root, 20);
        root.addView(Ui.text(this, "Recent PDFs", 28, Ui.INK, true)); Ui.gap(root, 8);
        root.addView(Ui.text(this, "Saved privately on this device. Export copies to keep them after uninstalling.", 14, Ui.MUTED, false)); Ui.gap(root, 16);
        toggle = Ui.button(this, "", false); toggle.setOnClickListener(v -> { removed = !removed; page = 0; load(); }); root.addView(toggle); Ui.gap(root, 16);
        items = Ui.column(this); root.addView(items);
        Button back = Ui.button(this, "Back to PaperBuild", false); back.setOnClickListener(v -> finish()); root.addView(back); setContentView(scroll); load();
    }
    private void load() {
        int request = ++generation; boolean showRemoved = removed;
        toggle.setText(removed ? "Show recent PDFs" : "Removed PDFs"); items.removeAllViews();
        recyclePreviews(); items.addView(Ui.text(this, "Loading…", 14, Ui.MUTED, false));
        worker.execute(() -> {
            List<PdfLibrary.Entry> entries = library.list(showRemoved);
            runOnUiThread(() -> {
                if (closed || generation != request) return; items.removeAllViews();
                if (entries.isEmpty()) {
                    items.addView(Ui.text(this, removed ? "No removed PDFs." : "Your next PDF will appear here.", 16, Ui.MUTED, false)); Ui.gap(items, 20);
                }
                page = Math.min(page, Math.max(0, (entries.size() - 1) / 20));
                for (int i = page * 20; i < Math.min((page + 1) * 20, entries.size()); i++) addEntry(entries.get(i), request);
                if (page > 0) {
                    Button previous = Ui.button(this, "Previous PDFs", false); previous.setOnClickListener(v -> { page--; load(); }); items.addView(previous); Ui.gap(items, 12);
                }
                if (entries.size() > (page + 1) * 20) {
                    Button more = Ui.button(this, "Next PDFs", false); more.setOnClickListener(v -> { page++; load(); }); items.addView(more); Ui.gap(items, 12);
                }
            });
        });
    }
    private void addEntry(PdfLibrary.Entry entry, int request) {
        LinearLayout card = Ui.card(this, R.drawable.card), row = new LinearLayout(this);
        ImageView preview = new ImageView(this); preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        preview.setImageResource(entry.protectedPdf ? R.drawable.ic_shield : R.drawable.ic_document_build);
        preview.setImportantForAccessibility(android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        row.addView(preview, new LinearLayout.LayoutParams(Ui.dp(this, 54), Ui.dp(this, 72)));
        LinearLayout copy = Ui.column(this); copy.setPadding(Ui.dp(this, 12), 0, 0, 0);
        copy.addView(Ui.text(this, entry.title, 17, Ui.INK, true)); Ui.gap(copy, 6);
        String size = String.format(Locale.US, "%.1f MB", library.file(entry).length() / 1048576f);
        if (library.file(entry).length() < 1048576) size = Math.max(1, library.file(entry).length() / 1024) + " KB";
        String details = DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date(entry.created)) + " · " + size;
        if (entry.pages > 0) details += " · " + entry.pages + (entry.pages == 1 ? " page" : " pages");
        if (entry.protectedPdf) details += "\nPassword protected";
        copy.addView(Ui.text(this, details, 12, Ui.MUTED, false)); row.addView(copy, new LinearLayout.LayoutParams(0, -2, 1)); card.addView(row); Ui.gap(card, 12);
        Button action = Ui.button(this, entry.removed ? "Restore PDF" : "Open PDF", !entry.removed);
        action.setContentDescription((entry.removed ? "Restore " : "Open ") + entry.title);
        action.setOnClickListener(v -> { if (entry.removed) mutate(() -> library.setRemoved(entry, false), "PDF restored."); else open(entry, false); }); card.addView(action);
        Ui.gap(card, 8); Button more = Ui.button(this, entry.removed ? "Delete permanently" : "More options", false);
        more.setContentDescription("Options for " + entry.title); more.setOnClickListener(v -> options(entry)); card.addView(more);
        items.addView(card); Ui.gap(items, 12);
        if (!entry.protectedPdf) worker.execute(() -> {
            if (closed || generation != request) return;
            Bitmap bitmap = null;
            try (ParcelFileDescriptor fd = ParcelFileDescriptor.open(library.file(entry), ParcelFileDescriptor.MODE_READ_ONLY);
                 PdfRenderer pdf = new PdfRenderer(fd)) {
                if (pdf.getPageCount() > 0) try (PdfRenderer.Page page = pdf.openPage(0)) {
                    bitmap = Bitmap.createBitmap(120, Math.max(1, Math.min(240, 120 * page.getHeight() / Math.max(1, page.getWidth()))), Bitmap.Config.ARGB_8888);
                    bitmap.eraseColor(Color.WHITE); page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                }
            } catch (Exception | OutOfMemoryError ignored) { if (bitmap != null) bitmap.recycle(); bitmap = null; }
            Bitmap result = bitmap;
            runOnUiThread(() -> {
                if (result == null) return;
                if (closed || generation != request) { result.recycle(); return; }
                previews.add(result); preview.setImageBitmap(result);
            });
        });
    }
    private void options(PdfLibrary.Entry entry) {
        if (working) return;
        if (entry.removed) {
            new AlertDialog.Builder(this).setTitle("Delete permanently?")
                    .setMessage("Only PaperBuild’s private copy will be deleted. This cannot be undone. Copies you exported stay untouched.")
                    .setNegativeButton("Cancel", null).setPositiveButton("Delete", (d, w) -> mutate(() -> library.deleteForever(entry), "PDF deleted.")).show(); return;
        }
        new AlertDialog.Builder(this).setTitle(entry.title).setItems(new String[]{"Share", "Save to Downloads", "Rename", "Move to Removed PDFs"}, (d, which) -> {
            if (which == 0) open(entry, true);
            else if (which == 1) save(entry);
            else if (which == 2) {
                EditText name = new EditText(this); name.setText(entry.title); name.setSingleLine(true); name.setContentDescription("PDF name");
                AlertDialog rename = new AlertDialog.Builder(this).setTitle("Rename PDF").setView(name).setNegativeButton("Cancel", null).setPositiveButton("Save", null).create();
                rename.setOnShowListener(ignored -> rename.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    if (name.getText().toString().trim().isEmpty()) { name.setError("Enter a name."); return; }
                    String title = name.getText().toString();
                    mutate(() -> library.rename(entry, title), "PDF renamed."); rename.dismiss();
                })); rename.show();
            } else new AlertDialog.Builder(this).setTitle("Remove from Recent PDFs?")
                    .setMessage("You can restore it from Removed PDFs. Exported copies stay untouched.")
                    .setNegativeButton("Cancel", null).setPositiveButton("Remove", (dialog, w) -> mutate(() -> library.setRemoved(entry, true), "PDF moved to Removed PDFs.")).show();
        }).show();
    }
    private void open(PdfLibrary.Entry entry, boolean share) {
        try {
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", library.file(entry), entry.title + ".pdf");
            Intent intent = share ? new Intent(Intent.ACTION_SEND).setType("application/pdf").putExtra(Intent.EXTRA_STREAM, uri)
                    : new Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/pdf");
            intent.setClipData(ClipData.newRawUri(entry.title, uri)); intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, share ? "Share PDF" : "Open PDF"));
        } catch (android.content.ActivityNotFoundException | IllegalArgumentException error) { Toast.makeText(this, "No PDF app is available.", Toast.LENGTH_LONG).show(); }
    }
    private void save(PdfLibrary.Entry entry) {
        if (Build.VERSION.SDK_INT <= 28 && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            pendingSave = entry; requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1); return;
        }
        mutate(() -> new MainActivity.ContextSave(getContentResolver()).save(library.file(entry), entry.title + ".pdf"), "Saved to Downloads/PaperBuild.");
    }
    @Override public void onRequestPermissionsResult(int request, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(request, permissions, results);
        PdfLibrary.Entry entry = pendingSave; pendingSave = null;
        if (request == 1 && entry != null) {
            if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) save(entry);
            else Toast.makeText(this, "Storage permission is needed to save on this Android version.", Toast.LENGTH_LONG).show();
        }
    }
    private interface Work { void run() throws IOException; }
    private void mutate(Work work, String success) {
        if (working) return; working = true;
        // Library writes and Downloads saves must finish even if this screen is closed or rotated.
        DocumentSession.get(this).worker.execute(() -> {
            String message = success;
            try { work.run(); } catch (IOException | RuntimeException error) { message = "Couldn’t update this PDF. Try again."; }
            String result = message;
            runOnUiThread(() -> { working = false; if (!closed) { Toast.makeText(this, result, Toast.LENGTH_LONG).show(); load(); } });
        });
    }
    private void recyclePreviews() { for (Bitmap bitmap : previews) bitmap.recycle(); previews.clear(); }
    @Override protected void onSaveInstanceState(Bundle state) {
        state.putBoolean("removed", removed); state.putInt("page", page); super.onSaveInstanceState(state);
    }
    @Override protected void onDestroy() { closed = true; generation++; worker.shutdownNow(); recyclePreviews(); super.onDestroy(); }
}
