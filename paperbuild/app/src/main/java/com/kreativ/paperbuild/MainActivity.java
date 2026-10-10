package com.kreativ.paperbuild;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.pdf.PdfDocument;
import android.graphics.pdf.PdfRenderer;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A small, offline document maker. It deliberately uses the system picker and the user's chosen
 * camera app, so it never asks for broad photo-library or camera access.
 */
public class MainActivity extends Activity {
    private static final int PICK_IMAGES = 101;
    private static final int CAPTURE_PAGE = 102;
    private static final int WRITE_DOCUMENT = 103;
    private static final int PICK_PDFS_TO_MERGE = 104;
    private static final int PICK_PDF_TO_SPLIT = 105;
    private static final int A4_WIDTH = 595;
    private static final int A4_HEIGHT = 842;
    private static final int LETTER_WIDTH = 612;
    private static final int LETTER_HEIGHT = 792;
    private static final int ENHANCE_COLOR = 0;
    private static final int ENHANCE_GRAYSCALE = 1;
    private static final int ENHANCE_CONTRAST = 2;
    private static final int COMPRESSION_STANDARD = 0;
    private static final int COMPRESSION_SMALL = 1;
    private static final int FREE_PAGE_LIMIT = 10;

    private final ArrayList<PageItem> pages = new ArrayList<>();
    private final ExecutorService worker = Executors.newSingleThreadExecutor();

    private LinearLayout pageList;
    private TextView pageSummary;
    private TextView statusText;
    private TextView resultSummary;
    private EditText documentName;
    private Button makePdfButton;
    private Button shareButton;
    private Button saveButton;
    private Button openButton;
    private ProgressBar progressBar;
    private RadioGroup paperSizeGroup;
    private RadioGroup enhancementGroup;
    private RadioGroup compressionGroup;
    private File pendingCapture;
    private File latestPdf;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildScreen());
    }

    @Override
    protected void onDestroy() {
        worker.shutdown();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ProBilling.refresh(this, () -> { });
    }

    private View buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(247, 248, 252));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(30), dp(24), dp(132));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -1));

        TextView badge = label("PRIVATE PDF MAKER", 12, Color.rgb(49, 94, 170));
        badge.setLetterSpacing(.12f);
        root.addView(badge);

        TextView title = label("Turn pages into\na clean PDF.", 28, Color.rgb(30, 41, 53));
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setPadding(0, dp(12), 0, dp(10));
        root.addView(title);

        TextView intro = label("Scan or add images, arrange them, then export. Everything stays on your device.", 16, Color.rgb(91, 104, 120));
        intro.setLineSpacing(dp(3), 1f);
        root.addView(intro);

        ImageView documentIllustration = new ImageView(this);
        documentIllustration.setImageResource(R.drawable.ic_document_build);
        documentIllustration.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        documentIllustration.setBackgroundResource(R.drawable.hero_panel);
        documentIllustration.setContentDescription("Illustration of a document being prepared");
        documentIllustration.setPadding(dp(16), dp(8), dp(16), dp(8));
        LinearLayout.LayoutParams illustrationParams = matchWrap();
        illustrationParams.topMargin = dp(18);
        illustrationParams.height = dp(92);
        root.addView(documentIllustration, illustrationParams);

        addSpacer(root, 22);
        TextView nameLabel = sectionTitle("PDF name");
        root.addView(nameLabel);
        documentName = new EditText(this);
        documentName.setText("My document");
        documentName.setSingleLine(true);
        documentName.setTextSize(16);
        documentName.setTextColor(Color.rgb(30, 41, 53));
        documentName.setSelectAllOnFocus(false);
        root.addView(documentName, matchWrap());

        addSpacer(root, 18);
        LinearLayout choiceRow = new LinearLayout(this);
        choiceRow.setOrientation(LinearLayout.HORIZONTAL);
        Button scanButton = button("Scan a page", true);
        scanButton.setOnClickListener(v -> capturePage());
        choiceRow.addView(scanButton, weightWrap(1));
        addHorizontalSpacer(choiceRow, 10);
        Button addButton = button("Add images", false);
        addButton.setTextColor(Color.rgb(49, 94, 170));
        addButton.setBackgroundResource(R.drawable.button_import);
        addButton.setOnClickListener(v -> chooseImages());
        choiceRow.addView(addButton, weightWrap(1));
        root.addView(choiceRow, matchWrap());

        pageSummary = label("No pages yet", 15, Color.rgb(91, 104, 120));
        pageSummary.setPadding(0, dp(12), 0, dp(8));
        root.addView(pageSummary);

        pageList = new LinearLayout(this);
        pageList.setOrientation(LinearLayout.VERTICAL);
        root.addView(pageList, matchWrap());

        TextView orderHint = label("Reorder or rotate pages.", 13, Color.rgb(91, 104, 120));
        orderHint.setLineSpacing(dp(2), 1f);
        orderHint.setPadding(0, dp(8), 0, 0);
        root.addView(orderHint);

        addSpacer(root, 22);
        root.addView(sectionTitle("Page size"));
        TextView sizeHint = label("Choose your paper size.", 14, Color.rgb(91, 104, 120));
        sizeHint.setPadding(0, dp(4), 0, dp(5));
        root.addView(sizeHint);
        paperSizeGroup = new RadioGroup(this);
        paperSizeGroup.setOrientation(LinearLayout.VERTICAL);
        paperSizeGroup.addView(radio("A4 - international standard", 0, true));
        paperSizeGroup.addView(radio("US Letter", 1, false));
        root.addView(paperSizeGroup, matchWrap());

        addSpacer(root, 14);
        root.addView(sectionTitle("Document treatment"));
        TextView treatmentHint = label("Applied to every page.", 14, Color.rgb(91, 104, 120));
        treatmentHint.setPadding(0, dp(4), 0, dp(5));
        root.addView(treatmentHint);
        enhancementGroup = new RadioGroup(this);
        enhancementGroup.setOrientation(LinearLayout.VERTICAL);
        enhancementGroup.addView(radio("Keep original colours", ENHANCE_COLOR, true));
        enhancementGroup.addView(radio("Grayscale - smaller, easier to read", ENHANCE_GRAYSCALE, false));
        enhancementGroup.addView(radio("High contrast - best for clear text", ENHANCE_CONTRAST, false));
        root.addView(enhancementGroup, matchWrap());

        addSpacer(root, 14);
        root.addView(sectionTitle("PDF size"));
        TextView compressionHint = label("Compact export is included with Pro.", 14, Color.rgb(91, 104, 120));
        compressionHint.setPadding(0, dp(4), 0, dp(5));
        root.addView(compressionHint);
        compressionGroup = new RadioGroup(this);
        compressionGroup.setOrientation(LinearLayout.VERTICAL);
        compressionGroup.addView(radio("Standard quality", COMPRESSION_STANDARD, true));
        RadioButton smallPdf = radio("Smaller PDF - Pro", COMPRESSION_SMALL, false);
        smallPdf.setOnClickListener(v -> {
            if (!isPro()) {
                compressionGroup.check(((RadioButton) compressionGroup.getChildAt(0)).getId());
                showPro();
            }
        });
        compressionGroup.addView(smallPdf);
        root.addView(compressionGroup, matchWrap());

        addSpacer(root, 20);
        makePdfButton = button("Build PDF", true);
        makePdfButton.setEnabled(false);
        makePdfButton.setOnClickListener(v -> createPdf());
        root.addView(makePdfButton, matchWrap());

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setVisibility(View.GONE);
        root.addView(progressBar, matchWrap());

        statusText = label("", 14, Color.rgb(91, 104, 120));
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(0, dp(12), 0, dp(4));
        root.addView(statusText, matchWrap());

        resultSummary = label("", 14, Color.rgb(33, 73, 136));
        resultSummary.setLineSpacing(dp(3), 1f);
        resultSummary.setPadding(dp(16), dp(14), dp(16), dp(14));
        resultSummary.setBackgroundResource(R.drawable.result_card);
        resultSummary.setVisibility(View.GONE);
        root.addView(resultSummary, matchWrap());

        addSpacer(root, 10);
        shareButton = button("Share PDF", false);
        shareButton.setEnabled(false);
        shareButton.setVisibility(View.GONE);
        shareButton.setOnClickListener(v -> sharePdf());
        root.addView(shareButton, matchWrap());

        addSpacer(root, 10);
        saveButton = button("Save to Downloads", false);
        saveButton.setEnabled(false);
        saveButton.setVisibility(View.GONE);
        saveButton.setOnClickListener(v -> savePdf());
        root.addView(saveButton, matchWrap());

        addSpacer(root, 10);
        openButton = button("Open PDF", false);
        openButton.setEnabled(false);
        openButton.setVisibility(View.GONE);
        openButton.setOnClickListener(v -> openPdf());
        root.addView(openButton, matchWrap());

        addSpacer(root, 26);
        root.addView(sectionTitle("PDF tools"));
        TextView toolsHint = label("Merge and split are included with Pro.", 14, Color.rgb(91, 104, 120));
        toolsHint.setPadding(0, dp(4), 0, dp(10));
        root.addView(toolsHint);
        Button mergeButton = button("Merge PDFs - Pro", false);
        mergeButton.setOnClickListener(v -> {
            if (requirePro()) choosePdfsForMerge();
        });
        root.addView(mergeButton, matchWrap());
        addSpacer(root, 10);
        Button splitButton = button("Split a PDF - Pro", false);
        splitButton.setOnClickListener(v -> {
            if (requirePro()) choosePdfForSplit();
        });
        root.addView(splitButton, matchWrap());

        addSpacer(root, 28);
        root.addView(proCard());
        addSpacer(root, 14);
        Button aboutButton = button("Privacy & About", false);
        aboutButton.setOnClickListener(v -> startActivity(new Intent(this, AboutActivity.class)));
        root.addView(aboutButton, matchWrap());
        return scroll;
    }

    private View proCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(16), dp(18), dp(16));
        card.setBackgroundResource(R.drawable.card);
        TextView title = sectionTitle("PaperBuild Pro");
        card.addView(title);
        TextView copy = label("Smaller PDFs, unlimited pages, merge and split. One-time €2.99 unlock.", 14, Color.rgb(91, 104, 120));
        copy.setLineSpacing(dp(2), 1f);
        copy.setPadding(0, dp(5), 0, dp(10));
        card.addView(copy);
        Button details = button(isPro() ? "Manage Pro" : "Unlock Pro - €2.99", false);
        details.setOnClickListener(v -> showPro());
        card.addView(details, matchWrap());
        return card;
    }

    private boolean isPro() {
        return ProBilling.isPro(this);
    }

    private boolean requirePro() {
        if (isPro()) return true;
        showPro();
        return false;
    }

    private void showPro() {
        startActivity(new Intent(this, ProActivity.class));
    }

    private void chooseImages() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(Intent.createChooser(intent, "Add pages"), PICK_IMAGES);
    }

    private void choosePdfsForMerge() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(Intent.createChooser(intent, "Choose PDFs to merge"), PICK_PDFS_TO_MERGE);
    }

    private void choosePdfForSplit() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(Intent.createChooser(intent, "Choose a PDF to split"), PICK_PDF_TO_SPLIT);
    }

    private ArrayList<Uri> collectSelectedUris(Intent data) {
        Set<Uri> selected = new LinkedHashSet<>();
        if (data.getData() != null) selected.add(data.getData());
        ClipData clips = data.getClipData();
        if (clips != null) {
            for (int i = 0; i < clips.getItemCount(); i++) selected.add(clips.getItemAt(i).getUri());
        }
        for (Uri uri : selected) {
            try {
                getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (SecurityException ignored) {
                // Temporary access from some document providers is still enough for this action.
            }
        }
        return new ArrayList<>(selected);
    }

    private void askSplitRange(Uri uri) {
        EditText range = new EditText(this);
        range.setHint("1-3");
        range.setInputType(InputType.TYPE_CLASS_TEXT);
        new AlertDialog.Builder(this)
                .setTitle("Split PDF")
                .setMessage("Enter the page range to keep, for example 1-3.")
                .setView(range)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Create PDF", (dialog, which) -> splitPdf(uri, range.getText().toString()))
                .show();
    }

    private void mergePdfs(ArrayList<Uri> sources) {
        startPdfTool("Merging PDFs…");
        worker.execute(() -> {
            try {
                File output = writeRenderedPdf(sources, 1, Integer.MAX_VALUE, "Merged PDF", COMPRESSION_STANDARD);
                runOnUiThread(() -> showPdfToolResult(output, "Merged PDF"));
            } catch (Exception e) {
                runOnUiThread(() -> showPdfToolError(e));
            }
        });
    }

    private void splitPdf(Uri source, String rawRange) {
        int[] range = parseRange(rawRange);
        if (range == null) {
            Toast.makeText(this, "Use a page number or range such as 1-3.", Toast.LENGTH_LONG).show();
            return;
        }
        startPdfTool("Creating selected pages…");
        worker.execute(() -> {
            try {
                File output = writeRenderedPdf(new ArrayList<>(Collections.singletonList(source)), range[0], range[1], "Extracted pages", COMPRESSION_STANDARD);
                runOnUiThread(() -> showPdfToolResult(output, "Extracted pages"));
            } catch (Exception e) {
                runOnUiThread(() -> showPdfToolError(e));
            }
        });
    }

    private int[] parseRange(String raw) {
        if (raw == null) return null;
        String normalized = raw.trim();
        if (!normalized.matches("\\d+(?:\\s*-\\s*\\d+)?")) return null;
        String[] parts = normalized.split("-");
        try {
            int first = Integer.parseInt(parts[0].trim());
            int last = parts.length == 1 ? first : Integer.parseInt(parts[1].trim());
            return first > 0 && last >= first ? new int[]{first, last} : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private void startPdfTool(String message) {
        makePdfButton.setEnabled(false);
        progressBar.setIndeterminate(true);
        progressBar.setVisibility(View.VISIBLE);
        hideResult();
        statusText.setText(message);
    }

    private void showPdfToolResult(File pdf, String action) {
        latestPdf = pdf;
        progressBar.setIndeterminate(false);
        progressBar.setVisibility(View.GONE);
        makePdfButton.setEnabled(!pages.isEmpty());
        statusText.setText(action + " is ready.");
        resultSummary.setText(pdf.getName() + "\n" + readableSize(pdf.length()) + "\nCreated locally on this device");
        resultSummary.setVisibility(View.VISIBLE);
        shareButton.setEnabled(true);
        shareButton.setVisibility(View.VISIBLE);
        saveButton.setEnabled(true);
        saveButton.setVisibility(View.VISIBLE);
        openButton.setEnabled(true);
        openButton.setVisibility(View.VISIBLE);
    }

    private void showPdfToolError(Exception error) {
        progressBar.setIndeterminate(false);
        progressBar.setVisibility(View.GONE);
        makePdfButton.setEnabled(!pages.isEmpty());
        statusText.setText("Couldn’t create that PDF.");
        Toast.makeText(this, error.getMessage() == null ? "PDF tool failed" : error.getMessage(), Toast.LENGTH_LONG).show();
    }

    private void capturePage() {
        File folder = new File(getCacheDir(), "paperbuild");
        if (!folder.exists() && !folder.mkdirs()) {
            Toast.makeText(this, "Couldn’t prepare the camera page.", Toast.LENGTH_LONG).show();
            return;
        }
        pendingCapture = new File(folder, "scan-" + System.currentTimeMillis() + ".jpg");
        Uri output = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", pendingCapture);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, output);
        intent.setClipData(ClipData.newRawUri("New scan", output));
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (intent.resolveActivity(getPackageManager()) == null) {
            Toast.makeText(this, "No camera app is available.", Toast.LENGTH_LONG).show();
            return;
        }
        startActivityForResult(intent, CAPTURE_PAGE);
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAPTURE_PAGE) {
            if (resultCode == RESULT_OK && pendingCapture != null && pendingCapture.exists() && pendingCapture.length() > 0) {
                addPage(FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", pendingCapture), "Scanned page");
            }
            pendingCapture = null;
            return;
        }
        if (requestCode == PICK_PDFS_TO_MERGE && resultCode == RESULT_OK && data != null) {
            ArrayList<Uri> selected = collectSelectedUris(data);
            if (selected.size() < 2) {
                Toast.makeText(this, "Choose at least two PDFs to merge.", Toast.LENGTH_LONG).show();
            } else {
                mergePdfs(selected);
            }
            return;
        }
        if (requestCode == PICK_PDF_TO_SPLIT && resultCode == RESULT_OK && data != null && data.getData() != null) {
            askSplitRange(data.getData());
            return;
        }
        if (requestCode != PICK_IMAGES || resultCode != RESULT_OK || data == null) return;

        Set<Uri> selected = new LinkedHashSet<>(collectSelectedUris(data));
        for (Uri uri : selected) {
            if (!addPage(uri, displayName(uri))) break;
        }
    }

    private boolean addPage(Uri uri, String name) {
        if (!isPro() && pages.size() >= FREE_PAGE_LIMIT) {
            Toast.makeText(this, "Free documents can contain up to " + FREE_PAGE_LIMIT + " pages.", Toast.LENGTH_LONG).show();
            showPro();
            return false;
        }
        pages.add(new PageItem(uri, name));
        latestPdf = null;
        hideResult();
        refreshPages();
        return true;
    }

    private void refreshPages() {
        pageList.removeAllViews();
        int count = pages.size();
        pageSummary.setText(count == 0 ? "No pages yet" : count + (count == 1 ? " page ready" : " pages ready"));
        makePdfButton.setEnabled(count > 0);
        for (int i = 0; i < count; i++) {
            final int position = i;
            PageItem page = pages.get(i);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(dp(14), dp(12), dp(14), dp(10));
            row.setBackgroundResource(R.drawable.card);
            LinearLayout.LayoutParams rowParams = matchWrap();
            rowParams.bottomMargin = dp(8);
            pageList.addView(row, rowParams);

            LinearLayout top = new LinearLayout(this);
            top.setGravity(Gravity.CENTER_VERTICAL);
            ImageView preview = new ImageView(this);
            preview.setScaleType(ImageView.ScaleType.CENTER_CROP);
            preview.setBackgroundResource(R.drawable.page_preview);
            preview.setContentDescription("Preview of page " + (i + 1));
            Bitmap thumbnail = loadThumbnail(page);
            if (thumbnail != null) preview.setImageBitmap(thumbnail);
            LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(dp(58), dp(76));
            previewParams.rightMargin = dp(12);
            top.addView(preview, previewParams);
            TextView name = label("Page " + (i + 1) + "  •  " + page.name, 15, Color.rgb(30, 41, 53));
            name.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            name.setMaxLines(2);
            name.setEllipsize(TextUtils.TruncateAt.END);
            top.addView(name, weightWrap(1));
            row.addView(top, matchWrap());

            LinearLayout controls = new LinearLayout(this);
            controls.setGravity(Gravity.CENTER_VERTICAL);
            controls.setPadding(0, dp(7), 0, 0);
            Button up = tinyButton("↑");
            up.setEnabled(position > 0);
            up.setOnClickListener(v -> movePage(position, position - 1));
            controls.addView(up, new LinearLayout.LayoutParams(dp(46), dp(38)));
            addHorizontalSpacer(controls, 6);
            Button down = tinyButton("↓");
            down.setEnabled(position < pages.size() - 1);
            down.setOnClickListener(v -> movePage(position, position + 1));
            controls.addView(down, new LinearLayout.LayoutParams(dp(46), dp(38)));
            addHorizontalSpacer(controls, 6);
            Button rotate = tinyButton("↻ Rotate");
            rotate.setOnClickListener(v -> {
                pages.get(position).rotation = (pages.get(position).rotation + 90) % 360;
                latestPdf = null;
                hideResult();
                refreshPages();
            });
            controls.addView(rotate, new LinearLayout.LayoutParams(-2, dp(38)));
            addHorizontalSpacer(controls, 6);
            Button remove = tinyButton("Remove");
            remove.setTextColor(Color.rgb(181, 61, 61));
            remove.setBackgroundResource(R.drawable.button_compact_danger);
            remove.setOnClickListener(v -> {
                pages.remove(position);
                latestPdf = null;
                hideResult();
                refreshPages();
            });
            controls.addView(remove, new LinearLayout.LayoutParams(-2, dp(38)));
            row.addView(controls, matchWrap());
        }
    }

    private void movePage(int from, int to) {
        if (to < 0 || to >= pages.size()) return;
        PageItem page = pages.remove(from);
        pages.add(to, page);
        latestPdf = null;
        hideResult();
        refreshPages();
    }

    private void createPdf() {
        if (pages.isEmpty()) return;
        ArrayList<PageItem> exportPages = new ArrayList<>(pages);
        String title = safeDocumentName(documentName.getText().toString());
        int outputWidth = selectedPaperWidth();
        int outputHeight = selectedPaperHeight();
        int enhancement = selectedEnhancement();
        int compression = selectedCompression();
        makePdfButton.setEnabled(false);
        progressBar.setProgress(0);
        progressBar.setVisibility(View.VISIBLE);
        statusText.setText("Building your PDF…");
        hideResult();
        worker.execute(() -> {
            try {
                File pdf = writePdf(exportPages, title, outputWidth, outputHeight, enhancement, compression);
                runOnUiThread(() -> {
                    latestPdf = pdf;
                    progressBar.setVisibility(View.GONE);
                    makePdfButton.setEnabled(true);
                    statusText.setText("Your PDF is ready.");
                    resultSummary.setText(pdf.getName() + "\n" + exportPages.size() + (exportPages.size() == 1 ? " page" : " pages") + "  •  " + readableSize(pdf.length()) + "\n" + paperSizeName() + "  •  " + enhancementName(enhancement) + "  •  " + compressionName(compression) + "\nCreated locally on this device");
                    resultSummary.setVisibility(View.VISIBLE);
                    shareButton.setEnabled(true);
                    shareButton.setVisibility(View.VISIBLE);
                    saveButton.setEnabled(true);
                    saveButton.setVisibility(View.VISIBLE);
                    openButton.setEnabled(true);
                    openButton.setVisibility(View.VISIBLE);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    makePdfButton.setEnabled(!pages.isEmpty());
                    statusText.setText("Couldn’t create this PDF. Try different image files.");
                    Toast.makeText(this, e.getMessage() == null ? "PDF creation failed" : e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private File writePdf(ArrayList<PageItem> exportPages, String documentTitle, int outputWidth, int outputHeight, int enhancement, int compression) throws IOException {
        File folder = new File(getCacheDir(), "paperbuild");
        if (!folder.exists() && !folder.mkdirs()) throw new IOException("Could not prepare the PDF folder");
        File output = new File(folder, documentTitle + "-" + new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date()) + ".pdf");
        PdfDocument pdf = new PdfDocument();
        boolean complete = false;
        try {
            for (int i = 0; i < exportPages.size(); i++) {
                PageItem item = exportPages.get(i);
                Bitmap bitmap = loadBitmap(item, enhancement, compressionMaxSide(compression));
                if (bitmap == null) throw new IOException("One selected page could not be read");
                PdfDocument.Page page = pdf.startPage(new PdfDocument.PageInfo.Builder(outputWidth, outputHeight, i + 1).create());
                drawBitmapOnPage(page.getCanvas(), bitmap, outputWidth, outputHeight);
                pdf.finishPage(page);
                bitmap.recycle();
                final int progress = Math.round(((i + 1) * 100f) / exportPages.size());
                runOnUiThread(() -> progressBar.setProgress(progress));
            }
            try (FileOutputStream stream = new FileOutputStream(output)) {
                pdf.writeTo(stream);
            }
            complete = true;
            return output;
        } finally {
            pdf.close();
            if (!complete && output.exists()) output.delete();
        }
    }

    private File writeRenderedPdf(ArrayList<Uri> sources, int firstPage, int lastPage, String documentTitle, int compression) throws IOException {
        File folder = new File(getCacheDir(), "paperbuild");
        if (!folder.exists() && !folder.mkdirs()) throw new IOException("Could not prepare the PDF folder");
        File output = new File(folder, documentTitle + "-" + new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date()) + ".pdf");
        PdfDocument pdf = new PdfDocument();
        int pageNumber = 1;
        boolean complete = false;
        try {
            for (Uri source : sources) {
                ParcelFileDescriptor descriptor = getContentResolver().openFileDescriptor(source, "r");
                if (descriptor == null) throw new IOException("Could not open a selected PDF");
                PdfRenderer renderer = new PdfRenderer(descriptor);
                try {
                    int start = Math.max(1, firstPage);
                    int end = Math.min(lastPage, renderer.getPageCount());
                    if (start > end) throw new IOException("That page range is not in this PDF");
                    for (int index = start - 1; index < end; index++) {
                        Bitmap bitmap = renderPdfPage(renderer, index, compressionMaxSide(compression));
                        PdfDocument.Page page = pdf.startPage(new PdfDocument.PageInfo.Builder(selectedPaperWidth(), selectedPaperHeight(), pageNumber++).create());
                        drawBitmapOnPage(page.getCanvas(), bitmap, selectedPaperWidth(), selectedPaperHeight());
                        pdf.finishPage(page);
                        bitmap.recycle();
                    }
                } finally {
                    renderer.close();
                    descriptor.close();
                }
            }
            if (pageNumber == 1) throw new IOException("No pages were available in the selected PDF");
            try (FileOutputStream stream = new FileOutputStream(output)) {
                pdf.writeTo(stream);
            }
            complete = true;
            return output;
        } finally {
            pdf.close();
            if (!complete && output.exists()) output.delete();
        }
    }

    private Bitmap renderPdfPage(PdfRenderer renderer, int index, int maximumSide) {
        PdfRenderer.Page source = renderer.openPage(index);
        try {
            float scale = Math.min(1f, maximumSide / (float) Math.max(source.getWidth(), source.getHeight()));
            int width = Math.max(1, Math.round(source.getWidth() * scale));
            int height = Math.max(1, Math.round(source.getHeight() * scale));
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            source.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            return bitmap;
        } finally {
            source.close();
        }
    }

    private void drawBitmapOnPage(Canvas canvas, Bitmap bitmap, int pageWidth, int pageHeight) {
        canvas.drawColor(Color.WHITE);
        float margin = 30f;
        float availableWidth = pageWidth - margin * 2;
        float availableHeight = pageHeight - margin * 2;
        float scale = Math.min(availableWidth / bitmap.getWidth(), availableHeight / bitmap.getHeight());
        float width = bitmap.getWidth() * scale;
        float height = bitmap.getHeight() * scale;
        float left = (pageWidth - width) / 2f;
        float top = (pageHeight - height) / 2f;
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
        canvas.drawBitmap(bitmap, null, new RectF(left, top, left + width, top + height), paint);
    }

    private Bitmap loadBitmap(PageItem item, int enhancement, int maximumExportSide) throws IOException {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream input = getContentResolver().openInputStream(item.uri)) {
            if (input == null) return null;
            BitmapFactory.decodeStream(input, null, bounds);
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null;
        int sample = 1;
        int maximumSide = Math.max(bounds.outWidth, bounds.outHeight);
        while (maximumSide / sample > maximumExportSide) sample *= 2;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sample;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap;
        try (InputStream input = getContentResolver().openInputStream(item.uri)) {
            if (input == null) return null;
            bitmap = BitmapFactory.decodeStream(input, null, options);
        }
        if (bitmap == null) return null;
        int rotation = (exifRotation(item.uri) + item.rotation) % 360;
        if (rotation != 0) {
            Matrix matrix = new Matrix();
            matrix.postRotate(rotation);
            Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            if (rotated != bitmap) bitmap.recycle();
            bitmap = rotated;
        }
        return applyEnhancement(bitmap, enhancement);
    }

    private Bitmap loadThumbnail(PageItem item) {
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (InputStream input = getContentResolver().openInputStream(item.uri)) {
                if (input == null) return null;
                BitmapFactory.decodeStream(input, null, bounds);
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null;
            int sample = 1;
            int maximumSide = Math.max(bounds.outWidth, bounds.outHeight);
            while (maximumSide / sample > 240) sample *= 2;
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = sample;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap bitmap;
            try (InputStream input = getContentResolver().openInputStream(item.uri)) {
                if (input == null) return null;
                bitmap = BitmapFactory.decodeStream(input, null, options);
            }
            if (bitmap == null) return null;
            int rotation = (exifRotation(item.uri) + item.rotation) % 360;
            if (rotation == 0) return bitmap;
            Matrix matrix = new Matrix();
            matrix.postRotate(rotation);
            Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            if (rotated != bitmap) bitmap.recycle();
            return rotated;
        } catch (Exception ignored) {
            return null;
        }
    }

    private Bitmap applyEnhancement(Bitmap bitmap, int enhancement) {
        if (enhancement == ENHANCE_COLOR) return bitmap;
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        for (int i = 0; i < pixels.length; i++) {
            int pixel = pixels[i];
            int red = (pixel >> 16) & 0xFF;
            int green = (pixel >> 8) & 0xFF;
            int blue = pixel & 0xFF;
            int luma = (red * 299 + green * 587 + blue * 114) / 1000;
            if (enhancement == ENHANCE_CONTRAST) luma = luma < 160 ? 0 : 255;
            pixels[i] = Color.rgb(luma, luma, luma);
        }
        Bitmap adjusted = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        adjusted.setPixels(pixels, 0, width, 0, 0, width, height);
        bitmap.recycle();
        return adjusted;
    }

    private int exifRotation(Uri uri) {
        try (InputStream input = getContentResolver().openInputStream(uri)) {
            if (input == null) return 0;
            ExifInterface exif = new ExifInterface(input);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            if (orientation == ExifInterface.ORIENTATION_ROTATE_90) return 90;
            if (orientation == ExifInterface.ORIENTATION_ROTATE_180) return 180;
            if (orientation == ExifInterface.ORIENTATION_ROTATE_270) return 270;
        } catch (Exception ignored) {
            // Files without readable EXIF need no correction.
        }
        return 0;
    }

    private void sharePdf() {
        if (latestPdf == null || !latestPdf.exists()) return;
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", latestPdf);
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("application/pdf");
        send.putExtra(Intent.EXTRA_STREAM, uri);
        send.setClipData(ClipData.newRawUri("PaperBuild PDF", uri));
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(send, "Share PDF"));
    }

    private void openPdf() {
        if (latestPdf == null || !latestPdf.exists()) return;
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", latestPdf);
        Intent view = new Intent(Intent.ACTION_VIEW);
        view.setDataAndType(uri, "application/pdf");
        view.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(Intent.createChooser(view, "Open PDF"));
        } catch (Exception e) {
            Toast.makeText(this, "No app on this device can open PDFs.", Toast.LENGTH_LONG).show();
        }
    }

    private void savePdf() {
        if (latestPdf == null || !latestPdf.exists()) return;
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_DOCUMENT);
            return;
        }
        saveButton.setEnabled(false);
        statusText.setText("Saving to Downloads…");
        File fileToSave = latestPdf;
        worker.execute(() -> {
            try {
                saveToDownloads(fileToSave);
                runOnUiThread(() -> {
                    statusText.setText("Saved to Downloads/PaperBuild.");
                    saveButton.setEnabled(true);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    statusText.setText("Couldn’t save the PDF.");
                    saveButton.setEnabled(true);
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    @SuppressWarnings("deprecation")
    private void saveToDownloads(File source) throws IOException {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            File folder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "PaperBuild");
            if (!folder.exists() && !folder.mkdirs()) throw new IOException("Could not create the downloads folder");
            copyFile(source, new File(folder, source.getName()));
            return;
        }
        ContentResolver resolver = getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.Downloads.DISPLAY_NAME, source.getName());
        values.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
        values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/PaperBuild");
        values.put(MediaStore.Downloads.IS_PENDING, 1);
        Uri destination = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        if (destination == null) throw new IOException("Could not create the download");
        boolean complete = false;
        try (InputStream input = new FileInputStream(source); OutputStream output = resolver.openOutputStream(destination)) {
            if (output == null) throw new IOException("Could not write the download");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
            complete = true;
        } finally {
            if (complete) {
                ContentValues done = new ContentValues();
                done.put(MediaStore.Downloads.IS_PENDING, 0);
                resolver.update(destination, done, null, null);
            } else if (!complete) {
                resolver.delete(destination, null, null);
            }
        }
    }

    private void copyFile(File source, File destination) throws IOException {
        try (InputStream input = new FileInputStream(source); OutputStream output = new FileOutputStream(destination)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == WRITE_DOCUMENT) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) savePdf();
            else Toast.makeText(this, "Storage permission is needed to save on this Android version.", Toast.LENGTH_LONG).show();
        }
    }

    private void hideResult() {
        resultSummary.setVisibility(View.GONE);
        shareButton.setEnabled(false);
        shareButton.setVisibility(View.GONE);
        saveButton.setEnabled(false);
        saveButton.setVisibility(View.GONE);
        openButton.setEnabled(false);
        openButton.setVisibility(View.GONE);
        statusText.setText("");
    }

    private String displayName(Uri uri) {
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (column >= 0) return cursor.getString(column);
            }
        } catch (Exception ignored) { }
        return "Selected image";
    }

    private String safeDocumentName(String raw) {
        String name = raw == null ? "" : raw.trim().replaceAll("[^a-zA-Z0-9._ -]", "").replaceAll("\\s+", " ");
        if (name.isEmpty()) name = "My document";
        if (name.length() > 64) name = name.substring(0, 64).trim();
        return name;
    }

    private String readableSize(long bytes) {
        if (bytes < 1024 * 1024) return Math.max(1, bytes / 1024) + " KB";
        return String.format(Locale.US, "%.1f MB", bytes / (1024f * 1024f));
    }

    private int selectedPaperWidth() {
        return selectedPaper() == 1 ? LETTER_WIDTH : A4_WIDTH;
    }

    private int selectedPaperHeight() {
        return selectedPaper() == 1 ? LETTER_HEIGHT : A4_HEIGHT;
    }

    private int selectedPaper() {
        int id = paperSizeGroup.getCheckedRadioButtonId();
        View selected = paperSizeGroup.findViewById(id);
        return selected == null ? 0 : (Integer) selected.getTag();
    }

    private String paperSizeName() {
        return selectedPaper() == 1 ? "US Letter" : "A4";
    }

    private int selectedEnhancement() {
        int id = enhancementGroup.getCheckedRadioButtonId();
        View selected = enhancementGroup.findViewById(id);
        return selected == null ? ENHANCE_COLOR : (Integer) selected.getTag();
    }

    private int selectedCompression() {
        int id = compressionGroup.getCheckedRadioButtonId();
        View selected = compressionGroup.findViewById(id);
        int value = selected == null ? COMPRESSION_STANDARD : (Integer) selected.getTag();
        return value == COMPRESSION_SMALL && !isPro() ? COMPRESSION_STANDARD : value;
    }

    private int compressionMaxSide(int compression) {
        return compression == COMPRESSION_SMALL ? 1440 : 2560;
    }

    private String compressionName(int compression) {
        return compression == COMPRESSION_SMALL ? "Smaller PDF" : "Standard";
    }

    private String enhancementName(int enhancement) {
        if (enhancement == ENHANCE_GRAYSCALE) return "Grayscale";
        if (enhancement == ENHANCE_CONTRAST) return "High contrast";
        return "Original colour";
    }

    private TextView sectionTitle(String text) {
        TextView title = label(text, 18, Color.rgb(30, 41, 53));
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        return title;
    }

    private TextView label(String text, float size, int color) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);
        return view;
    }

    private Button button(String text, boolean primary) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(16);
        button.setAllCaps(false);
        button.setMinHeight(dp(54));
        button.setTextColor(primary ? Color.WHITE : Color.rgb(30, 41, 53));
        button.setBackgroundResource(primary ? R.drawable.button_primary : R.drawable.button_secondary);
        return button;
    }

    private Button tinyButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(13);
        button.setAllCaps(false);
        button.setTextColor(Color.rgb(49, 94, 170));
        button.setPadding(dp(7), 0, dp(7), 0);
        button.setBackgroundResource(R.drawable.button_compact);
        return button;
    }

    private RadioButton radio(String text, int value, boolean checked) {
        RadioButton radio = new RadioButton(this);
        radio.setId(View.generateViewId());
        radio.setText(text);
        radio.setTextSize(15);
        radio.setTextColor(Color.rgb(30, 41, 53));
        radio.setTag(value);
        radio.setChecked(checked);
        radio.setPadding(0, dp(5), 0, dp(5));
        return radio;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams weightWrap(float weight) {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight);
    }

    private void addSpacer(LinearLayout layout, int heightDp) {
        View spacer = new View(this);
        layout.addView(spacer, new LinearLayout.LayoutParams(1, dp(heightDp)));
    }

    private void addHorizontalSpacer(LinearLayout layout, int widthDp) {
        View spacer = new View(this);
        layout.addView(spacer, new LinearLayout.LayoutParams(dp(widthDp), 1));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static class PageItem {
        final Uri uri;
        final String name;
        int rotation;

        PageItem(Uri uri, String name) {
            this.uri = uri;
            this.name = name;
        }
    }
}
