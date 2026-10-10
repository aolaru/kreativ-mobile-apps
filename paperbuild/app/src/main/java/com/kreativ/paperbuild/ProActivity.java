package com.kreativ.paperbuild;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Purchase and restore screen for the permanent PaperBuild Pro upgrade. */
public class ProActivity extends Activity {
    private BillingClient billingClient;
    private ProductDetails productDetails;
    private TextView state;
    private Button purchaseButton;
    private Button restoreButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildScreen());
        connectBilling();
    }

    @Override
    protected void onDestroy() {
        if (billingClient != null) billingClient.endConnection();
        super.onDestroy();
    }

    private View buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(247, 248, 252));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(30), dp(24), dp(132));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -1));

        TextView badge = label("PAPERBUILD PRO", 12, Color.rgb(49, 94, 170));
        badge.setLetterSpacing(.12f);
        root.addView(badge);
        TextView title = label("More control.\nOne price.", 30, Color.rgb(30, 41, 53));
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setPadding(0, dp(12), 0, dp(10));
        root.addView(title);
        TextView intro = label("A permanent €2.99 unlock. No subscription.", 16, Color.rgb(91, 104, 120));
        intro.setPadding(0, 0, 0, dp(22));
        root.addView(intro);

        addFeature(root, "Smaller PDFs", "Choose a compact export for easier sharing.");
        addFeature(root, "Unlimited pages", "Build documents beyond the free 10-page limit.");
        addFeature(root, "Merge & split", "Combine PDFs or extract the pages you need.");

        state = label("Checking your purchase…", 14, Color.rgb(91, 104, 120));
        state.setGravity(Gravity.CENTER);
        state.setPadding(0, dp(16), 0, dp(8));
        root.addView(state, matchWrap());
        purchaseButton = button("Unlock Pro - €2.99", true);
        purchaseButton.setEnabled(false);
        purchaseButton.setOnClickListener(v -> launchPurchase());
        root.addView(purchaseButton, matchWrap());
        addSpacer(root, 10);
        restoreButton = button("Restore purchase", false);
        restoreButton.setOnClickListener(v -> queryPurchases());
        root.addView(restoreButton, matchWrap());
        addSpacer(root, 10);
        Button close = button("Not now", false);
        close.setOnClickListener(v -> finish());
        root.addView(close, matchWrap());
        return scroll;
    }

    private void addFeature(LinearLayout root, String title, String body) {
        TextView heading = label(title, 18, Color.rgb(30, 41, 53));
        heading.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        root.addView(heading);
        TextView copy = label(body, 15, Color.rgb(91, 104, 120));
        copy.setPadding(0, dp(4), 0, dp(18));
        root.addView(copy);
    }

    private void connectBilling() {
        PurchasesUpdatedListener listener = (result, purchases) -> {
            if (result.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
                processPurchases(purchases);
            } else if (result.getResponseCode() != BillingClient.BillingResponseCode.USER_CANCELED) {
                state.setText("Purchase could not be completed. Please try again.");
            }
        };
        billingClient = BillingClient.newBuilder(this)
                .setListener(listener)
                .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
                .enableAutoServiceReconnection()
                .build();
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult result) {
                if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    queryProduct();
                    queryPurchases();
                } else {
                    state.setText("Google Play is unavailable. Try again later.");
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                state.setText("Google Play disconnected. Reopen Pro to try again.");
            }
        });
    }

    private void queryProduct() {
        QueryProductDetailsParams.Product product = QueryProductDetailsParams.Product.newBuilder()
                .setProductId(ProBilling.PRODUCT_ID)
                .setProductType(BillingClient.ProductType.INAPP)
                .build();
        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(Collections.singletonList(product))
                .build();
        billingClient.queryProductDetailsAsync(params, (result, response) -> {
            if (result.getResponseCode() == BillingClient.BillingResponseCode.OK && !response.getProductDetailsList().isEmpty()) {
                productDetails = response.getProductDetailsList().get(0);
                if (!ProBilling.isPro(this)) {
                    state.setText("One-time purchase. Yours permanently.");
                    purchaseButton.setEnabled(true);
                }
            } else if (!ProBilling.isPro(this)) {
                state.setText("Pro will be available after this version is published to Google Play.");
            }
        });
    }

    private void queryPurchases() {
        if (billingClient == null || !billingClient.isReady()) return;
        QueryPurchasesParams params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build();
        billingClient.queryPurchasesAsync(params, (result, purchases) -> {
            if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) processPurchases(purchases);
        });
    }

    private void processPurchases(List<Purchase> purchases) {
        boolean ownsPro = false;
        for (Purchase purchase : purchases) {
            if (ProBilling.isProPurchase(purchase)) {
                ownsPro = true;
                ProBilling.acknowledge(billingClient, purchase);
            }
        }
        ProBilling.setPro(this, ownsPro);
        if (ownsPro) {
            state.setText("PaperBuild Pro is unlocked.");
            purchaseButton.setText("Pro unlocked");
            purchaseButton.setEnabled(false);
            Toast.makeText(this, "PaperBuild Pro is ready.", Toast.LENGTH_LONG).show();
        }
    }

    private void launchPurchase() {
        if (productDetails == null || productDetails.getOneTimePurchaseOfferDetailsList().isEmpty()) {
            Toast.makeText(this, "Pro is not ready in Google Play yet.", Toast.LENGTH_LONG).show();
            return;
        }
        String offerToken = productDetails.getOneTimePurchaseOfferDetailsList().get(0).getOfferToken();
        BillingFlowParams.ProductDetailsParams product = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offerToken)
                .build();
        BillingFlowParams params = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(Collections.singletonList(product))
                .build();
        BillingResult result = billingClient.launchBillingFlow(this, params);
        if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) {
            state.setText("Couldn’t open Google Play purchase right now.");
        }
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
