package com.kreativ.paperbuild;

import org.junit.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;

public class PdfPagePlanTest {
    @Test public void editsKeepSourceIdentityAndUndoRestoresEachStep() {
        PdfPagePlan plan = new PdfPagePlan(3);
        plan.move(2, 0); assertEquals(2, plan.get(0).source);
        plan.rotate(0); plan.duplicate(0); plan.remove(2);
        assertEquals(3, plan.size()); assertEquals(2, plan.get(1).source); assertEquals(90, plan.get(1).rotation);
        plan.undo(); assertEquals(4, plan.size()); assertEquals(0, plan.get(2).source);
        plan.undo(); assertEquals(3, plan.size());
        plan.undo(); assertEquals(0, plan.get(0).rotation);
        plan.undo(); assertEquals(0, plan.get(0).source); assertFalse(plan.canUndo());
    }
    @Test public void snapshotCannotChangeDuringExportAndRotationWraps() {
        PdfPagePlan plan = new PdfPagePlan(2); List<PdfPagePlan.Page> exporting = plan.snapshot();
        for (int i = 0; i < 5; i++) plan.rotate(0);
        plan.move(0, 1); assertEquals(90, plan.get(1).rotation); assertEquals(0, exporting.get(0).rotation);
        try { exporting.clear(); fail(); } catch (UnsupportedOperationException expected) { }
        assertEquals(270, new PdfPagePlan.Page(0, -90).rotation);
    }
    @Test public void neverAllowsAnEmptyOrInvalidRestoredDocument() {
        PdfPagePlan plan = new PdfPagePlan(1);
        try { plan.remove(0); fail(); } catch (IllegalArgumentException expected) { }
        assertEquals(1, plan.size()); assertFalse(plan.canUndo());
        try { new PdfPagePlan(Arrays.asList(new PdfPagePlan.Page(2, 0)), 2); fail(); } catch (IllegalArgumentException expected) { }
        try { new PdfPagePlan.Page(0, 45); fail(); } catch (IllegalArgumentException expected) { }
        plan.move(0, 1); assertFalse(plan.canUndo());
    }
}
