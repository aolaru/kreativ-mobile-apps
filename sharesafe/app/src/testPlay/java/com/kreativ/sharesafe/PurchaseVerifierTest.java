package com.kreativ.sharesafe;

import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.Base64;

public class PurchaseVerifierTest {
    @Test public void acceptsSignedReceiptButRejectsModifiedReceipt() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA"); generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        String key = Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());
        String receipt = "{\"productId\":\"sharesafe_pro\"}";
        Signature signer = Signature.getInstance("SHA1withRSA"); signer.initSign(pair.getPrivate());
        signer.update(receipt.getBytes(StandardCharsets.UTF_8));
        String signature = Base64.getEncoder().encodeToString(signer.sign());
        assertTrue(PurchaseVerifier.verify(key, receipt, signature));
        assertFalse(PurchaseVerifier.verify(key, receipt + " ", signature));
        assertFalse(PurchaseVerifier.verify(key, receipt, "invalid"));
    }
    @Test public void rejectsMissingOrInvalidKey() {
        assertFalse(PurchaseVerifier.verify("", "receipt", "signature"));
        assertFalse(PurchaseVerifier.verify("invalid", "receipt", "signature"));
        assertFalse(PurchaseVerifier.verify(null, null, null));
    }
    @Test public void successfulEmptyQueryRevokesAccess() { assertFalse(EntitlementRules.reconcile(true, true, false)); }
    @Test public void networkFailureKeepsVerifiedOfflineAccess() { assertTrue(EntitlementRules.reconcile(true, false, false)); }
    @Test public void pendingOrUnverifiedPurchaseDoesNotUnlock() { assertFalse(EntitlementRules.reconcile(false, true, false)); }
    @Test public void confirmedPurchaseUnlocks() { assertTrue(EntitlementRules.reconcile(false, true, true)); }
}
