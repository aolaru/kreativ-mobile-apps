package com.kreativ.northlight;
import com.android.billingclient.api.Purchase;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.*;

public class PurchaseInventoryTest {
    private Purchase purchase(String product, int playState, boolean acknowledged) throws Exception {
        return new Purchase("{\"productId\":\"" + product + "\",\"purchaseState\":" + playState
                + ",\"acknowledged\":" + acknowledged + ",\"purchaseToken\":\"test-token\"}", "test-signature");
    }
    @Test public void pendingPaymentNeverUnlocksOrAcknowledges() throws Exception {
        PurchaseInventory state = PurchaseInventory.inspect(Collections.singletonList(purchase("northlight_pro", 4, false)), "northlight_pro");
        assertTrue(state.pending); assertFalse(state.owned); assertTrue(state.confirmations.isEmpty());
    }
    @Test public void completedPurchaseQueuesConfirmationOnlyOnce() throws Exception {
        Purchase item = purchase("northlight_pro", 0, false);
        PurchaseInventory state = PurchaseInventory.inspect(Arrays.asList(item, item), "northlight_pro");
        assertTrue(state.owned); assertFalse(state.pending); assertEquals(1, state.confirmations.size());
    }
    @Test public void alreadyAcknowledgedPurchaseDoesNotQueueWork() throws Exception {
        PurchaseInventory state = PurchaseInventory.inspect(Collections.singletonList(purchase("northlight_pro", 0, true)), "northlight_pro");
        assertTrue(state.owned); assertTrue(state.confirmations.isEmpty());
    }
    @Test public void unrelatedProductsCannotGrantPro() throws Exception {
        PurchaseInventory state = PurchaseInventory.inspect(Collections.singletonList(purchase("unrelated", 0, false)), "northlight_pro");
        assertFalse(state.owned); assertTrue(state.confirmations.isEmpty());
    }
    @Test public void jobNamesAreStableAndDoNotExposeReceiptTokens() {
        String name = AcknowledgeWorker.workName("private-receipt-token");
        assertEquals(name, AcknowledgeWorker.workName("private-receipt-token"));
        assertNotEquals(name, AcknowledgeWorker.workName("different-receipt"));
        assertFalse(name.contains("private-receipt-token"));
    }
}
