package com.kreativ.sharesafe;
import android.app.Activity;
import android.content.Context;

final class StoreBillingManager extends BillingManager {
    StoreBillingManager(Context context, Listener listener) { }
    void connect() { }
    void refresh() { }
    void restore() { }
    boolean isProEnabled() { return false; }
    boolean canPurchase() { return false; }
    String price() { return null; }
    String purchaseStatus() { return "Pro purchases are not available in this store edition."; }
    void launchPurchase(Activity activity) { }
    void destroy() { }
}
