package com.kreativ.paperbuild;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/** Offline verification only. The private issuing key is never included in the app. */
final class ReviewCode {
    static final String PAYLOAD = "paperbuild-review-v1:com.kreativ.paperbuild:pro";
    static final String PREFIX = "PBR1.";
    static final String PUBLIC_KEY = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEmTO/zahpqEYaNHu91cE+LhHOXF8u02moz/acxtvy18jjA3YiHk952idjGGuGU2cHPHovyXQ6LP4Vdpj4xi54Lw==";
    private ReviewCode() { }

    static boolean valid(String input) { return valid(input, PUBLIC_KEY); }

    static boolean valid(String input, String publicKey) {
        if (input == null || input.length() > 256) return false;
        String code = input.trim();
        if (!code.startsWith(PREFIX)) return false;
        try {
            String encoded = code.substring(PREFIX.length());
            byte[] signature = Base64.getUrlDecoder().decode(encoded);
            // Accept only the canonical, unpadded representation; reject malformed input early.
            if (signature.length < 64 || signature.length > 80
                    || !Base64.getUrlEncoder().withoutPadding().encodeToString(signature).equals(encoded)) return false;
            PublicKey key = KeyFactory.getInstance("EC").generatePublic(
                    new X509EncodedKeySpec(Base64.getDecoder().decode(publicKey)));
            Signature verifier = Signature.getInstance("SHA256withECDSA");
            verifier.initVerify(key);
            verifier.update(PAYLOAD.getBytes(StandardCharsets.UTF_8));
            return verifier.verify(signature);
        } catch (GeneralSecurityException | IllegalArgumentException error) {
            return false;
        }
    }
}
