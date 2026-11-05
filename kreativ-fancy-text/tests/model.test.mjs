import test from 'node:test';
import assert from 'node:assert/strict';
import { DEFAULT, clone, editText, outputFor, sanitize, parseLibrary } from '../src/model.js';
import { createHistory } from '../src/history.js';
test('word overrides survive insertions without moving onto the wrong word', () => {
  const original = { ...clone(DEFAULT), text: 'Hello wonderful world', baseStyle: 'plain', overrides: { 2: { style: 'bold', color: '#ff0000' } } };
  const updated = editText(original, 'Well Hello wonderful world!');
  assert.equal(updated.overrides[4].style, 'bold');
  assert.match(outputFor(updated), /𝐰𝐨𝐧𝐝𝐞𝐫𝐟𝐮𝐥/);
  const removed = editText(original, 'Hello world');
  assert.deepEqual(removed.overrides, {});
});
test('mixed styles preserve whitespace, emoji, and accents', () => {
  const design = { ...clone(DEFAULT), text: 'Ștefan\nhello 🌸', baseStyle: 'plain', overrides: { 2: { style: 'script' } } };
  assert.equal(outputFor(design), 'Ștefan\n𝓱𝓮𝓵𝓵𝓸 🌸');
});
test('damaged saved data cannot introduce unsupported settings or oversized output', () => {
  const design = sanitize({ text: 'a'.repeat(9000), scale: 99, size: Infinity, theme: '<script>', overrides: { 0: { color: 'red;bad', style: '__proto__' }, 9000: { style: 'bold' } } });
  assert.equal(design.text.length, 1200); assert.equal(design.scale, 1);
  assert.equal(design.theme, 'studio'); assert.deepEqual(design.overrides, {});
  assert.deepEqual(parseLibrary([null, { id: 1 }, { id: 'good', design: { text: 'hello' } }]).length, 1);
  assert.equal(sanitize(null).text, DEFAULT.text);
});
test('undo restores complete designs, groups continuous input, and discards a replaced redo branch', () => {
  let time = 0;
  const history = createHistory(DEFAULT, { now: () => time });
  let draft = history.commit({ ...DEFAULT, text: 'Hello' }, 'typing');
  time += 100;
  draft = history.commit({ ...draft, text: 'Hello world' }, 'typing');
  history.commit({ ...draft, theme: 'night', scale: 2, transparent: true, overrides: { 2: { color: '#ff0000', style: 'script' } } });
  assert.deepEqual(history.undo(), draft);
  assert.equal(history.undo().text, DEFAULT.text);
  assert.equal(history.canUndo, false);
  assert.equal(history.redo().text, 'Hello world');
  const styled = history.redo();
  assert.equal(styled.overrides[2].style, 'script'); assert.equal(styled.scale, 2);
  styled.overrides[2].color = '#000000';
  assert.equal(history.undo().theme, DEFAULT.theme);
  assert.equal(history.redo().overrides[2].color, '#ff0000', 'history snapshots must not share references');
  history.undo();
  history.commit({ ...draft, signature: 'new branch' });
  assert.equal(history.canRedo, false);
});
test('history respects pauses, focus boundaries and its memory limit', () => {
  let time = 0;
  const history = createHistory(DEFAULT, { limit: 2, now: () => time });
  const one = history.commit({ ...DEFAULT, text: 'one' }, 'typing');
  time = 900;
  const two = history.commit({ ...one, text: 'two' }, 'typing');
  history.breakGroup();
  history.commit({ ...two, text: 'three' }, 'typing');
  assert.equal(history.undo().text, 'two');
  assert.equal(history.undo().text, 'one');
  assert.equal(history.canUndo, false);
});
