package com.kreativ.northlight;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import com.android.billingclient.api.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Application-scoped billing. Cached access survives transient errors, not a successful revocation query. */
final class BillingManager implements PurchasesUpdatedListener {
    static final String PRODUCT = "northlight_pro";
    static final String PREFS = "northlight_pro";
    interface Listener { void onBillingChanged(); }
    static BillingManager get(Context context) {
        return ((NorthlightApplication) context.getApplicationContext()).billing();
    }
    private final Context context;
    private final SharedPreferences prefs;
    private final BillingClient client;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<Listener> listeners = new ArrayList<>();
    private ProductDetails product;
    private String offerToken, price;
    private boolean entitled, connecting, foreground, productQuery, purchasesQuery, restoreRequested;
    private int attempt, status = R.string.billing_checking;
    private final Runnable retry = this::connectOrQuery;

    BillingManager(Context context) {
        this.context = context;
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        entitled = prefs.getBoolean("pro_entitled", false);
        client = BillingClient.newBuilder(context).setListener(this)
                .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
                .enableAutoServiceReconnection().build();
    }
    void addListener(Listener listener) { if (!listeners.contains(listener)) listeners.add(listener); listener.onBillingChanged(); }
    void removeListener(Listener listener) { listeners.remove(listener); }
    boolean entitled() { return entitled; }
    boolean canBuy() { return !entitled && product != null && offerToken != null && !offerToken.isEmpty(); }
    String price() { return price == null ? "" : price; }
    int status() {
        if (!entitled && product == null && !productQuery && status == R.string.billing_one_time) return R.string.billing_product_unavailable;
        return status;
    }
    boolean needsRetry() {
        int message = status();
        return message == R.string.billing_unavailable || message == R.string.billing_reconnecting
                || message == R.string.billing_restore_failed || message == R.string.billing_product_unavailable
                || message == R.string.billing_purchase_failed
                || (!entitled && !productQuery && product == null && message == R.string.billing_no_purchase);
    }
    void refresh(boolean restore) {
        foreground = true;
        restoreRequested |= restore;
        attempt = 0;
        handler.removeCallbacks(retry);
        status = R.string.billing_checking;
        notifyChanged();
        connectOrQuery();
    }
    void pauseRetries() { foreground = false; handler.removeCallbacks(retry); }
    private void notifyChanged() { for (Listener listener : new ArrayList<>(listeners)) listener.onBillingChanged(); }
    private void connectOrQuery() {
        if (!foreground || connecting) return;
        if (client.isReady()) { queryProduct(); queryPurchases(); return; }
        connecting = true;
        client.startConnection(new BillingClientStateListener() {
            @Override public void onBillingSetupFinished(BillingResult result) {
                handler.post(() -> {
                    connecting = false;
                    if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        queryProduct(); queryPurchases();
                    } else failed(R.string.billing_unavailable);
                });
            }
            @Override public void onBillingServiceDisconnected() {
                handler.post(() -> {
                    connecting = false;
                    product = null; offerToken = null;
                    failed(R.string.billing_reconnecting);
                });
            }
        });
    }
    private void failed(int message) {
        status = message;
        notifyChanged();
        if (foreground && attempt < 3) {
            handler.removeCallbacks(retry);
            handler.postDelayed(retry, RetryPolicy.delay(attempt++));
        }
    }
    private void queryProduct() {
        if (productQuery) return;
        productQuery = true;
        QueryProductDetailsParams.Product item = QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT).setProductType(BillingClient.ProductType.INAPP).build();
        client.queryProductDetailsAsync(QueryProductDetailsParams.newBuilder()
                .setProductList(Collections.singletonList(item)).build(), (result, details) -> handler.post(() -> {
            productQuery = false;
            if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                product = null; offerToken = null; price = null;
                for (ProductDetails candidate : details.getProductDetailsList()) {
                    if (!PRODUCT.equals(candidate.getProductId())) continue;
                    List<ProductDetails.OneTimePurchaseOfferDetails> offers = candidate.getOneTimePurchaseOfferDetailsList();
                    if (offers != null && !offers.isEmpty()) {
                        product = candidate;
                        offerToken = offers.get(0).getOfferToken();
                        price = offers.get(0).getFormattedPrice();
                    }
                }
                if (product == null && !entitled) status = R.string.billing_product_unavailable;
                notifyChanged();
            } else { product = null; offerToken = null; failed(R.string.billing_unavailable); }
        }));
    }
    private void queryPurchases() {
        if (purchasesQuery) return;
        purchasesQuery = true;
        // Do not gate this API call on isReady(): Billing 8 reconnects on calls made while disconnected.
        client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
                (result, purchases) -> handler.post(() -> {
                    purchasesQuery = false;
                    if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        handlePurchases(purchases, true);
                        restoreRequested = false;
                    } else failed(R.string.billing_restore_failed);
                }));
    }
    void buy(Activity activity) {
        if (!canBuy()) { refresh(false); return; }
        BillingFlowParams.ProductDetailsParams item = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(product).setOfferToken(offerToken).build();
        BillingResult result = client.launchBillingFlow(activity, BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(Collections.singletonList(item)).build());
        if (result.getResponseCode() == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) refresh(true);
        else if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) failed(R.string.billing_purchase_failed);
    }
    @Override public void onPurchasesUpdated(BillingResult result, List<Purchase> purchases) {
        handler.post(() -> {
            if (result.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) handlePurchases(purchases, false);
            else if (result.getResponseCode() == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) refresh(true);
            else if (result.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
                status = R.string.billing_cancelled; notifyChanged();
            } else failed(R.string.billing_purchase_failed);
        });
    }
    private void handlePurchases(List<Purchase> purchases, boolean complete) {
        PurchaseInventory inventory = PurchaseInventory.inspect(purchases, PRODUCT);
        for (String token : inventory.confirmations) AcknowledgeWorker.enqueue(context, token);
        entitled = RetryPolicy.entitlement(entitled, inventory.owned, complete);
        prefs.edit().putBoolean("pro_entitled", entitled).apply();
        status = entitled ? R.string.billing_unlocked : inventory.pending ? R.string.billing_pending
                : restoreRequested ? R.string.billing_no_purchase : R.string.billing_one_time;
        notifyChanged();
    }
}
