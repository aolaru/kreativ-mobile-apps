package com.kreativ.paperbuild;

import android.app.Activity;
import android.app.AlertDialog;
import android.text.InputFilter;
import android.text.InputType;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.ImageView;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.util.function.Consumer;

/** Shared by the image builder and existing-PDF editor. Password fields never save view state. */
final class ExportOptionsDialog {
    static void show(Activity activity, ExportOptions current, Consumer<ExportOptions> apply,
                     Consumer<ExportOptions> chooseLogo) {
        ScrollView scroll = new ScrollView(activity); LinearLayout fields = Ui.column(activity);
        int padding = Ui.dp(activity, 20); fields.setPadding(padding, Ui.dp(activity, 8), padding, Ui.dp(activity, 12)); scroll.addView(fields);
        fields.addView(Ui.text(activity, "For every PDF you export.", 13, Ui.MUTED, false)); Ui.gap(fields, 12);
        EditText watermark = new EditText(activity); watermark.setSingleLine(true); watermark.setText(current.watermark);
        watermark.setHint("Watermark text · optional"); watermark.setContentDescription("Watermark text");
        watermark.setFilters(new InputFilter[]{new InputFilter.LengthFilter(80)}); fields.addView(watermark);
        Ui.gap(fields, 8);
        CheckBox logo = new CheckBox(activity); logo.setText("Include logo · top right");
        logo.setChecked(!current.logo.isEmpty()); logo.setEnabled(!current.logo.isEmpty()); fields.addView(logo);
        ImageView preview = new ImageView(activity); preview.setContentDescription("Selected watermark logo");
        preview.setScaleType(ImageView.ScaleType.FIT_CENTER); preview.setVisibility(current.logo.isEmpty() ? View.GONE : View.VISIBLE);
        if (!current.logo.isEmpty()) fields.addView(preview, new LinearLayout.LayoutParams(-1, Ui.dp(activity, 64)));
        java.util.concurrent.atomic.AtomicBoolean closed = new java.util.concurrent.atomic.AtomicBoolean();
        Bitmap[] thumbnail = new Bitmap[1];
        Button pickLogo = Ui.button(activity, current.logo.isEmpty() ? "Choose logo" : "Change logo", false);
        fields.addView(pickLogo);
        fields.addView(Ui.text(activity, "PNG keeps transparency. Logos are saved privately for reuse in presets.", 12, Ui.MUTED, false)); Ui.gap(fields, 8);
        CheckBox numbers = new CheckBox(activity); numbers.setText("Add page numbers · 1 / total"); numbers.setChecked(current.pageNumbers); fields.addView(numbers);
        CheckBox protect = new CheckBox(activity); protect.setText("Require a password to open"); protect.setChecked(current.protect); fields.addView(protect);
        EditText password = passwordField(activity, "Password · 8–64 characters"), confirm = passwordField(activity, "Confirm password");
        fields.addView(password); fields.addView(confirm); password.setText(current.password); confirm.setText(current.password);
        password.setEnabled(protect.isChecked()); confirm.setEnabled(protect.isChecked());
        protect.setOnCheckedChangeListener((v, checked) -> { password.setEnabled(checked); confirm.setEnabled(checked); });
        Ui.gap(fields, 8); fields.addView(Ui.text(activity, "Passwords aren’t saved. Enter one for each export.", 12, Ui.MUTED, false));
        AlertDialog dialog = new AlertDialog.Builder(activity).setTitle("Pro export options").setView(scroll)
                .setNegativeButton("Cancel", null).setPositiveButton("Apply", null).create();
        pickLogo.setOnClickListener(v -> {
            try {
                ExportOptions staged = new ExportOptions(watermark.getText().toString(), numbers.isChecked(), protect.isChecked(),
                        password.getText().toString().equals(confirm.getText().toString()) ? password.getText().toString() : "", logo.isChecked() ? current.logo : "");
                password.setText(""); confirm.setText(""); dialog.dismiss(); chooseLogo.accept(staged);
            } catch (IllegalArgumentException invalid) { watermark.setError(invalid.getMessage()); }
        });
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (protect.isChecked() && !password.getText().toString().equals(confirm.getText().toString())) { confirm.setError("Passwords don’t match."); return; }
            try {
                ExportOptions options = new ExportOptions(watermark.getText().toString(), numbers.isChecked(), protect.isChecked(), password.getText().toString(), logo.isChecked() ? current.logo : "");
                options.validate(); apply.accept(options); password.setText(""); confirm.setText(""); dialog.dismiss();
            } catch (IllegalArgumentException invalid) { password.setError(invalid.getMessage()); }
        }));
        dialog.setOnDismissListener(d -> {
            password.setText(""); confirm.setText(""); closed.set(true); preview.setImageDrawable(null);
            if (thumbnail[0] != null) { thumbnail[0].recycle(); thumbnail[0] = null; }
        });
        dialog.show(); dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        if (!current.logo.isEmpty()) {
            LogoStore logos = new LogoStore(activity);
            DocumentSession.get(activity).worker.execute(() -> {
                Bitmap bitmap = null;
                try { BitmapFactory.Options sample = new BitmapFactory.Options(); sample.inSampleSize = 4;
                    bitmap = BitmapFactory.decodeFile(logos.file(current.logo).getPath(), sample);
                } catch (java.io.IOException | RuntimeException ignored) { }
                Bitmap ready = bitmap;
                activity.runOnUiThread(() -> {
                    if (closed.get() || activity.isDestroyed()) { if (ready != null) ready.recycle(); return; }
                    thumbnail[0] = ready; preview.setImageBitmap(ready);
                });
            });
        }
    }
    private static EditText passwordField(Activity activity, String hint) {
        EditText field = new EditText(activity); field.setHint(hint); field.setContentDescription(hint); field.setSingleLine(true);
        field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        field.setFilters(new InputFilter[]{new InputFilter.LengthFilter(64)});
        field.setSaveEnabled(false); field.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO); return field;
    }
}
