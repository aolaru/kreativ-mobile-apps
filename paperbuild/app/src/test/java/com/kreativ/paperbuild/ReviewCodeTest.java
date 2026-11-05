package com.kreativ.paperbuild;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class ReviewCodeTest {
    private KeyPair key;
    private String publicKey;
    @Before public void setup() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        key = generator.generateKeyPair();
        publicKey = Base64.getEncoder().encodeToString(key.getPublic().getEncoded());
    }
    private String signed(String payload) throws Exception {
        Signature signer = Signature.getInstance("SHA256withECDSA");
        signer.initSign(key.getPrivate()); signer.update(payload.getBytes(StandardCharsets.UTF_8));
        return ReviewCode.PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(signer.sign());
    }
    @Test public void acceptsReusableValidCodeWithOuterWhitespace() throws Exception {
        String code = signed(ReviewCode.PAYLOAD);
        assertTrue(ReviewCode.valid(code, publicKey));
        assertTrue(ReviewCode.valid("\n " + code + " \n", publicKey));
        assertTrue(ReviewCode.valid(code, publicKey));
    }
    @Test public void rejectsWrongAppScopeVersionAndSigner() throws Exception {
        assertFalse(ReviewCode.valid(signed("paperbuild-review-v1:another.app:pro"), publicKey));
        assertFalse(ReviewCode.valid(signed(ReviewCode.PAYLOAD + ":other"), publicKey));
        assertFalse(ReviewCode.valid(signed(ReviewCode.PAYLOAD).replace("PBR1.", "PBR2."), publicKey));
        assertFalse(ReviewCode.valid(signed(ReviewCode.PAYLOAD)));
    }
    @Test public void rejectsTamperingAndMalformedInput() throws Exception {
        String code = signed(ReviewCode.PAYLOAD);
        char changed = code.charAt(12) == 'A' ? 'B' : 'A';
        assertFalse(ReviewCode.valid(code.substring(0, 12) + changed + code.substring(13), publicKey));
        for (String invalid : new String[] { null, "", "PBR1.", "PBR1.!!!!", code + "=", code + ".", "x".repeat(257) }) {
            assertFalse(ReviewCode.valid(invalid, publicKey));
        }
        assertFalse(ReviewCode.valid(code, "not-a-key"));
    }
}
