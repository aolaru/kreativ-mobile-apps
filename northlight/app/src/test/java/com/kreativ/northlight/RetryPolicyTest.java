package com.kreativ.northlight;
import org.junit.Test;
import static org.junit.Assert.*;

public class RetryPolicyTest {
    @Test public void retryDelayIsBoundedAndExponential() {
        assertEquals(2000L, RetryPolicy.delay(0)); assertEquals(4000L, RetryPolicy.delay(1));
        assertEquals(60000L, RetryPolicy.delay(99)); assertEquals(2000L, RetryPolicy.delay(-1));
    }
    @Test public void partialOrPendingResultsDoNotRevokeCachedAccess() {
        assertTrue(RetryPolicy.entitlement(true, false, false));
        assertFalse(RetryPolicy.entitlement(false, false, false));
    }
    @Test public void completeInventoryCanRevokeRefundedOwnership() {
        assertFalse(RetryPolicy.entitlement(true, false, true));
        assertTrue(RetryPolicy.entitlement(false, true, true));
        assertTrue(RetryPolicy.entitlement(false, true, false));
    }
}
