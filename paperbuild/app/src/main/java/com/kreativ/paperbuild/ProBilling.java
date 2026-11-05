package com.kreativ.paperbuild;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Application-scoped connection: acknowledgement survives closing or rotating a screen. */
final class ProBilling {
    static final String PRODUCT_ID = "paperbuild_pro_lifetime";
    private static ProBilling instance;
    private final Context context;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Set<Runnable> observers = new HashSet<>();
    private final ArrayList<Runnable> completions = new ArrayList<>();
    private final BillingClient client;
    private final AcknowledgementQueue acknowledgements;
    private boolean connecting, querying, purchaseInFlight, ownershipKnown, paymentPending;
    private int purchaseRevision;
    ProductDetails product;
    ProductDetails.OneTimePurchaseOfferDetails offer;
    String status = "Checking Google Play…";

    static synchronized ProBilling get(Context context) {
        if (instance == null) instance = new ProBilling(context.getApplicationContext());
        return instance;
    }
    private ProBilling(Context context) {
        this.context = context;
        client = BillingClient.newBuilder(context)
                .setListener((result, purchases) -> main.post(() -> {
                    purchaseInFlight = false;
                    int code = result.getResponseCode();
                    if (code == BillingClient.BillingResponseCode.OK && purchases != null) {
                        purchaseRevision++; process(purchases, false);
                    } else if (code == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) refresh(null);
                    else {
                        status = code == BillingClient.BillingResponseCode.USER_CANCELED
                                ? "Purchase cancelled. You haven’t been charged." : "Purchase could not be completed. Try again.";
                        notifyUi();
                    }
                }))
                .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
                .enableAutoServiceReconnection().build();
        acknowledgements = new AcknowledgementQueue((token, callback) ->
                client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(token).build(),
                        result -> main.post(() -> callback.accept(result.getResponseCode() == BillingClient.BillingResponseCode.OK))),
                (task, delay) -> main.postDelayed(task, delay), success -> {
                    if (!success && isPro(context)) status = "Pro is unlocked. Google Play confirmation needs a connection; we’ll retry.";
                    else if (success && isPro(context)) status = "PaperBuild Pro is unlocked.";
                    notifyUi();
                });
    }
    static boolean isPro(Context context) {
        return context.getSharedPreferences("paperbuild_pro", Context.MODE_PRIVATE).getBoolean("owned", false)
                || ReviewAccess.active(context);
    }
    private void setOwned(boolean owned) {
        context.getSharedPreferences("paperbuild_pro", Context.MODE_PRIVATE).edit().putBoolean("owned", owned).apply();
    }
    static void refresh(Context context, Runnable completion) { get(context).refresh(completion); }
    void observe(Runnable observer) { observers.add(observer); observer.run(); }
    void removeObserver(Runnable observer) { observers.remove(observer); }
    String price() { return offer == null ? null : offer.getFormattedPrice(); }
    boolean canPurchase() { return client.isReady() && offer != null && ownershipKnown && !isPro(context) && !purchaseInFlight && !paymentPending; }

    void refresh(Runnable completion) {
        if (completion != null) {
            completions.add(completion);
            main.postDelayed(() -> { if (completions.remove(completion)) completion.run(); }, 15000);
        }
        if (client.isReady()) { queryProduct(); queryOwned(); return; }
        if (connecting) return;
        connecting = true;
        client.startConnection(new BillingClientStateListener() {
            public void onBillingSetupFinished(BillingResult result) {
                main.post(() -> {
                    connecting = false;
                    querying = false;
                    if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) { queryProduct(); queryOwned(); }
                    else { status = "Google Play is unavailable. Tap Restore purchase to retry."; finishRefresh(); notifyUi(); }
                });
            }
            public void onBillingServiceDisconnected() {
                main.post(() -> {
                    connecting = false;
                    status = "Google Play disconnected. Tap Restore purchase to retry.";
                    finishRefresh(); notifyUi();
                });
            }
        });
    }
    void restore() { status = "Restoring purchase…"; notifyUi(); refresh(null); }
    private void queryProduct() {
        QueryProductDetailsParams.Product request = QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_ID).setProductType(BillingClient.ProductType.INAPP).build();
        client.queryProductDetailsAsync(QueryProductDetailsParams.newBuilder().setProductList(Collections.singletonList(request)).build(),
                (result, response) -> main.post(() -> {
                    product = null; offer = null;
                    if (result.getResponseCode() == BillingClient.BillingResponseCode.OK && !response.getProductDetailsList().isEmpty()) {
                        product = response.getProductDetailsList().get(0);
                        List<ProductDetails.OneTimePurchaseOfferDetails> offers = product.getOneTimePurchaseOfferDetailsList();
                        if (offers != null) for (ProductDetails.OneTimePurchaseOfferDetails candidate : offers) {
                            if ("lifetime".equals(candidate.getPurchaseOptionId())) { offer = candidate; break; }
                        }
                    }
                    if (offer == null && !isPro(context)) status = "Price unavailable. Check Google Play and try Restore purchase.";
                    notifyUi();
                }));
    }
    private void queryOwned() {
        if (querying) return;
        querying = true;
        int revision = purchaseRevision;
        client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
                (result, purchases) -> main.post(() -> {
                    querying = false;
                    if (revision != purchaseRevision) { queryOwned(); return; }
                    if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        ownershipKnown = true; process(purchases, true);
                    } else { status = "Couldn’t restore from Google Play. Your saved Pro access is unchanged."; notifyUi(); }
                    finishRefresh();
                }));
    }
    void process(List<Purchase> purchases, boolean fullSnapshot) {
        boolean owned = false, pending = false;
        for (Purchase purchase : purchases) {
            if (!purchase.getProducts().contains(PRODUCT_ID)) continue;
            if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                owned = true; setOwned(true);
                if (!purchase.isAcknowledged()) acknowledgements.add(purchase.getPurchaseToken());
            } else if (purchase.getPurchaseState() == Purchase.PurchaseState.PENDING) pending = true;
        }
        if (fullSnapshot) setOwned(owned);
        paymentPending = pending;
        if (owned || isPro(context)) status = "PaperBuild Pro is unlocked.";
        else if (pending) status = "Payment is pending. Pro unlocks when Google Play confirms it.";
        else status = "No Pro purchase found for this Google Play account.";
        notifyUi();
    }
    void purchase(Activity activity) {
        if (!canPurchase()) { status = "Checking price and purchase status. Tap Restore purchase."; notifyUi(); return; }
        BillingFlowParams.ProductDetailsParams details = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(product).setOfferToken(offer.getOfferToken()).build();
        purchaseInFlight = true;
        BillingResult result = client.launchBillingFlow(activity, BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(Collections.singletonList(details)).build());
        if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) {
            purchaseInFlight = false;
            if (result.getResponseCode() == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) restore();
            else status = "Couldn’t open Google Play. Try again.";
        }
        notifyUi();
    }
    private void finishRefresh() {
        ArrayList<Runnable> callbacks = new ArrayList<>(completions); completions.clear();
        for (Runnable callback : callbacks) callback.run();
    }
    private void notifyUi() { for (Runnable observer : new ArrayList<>(observers)) observer.run(); }
}
