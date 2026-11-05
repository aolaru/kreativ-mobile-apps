import { clone } from './model.js';

// History stays in memory; the current draft continues to use the existing storage key.
export function createHistory(initial, { limit = 60, now = () => Date.now() } = {}) {
  let current = clone(initial), past = [], future = [], lastGroup = null, lastTime = 0;
  const resetGroup = () => { lastGroup = null; };
  return {
    get canUndo() { return past.length > 0; },
    get canRedo() { return future.length > 0; },
    breakGroup: resetGroup,
    commit(next, group = null) {
      if (JSON.stringify(next) === JSON.stringify(current)) return clone(current);
      const time = now();
      if (!group || group !== lastGroup || time - lastTime > 800) {
        past.push(current);
        if (past.length > limit) past.shift();
      }
      current = clone(next); future = []; lastGroup = group; lastTime = time;
      return clone(current);
    },
    undo() {
      if (past.length) { future.push(current); current = past.pop(); }
      resetGroup(); return clone(current);
    },
    redo() {
      if (future.length) { past.push(current); current = future.pop(); }
      resetGroup(); return clone(current);
    },
  };
}
