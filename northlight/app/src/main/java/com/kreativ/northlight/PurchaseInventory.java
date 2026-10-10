package com.kreativ.northlight;

import com.android.billingclient.api.Purchase;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Only completed purchases for our product grant access or enter the acknowledgement queue. */
final class PurchaseInventory {
    boolean owned;
    boolean pending;
    final Set<String> confirmations = new LinkedHashSet<>();
    static PurchaseInventory inspect(List<Purchase> purchases, String product) {
        PurchaseInventory inventory = new PurchaseInventory();
        for (Purchase purchase : purchases) {
            if (!purchase.getProducts().contains(product)) continue;
            if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                inventory.owned = true;
                if (!purchase.isAcknowledged()) inventory.confirmations.add(purchase.getPurchaseToken());
            } else if (purchase.getPurchaseState() == Purchase.PurchaseState.PENDING) inventory.pending = true;
        }
        return inventory;
    }
}
