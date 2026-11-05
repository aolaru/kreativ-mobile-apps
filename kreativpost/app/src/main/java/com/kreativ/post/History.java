package com.kreativ.post;
import java.util.ArrayDeque;
final class History {
    private final ArrayDeque<Project> undo=new ArrayDeque<>(), redo=new ArrayDeque<>();
    void record(Project p) { undo.addLast(p.copy()); if(undo.size()>60) undo.removeFirst(); redo.clear(); }
    Project undo(Project p) { if(undo.isEmpty()) return p; redo.addLast(p.copy()); return undo.removeLast(); }
    Project redo(Project p) { if(redo.isEmpty()) return p; undo.addLast(p.copy()); return redo.removeLast(); }
    boolean canUndo(){return !undo.isEmpty();} boolean canRedo(){return !redo.isEmpty();}
}
