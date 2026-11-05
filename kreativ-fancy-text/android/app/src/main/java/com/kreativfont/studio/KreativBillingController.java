package com.kreativfont.studio;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import com.android.billingclient.api.*;
import java.util.Collections;
import java.util.List;
import com.getcapacitor.JSObject;
import org.json.JSONArray;

final class KreativBillingController implements PurchasesUpdatedListener {
    interface Listener {
        void onBillingMessage(String message);
        void onProductChanged();
        void onProStatusChanged(boolean enabled);
    }
    private final Context context;
    private final Listener listener;
    private final SharedPreferences preferences;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final BillingClient client;
    private ProductDetails product;
    private boolean connecting, setupComplete, destroyed;
    private int connectionAttempts;
    private boolean restoreRequested;
    private boolean purchaseInFlight, pendingPayment;
    private long ownershipRevision;
    private int connectionResponse = Integer.MIN_VALUE, productResponse = Integer.MIN_VALUE,
            ownershipResponse = Integer.MIN_VALUE, fetchedProductCount, matchingPurchaseCount;
    private JSONArray unfetchedProducts = new JSONArray(), purchaseOptions = new JSONArray();
    private int configResponse = Integer.MIN_VALUE;
    private String playCountry;
    private String purchaseStatus = "Connecting to Google Play. Purchasing becomes available when a price is loaded.";

    KreativBillingController(Context context, Listener listener) {
        this.context = context.getApplicationContext(); this.listener = listener;
        preferences = this.context.getSharedPreferences("kreativ_billing", Context.MODE_PRIVATE);
        // Old prototype boolean unlocks are not a verified entitlement.
        preferences.edit().remove("pro_enabled").apply();
        client = BillingClient.newBuilder(this.context).setListener(this)
                .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
                .enableAutoServiceReconnection().build();
    }
    private void dispatch(Runnable callback) { main.post(() -> { if (!destroyed) callback.run(); }); }
    private void message(String text) { dispatch(() -> listener.onBillingMessage(text)); }

    void connect() {
        // With automatic reconnection, isReady() can be true before initial setup.
        // Always complete our own initial handshake before querying the catalogue.
        if (destroyed || connecting || (setupComplete && client.isReady())) return;
        connecting = true;
        client.startConnection(new BillingClientStateListener() {
            public void onBillingSetupFinished(BillingResult result) {
                dispatch(() -> {
                    connecting = false;
                    connectionResponse = result.getResponseCode();
                    setupComplete = result.getResponseCode() == BillingClient.BillingResponseCode.OK;
                    if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        connectionAttempts = 0; queryProduct(null); queryOwnership();
                        client.getBillingConfigAsync(GetBillingConfigParams.newBuilder().build(), (configResult, config) -> dispatch(() -> {
                            configResponse = configResult.getResponseCode();
                            playCountry = config == null ? null : config.getCountryCode();
                            listener.onProductChanged();
                        }));
                    } else {
                        product = null;
                        purchaseStatus = "Couldn’t connect to Google Play. Check your connection and try again. The free editor is still available.";
                        listener.onProductChanged();
                        if (restoreRequested) { restoreRequested = false; listener.onBillingMessage("Couldn’t connect to Google Play. Your existing Pro access has not been changed."); }
                        if (connectionAttempts++ < 4) main.postDelayed(KreativBillingController.this::connect, Math.min(60_000, 2_000L << connectionAttempts));
                    }
                });
            }
            public void onBillingServiceDisconnected() { dispatch(() -> { setupComplete = false; product = null; purchaseStatus = "Google Play disconnected. Please try again shortly."; listener.onProductChanged(); }); }
        });
    }
    void refresh() {
        if (destroyed) return;
        if (!setupComplete || !client.isReady()) { connect(); return; }
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
                ? null : product.getOneTimePurchaseOfferDetailsList().stream().filter(o -> "buy".equals(o.getPurchaseOptionId()) && o.getOfferId() == null).findFirst().orElse(null);
    }
    boolean canPurchase() { return !destroyed && setupComplete && !purchaseInFlight && !pendingPayment && client.isReady() && offer() != null && !context.getString(R.string.play_billing_public_key).isEmpty(); }
    boolean isPending() { return pendingPayment; }
    boolean isPurchasing() { return purchaseInFlight; }
    String price() { return offer() == null ? null : offer().getFormattedPrice(); }
    String purchaseStatus() { return purchaseStatus; }
    // Support diagnostics contain public catalogue identifiers and response codes only.
    // Never expose purchase tokens, receipts, signatures, account IDs or offer tokens.
    JSObject diagnostics() {
        JSObject value = new JSObject();
        value.put("productId", PlayPurchaseValidator.PRODUCT);
        value.put("productType", BillingClient.ProductType.INAPP);
        value.put("connectionResponse", connectionResponse);
        value.put("setupComplete", setupComplete);
        value.put("configResponse", configResponse);
        value.put("playCountry", playCountry);
        value.put("productResponse", productResponse);
        value.put("ownershipResponse", ownershipResponse);
        value.put("fetchedProductCount", fetchedProductCount);
        value.put("matchingPurchaseCount", matchingPurchaseCount);
        value.put("unfetchedProducts", unfetchedProducts);
        value.put("purchaseOptions", purchaseOptions);
        return value;
    }
    void launchPurchase(Activity activity) {
        if (destroyed || purchaseInFlight || pendingPayment) return;
        if (!setupComplete || !client.isReady()) { connect(); message("Connecting to Google Play. Please try again when the price appears."); return; }
        purchaseInFlight = true; listener.onProductChanged();
        // Never launch using stale ProductDetails; fetch the current eligible offer.
        queryProduct(() -> {
            purchaseInFlight = false;
            if (activity.isFinishing() || activity.isDestroyed()) { listener.onProductChanged(); return; }
            if (!canPurchase()) { message("Fancy Text Pro is unavailable on Google Play right now. No payment was taken."); return; }
            BillingFlowParams.ProductDetailsParams details = BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(product).setOfferToken(offer().getOfferToken()).build();
            BillingResult result = client.launchBillingFlow(activity, BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(Collections.singletonList(details)).build());
            purchaseInFlight = result.getResponseCode() == BillingClient.BillingResponseCode.OK;
            listener.onProductChanged();
            if (result.getResponseCode() == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) { restoreRequested = true; queryOwnership(); }
            else if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) message("Couldn’t open the purchase. Please check Google Play and try again.");
        });
    }
    private void queryProduct(Runnable after) {
        QueryProductDetailsParams.Product sku = QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PlayPurchaseValidator.PRODUCT).setProductType(BillingClient.ProductType.INAPP).build();
        client.queryProductDetailsAsync(QueryProductDetailsParams.newBuilder().setProductList(Collections.singletonList(sku)).build(), (result, details) -> dispatch(() -> {
            productResponse = result.getResponseCode();
            fetchedProductCount = details.getProductDetailsList().size();
            unfetchedProducts = new JSONArray();
            for (UnfetchedProduct item : details.getUnfetchedProductList()) {
                JSObject entry = new JSObject(); entry.put("productId", item.getProductId());
                entry.put("statusCode", item.getStatusCode()); unfetchedProducts.put(entry);
            }
            product = result.getResponseCode() == BillingClient.BillingResponseCode.OK && !details.getProductDetailsList().isEmpty()
                    ? details.getProductDetailsList().get(0) : null;
            purchaseOptions = new JSONArray();
            if (product != null && product.getOneTimePurchaseOfferDetailsList() != null) {
                for (ProductDetails.OneTimePurchaseOfferDetails item : product.getOneTimePurchaseOfferDetailsList()) {
                    JSObject entry = new JSObject(); entry.put("purchaseOptionId", item.getPurchaseOptionId());
                    entry.put("offerId", item.getOfferId()); entry.put("price", item.getFormattedPrice());
                    purchaseOptions.put(entry);
                }
            }
            if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                purchaseStatus = "Couldn’t load the Google Play price. Check your connection and try again. The free editor is still available.";
            } else if (product == null) {
                boolean notFound = false;
                for (UnfetchedProduct item : details.getUnfetchedProductList()) {
                    if (PlayPurchaseValidator.PRODUCT.equals(item.getProductId()) && item.getStatusCode() == UnfetchedProduct.StatusCode.PRODUCT_NOT_FOUND) notFound = true;
                }
                purchaseStatus = notFound ? "Google Play couldn’t find Fancy Text Pro. Please try again later. The free editor is still available."
                        : "Fancy Text Pro is unavailable for this Google Play account right now. The free editor is still available.";
            } else {
                purchaseStatus = offer() == null ? "Google Play has no available Fancy Text Pro purchase option for this account."
                        : "One-time purchase through Google Play. No subscription.";
            }
            listener.onProductChanged(); if (after != null) after.run();
        }));
    }
    private void queryOwnership() {
        long requestedRevision = ++ownershipRevision;
        client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(), (result, purchases) -> dispatch(() -> {
            if (requestedRevision != ownershipRevision) return;
            ownershipResponse = result.getResponseCode();
            boolean requested = restoreRequested; restoreRequested = false;
            if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                if (requested) listener.onBillingMessage("Couldn’t restore right now. Check your connection and Google Play account; existing access is kept.");
                return;
            }
            Purchase verified = null; boolean pending = false;
            matchingPurchaseCount = 0;
            for (Purchase purchase : purchases) {
                if (purchase.getProducts().contains(PlayPurchaseValidator.PRODUCT)) matchingPurchaseCount++;
                if (PlayPurchaseValidator.valid(context, purchase)) { verified = purchase; break; }
                if (purchase.getProducts().contains(PlayPurchaseValidator.PRODUCT) && purchase.getPurchaseState() == Purchase.PurchaseState.PENDING) pending = true;
            }
            pendingPayment = pending; updateEntitlement(verified); listener.onProductChanged();
            if (verified != null && !verified.isAcknowledged()) AcknowledgementWorker.enqueue(context, verified.getPurchaseToken());
            if (requested) listener.onBillingMessage(verified != null ? "Fancy Text Pro purchase restored." : pending ? "Payment is pending. Pro unlocks after Google Play confirms it." : "No completed Pro purchase found for the current Google Play account.");
        }));
    }
    @Override public void onPurchasesUpdated(BillingResult result, List<Purchase> purchases) {
        dispatch(() -> {
            purchaseInFlight = false;
            if (result.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
                ++ownershipRevision; // A pre-purchase empty query must not overwrite this update.
                for (Purchase purchase : purchases) {
                    if (PlayPurchaseValidator.valid(context, purchase)) {
                        pendingPayment = false;
                        updateEntitlement(purchase);
                        if (!purchase.isAcknowledged()) AcknowledgementWorker.enqueue(context, purchase.getPurchaseToken());
                        listener.onBillingMessage("Fancy Text Pro unlocked. Thank you!");
                    } else if (purchase.getProducts().contains(PlayPurchaseValidator.PRODUCT)) {
                        pendingPayment = purchase.getPurchaseState() == Purchase.PurchaseState.PENDING;
                        listener.onBillingMessage(purchase.getPurchaseState() == Purchase.PurchaseState.PENDING
                                ? "Payment is pending. Pro will unlock after confirmation." : "Couldn’t verify this purchase. Please use Restore purchase or contact support.");
                    }
                }
            } else if (result.getResponseCode() == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) { restoreRequested = true; queryOwnership(); }
            else if (result.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) listener.onBillingMessage("Purchase cancelled. No payment was taken.");
            else listener.onBillingMessage("Purchase wasn’t completed. Please try again.");
            listener.onProStatusChanged(isProEnabled());
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
