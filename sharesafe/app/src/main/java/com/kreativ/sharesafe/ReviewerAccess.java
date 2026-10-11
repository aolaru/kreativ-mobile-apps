package com.kreativ.sharesafe;

import android.content.Context;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Complimentary review access is separate from paid purchase entitlement. */
final class ReviewerAccess {
    private static final String DIGEST = "25dc02031bb60b0e58d2401f59e58aa9499ec9e13e304a86e028d807be4c800c";
    private static final String PREFS = "sharesafe_reviewer_access";

    static boolean validCode(String code) {
        if (code == null || code.trim().length() != 32) return false;
        try {
            byte[] actual = MessageDigest.getInstance("SHA-256").digest(code.trim().getBytes(StandardCharsets.UTF_8));
            byte[] expected = new byte[32];
            for (int i = 0; i < expected.length; i++) expected[i] = (byte) Integer.parseInt(DIGEST.substring(i * 2, i * 2 + 2), 16);
            return MessageDigest.isEqual(expected, actual);
        } catch (java.security.NoSuchAlgorithmException e) { return false; }
    }

    static boolean enabled(Context context) {
        return BuildConfig.PRO_AVAILABLE && validCode(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("code", null));
    }

    static boolean activate(Context context, String code) {
        if (!BuildConfig.PRO_AVAILABLE || !validCode(code)) return false;
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("code", code.trim()).commit();
    }

    static void disable(Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply();
    }
}
