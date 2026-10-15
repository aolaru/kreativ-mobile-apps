package com.kreativ.paperbuild;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends Activity {
    private static final int PICK_IMAGES = 101, CAPTURE_PAGE = 102, WRITE_DOCUMENT = 103,
            PICK_PDFS_TO_MERGE = 104, PICK_PDF_TO_SPLIT = 105;
    private static final int A4_WIDTH = 595, A4_HEIGHT = 842, LETTER_WIDTH = 612, LETTER_HEIGHT = 792;
    private static final int ENHANCE_COLOR = 0, ENHANCE_GRAYSCALE = 1, ENHANCE_CONTRAST = 2;
    private static final int COMPRESSION_STANDARD = 0, COMPRESSION_SMALL = 1, FREE_PAGE_LIMIT = 10;
    private DocumentSession session;
    private ArrayList<PageItem> pages;
    private RecyclerView pageList;
    private PageAdapter pageAdapter;
    private TextView pageSummary, statusText, resultSummary;
    private EditText documentName;
    private Button makePdfButton, shareButton, saveButton, openButton, scanButton, addButton,
            mergeButton, splitButton, clearButton, proButton;
    private ProgressBar progressBar;
    private RadioGroup paperSizeGroup, enhancementGroup, compressionGroup;
    private LinearLayout exportSection, resultSection, settingsPanel;
    private View emptyState;
    private Button settingsButton;
    private boolean settingsExpanded;
    private Button recentButton, exportOptionsButton, presetsButton;
    private int shownRevision = -1;
    private final Runnable sessionListener = this::renderSession;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        session = DocumentSession.get(this);
        pages = session.pages;
        settingsExpanded = state != null && state.getBoolean("settingsExpanded");
        setContentView(buildScreen());
        documentName.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                session.name = s.toString(); session.changed();
            }
            public void afterTextChanged(Editable e) { }
        });
        paperSizeGroup.setOnCheckedChangeListener((group, id) -> {
            session.paper = selectedPaper(); session.changed();
        });
        enhancementGroup.setOnCheckedChangeListener((group, id) -> {
            session.enhancement = selectedEnhancement(); session.changed();
        });
        compressionGroup.setOnCheckedChangeListener((group, id) -> {
            session.compression = selectedCompression(); session.changed();
        });
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        state.putBoolean("settingsExpanded", settingsExpanded);
        super.onSaveInstanceState(state);
    }
    @Override protected void onStart() { super.onStart(); session.attach(sessionListener); }
    @Override protected void onStop() { session.detach(sessionListener); super.onStop(); }
    @Override protected void onDestroy() { pageAdapter.close(); super.onDestroy(); }
    @Override protected void onResume() {
        super.onResume();
        ProBilling.refresh(this, this::updateProUi);
    }

    private View buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true); scroll.setBackgroundColor(Ui.SURFACE); scroll.setClipToPadding(false);
        LinearLayout root = Ui.column(this);
        Ui.applyInsets(scroll, root); scroll.addView(root, new ScrollView.LayoutParams(-1, -1));
        root.addView(Ui.brand(this)); Ui.gap(root, 20);
        root.addView(Ui.hero(this, "YOUR PRIVATE PDF STUDIO", "From paper\nto polished.", "Scan. Arrange. Share."));
        TextView trust = label("Offline documents · No ads · No account", 12, Ui.MUTED);
        trust.setGravity(Gravity.CENTER); trust.setPadding(0, dp(14), 0, dp(18)); root.addView(trust);

        LinearLayout addCard = Ui.card(this, R.drawable.card);
        Ui.step(addCard, "01", "Add your pages"); Ui.gap(addCard, 16);
        scanButton = button("Scan", true); Ui.icon(scanButton, R.drawable.ic_camera, Color.WHITE);
        scanButton.setOnClickListener(v -> capturePage());
        addButton = button("Add images", false); addButton.setBackgroundResource(R.drawable.button_import);
        Ui.icon(addButton, R.drawable.ic_images, Ui.ACCENT); addButton.setOnClickListener(v -> chooseImages());
        addCard.addView(actionRow(scanButton, addButton)); Ui.gap(addCard, 16);
        LinearLayout empty = Ui.column(this); empty.setBackgroundResource(R.drawable.field_background);
        empty.setPadding(dp(16), dp(18), dp(16), dp(18));
        TextView emptyTitle = Ui.text(this, "Your next PDF starts here", 16, Ui.INK, true);
        emptyTitle.setGravity(Gravity.CENTER); empty.addView(emptyTitle); Ui.gap(empty, 6);
        TextView emptyCopy = label("Choose photos or capture a page.", 13, Ui.MUTED);
        emptyCopy.setGravity(Gravity.CENTER); empty.addView(emptyCopy); Ui.gap(empty, 10);
        TextView freeLimit = label("Free · up to 10 pages per PDF", 12, Ui.MUTED);
        freeLimit.setGravity(Gravity.CENTER); empty.addView(freeLimit); emptyState = empty; addCard.addView(empty);
        pageSummary = Ui.text(this, "", 14, Ui.MUTED, true); pageSummary.setPadding(0, 0, 0, dp(12)); addCard.addView(pageSummary);
        pageList = new RecyclerView(this); pageList.setLayoutManager(new LinearLayoutManager(this));
        pageAdapter = new PageAdapter(pages, session.processor, new PageAdapter.Actions() {
            public void move(int from, int to) { movePage(from, to); }
            public void rotate(int index) {
                PageItem page = pages.get(index);
                pages.set(index, page.clockwise()); session.changed();
            }
            public void crop(int index) {
                if (!session.busy) startActivity(new Intent(MainActivity.this, CropActivity.class).putExtra("index", index));
            }
            public void remove(int index) {
                PageItem removed = pages.remove(index); session.deletePageFile(removed); session.changed();
            }
        });
        pageList.setAdapter(pageAdapter); addCard.addView(pageList, new LinearLayout.LayoutParams(-1, dp(320)));
        clearButton = button("New document", false); clearButton.setOnClickListener(v -> clearDocument());
        addCard.addView(clearButton, matchWrap()); root.addView(addCard); Ui.gap(root, 16);

        exportSection = Ui.card(this, R.drawable.card);
        Ui.step(exportSection, "02", "Make it yours"); Ui.gap(exportSection, 16);
        exportSection.addView(label("Document name", 12, Ui.MUTED)); Ui.gap(exportSection, 8);
        documentName = new EditText(this); documentName.setText(session.name); documentName.setSingleLine(true);
        documentName.setTextSize(16); documentName.setTextColor(Ui.INK);
        documentName.setBackgroundResource(R.drawable.field_background);
        documentName.setPadding(dp(14), dp(14), dp(14), dp(14));
        documentName.setContentDescription("Document name"); documentName.setMinHeight(dp(52));
        exportSection.addView(documentName, matchWrap()); Ui.gap(exportSection, 12);
        settingsButton = button("PDF settings", false);
        settingsButton.setOnClickListener(v -> {
            settingsExpanded = !settingsExpanded;
            settingsPanel.setVisibility(settingsExpanded ? View.VISIBLE : View.GONE); updateSettingsLabel();
        }); exportSection.addView(settingsButton, matchWrap());
        settingsPanel = Ui.column(this); settingsPanel.setPadding(0, dp(16), 0, dp(4));
        settingsPanel.addView(label("PAGE SIZE", 11, Ui.MUTED)); Ui.gap(settingsPanel, 8);
        paperSizeGroup = choiceGroup();
        paperSizeGroup.addView(radio("A4", 0, session.paper == 0));
        paperSizeGroup.addView(radio("US Letter", 1, session.paper == 1)); settingsPanel.addView(paperSizeGroup, matchWrap());
        Ui.gap(settingsPanel, 16); settingsPanel.addView(label("PAGE STYLE", 11, Ui.MUTED)); Ui.gap(settingsPanel, 8);
        enhancementGroup = choiceGroup();
        enhancementGroup.addView(radio("Colour", ENHANCE_COLOR, session.enhancement == 0));
        enhancementGroup.addView(radio("Grayscale", ENHANCE_GRAYSCALE, session.enhancement == 1));
        enhancementGroup.addView(radio("Contrast", ENHANCE_CONTRAST, session.enhancement == 2));
        settingsPanel.addView(enhancementGroup, matchWrap());
        Ui.gap(settingsPanel, 16); settingsPanel.addView(label("FILE SIZE", 11, Ui.MUTED)); Ui.gap(settingsPanel, 8);
        compressionGroup = choiceGroup();
        compressionGroup.addView(radio("Standard", COMPRESSION_STANDARD, session.compression == 0 || !isPro()));
        RadioButton compact = radio("Smaller · Pro", COMPRESSION_SMALL, session.compression == 1 && isPro());
        compact.setOnClickListener(v -> {
            if (!isPro()) { compressionGroup.check(compressionGroup.getChildAt(0).getId()); showPro(); }
        }); compressionGroup.addView(compact); settingsPanel.addView(compressionGroup, matchWrap());
        settingsPanel.setVisibility(settingsExpanded ? View.VISIBLE : View.GONE);
        exportSection.addView(settingsPanel); Ui.gap(exportSection, 16);
        makePdfButton = button("Build PDF", true); makePdfButton.setEnabled(false);
        makePdfButton.setOnClickListener(v -> createPdf()); exportSection.addView(makePdfButton, matchWrap());
        root.addView(exportSection);
        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal); progressBar.setMax(100);
        progressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(Ui.ACCENT));
        progressBar.setVisibility(View.GONE); root.addView(progressBar, matchWrap());
        statusText = label("", 14, Ui.MUTED); statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(0, dp(12), 0, dp(12)); root.addView(statusText, matchWrap());

        resultSection = Ui.card(this, R.drawable.result_card);
        resultSection.addView(Ui.text(this, "PDF READY", 11, Color.rgb(37, 104, 79), true)); Ui.gap(resultSection, 8);
        resultSection.addView(Ui.text(this, "Ready to share.", 24, Ui.INK, true)); Ui.gap(resultSection, 10);
        resultSummary = label("", 13, Ui.MUTED); resultSummary.setLineSpacing(dp(4), 1f); resultSection.addView(resultSummary);
        Ui.gap(resultSection, 16); shareButton = button("Share PDF", true); shareButton.setOnClickListener(v -> sharePdf());
        resultSection.addView(shareButton, matchWrap()); Ui.gap(resultSection, 10);
        saveButton = button("Save PDF", false); saveButton.setContentDescription("Save PDF to Downloads"); saveButton.setOnClickListener(v -> savePdf());
        openButton = button("Open PDF", false); openButton.setOnClickListener(v -> openPdf());
        resultSection.addView(actionRow(saveButton, openButton)); root.addView(resultSection); Ui.gap(root, 16);

        LinearLayout tools = Ui.card(this, R.drawable.card);
        LinearLayout toolHeading = new LinearLayout(this); toolHeading.setGravity(Gravity.CENTER_VERTICAL);
        toolHeading.addView(sectionTitle("PDF tools"), weightWrap(1));
        toolHeading.addView(Ui.pill(this, "PRO", Color.rgb(237, 240, 255), Ui.ACCENT)); tools.addView(toolHeading);
        Ui.gap(tools, 6); tools.addView(label("Original pages. Original quality.", 13, Ui.MUTED)); Ui.gap(tools, 16);
        mergeButton = button("Merge PDFs", false); Ui.icon(mergeButton, R.drawable.ic_merge, Ui.ACCENT);
        mergeButton.setOnClickListener(v -> { if (requirePro()) choosePdfsForMerge(); });
        splitButton = button("Extract pages", false); Ui.icon(splitButton, R.drawable.ic_extract, Ui.ACCENT);
        splitButton.setOnClickListener(v -> { if (requirePro()) choosePdfForSplit(); });
        tools.addView(actionRow(mergeButton, splitButton)); Ui.gap(tools, 12);
        exportOptionsButton = button("Pro export options", false); exportOptionsButton.setOnClickListener(v -> showExportOptions(null));
        exportOptionsButton.setId(R.id.export_options);
        tools.addView(exportOptionsButton, matchWrap()); Ui.gap(tools, 10);
        presetsButton = button("Saved presets · Pro", false); presetsButton.setOnClickListener(v -> showPresets()); tools.addView(presetsButton, matchWrap());
        presetsButton.setId(R.id.saved_presets);
        root.addView(tools); Ui.gap(root, 16);
        recentButton = button("Recent PDFs · Free", false);
        recentButton.setId(R.id.recent_pdfs);
        recentButton.setOnClickListener(v -> startActivity(new Intent(this, RecentPdfsActivity.class)));
        root.addView(recentButton, matchWrap()); Ui.gap(root, 16);
        root.addView(proCard()); Ui.gap(root, 18);
        Button about = button("Privacy & About", false); Ui.icon(about, R.drawable.ic_shield, Ui.MUTED);
        about.setOnClickListener(v -> startActivity(new Intent(this, AboutActivity.class))); root.addView(about, matchWrap());
        TextView footer = label(getString(R.string.version_credit, BuildConfig.VERSION_NAME), 12, Ui.MUTED);
        footer.setId(R.id.version_credit);
        footer.setGravity(Gravity.CENTER); footer.setPadding(0, dp(18), 0, 0); root.addView(footer, matchWrap());
        return scroll;
    }

    private View proCard() {
        LinearLayout card = Ui.card(this, R.drawable.pro_panel);
        card.addView(Ui.text(this, "PAPERBUILD PRO", 11, Ui.ACCENT, true)); Ui.gap(card, 10);
        card.addView(Ui.text(this, "A little more possibility.", 22, Ui.INK, true)); Ui.gap(card, 8);
        card.addView(label("Protect. Personalise. Export your way.", 14, Ui.MUTED)); Ui.gap(card, 16);
        proButton = button(isPro() ? "Manage Pro" : "Explore Pro", true); proButton.setOnClickListener(v -> showPro());
        card.addView(proButton, matchWrap()); Ui.gap(card, 10);
        TextView copy = label("One-time purchase · No subscription", 12, Ui.MUTED); copy.setGravity(Gravity.CENTER); card.addView(copy);
        return card;
    }
    private boolean narrowLayout() {
        return getResources().getConfiguration().screenWidthDp < 360 || getResources().getConfiguration().fontScale > 1.15f;
    }
    private LinearLayout actionRow(Button first, Button second) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(narrowLayout() ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        row.addView(first, narrowLayout() ? matchWrap() : weightWrap(1));
        if (narrowLayout()) Ui.gap(row, 10); else addHorizontalSpacer(row, 10);
        row.addView(second, narrowLayout() ? matchWrap() : weightWrap(1)); return row;
    }
    private RadioGroup choiceGroup() {
        RadioGroup group = new RadioGroup(this); group.setOrientation(narrowLayout() ? RadioGroup.VERTICAL : RadioGroup.HORIZONTAL); return group;
    }
    private void updateSettingsLabel() {
        String paper = selectedPaper() == 1 ? "Letter" : "A4";
        String treatment = selectedEnhancement() == 1 ? "Grayscale" : selectedEnhancement() == 2 ? "Contrast" : "Colour";
        settingsButton.setText(paper + " · " + treatment + " · " + (selectedCompression() == 1 ? "Compact" : "Standard") + (settingsExpanded ? "  ↑" : "  ↓"));
        settingsButton.setContentDescription("PDF settings. " + paper + ", " + treatment + ". " + (settingsExpanded ? "Collapse" : "Expand"));
    }

    private boolean isPro() { return ProBilling.isPro(this); }
    private boolean requirePro() { if (isPro()) return true; showPro(); return false; }
    private void showPro() { startActivity(new Intent(this, ProActivity.class)); }
    private void updateProUi() {
        if (isFinishing() || isDestroyed()) return;
        proButton.setText(isPro() ? "Manage Pro" : "Explore Pro");
        exportOptionsButton.setText(isPro() ? session.options.summary() : "Pro export options");
        if (!isPro() && selectedCompression() == COMPRESSION_STANDARD) {
            compressionGroup.check(compressionGroup.getChildAt(0).getId());
        }
    }

    private void renderSession() {
        if (isDestroyed()) return;
        boolean busy = session.busy;
        scanButton.setEnabled(!busy); addButton.setEnabled(!busy);
        mergeButton.setEnabled(!busy); splitButton.setEnabled(!busy); clearButton.setEnabled(!busy && !pages.isEmpty());
        recentButton.setEnabled(!busy); exportOptionsButton.setEnabled(!busy); presetsButton.setEnabled(!busy);
        exportOptionsButton.setText(isPro() ? session.options.summary() : "Pro export options");
        documentName.setEnabled(!busy);
        enableGroup(paperSizeGroup, !busy); enableGroup(enhancementGroup, !busy); enableGroup(compressionGroup, !busy);
        makePdfButton.setEnabled(!busy && !pages.isEmpty());
        emptyState.setVisibility(pages.isEmpty() ? View.VISIBLE : View.GONE);
        pageSummary.setVisibility(pages.isEmpty() ? View.GONE : View.VISIBLE);
        clearButton.setVisibility(pages.isEmpty() ? View.GONE : View.VISIBLE);
        pageSummary.setText(pages.size() + (pages.size() == 1 ? " page · Draft saved" : " pages · Draft saved"));
        exportSection.setVisibility(pages.isEmpty() ? View.GONE : View.VISIBLE);
        updateSettingsLabel();
        pageList.setVisibility(pages.isEmpty() ? View.GONE : View.VISIBLE);
        pageList.getLayoutParams().height = dp(Math.min(420, Math.max(160, pages.size() * (narrowLayout() ? 225 : 155))));
        pageAdapter.setBusy(busy);
        if (shownRevision != session.revision) { shownRevision = session.revision; pageAdapter.notifyDataSetChanged(); }
        progressBar.setVisibility(busy ? View.VISIBLE : View.GONE);
        progressBar.setIndeterminate(session.progress < 0);
        if (session.progress >= 0) progressBar.setProgress(session.progress);
        statusText.setText(session.status);
        statusText.setVisibility(session.status.isEmpty() ? View.GONE : View.VISIBLE);
        boolean result = !busy && session.latestPdf != null && session.latestPdf.exists();
        resultSection.setVisibility(result ? View.VISIBLE : View.GONE);
        resultSummary.setText(session.summary.replaceAll("-[0-9]{5,}\\.pdf", ".pdf"));
        resultSummary.setVisibility(result ? View.VISIBLE : View.GONE);
        for (Button button : new Button[]{shareButton, saveButton, openButton}) {
            button.setEnabled(result); button.setVisibility(result ? View.VISIBLE : View.GONE);
        }
    }
    private void enableGroup(RadioGroup group, boolean enabled) {
        for (int i = 0; i < group.getChildCount(); i++) group.getChildAt(i).setEnabled(enabled);
    }
    private void movePage(int from, int to) {
        if (session.busy || to < 0 || to >= pages.size()) return;
        pages.add(to, pages.remove(from)); session.changed();
    }
    private void clearDocument() {
        new AlertDialog.Builder(this).setTitle("Start a new document?")
                .setMessage("This removes the current draft. Recent PDFs and exported copies stay available.")
                .setNegativeButton("Cancel", null).setPositiveButton("New document", (dialog, which) -> {
                    if (session.busy) return;
                    for (PageItem page : pages) session.deletePageFile(page);
                    pages.clear(); session.name = "My document"; session.options = session.options.withoutPassword();
                    documentName.setText(session.name); session.changed();
                }).show();
    }

    private void chooseImages() { chooseFiles("image/*", true, PICK_IMAGES, "Add pages"); }
    private void choosePdfsForMerge() { chooseFiles("application/pdf", true, PICK_PDFS_TO_MERGE, "Choose PDFs to merge"); }
    private void choosePdfForSplit() { chooseFiles("application/pdf", false, PICK_PDF_TO_SPLIT, "Choose a PDF to split"); }
    private void chooseFiles(String type, boolean multiple, int request, String title) {
        if (session.busy) return;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType(type);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, multiple);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try { startActivityForResult(Intent.createChooser(intent, title), request); }
        catch (ActivityNotFoundException error) { Toast.makeText(this, "No file picker is available.", Toast.LENGTH_LONG).show(); }
    }
    private ArrayList<Uri> selectedUris(Intent data) {
        Set<Uri> selected = new LinkedHashSet<>();
        if (data.getData() != null) selected.add(data.getData());
        ClipData clips = data.getClipData();
        if (clips != null) for (int i = 0; i < clips.getItemCount(); i++) selected.add(clips.getItemAt(i).getUri());
        return new ArrayList<>(selected);
    }
    private void capturePage() {
        if (session.busy) return;
        if (!isPro() && pages.size() >= FREE_PAGE_LIMIT) { showPro(); return; }
        try {
            session.pendingCapture = session.processor.capture(); session.save();
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", session.pendingCapture);
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
            intent.setClipData(ClipData.newRawUri("New scan", uri));
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(intent, CAPTURE_PAGE);
        } catch (IOException | ActivityNotFoundException | SecurityException failure) {
            if (session.pendingCapture != null) session.pendingCapture.delete();
            session.pendingCapture = null; session.save();
            Toast.makeText(this, "Couldn’t open the camera. You can add an image instead.", Toast.LENGTH_LONG).show();
        }
    }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == CAPTURE_PAGE) {
            File capture = session.pendingCapture;
            session.pendingCapture = null;
            if (result == RESULT_OK && capture != null && capture.length() > 0) {
                pages.add(new PageItem(Uri.fromFile(capture), "Scanned page")); session.changed();
            } else { if (capture != null) capture.delete(); session.save(); }
            return;
        }
        if (result != RESULT_OK || data == null || session.busy) return;
        ArrayList<Uri> selected = selectedUris(data);
        if (request == PICK_PDFS_TO_MERGE) {
            if (!requirePro()) return;
            if (selected.size() < 2) { Toast.makeText(this, "Choose at least two PDFs.", Toast.LENGTH_LONG).show(); return; }
            DocumentProcessor processor = session.processor;
            withExportOptions(options -> session.run("Merging PDFs…", progress -> {
                File pdf = processor.merge(selected, options, progress);
                return pdfResult(pdf, "Merged PDF is ready.", "Merged PDF");
            }));
        } else if (request == PICK_PDF_TO_SPLIT && !selected.isEmpty()) {
            if (requirePro()) askSplitRange(selected.get(0));
        } else if (request == PICK_IMAGES) { importImages(selected); }
    }
    private void importImages(ArrayList<Uri> selected) {
        int allowed = isPro() ? selected.size() : Math.min(selected.size(), FREE_PAGE_LIMIT - pages.size());
        if (allowed <= 0) { showPro(); return; }
        ArrayList<Uri> sources = new ArrayList<>(selected.subList(0, allowed));
        // Provider metadata is read on the same background worker as the file copies.
        ContentResolver resolver = getApplicationContext().getContentResolver();
        DocumentProcessor processor = session.processor;
        boolean limited = allowed < selected.size();
        session.run("Adding images…", progress -> {
            ArrayList<PageItem> imported = new ArrayList<>();
            int failed = 0;
            try {
                for (int i = 0; i < sources.size(); i++) {
                    try { imported.add(processor.importImage(sources.get(i), displayName(resolver, sources.get(i)))); }
                    catch (IOException | RuntimeException error) { failed++; }
                    progress.update((i + 1) * 100 / sources.size());
                }
                String message = imported.size() + (imported.size() == 1 ? " page added." : " pages added.");
                if (failed > 0) message += " " + failed + " unreadable images skipped.";
                if (limited) message += " Free documents allow 10 pages.";
                return new DocumentSession.Result(null, "", message, imported);
            } catch (OutOfMemoryError error) {
                for (PageItem item : imported) new File(item.uri.getPath()).delete();
                throw error;
            }
        });
    }
    private void askSplitRange(Uri uri) {
        EditText range = new EditText(this); range.setHint("1-3"); range.setInputType(InputType.TYPE_CLASS_TEXT);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Extract PDF pages")
                .setMessage("Keep a page or range, such as 1-3. Original page sizes and text are preserved.")
                .setView(range).setNegativeButton("Cancel", null).setPositiveButton("Create PDF", null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            int[] parsed = PageRange.parse(range.getText().toString());
            if (parsed == null) { range.setError("Use a page or range such as 1-3."); return; }
            if (!requirePro() || session.busy) return;
            DocumentProcessor processor = session.processor;
            withExportOptions(options -> session.run("Extracting pages…", progress -> pdfResult(processor.split(uri, parsed[0], parsed[1], options), "Extracted pages are ready.", "Extracted pages")));
            dialog.dismiss();
        }));
        dialog.show();
    }
    private void createPdf() {
        if (session.busy || pages.isEmpty()) return;
        if (!isPro() && pages.size() > FREE_PAGE_LIMIT) { showPro(); return; }
        ArrayList<PageItem> snapshot = new ArrayList<>(pages);
        String title = session.name;
        int width = selectedPaperWidth(), height = selectedPaperHeight();
        int treatment = selectedEnhancement();
        boolean compact = selectedCompression() == COMPRESSION_SMALL;
        DocumentProcessor processor = session.processor;
        withExportOptions(options -> session.run("Building your PDF…", progress -> {
            File pdf = processor.exportImages(snapshot, title, width, height, treatment, compact, options, progress);
            return new DocumentSession.Result(pdf, DocumentProcessor.safeName(title) + ".pdf\n" + snapshot.size() + (snapshot.size() == 1 ? " page · " : " pages · ") + readableSize(pdf.length())
                    + "\n" + (width == LETTER_WIDTH ? "US Letter" : "A4") + " · " + (compact ? "Compact" : "Standard")
                    + (options.protect ? " · Password protected" : "")
                    + "\nSaved privately in Recent PDFs", "Your PDF is ready.", null, title);
        }));
    }
    private static DocumentSession.Result pdfResult(File pdf, String message, String title) {
        return new DocumentSession.Result(pdf, title + ".pdf\n" + readableSize(pdf.length()) + "\nSaved privately in Recent PDFs", message, null, title);
    }
    private void withExportOptions(java.util.function.Consumer<ExportOptions> export) {
        if (session.busy) return;
        ExportOptions options = isPro() ? session.options : ExportOptions.NONE;
        try { options.validate(); }
        catch (IllegalArgumentException missingPassword) { showExportOptions(() -> withExportOptions(export)); return; }
        session.options = session.options.withoutPassword(); session.save(); export.accept(options);
    }

    private void showExportOptions(Runnable afterSave) {
        if (session.busy || !requirePro()) return;
        ScrollView scroll = new ScrollView(this); LinearLayout fields = Ui.column(this);
        int padding = dp(20); fields.setPadding(padding, dp(8), padding, dp(12)); scroll.addView(fields);
        fields.addView(label("Applies to photos, merged PDFs and extracted pages.", 13, Ui.MUTED)); Ui.gap(fields, 12);
        fields.addView(label("Watermark text (optional)", 13, Ui.MUTED));
        EditText watermark = new EditText(this); watermark.setSingleLine(true); watermark.setText(session.options.watermark);
        watermark.setHint("e.g. Confidential"); watermark.setContentDescription("Watermark text");
        watermark.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(80)}); fields.addView(watermark);
        CheckBox numbers = new CheckBox(this); numbers.setText("Add page numbers · 1 / total"); numbers.setChecked(session.options.pageNumbers); fields.addView(numbers);
        CheckBox protect = new CheckBox(this); protect.setText("Require a password to open"); protect.setChecked(session.options.protect); fields.addView(protect);
        EditText password = passwordField("Password · 8–64 characters"), confirm = passwordField("Confirm password");
        fields.addView(password); fields.addView(confirm);
        password.setText(session.options.password); confirm.setText(session.options.password);
        password.setEnabled(protect.isChecked()); confirm.setEnabled(protect.isChecked());
        protect.setOnCheckedChangeListener((v, checked) -> { password.setEnabled(checked); confirm.setEnabled(checked); });
        Ui.gap(fields, 8); fields.addView(label("Passwords aren’t saved in drafts or presets. Enter one for each export. Forgotten passwords cannot be recovered.", 12, Ui.MUTED));
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Pro export options").setView(scroll)
                .setNegativeButton("Cancel", null).setPositiveButton("Apply", null).create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (session.busy || !requirePro()) return;
            if (protect.isChecked() && !password.getText().toString().equals(confirm.getText().toString())) { confirm.setError("Passwords don’t match."); return; }
            try {
                ExportOptions options = new ExportOptions(watermark.getText().toString(), numbers.isChecked(), protect.isChecked(), password.getText().toString());
                options.validate(); session.options = options; session.changed();
                password.setText(""); confirm.setText(""); dialog.dismiss(); if (afterSave != null) afterSave.run();
            } catch (IllegalArgumentException invalid) { password.setError(invalid.getMessage()); }
        })); dialog.show();
        dialog.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE);
    }
    private EditText passwordField(String hint) {
        EditText field = new EditText(this); field.setHint(hint); field.setContentDescription(hint); field.setSingleLine(true);
        field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        field.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(64)});
        field.setSaveEnabled(false); field.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO); return field;
    }
    private void showPresets() {
        if (session.busy || !requirePro()) return;
        PresetStore store = new PresetStore(this); java.util.List<PresetStore.Preset> presets = store.list();
        String[] names = new String[presets.size() + 1]; names[0] = "Save current settings…";
        for (int i = 0; i < presets.size(); i++) names[i + 1] = presets.get(i).name;
        new AlertDialog.Builder(this).setTitle("Saved presets · Pro").setItems(names, (d, which) -> {
            if (!requirePro() || session.busy) return;
            if (which == 0) {
                EditText name = new EditText(this); name.setHint("e.g. Small A4 with page numbers"); name.setContentDescription("Preset name"); name.setSingleLine(true);
                AlertDialog save = new AlertDialog.Builder(this).setTitle("Save export preset")
                        .setMessage("Saves page size, style, compression and Pro options. Never saves your password. An existing name will be replaced.")
                        .setView(name).setNegativeButton("Cancel", null).setPositiveButton("Save", null).create();
                save.setOnShowListener(ignored -> save.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    if (!requirePro() || session.busy) return;
                    if (name.getText().toString().trim().isEmpty()) { name.setError("Enter a preset name."); return; }
                    try {
                        if (!store.save(new PresetStore.Preset(name.getText().toString(), selectedPaper(), selectedEnhancement(), selectedCompression(), session.options))) {
                            name.setError("Couldn’t save. Try again."); return;
                        }
                        Toast.makeText(this, "Preset saved. Password not included.", Toast.LENGTH_SHORT).show(); save.dismiss();
                    } catch (IllegalArgumentException invalid) { name.setError(invalid.getMessage()); }
                })); save.show();
            } else {
                PresetStore.Preset preset = presets.get(which - 1);
                new AlertDialog.Builder(this).setTitle(preset.name).setMessage("Use these export settings? Protected exports will ask for a fresh password.")
                        .setNegativeButton("Cancel", null).setNeutralButton("Delete preset", (dialog, w) -> { if (!store.remove(preset.name)) Toast.makeText(this, "Couldn’t delete preset.", Toast.LENGTH_LONG).show(); })
                        .setPositiveButton("Use preset", (dialog, w) -> {
                            if (!requirePro() || session.busy) return;
                            paperSizeGroup.check(paperSizeGroup.getChildAt(preset.paper).getId());
                            enhancementGroup.check(enhancementGroup.getChildAt(preset.enhancement).getId());
                            compressionGroup.check(compressionGroup.getChildAt(preset.compression).getId());
                            session.options = preset.options; session.changed();
                            Toast.makeText(this, preset.options.protect ? "Preset applied. Enter a password before exporting." : "Preset applied.", Toast.LENGTH_LONG).show();
                        }).show();
            }
        }).show();
    }
    private void sharePdf() {
        File pdf = session.latestPdf; if (pdf == null || !pdf.exists()) return;
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", pdf, session.latestTitle + ".pdf");
        Intent send = new Intent(Intent.ACTION_SEND).setType("application/pdf");
        send.putExtra(Intent.EXTRA_STREAM, uri); send.setClipData(ClipData.newRawUri("PaperBuild PDF", uri));
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try { startActivity(Intent.createChooser(send, "Share PDF")); }
        catch (ActivityNotFoundException error) { Toast.makeText(this, "No sharing app is available.", Toast.LENGTH_LONG).show(); }
    }
    private void openPdf() {
        File pdf = session.latestPdf; if (pdf == null || !pdf.exists()) return;
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", pdf, session.latestTitle + ".pdf");
        Intent view = new Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/pdf").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try { startActivity(Intent.createChooser(view, "Open PDF")); }
        catch (ActivityNotFoundException error) { Toast.makeText(this, "No PDF viewer is available.", Toast.LENGTH_LONG).show(); }
    }
    private void savePdf() {
        if (session.busy || session.latestPdf == null || !session.latestPdf.exists()) return;
        if (Build.VERSION.SDK_INT <= 28 && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_DOCUMENT); return;
        }
        File source = session.latestPdf;
        String filename = DocumentProcessor.safeName(session.latestTitle) + ".pdf";
        ContextSave save = new ContextSave(getApplicationContext().getContentResolver());
        session.run("Saving to Downloads…", progress -> {
            save.save(source, filename);
            return new DocumentSession.Result(null, "", "Saved to Downloads/PaperBuild.");
        });
    }
    /** Contains no Activity reference, so a save continues safely during rotation. */
    static final class ContextSave {
        final ContentResolver resolver;
        ContextSave(ContentResolver resolver) { this.resolver = resolver; }
        void save(File source) throws IOException {
            save(source, source.getName());
        }
        void save(File source, String displayName) throws IOException {
            if (Build.VERSION.SDK_INT < 29) {
                File folder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "PaperBuild");
                if (!folder.isDirectory() && !folder.mkdirs()) throw new IOException("Could not create the Downloads folder.");
                String name = displayName; File destination = new File(folder, name);
                int suffix = 1;
                while (destination.exists()) destination = new File(folder, name.replace(".pdf", "-" + suffix++ + ".pdf"));
                try (InputStream input = new FileInputStream(source); OutputStream output = new FileOutputStream(destination)) { copy(input, output); }
                catch (IOException error) { destination.delete(); throw error; }
                return;
            }
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, displayName);
            values.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
            values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/PaperBuild");
            values.put(MediaStore.Downloads.IS_PENDING, 1);
            Uri destination = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (destination == null) throw new IOException("Could not create the download.");
            boolean complete = false;
            try {
                try (InputStream input = new FileInputStream(source); OutputStream output = resolver.openOutputStream(destination)) {
                    if (output == null) throw new IOException("Could not write the download.");
                    copy(input, output);
                }
                ContentValues done = new ContentValues(); done.put(MediaStore.Downloads.IS_PENDING, 0);
                if (resolver.update(destination, done, null, null) != 1) throw new IOException("Could not finish saving the PDF.");
                complete = true;
            } finally { if (!complete) resolver.delete(destination, null, null); }
        }
        private static void copy(InputStream input, OutputStream output) throws IOException {
            byte[] buffer = new byte[16384]; int read;
            while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
        }
    }
    @Override public void onRequestPermissionsResult(int request, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(request, permissions, results);
        if (request == WRITE_DOCUMENT) {
            if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) savePdf();
            else Toast.makeText(this, "Storage permission is needed to save on this Android version.", Toast.LENGTH_LONG).show();
        }
    }
    private static String displayName(ContentResolver resolver, Uri uri) {
        try (Cursor cursor = resolver.query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) return cursor.getString(0);
        } catch (RuntimeException ignored) { }
        return "Selected image";
    }
    private static String readableSize(long bytes) {
        return bytes < 1024 * 1024 ? Math.max(1, bytes / 1024) + " KB" : String.format(Locale.US, "%.1f MB", bytes / (1024f * 1024f));
    }
    private int selectedPaperWidth() { return selectedPaper() == 1 ? LETTER_WIDTH : A4_WIDTH; }
    private int selectedPaperHeight() { return selectedPaper() == 1 ? LETTER_HEIGHT : A4_HEIGHT; }
    private int selectedPaper() { return selected(paperSizeGroup); }
    private int selectedEnhancement() { return selected(enhancementGroup); }
    private int selectedCompression() { return isPro() ? selected(compressionGroup) : 0; }
    private int selected(RadioGroup group) {
        View view = group.findViewById(group.getCheckedRadioButtonId()); return view == null ? 0 : (Integer) view.getTag();
    }
    private TextView sectionTitle(String text) {
        TextView view = label(text, 18, Color.rgb(30, 41, 53)); view.setTypeface(android.graphics.Typeface.DEFAULT_BOLD); return view;
    }
    private TextView label(String text, float size, int color) { return Ui.text(this, text, size, color, false); }
    private Button button(String text, boolean primary) { return Ui.button(this, text, primary); }
    private RadioButton radio(String text, int value, boolean checked) {
        RadioButton button = new RadioButton(this); button.setId(View.generateViewId()); button.setTag(value);
        button.setText(text); button.setTextSize(13); button.setTextColor(getColorStateList(R.color.choice_text));
        button.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)); button.setButtonDrawable((android.graphics.drawable.Drawable) null);
        button.setGravity(Gravity.CENTER); button.setMinHeight(dp(48)); button.setPadding(dp(8), dp(12), dp(8), dp(12));
        button.setBackgroundResource(R.drawable.choice_background); button.setChecked(checked);
        RadioGroup.LayoutParams params = new RadioGroup.LayoutParams(narrowLayout() ? -1 : 0, -2, narrowLayout() ? 0 : 1);
        if (narrowLayout()) params.bottomMargin = dp(6); else params.setMarginEnd(dp(4));
        button.setLayoutParams(params); return button;
    }
    private LinearLayout.LayoutParams matchWrap() { return new LinearLayout.LayoutParams(-1, -2); }
    private LinearLayout.LayoutParams weightWrap(float weight) { return new LinearLayout.LayoutParams(0, -2, weight); }
    private void addSpacer(LinearLayout root, int height) { root.addView(new View(this), new LinearLayout.LayoutParams(1, dp(height))); }
    private void addHorizontalSpacer(LinearLayout root, int width) { root.addView(new View(this), new LinearLayout.LayoutParams(dp(width), 1)); }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
