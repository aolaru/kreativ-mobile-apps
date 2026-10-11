package com.kreativ.sharesafe;

import org.junit.Test;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeNotNull;

public class ReviewerAccessTest {
    @Test public void acceptsPrivateReviewerCodeAndWhitespace() {
        String code = System.getenv("KREATIV_SHARESAFE_REVIEWER_CODE");
        assumeNotNull(code);
        assertTrue(ReviewerAccess.validCode(code));
        assertTrue(ReviewerAccess.validCode("  " + code + "\n"));
        assertFalse(ReviewerAccess.validCode(code.substring(0, 31) + (code.endsWith("0") ? "1" : "0")));
    }
    @Test public void rejectsInvalidCodes() {
        assertFalse(ReviewerAccess.validCode(null));
        assertFalse(ReviewerAccess.validCode(""));
        assertFalse(ReviewerAccess.validCode("00000000000000000000000000000000"));
        assertFalse(ReviewerAccess.validCode("25dc02031bb60b0e58d2401f59e58aa9499ec9e13e304a86e028d807be4c800c"));
    }
}
