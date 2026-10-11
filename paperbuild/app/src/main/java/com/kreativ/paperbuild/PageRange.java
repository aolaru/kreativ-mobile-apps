package com.kreativ.paperbuild;

final class PageRange {
    private PageRange() { }
    static int[] parse(String raw) {
        if (raw == null || !raw.trim().matches("\\d+(?:\\s*-\\s*\\d+)?")) return null;
        String[] parts = raw.trim().split("-");
        try {
            int first = Integer.parseInt(parts[0].trim());
            int last = parts.length == 1 ? first : Integer.parseInt(parts[1].trim());
            return first > 0 && last >= first ? new int[]{first, last} : null;
        } catch (NumberFormatException ignored) { return null; }
    }
}
