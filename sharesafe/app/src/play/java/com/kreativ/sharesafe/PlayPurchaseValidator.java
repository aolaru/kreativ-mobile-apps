package com.kreativ.sharesafe;

import android.content.Context;
import com.android.billingclient.api.Purchase;
import org.json.JSONObject;

final class PlayPurchaseValidator {
    static final String PRODUCT = "sharesafe_pro";
    private PlayPurchaseValidator() { }
    static boolean valid(Context context, Purchase purchase) {
        if (!purchase.getProducts().contains(PRODUCT) || purchase.getPurchaseState() != Purchase.PurchaseState.PURCHASED) return false;
        try {
            if (!context.getPackageName().equals(new JSONObject(purchase.getOriginalJson()).optString("packageName"))) return false;
            return PurchaseVerifier.verify(context.getString(R.string.play_billing_public_key), purchase.getOriginalJson(), purchase.getSignature());
        } catch (Exception e) { return false; }
    }
}
