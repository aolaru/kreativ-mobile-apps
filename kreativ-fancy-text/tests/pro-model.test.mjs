import test from 'node:test';
import assert from 'node:assert/strict';
import { DEFAULT, sanitize, parseLibrary } from '../src/model.js';
import { TEMPLATES, lookFrom, applyLook, applyTemplate, parsePresets, requiresPro, freeLook } from '../src/pro-model.js';

test('legacy designs remain free and keep their original words, style and exports', () => {
  const legacy = { text: 'Ștefan 🌸', theme: 'lilac', baseStyle: 'script', overrides: { 0: { color: '#ff0000' } }, scale: 2, transparent: true };
  const restored = parseLibrary([{ id: 'old', design: legacy }])[0].design;
  assert.equal(requiresPro(restored), false); assert.equal(restored.text, legacy.text); assert.equal(restored.scale, 2); assert.equal(restored.transparent, true);
  assert.equal(restored.overrides[0].color, '#ff0000');
});
test('templates preserve personal words by default, use sample words explicitly, and are independent snapshots', () => {
  assert.equal(TEMPLATES.length, 12); assert.equal(new Set(TEMPLATES.map(t => t.id)).size, 12);
  for (const template of TEMPLATES) {
    const original = sanitize({ ...DEFAULT, text: 'My own words', signature: '@mine', overrides: { 0: { color: '#ff0000' } } });
    const applied = applyTemplate(original, template);
    assert.equal(applied.text, 'My own words'); assert.equal(applied.signature, '@mine'); assert.deepEqual(applied.overrides, {});
    assert(requiresPro(applied)); assert.equal(applyTemplate(original, template, false).text, template.text);
    applied.background.from = '#000000'; assert.notEqual(template.look.background.from, '#000000');
    const free = freeLook(applied); assert(!requiresPro(free)); assert.equal(free.text, original.text); assert.equal(free.signature, '@mine');
  }
});
test('style presets cannot transfer old words or positional overrides to a different message', () => {
  const first = applyTemplate(DEFAULT, TEMPLATES[1]); first.signature = '@brand'; first.overrides = { 0: { color: '#ff0000' } };
  const look = lookFrom(first); assert(!Object.hasOwn(look, 'text')); assert(!Object.hasOwn(look, 'overrides'));
  const second = applyLook({ ...DEFAULT, text: 'Different message', overrides: { 2: { style: 'bold' } } }, { ...look, text: 'Injected text', overrides: { 0: { style: 'tiny' } } });
  assert.equal(second.text, 'Different message'); assert.deepEqual(second.overrides, {}); assert.equal(second.signature, '@brand');
  second.background.from = '#112233'; assert.notEqual(look.background.from, second.background.from);
});
test('malformed backgrounds and saved presets cannot inject unsupported rendering settings', () => {
  const design = sanitize({ background: { kind: 'url', from: 'javascript:alert(1)', to: '#123456' }, frame: 'bad', ink: 'red' });
  assert.equal(design.background, null); assert.equal(design.ink, null); assert.equal(design.frame, 'theme');
  assert.equal(sanitize({ background: { kind: 'linear', from: '#112233', to: '#abcdef', angle: 999 } }).background.angle, 360);
  const presets = parsePresets([null, { id: 'same', name: '  My look  ', look: { background: { kind: 'solid', from: '#112233', to: '#112233' } } }, { id: 'same', name: 'duplicate', look: {} }, { id: 'bad', name: '', look: {} }]);
  assert.equal(presets.length, 1); assert.equal(presets[0].name, 'My look'); assert(!Object.hasOwn(presets[0].look, 'text'));
  assert.equal(parsePresets(Array.from({ length: 50 }, (_, i) => ({ id: String(i), name: String(i), look: {} }))).length, 30);
});
