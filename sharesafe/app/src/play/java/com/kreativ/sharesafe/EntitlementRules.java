package com.kreativ.sharesafe;

final class EntitlementRules {
    private EntitlementRules() { }
    // Never revoke on a network error. A successful empty response revokes cached access.
    static boolean reconcile(boolean previous, boolean querySucceeded, boolean verifiedPurchase) {
        return querySucceeded ? verifiedPurchase : previous;
    }
}
