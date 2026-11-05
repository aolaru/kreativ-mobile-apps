package com.kreativfont.studio;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import android.util.Base64;

/** Offline signature verification; no design or receipt is sent to a Kreativ server. */
final class PurchaseVerifier {
    private PurchaseVerifier() { }
    static boolean verify(String publicKey, String data, String signature) {
        if (publicKey == null || publicKey.isEmpty() || data == null || signature == null) return false;
        try {
            return verifyBytes(Base64.decode(publicKey, Base64.DEFAULT), data, Base64.decode(signature, Base64.DEFAULT));
        } catch (Exception e) { return false; }
    }
    static boolean verifyBytes(byte[] publicKey, String data, byte[] signature) {
        if (publicKey == null || data == null || signature == null) return false;
        try {
            Signature verifier = Signature.getInstance("SHA1withRSA");
            verifier.initVerify(KeyFactory.getInstance("RSA").generatePublic(
                    new X509EncodedKeySpec(publicKey)));
            verifier.update(data.getBytes(StandardCharsets.UTF_8));
            return verifier.verify(signature);
        } catch (Exception e) { return false; }
    }
}
