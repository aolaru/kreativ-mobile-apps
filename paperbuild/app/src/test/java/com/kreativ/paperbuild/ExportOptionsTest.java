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
    @Test public void logoSurvivesPasswordRemovalButCannotEscapePrivateStorage() {
        String logo = "01234567-89ab-cdef-0123-456789abcdef.png";
        ExportOptions options = new ExportOptions("Brand", true, true, "test-password", logo);
        assertEquals(logo, options.withoutPassword().logo); assertEquals("", options.withoutPassword().password);
        assertTrue(options.summary().contains("Logo")); assertEquals("", options.withLogo("").logo);
        for (String path : new String[]{"../outside.png", "/tmp/logo.png", "content://logo"}) {
            try { options.withLogo(path); fail(); } catch (IllegalArgumentException expected) { }
        }
    }
}
