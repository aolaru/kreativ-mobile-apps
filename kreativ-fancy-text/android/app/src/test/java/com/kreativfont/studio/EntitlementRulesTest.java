package com.kreativfont.studio;

import org.junit.Test;
import static org.junit.Assert.*;

public class EntitlementRulesTest {
    @Test public void interruptedRestoreKeepsVerifiedAccessButNeverCreatesNewAccess() {
        assertTrue(EntitlementRules.reconcile(true, false, false));
        assertFalse(EntitlementRules.reconcile(false, false, false));
    }
    @Test public void completedQueriesRevokeMissingPurchasesAndGrantOnlyVerifiedPurchases() {
        assertFalse(EntitlementRules.reconcile(true, true, false));
        assertFalse(EntitlementRules.reconcile(false, true, false));
        assertTrue(EntitlementRules.reconcile(false, true, true));
    }
}
