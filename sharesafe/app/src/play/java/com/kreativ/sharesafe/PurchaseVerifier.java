package com.kreativ.sharesafe;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/** Offline verification; no photo or receipt is sent to a Kreativ server. */
final class PurchaseVerifier {
    private PurchaseVerifier() { }
    static boolean verify(String publicKey, String data, String signature) {
        if (publicKey == null || publicKey.isEmpty() || data == null || signature == null) return false;
        try {
            Signature verifier = Signature.getInstance("SHA1withRSA");
            verifier.initVerify(KeyFactory.getInstance("RSA").generatePublic(
                    new X509EncodedKeySpec(Base64.getDecoder().decode(publicKey))));
            verifier.update(data.getBytes(StandardCharsets.UTF_8));
            return verifier.verify(Base64.getDecoder().decode(signature));
        } catch (Exception e) { return false; }
    }
}
