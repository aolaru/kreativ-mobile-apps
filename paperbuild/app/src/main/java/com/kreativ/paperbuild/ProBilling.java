package com.kreativ.paperbuild;

import android.content.Context;
import android.content.SharedPreferences;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryPurchasesParams;

import java.util.List;

/** Keeps the local entitlement in sync with the user's non-consumable Play purchase. */
final class ProBilling {
    static final String PRODUCT_ID = "paperbuild_pro_lifetime";
    private static final String PREFS = "paperbuild_pro";
    private static final String KEY_OWNED = "owned";

    private ProBilling() { }

    static boolean isPro(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_OWNED, false);
    }

    static void setPro(Context context, boolean owned) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_OWNED, owned).apply();
    }

    static void refresh(Context context, Runnable completion) {
        Context appContext = context.getApplicationContext();
        PurchasesUpdatedListener listener = (result, purchases) -> { };
        BillingClient client = BillingClient.newBuilder(appContext)
                .setListener(listener)
                .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
                .enableAutoServiceReconnection()
                .build();
        client.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult result) {
                if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                    completion.run();
                    client.endConnection();
                    return;
                }
                QueryPurchasesParams params = QueryPurchasesParams.newBuilder()
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build();
                client.queryPurchasesAsync(params, (queryResult, purchases) -> {
                    if (queryResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        boolean ownsPro = false;
                        for (Purchase purchase : purchases) {
                            if (isProPurchase(purchase)) {
                                ownsPro = true;
                                acknowledge(client, purchase);
                            }
                        }
                        setPro(appContext, ownsPro);
                    }
                    completion.run();
                    client.endConnection();
                });
            }

            @Override
            public void onBillingServiceDisconnected() {
                completion.run();
            }
        });
    }

    static boolean isProPurchase(Purchase purchase) {
        return purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED
                && purchase.getProducts().contains(PRODUCT_ID);
    }

    static void acknowledge(BillingClient client, Purchase purchase) {
        if (purchase.isAcknowledged()) return;
        AcknowledgePurchaseParams params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.getPurchaseToken())
                .build();
        client.acknowledgePurchase(params, result -> { });
    }
}
