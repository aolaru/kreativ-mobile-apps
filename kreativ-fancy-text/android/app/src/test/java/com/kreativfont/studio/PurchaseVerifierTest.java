package com.kreativfont.studio;

import org.junit.Test;
import static org.junit.Assert.*;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.nio.charset.StandardCharsets;

public class PurchaseVerifierTest {
    @Test public void acceptsAuthenticReceiptAndRejectsTamperingWrongKeyAndMalformedData() throws Exception {
        var generator = KeyPairGenerator.getInstance("RSA"); generator.initialize(2048);
        var pair = generator.generateKeyPair();
        String receipt = "{\"packageName\":\"com.kreativfont.studio\",\"productId\":\"kreativ_pro_lifetime\",\"purchaseState\":0}";
        var signer = Signature.getInstance("SHA1withRSA"); signer.initSign(pair.getPrivate()); signer.update(receipt.getBytes(StandardCharsets.UTF_8));
        byte[] signature = signer.sign();
        assertTrue(PurchaseVerifier.verifyBytes(pair.getPublic().getEncoded(), receipt, signature));
        assertFalse(PurchaseVerifier.verifyBytes(pair.getPublic().getEncoded(), receipt.replace("lifetime", "forged"), signature));
        assertFalse(PurchaseVerifier.verifyBytes(generator.generateKeyPair().getPublic().getEncoded(), receipt, signature));
        assertFalse(PurchaseVerifier.verifyBytes(new byte[]{1,2}, receipt, signature));
        assertFalse(PurchaseVerifier.verifyBytes(pair.getPublic().getEncoded(), receipt, new byte[]{0}));
        assertFalse(PurchaseVerifier.verifyBytes(null, receipt, signature));
    }
}
