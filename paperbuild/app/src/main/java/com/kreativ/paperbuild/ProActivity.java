package com.kreativ.paperbuild;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Purchase and restore screen for the permanent PaperBuild Pro upgrade. */
public class ProActivity extends Activity {
    private ProBilling billing;
    private TextView intro;
    private final Runnable billingObserver = this::renderBilling;
    private TextView state;
    private Button purchaseButton;
    private Button restoreButton;
    private Button closeButton;
    private Button reviewButton;
    private boolean restoring;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        billing = ProBilling.get(this);
        setContentView(buildScreen());
    }

    @Override protected void onStart() { super.onStart(); billing.observe(billingObserver); }
    @Override protected void onStop() { billing.removeObserver(billingObserver); super.onStop(); }
    @Override protected void onResume() { super.onResume(); billing.refresh(null); }

    private View buildScreen() {
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(Ui.SURFACE);
        LinearLayout root = Ui.column(this); Ui.applyInsets(scroll, root);
        scroll.addView(root, new ScrollView.LayoutParams(-1, -1));
        Button back = Ui.button(this, "← Back to PaperBuild", false); back.setOnClickListener(v -> finish()); root.addView(back, matchWrap());
        Ui.gap(root, 20);
        root.addView(Ui.hero(this, "PAPERBUILD PRO", "More pages.\nMore freedom.", "One purchase. Yours to keep."));
        Ui.gap(root, 22);
        root.addView(Ui.text(this, "Everything in Free, plus", 17, Ui.INK, true)); Ui.gap(root, 14);
        root.addView(Ui.feature(this, R.drawable.ic_compact, "Smaller PDFs", "Less file size. Easier to send.")); Ui.gap(root, 10);
        root.addView(Ui.feature(this, R.drawable.ic_stack, "Unlimited pages", "Go beyond the free 10-page limit.")); Ui.gap(root, 10);
        root.addView(Ui.feature(this, R.drawable.ic_merge, "Merge & extract", "Combine PDFs or keep selected pages.")); Ui.gap(root, 10);
        root.addView(Ui.feature(this, R.drawable.ic_shield, "Password protection", "Choose who can open your exported PDFs.")); Ui.gap(root, 10);
        root.addView(Ui.feature(this, R.drawable.ic_document_build, "Make it yours", "Custom watermarks and page numbers.")); Ui.gap(root, 10);
        root.addView(Ui.feature(this, R.drawable.ic_stack, "Saved presets", "Your favourite export settings, one tap away.")); Ui.gap(root, 20);

        LinearLayout purchase = Ui.card(this, R.drawable.pro_panel);
        purchase.addView(Ui.text(this, "ONE-TIME UNLOCK", 11, Ui.ACCENT, true)); Ui.gap(purchase, 10);
        intro = Ui.text(this, "PaperBuild Pro", 26, Ui.INK, true); purchase.addView(intro); Ui.gap(purchase, 6);
        purchase.addView(Ui.text(this, "No subscription. No recurring fees.", 14, Ui.MUTED, false)); Ui.gap(purchase, 16);
        purchaseButton = Ui.button(this, "Unlock Pro", true); purchaseButton.setEnabled(false);
        purchaseButton.setOnClickListener(v -> billing.purchase(this)); purchase.addView(purchaseButton, matchWrap()); Ui.gap(purchase, 12);
        state = Ui.text(this, "Checking Google Play…", 13, Ui.MUTED, false); state.setGravity(Gravity.CENTER); purchase.addView(state, matchWrap());
        root.addView(purchase); Ui.gap(root, 12);
        restoreButton = Ui.button(this, "Restore purchase", false);
        restoreButton.setOnClickListener(v -> { restoring = true; billing.restore(); }); root.addView(restoreButton, matchWrap());
        Ui.gap(root, 10); closeButton = Ui.button(this, ProBilling.isPro(this) ? "Back to my documents" : "Continue with Free", false);
        closeButton.setOnClickListener(v -> finish()); root.addView(closeButton, matchWrap());
        Ui.gap(root, 10); reviewButton = Ui.button(this, "Review access", false);
        reviewButton.setOnClickListener(v -> showReviewAccess()); root.addView(reviewButton, matchWrap());
        Ui.gap(root, 18); TextView note = Ui.text(this, "Your documents stay on your device.", 12, Ui.MUTED, false);
        note.setGravity(Gravity.CENTER); root.addView(note); return scroll;
    }

    private void renderBilling() {
        boolean review = ReviewAccess.active(this);
        state.setText(review ? "Review access active. No purchase required." : !restoring && billing.status.startsWith("No Pro purchase found")
                ? "Secure purchase through Google Play." : billing.status);
        boolean owned = ProBilling.isPro(this);
        String price = billing.price();
        purchaseButton.setText(owned ? "Pro unlocked" : price == null ? "Unlock Pro" : "Unlock Pro · " + price);
        purchaseButton.setEnabled(billing.canPurchase());
        intro.setText(review ? "Pro review access" : owned ? "Pro is yours." : price == null ? "PaperBuild Pro" : price);
        closeButton.setText(owned ? "Back to my documents" : "Continue with Free");
        reviewButton.setText(review ? "Leave review access" : "Review access");
    }

    private void showReviewAccess() {
        if (ReviewAccess.active(this)) {
            new AlertDialog.Builder(this).setTitle("Leave review access?")
                    .setMessage("Your Google Play purchase, if any, stays unchanged.")
                    .setNegativeButton("Cancel", null).setPositiveButton("Leave", (dialog, which) -> {
                        ReviewAccess.clear(this); renderBilling();
                    }).show();
            return;
        }
        EditText input = new EditText(this);
        input.setHint("Signed review code"); input.setContentDescription("Signed review code");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        input.setFilters(new InputFilter[] { new InputFilter.LengthFilter(256) });
        input.setPadding(dp(20), dp(12), dp(20), dp(12));
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Review access")
                .setMessage("For authorised reviewers. This unlocks Pro on this device without a purchase.")
                .setView(input).setNegativeButton("Cancel", null).setPositiveButton("Activate", null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String code = input.getText().toString();
            if (!ReviewCode.valid(code)) { input.setError("Invalid review code."); return; }
            if (!ReviewAccess.activate(this, code)) { input.setError("Couldn’t save access. Try again."); return; }
            restoring = false; renderBilling(); dialog.dismiss();
        }));
        dialog.show();
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(-1, -2);
    }

    private void addSpacer(LinearLayout layout, int heightDp) {
        layout.addView(new View(this), new LinearLayout.LayoutParams(1, dp(heightDp)));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
