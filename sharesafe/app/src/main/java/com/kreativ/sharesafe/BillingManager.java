package com.kreativ.sharesafe;

import android.app.Activity;
import android.content.Context;

/** Each storefront supplies its own implementation; no Play SDK in other stores. */
abstract class BillingManager {
    interface Listener {
        void onProStatusChanged(boolean enabled);
        void onBillingMessage(String message);
        void onProductChanged();
    }
    static BillingManager create(Context context, Listener listener) {
        return new StoreBillingManager(context, listener);
    }
    abstract void connect();
    abstract void refresh();
    abstract void restore();
    abstract boolean isProEnabled();
    abstract boolean canPurchase();
    abstract String price();
    abstract String purchaseStatus();
    abstract void launchPurchase(Activity activity);
    abstract void destroy();
}
