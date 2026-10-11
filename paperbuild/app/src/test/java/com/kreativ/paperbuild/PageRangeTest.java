package com.kreativ.paperbuild;

import org.junit.Test;
import static org.junit.Assert.*;

public class PageRangeTest {
    @Test public void acceptsSingleAndSpacedRange() {
        assertArrayEquals(new int[]{2, 2}, PageRange.parse(" 2 "));
        assertArrayEquals(new int[]{1, 13}, PageRange.parse("1 - 13"));
    }
    @Test public void rejectsInvalidAndOverflowRanges() {
        for (String value : new String[]{null, "", "0", "-1", "3-1", "1-", "1,3", "1-2-3", "2147483648", "1-a"}) {
            assertNull(value, PageRange.parse(value));
        }
    }
}
