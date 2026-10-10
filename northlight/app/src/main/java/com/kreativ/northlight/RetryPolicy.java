package com.kreativ.northlight;

final class RetryPolicy {
    private RetryPolicy() { }
    static long delay(int attempt) { return Math.min(60_000L, 2_000L * (1L << Math.min(5, Math.max(0, attempt)))); }
    static boolean entitlement(boolean cached, boolean purchased, boolean completeInventory) {
        return purchased || (!completeInventory && cached);
    }
}
