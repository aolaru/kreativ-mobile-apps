package com.kreativ.paperbuild;

import org.junit.Test;
import static org.junit.Assert.*;

public class ExportOptionsTest {
    @Test public void passwordsAreRemovedWithoutDisablingTheProtectionRequirement() {
        ExportOptions options = new ExportOptions(" Confidential ", true, true, "test-only-password"); options.validate();
        ExportOptions stored = options.withoutPassword();
        assertEquals("", stored.password); assertTrue(stored.protect); assertTrue(stored.pageNumbers);
        assertEquals("Confidential", stored.watermark);
        assertFalse(options.summary().contains(options.password));
        try { stored.validate(); fail(); } catch (IllegalArgumentException expected) { }
    }
    @Test public void validatesPasswordsAndSingleLineWatermarks() {
        ExportOptions.NONE.validate();
        try { new ExportOptions("one\ntwo", false, false, ""); fail(); } catch (IllegalArgumentException expected) { }
        try { new ExportOptions("", false, true, "short").validate(); fail(); } catch (IllegalArgumentException expected) { }
        assertEquals("", new ExportOptions("", false, false, "discard-this").password);
    }
}
