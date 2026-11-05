package com.kreativfont.studio;

final class EntitlementRules {
    private EntitlementRules() { }
    static boolean reconcile(boolean previous, boolean querySucceeded, boolean verifiedPurchase) {
        // A network/Play error is not evidence of a refund. A successful empty
        // ownership response is authoritative for the current Play account.
        return querySucceeded ? verifiedPurchase : previous;
    }
}
