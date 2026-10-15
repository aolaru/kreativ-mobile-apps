package com.kreativ.paperbuild;

/** Passwords live only in memory, never in a draft or saved preset. */
final class ExportOptions {
    static final ExportOptions NONE = new ExportOptions("", false, false, "");
    final String watermark, password;
    final boolean pageNumbers, protect;
    ExportOptions(String watermark, boolean pageNumbers, boolean protect, String password) {
        String text = watermark == null ? "" : watermark.trim();
        if (text.length() > 80 || text.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Use one line of up to 80 characters for the watermark.");
        }
        this.watermark = text; this.pageNumbers = pageNumbers; this.protect = protect;
        this.password = protect && password != null ? password : "";
    }
    void validate() {
        if (protect && (password.length() < 8 || password.length() > 64)) {
            throw new IllegalArgumentException("Enter an 8–64 character password before exporting.");
        }
        if (protect && password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 127) {
            throw new IllegalArgumentException("This password is too long in UTF-8. Use fewer characters.");
        }
    }
    ExportOptions withoutPassword() { return new ExportOptions(watermark, pageNumbers, protect, ""); }
    String summary() {
        return (protect ? "Password · " : "") + (watermark.isEmpty() ? "" : "Watermark · ")
                + (pageNumbers ? "Page numbers · " : "") + "Pro export options";
    }
}
