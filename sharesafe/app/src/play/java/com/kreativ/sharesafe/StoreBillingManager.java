package com.kreativ.sharesafe;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import com.android.billingclient.api.*;
import java.util.Collections;
import java.util.List;

final class StoreBillingManager extends BillingManager implements PurchasesUpdatedListener {
    private final Context context;
    private final Listener listener;
    private final SharedPreferences preferences;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final BillingClient client;
    private ProductDetails product;
    private boolean connecting, destroyed;
    private int connectionAttempts;
    private boolean restoreRequested;
    private long ownershipRevision;
    private String purchaseStatus = "Connecting to Google Play. Purchasing becomes available when a price is loaded.";

    StoreBillingManager(Context context, Listener listener) {
        this.context = context.getApplicationContext(); this.listener = listener;
        preferences = this.context.getSharedPreferences("sharesafe_billing", Context.MODE_PRIVATE);
        // Old prototype boolean unlocks are not a verified entitlement.
        preferences.edit().remove("pro_enabled").apply();
        client = BillingClient.newBuilder(this.context).setListener(this)
                .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
                .enableAutoServiceReconnection().build();
    }
    private void dispatch(Runnable callback) { main.post(() -> { if (!destroyed) callback.run(); }); }
    private void message(String text) { dispatch(() -> listener.onBillingMessage(text)); }

    void connect() {
        if (destroyed || connecting || client.isReady()) return;
        connecting = true;
        client.startConnection(new BillingClientStateListener() {
            public void onBillingSetupFinished(BillingResult result) {
                dispatch(() -> {
                    connecting = false;
                    if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        connectionAttempts = 0; queryProduct(null); queryOwnership();
                    } else {
                        product = null;
                        purchaseStatus = "Couldn’t connect to Google Play. Check your connection and try again. Metadata cleaning remains free.";
                        listener.onProductChanged();
                        if (restoreRequested) { restoreRequested = false; listener.onBillingMessage("Couldn’t connect to Google Play. Your existing Pro access has not been changed."); }
                        if (connectionAttempts++ < 4) main.postDelayed(StoreBillingManager.this::connect, Math.min(60_000, 2_000L << connectionAttempts));
                    }
                });
            }
            public void onBillingServiceDisconnected() { dispatch(() -> { product = null; purchaseStatus = "Google Play disconnected. Please try again shortly."; listener.onProductChanged(); }); }
        });
    }
    void refresh() {
        if (destroyed) return;
        if (!client.isReady()) { connect(); return; }
        queryProduct(null); queryOwnership();
    }
    void restore() { restoreRequested = true; refresh(); }
    boolean isProEnabled() {
        String data = preferences.getString("verified_data", null), signature = preferences.getString("verified_signature", null);
        if (data == null || signature == null) return false;
        try { return PlayPurchaseValidator.valid(context, new Purchase(data, signature)); }
        catch (Exception e) { return false; }
    }
    private ProductDetails.OneTimePurchaseOfferDetails offer() {
        return product == null || product.getOneTimePurchaseOfferDetailsList() == null || product.getOneTimePurchaseOfferDetailsList().isEmpty()
                ? null : product.getOneTimePurchaseOfferDetailsList().get(0);
    }
    boolean canPurchase() { return !destroyed && client.isReady() && offer() != null && !context.getString(R.string.play_billing_public_key).isEmpty(); }
    String price() { return offer() == null ? null : offer().getFormattedPrice(); }
    String purchaseStatus() { return purchaseStatus; }
    void launchPurchase(Activity activity) {
        if (destroyed) return;
        if (!client.isReady()) { connect(); message("Connecting to Google Play. Please try again when the price appears."); return; }
        // Never launch using stale ProductDetails; fetch the current eligible offer.
        queryProduct(() -> {
            if (activity.isFinishing() || activity.isDestroyed()) return;
            if (!canPurchase()) { message("ShareSafe Pro is unavailable on Google Play right now. No payment was taken."); return; }
            BillingFlowParams.ProductDetailsParams details = BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(product).setOfferToken(offer().getOfferToken()).build();
            BillingResult result = client.launchBillingFlow(activity, BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(Collections.singletonList(details)).build());
            if (result.getResponseCode() == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) { restoreRequested = true; queryOwnership(); }
            else if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) message("Couldn’t open the purchase. Please check Google Play and try again.");
        });
    }
    private void queryProduct(Runnable after) {
        QueryProductDetailsParams.Product sku = QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PlayPurchaseValidator.PRODUCT).setProductType(BillingClient.ProductType.INAPP).build();
        client.queryProductDetailsAsync(QueryProductDetailsParams.newBuilder().setProductList(Collections.singletonList(sku)).build(), (result, details) -> dispatch(() -> {
            product = result.getResponseCode() == BillingClient.BillingResponseCode.OK && !details.getProductDetailsList().isEmpty()
                    ? details.getProductDetailsList().get(0) : null;
            purchaseStatus = product == null ? "ShareSafe Pro is unavailable for this Google Play account or has not been activated yet. Metadata cleaning remains free."
                    : "One-time purchase through Google Play. No subscription.";
            listener.onProductChanged(); if (after != null) after.run();
        }));
    }
    private void queryOwnership() {
        long requestedRevision = ++ownershipRevision;
        client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(), (result, purchases) -> dispatch(() -> {
            if (requestedRevision != ownershipRevision) return;
            boolean requested = restoreRequested; restoreRequested = false;
            if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                if (requested) listener.onBillingMessage("Couldn’t restore right now. Check your connection and Google Play account; existing access is kept.");
                return;
            }
            Purchase verified = null; boolean pending = false;
            for (Purchase purchase : purchases) {
                if (PlayPurchaseValidator.valid(context, purchase)) { verified = purchase; break; }
                if (purchase.getProducts().contains(PlayPurchaseValidator.PRODUCT) && purchase.getPurchaseState() == Purchase.PurchaseState.PENDING) pending = true;
            }
            updateEntitlement(verified);
            if (verified != null && !verified.isAcknowledged()) AcknowledgementWorker.enqueue(context, verified.getPurchaseToken());
            if (requested) listener.onBillingMessage(verified != null ? "ShareSafe Pro purchase restored." : pending ? "Payment is pending. Pro unlocks after Google Play confirms it." : "No completed Pro purchase found for the current Google Play account.");
        }));
    }
    @Override public void onPurchasesUpdated(BillingResult result, List<Purchase> purchases) {
        dispatch(() -> {
            if (result.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
                ++ownershipRevision; // A pre-purchase empty query must not overwrite this update.
                for (Purchase purchase : purchases) {
                    if (PlayPurchaseValidator.valid(context, purchase)) {
                        updateEntitlement(purchase);
                        if (!purchase.isAcknowledged()) AcknowledgementWorker.enqueue(context, purchase.getPurchaseToken());
                        listener.onBillingMessage("ShareSafe Pro unlocked. Thank you!");
                    } else if (purchase.getProducts().contains(PlayPurchaseValidator.PRODUCT)) {
                        listener.onBillingMessage(purchase.getPurchaseState() == Purchase.PurchaseState.PENDING
                                ? "Payment is pending. Pro will unlock after confirmation." : "Couldn’t verify this purchase. Please use Restore purchase or contact support.");
                    }
                }
            } else if (result.getResponseCode() == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) { restoreRequested = true; queryOwnership(); }
            else if (result.getResponseCode() != BillingClient.BillingResponseCode.USER_CANCELED) listener.onBillingMessage("Purchase wasn’t completed. Please try again.");
        });
    }
    private void updateEntitlement(Purchase verified) {
        boolean previous = isProEnabled();
        boolean enabled = EntitlementRules.reconcile(previous, true, verified != null);
        SharedPreferences.Editor edit = preferences.edit();
        if (enabled) edit.putString("verified_data", verified.getOriginalJson()).putString("verified_signature", verified.getSignature());
        else edit.remove("verified_data").remove("verified_signature");
        edit.apply(); if (previous != enabled) listener.onProStatusChanged(enabled);
    }
    void destroy() { destroyed = true; main.removeCallbacksAndMessages(null); client.endConnection(); }
}
