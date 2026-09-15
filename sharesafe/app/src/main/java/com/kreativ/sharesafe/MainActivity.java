package com.kreativ.sharesafe;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * A fully offline photo cleaner. Each source image is decoded and rendered to a new JPEG,
 * which intentionally removes EXIF metadata, including location data.
 */
public class MainActivity extends Activity {
    private static final int PICK_PHOTOS = 44;
    private static final int WRITE_GALLERY_PERMISSION = 45;
    private final ArrayList<Uri> selectedPhotos = new ArrayList<>();
    private final ExecutorService worker = Executors.newSingleThreadExecutor();

    private TextView selectionText;
    private TextView statusText;
    private TextView resultSummary;
    private Button cleanButton;
    private Button shareButton;
    private Button saveButton;
    private ProgressBar progressBar;
    private RadioGroup qualityGroup;
    private RadioGroup shareFormatGroup;
    private ExportResult latestExport;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildScreen());
    }

    private View buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(247, 248, 243));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(30), dp(24), dp(30));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -1));

        TextView badge = label("PRIVATE PHOTO UTILITY", 12, Color.rgb(52, 116, 99));
        badge.setLetterSpacing(.12f);
        root.addView(badge);

        TextView title = label("Share photos\nwithout sharing location.", 31, Color.rgb(25, 44, 41));
        title.setPadding(0, dp(12), 0, dp(10));
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        root.addView(title);

        TextView intro = label("ShareSafe creates clean JPEG copies on your phone. Your original photos stay untouched and no file is uploaded.", 16, Color.rgb(96, 113, 109));
        intro.setLineSpacing(dp(3), 1f);
        root.addView(intro);

        addSpacer(root, 26);
        Button pickButton = button("Choose photos", true);
        pickButton.setOnClickListener(v -> choosePhotos());
        root.addView(pickButton, matchWrap());

        selectionText = label("No photos selected", 15, Color.rgb(96, 113, 109));
        selectionText.setGravity(Gravity.CENTER);
        selectionText.setPadding(0, dp(12), 0, 0);
        root.addView(selectionText, matchWrap());

        addSpacer(root, 28);
        TextView qualityTitle = label("Export quality", 17, Color.rgb(25, 44, 41));
        qualityTitle.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        root.addView(qualityTitle);
        TextView qualityHint = label("All exports are JPEGs with camera metadata removed.", 14, Color.rgb(96, 113, 109));
        qualityHint.setPadding(0, dp(4), 0, dp(6));
        root.addView(qualityHint);

        qualityGroup = new RadioGroup(this);
        qualityGroup.setOrientation(LinearLayout.VERTICAL);
        qualityGroup.addView(radio("Balanced — smaller files", 76, true));
        qualityGroup.addView(radio("High — sharper photos", 88, false));
        qualityGroup.addView(radio("Maximum — larger files", 95, false));
        root.addView(qualityGroup);

        addSpacer(root, 18);
        TextView formatTitle = label("Share format", 17, Color.rgb(25, 44, 41));
        formatTitle.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        root.addView(formatTitle);
        TextView formatHint = label("Individual photos are easiest to share in chat apps. Use ZIP for email or archives.", 14, Color.rgb(96, 113, 109));
        formatHint.setPadding(0, dp(4), 0, dp(6));
        root.addView(formatHint);
        shareFormatGroup = new RadioGroup(this);
        shareFormatGroup.setOrientation(LinearLayout.VERTICAL);
        shareFormatGroup.addView(radio("Individual photos — recommended", 0, true));
        shareFormatGroup.addView(radio("One ZIP file", 1, false));
        root.addView(shareFormatGroup);

        addSpacer(root, 22);
        cleanButton = button("Clean photos", true);
        cleanButton.setEnabled(false);
        cleanButton.setOnClickListener(v -> cleanPhotos());
        root.addView(cleanButton, matchWrap());

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setVisibility(View.GONE);
        progressBar.setMax(100);
        root.addView(progressBar, matchWrap());

        statusText = label("", 14, Color.rgb(96, 113, 109));
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(0, dp(12), 0, dp(4));
        root.addView(statusText, matchWrap());

        resultSummary = label("", 14, Color.rgb(32, 82, 70));
        resultSummary.setLineSpacing(dp(3), 1f);
        resultSummary.setPadding(dp(16), dp(14), dp(16), dp(14));
        resultSummary.setBackgroundResource(R.drawable.result_card);
        resultSummary.setVisibility(View.GONE);
        root.addView(resultSummary, matchWrap());

        addSpacer(root, 10);
        shareButton = button("Share cleaned photos", false);
        shareButton.setEnabled(false);
        shareButton.setVisibility(View.GONE);
        shareButton.setOnClickListener(v -> shareLatestExport());
        root.addView(shareButton, matchWrap());

        addSpacer(root, 10);
        saveButton = button("Save to gallery", false);
        saveButton.setEnabled(false);
        saveButton.setVisibility(View.GONE);
        saveButton.setOnClickListener(v -> saveToGallery());
        root.addView(saveButton, matchWrap());

        addSpacer(root, 24);
        TextView footer = label("How it works  •  ShareSafe redraws every selected photo into a new JPEG file. That process removes embedded camera details such as GPS coordinates, camera model, and timestamps.", 13, Color.rgb(96, 113, 109));
        footer.setLineSpacing(dp(2), 1f);
        root.addView(footer);
        return scroll;
    }

    private void choosePhotos() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(Intent.createChooser(intent, "Choose photos to clean"), PICK_PHOTOS);
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_PHOTOS || resultCode != RESULT_OK || data == null) return;

        Set<Uri> unique = new LinkedHashSet<>();
        if (data.getData() != null) unique.add(data.getData());
        ClipData clips = data.getClipData();
        if (clips != null) {
            for (int i = 0; i < clips.getItemCount(); i++) unique.add(clips.getItemAt(i).getUri());
        }
        selectedPhotos.clear();
        selectedPhotos.addAll(unique);
        latestExport = null;
        shareButton.setText("Share cleaned photos");
        shareButton.setEnabled(false);
        shareButton.setVisibility(View.GONE);
        saveButton.setEnabled(false);
        saveButton.setVisibility(View.GONE);
        resultSummary.setVisibility(View.GONE);
        cleanButton.setEnabled(!selectedPhotos.isEmpty());
        selectionText.setText(selectedPhotos.isEmpty() ? "No photos selected" : selectedPhotos.size() + " photo" + (selectedPhotos.size() == 1 ? "" : "s") + " selected");
        statusText.setText(selectedPhotos.isEmpty() ? "" : "Ready to create clean copies.");
    }

    private void cleanPhotos() {
        if (selectedPhotos.isEmpty()) return;
        final int quality = chosenQuality();
        final boolean shareAsZip = isZipSelected();
        cleanButton.setEnabled(false);
        shareButton.setEnabled(false);
        shareButton.setVisibility(View.GONE);
        saveButton.setEnabled(false);
        saveButton.setVisibility(View.GONE);
        resultSummary.setVisibility(View.GONE);
        progressBar.setProgress(0);
        progressBar.setVisibility(View.VISIBLE);
        statusText.setText("Preparing clean copies…");

        worker.execute(() -> {
            try {
                ExportResult export = exportCleanPhotos(quality, shareAsZip);
                runOnUiThread(() -> {
                    latestExport = export;
                    progressBar.setVisibility(View.GONE);
                    int count = export.photos.size();
                    String noun = count == 1 ? "photo is" : "photos are";
                    statusText.setText(count + " clean " + noun + (shareAsZip ? " ready in a ZIP." : " ready to share."));
                    resultSummary.setText("Cleaned " + count + (count == 1 ? " photo" : " photos")
                            + "\nRemoved: location, camera details, and timestamps"
                            + "\nExport: " + (shareAsZip ? "one ZIP file" : "individual JPEG photos"));
                    resultSummary.setVisibility(View.VISIBLE);
                    shareButton.setText(shareAsZip ? "Share ZIP" : "Share cleaned photos");
                    shareButton.setEnabled(true);
                    shareButton.setVisibility(View.VISIBLE);
                    saveButton.setEnabled(true);
                    saveButton.setVisibility(View.VISIBLE);
                    cleanButton.setEnabled(true);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    statusText.setText("Couldn’t clean these photos. Please try different image files.");
                    cleanButton.setEnabled(true);
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private ExportResult exportCleanPhotos(int quality, boolean shareAsZip) throws IOException {
        File exportDir = new File(getCacheDir(), "exports");
        if (!exportDir.exists() && !exportDir.mkdirs()) throw new IOException("Could not create export folder");
        String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date());
        File cleanedDir = new File(exportDir, "ShareSafe-" + stamp);
        if (!cleanedDir.mkdirs()) throw new IOException("Could not create photo folder");
        ArrayList<File> cleanedPhotos = new ArrayList<>();

        for (int i = 0; i < selectedPhotos.size(); i++) {
            Uri uri = selectedPhotos.get(i);
            Bitmap bitmap = loadRotatedBitmap(uri);
            if (bitmap == null) continue;
            File output = new File(cleanedDir, String.format(Locale.US, "%02d_%s", i + 1, cleanName(uri, i + 1)));
            try (FileOutputStream stream = new FileOutputStream(output)) {
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)) throw new IOException("Could not write cleaned photo");
            }
            bitmap.recycle();
            cleanedPhotos.add(output);
            final int progress = Math.round(((i + 1) * 100f) / selectedPhotos.size());
            runOnUiThread(() -> progressBar.setProgress(progress));
        }
        if (cleanedPhotos.isEmpty()) throw new IOException("No compatible images were selected");
        File zipFile = shareAsZip ? zipPhotos(exportDir, stamp, cleanedPhotos) : null;
        return new ExportResult(cleanedPhotos, zipFile);
    }

    private File zipPhotos(File exportDir, String stamp, ArrayList<File> photos) throws IOException {
        File zipFile = new File(exportDir, "ShareSafe-" + stamp + ".zip");
        byte[] buffer = new byte[8192];
        try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(zipFile))) {
            for (File photo : photos) {
                zip.putNextEntry(new ZipEntry(photo.getName()));
                try (FileInputStream input = new FileInputStream(photo)) {
                    int bytesRead;
                    while ((bytesRead = input.read(buffer)) != -1) zip.write(buffer, 0, bytesRead);
                }
                zip.closeEntry();
            }
        }
        return zipFile;
    }

    private Bitmap loadRotatedBitmap(Uri uri) throws IOException {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream input = getContentResolver().openInputStream(uri)) {
            BitmapFactory.decodeStream(input, null, bounds);
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null;
        int sample = 1;
        int maxSide = Math.max(bounds.outWidth, bounds.outHeight);
        while (maxSide / sample > 4096) sample *= 2;

        BitmapFactory.Options decode = new BitmapFactory.Options();
        decode.inSampleSize = sample;
        decode.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap;
        try (InputStream input = getContentResolver().openInputStream(uri)) {
            bitmap = BitmapFactory.decodeStream(input, null, decode);
        }
        if (bitmap == null) return null;
        int rotation = rotationFor(uri);
        if (rotation == 0) return bitmap;
        Matrix matrix = new Matrix();
        matrix.postRotate(rotation);
        Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        if (rotated != bitmap) bitmap.recycle();
        return rotated;
    }

    private int rotationFor(Uri uri) {
        try (InputStream input = new BufferedInputStream(getContentResolver().openInputStream(uri))) {
            ExifInterface exif = new ExifInterface(input);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            if (orientation == ExifInterface.ORIENTATION_ROTATE_90) return 90;
            if (orientation == ExifInterface.ORIENTATION_ROTATE_180) return 180;
            if (orientation == ExifInterface.ORIENTATION_ROTATE_270) return 270;
        } catch (Exception ignored) {
            // Files without EXIF stay in their decoded orientation.
        }
        return 0;
    }

    private String cleanName(Uri uri, int index) {
        String sourceName = "photo_" + index;
        try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (column >= 0) sourceName = cursor.getString(column);
            }
        } catch (Exception ignored) { }
        sourceName = sourceName.replaceAll("\\.[^.]+$", "").replaceAll("[^a-zA-Z0-9._-]", "_");
        return "clean_" + sourceName + ".jpg";
    }

    private int chosenQuality() {
        int id = qualityGroup.getCheckedRadioButtonId();
        Object tag = qualityGroup.findViewById(id).getTag();
        return (Integer) tag;
    }

    private boolean isZipSelected() {
        int id = shareFormatGroup.getCheckedRadioButtonId();
        return (Integer) shareFormatGroup.findViewById(id).getTag() == 1;
    }

    private void shareLatestExport() {
        if (latestExport == null) return;
        if (latestExport.zipFile != null) shareZip(latestExport.zipFile);
        else sharePhotos(latestExport.photos);
    }

    private void shareZip(File zipFile) {
        if (!zipFile.exists()) return;
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", zipFile);
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("application/zip");
        send.putExtra(Intent.EXTRA_STREAM, uri);
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(send, "Share cleaned photo ZIP"));
    }

    private void sharePhotos(ArrayList<File> photos) {
        ArrayList<Uri> uris = new ArrayList<>();
        for (File photo : photos) {
            if (photo.exists()) uris.add(FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photo));
        }
        if (uris.isEmpty()) return;
        Intent send = new Intent(uris.size() == 1 ? Intent.ACTION_SEND : Intent.ACTION_SEND_MULTIPLE);
        send.setType("image/jpeg");
        if (uris.size() == 1) send.putExtra(Intent.EXTRA_STREAM, uris.get(0));
        else send.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        ClipData clipData = ClipData.newUri(getContentResolver(), "Cleaned photos", uris.get(0));
        for (int i = 1; i < uris.size(); i++) clipData.addItem(new ClipData.Item(uris.get(i)));
        send.setClipData(clipData);
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(send, "Share cleaned photos"));
    }

    private void saveToGallery() {
        if (latestExport == null || latestExport.photos.isEmpty()) return;
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P
                && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_GALLERY_PERMISSION);
            return;
        }

        ArrayList<File> photosToSave = new ArrayList<>(latestExport.photos);
        saveButton.setEnabled(false);
        statusText.setText("Saving clean photos to gallery…");
        worker.execute(() -> {
            int saved = 0;
            try {
                for (File photo : photosToSave) {
                    savePhotoToGallery(photo);
                    saved++;
                }
                final int savedCount = saved;
                runOnUiThread(() -> {
                    statusText.setText("Saved " + savedCount + (savedCount == 1 ? " clean photo" : " clean photos") + " to Pictures/ShareSafe.");
                    saveButton.setEnabled(true);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    statusText.setText("Couldn’t save every photo to the gallery.");
                    saveButton.setEnabled(true);
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void savePhotoToGallery(File photo) throws IOException {
        ContentResolver resolver = getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, photo.getName());
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ShareSafe");
            values.put(MediaStore.Images.Media.IS_PENDING, 1);
        }
        Uri galleryUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (galleryUri == null) throw new IOException("Could not create gallery image");
        boolean complete = false;
        try (InputStream input = new FileInputStream(photo); OutputStream output = resolver.openOutputStream(galleryUri)) {
            if (output == null) throw new IOException("Could not open gallery image");
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) output.write(buffer, 0, bytesRead);
            complete = true;
        } finally {
            if (complete && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues completeValues = new ContentValues();
                completeValues.put(MediaStore.Images.Media.IS_PENDING, 0);
                resolver.update(galleryUri, completeValues, null, null);
            } else if (!complete) {
                resolver.delete(galleryUri, null, null);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == WRITE_GALLERY_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) saveToGallery();
            else Toast.makeText(this, "Gallery permission is needed to save on this Android version.", Toast.LENGTH_LONG).show();
        }
    }

    private static class ExportResult {
        final ArrayList<File> photos;
        final File zipFile;

        ExportResult(ArrayList<File> photos, File zipFile) {
            this.photos = photos;
            this.zipFile = zipFile;
        }
    }

    private RadioButton radio(String text, int quality, boolean checked) {
        RadioButton radio = new RadioButton(this);
        radio.setId(View.generateViewId());
        radio.setText(text);
        radio.setTextSize(15);
        radio.setTextColor(Color.rgb(25, 44, 41));
        radio.setTag(quality);
        radio.setChecked(checked);
        radio.setPadding(0, dp(5), 0, dp(5));
        return radio;
    }

    private Button button(String text, boolean primary) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(16);
        button.setAllCaps(false);
        button.setMinHeight(dp(54));
        button.setTextColor(primary ? Color.WHITE : Color.rgb(25, 44, 41));
        button.setBackgroundResource(primary ? R.drawable.button_primary : R.drawable.button_secondary);
        return button;
    }

    private TextView label(String text, float size, int color) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);
        return view;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(-1, -2);
    }

    private void addSpacer(LinearLayout root, int heightDp) {
        View spacer = new View(this);
        root.addView(spacer, new LinearLayout.LayoutParams(1, dp(heightDp)));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        worker.shutdownNow();
        super.onDestroy();
    }
}
