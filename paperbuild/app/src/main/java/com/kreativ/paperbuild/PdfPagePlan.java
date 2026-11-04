package com.kreativ.paperbuild;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Original page indices plus non-destructive rotations. No rendered page data. */
final class PdfPagePlan {
    static final class Page {
        final int source, rotation;
        Page(int source, int rotation) {
            if (source < 0 || rotation % 90 != 0) throw new IllegalArgumentException("Invalid page edit.");
            this.source = source; this.rotation = Math.floorMod(rotation, 360);
        }
    }
    private final ArrayList<Page> pages = new ArrayList<>();
    private final ArrayList<List<Page>> history = new ArrayList<>();
    PdfPagePlan(int count) {
        if (count < 1) throw new IllegalArgumentException("This PDF has no pages.");
        for (int i = 0; i < count; i++) pages.add(new Page(i, 0));
    }
    PdfPagePlan(List<Page> saved, int sourceCount) {
        if (saved.isEmpty()) throw new IllegalArgumentException("Keep at least one page.");
        for (Page page : saved) {
            if (page.source >= sourceCount) throw new IllegalArgumentException("The source PDF has changed.");
            pages.add(page);
        }
    }
    List<Page> snapshot() { return Collections.unmodifiableList(new ArrayList<>(pages)); }
    int size() { return pages.size(); }
    Page get(int index) { return pages.get(index); }
    boolean canUndo() { return !history.isEmpty(); }
    private void remember() {
        if (history.size() == 20) history.remove(0);
        history.add(new ArrayList<>(pages));
    }
    void move(int from, int to) {
        if (from < 0 || from >= size() || to < 0 || to >= size() || from == to) return;
        remember(); pages.add(to, pages.remove(from));
    }
    void rotate(int index) { Page page = pages.get(index); remember(); pages.set(index, new Page(page.source, page.rotation + 90)); }
    void duplicate(int index) { Page page = pages.get(index); remember(); pages.add(index + 1, new Page(page.source, page.rotation)); }
    void remove(int index) {
        if (size() <= 1) throw new IllegalArgumentException("Keep at least one page.");
        pages.get(index); remember(); pages.remove(index);
    }
    void undo() { if (canUndo()) { pages.clear(); pages.addAll(history.remove(history.size() - 1)); } }
}
