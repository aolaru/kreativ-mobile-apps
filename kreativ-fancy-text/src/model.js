import { translate } from './unicode.js';

export const STYLES = [
  ['plain', 'Original'], ['bold', 'Bold serif'], ['italic', 'Italic serif'],
  ['bold_italic', 'Bold italic'], ['sans', 'Sans'], ['sans_bold', 'Bold sans'],
  ['script', 'Script'], ['fraktur', 'Gothic'], ['double', 'Double-struck'],
  ['mono', 'Monospace'], ['bubble', 'Bubble'], ['small_caps', 'Small caps'],
  ['tiny', 'Tiny'], ['vaporwave', 'Fullwidth'], ['upsidedown', 'Upside down'],
  ['mirror', 'Mirror'], ['runic', 'Runic'],
];
export const THEMES = [
  { id: 'studio', name: 'The original', tag: 'A little editorial', background: '#eeecdf', ink: '#2e302c', accent: '#6961c9', style: 'bold', family: 'serif', ornament: 'studio' },
  { id: 'lilac', name: 'Lilac daydream', tag: 'Soft & expressive', background: '#dcd3f4', ink: '#32274d', accent: '#765299', style: 'script', family: 'serif', ornament: 'petals' },
  { id: 'night', name: 'After hours', tag: 'A neon state of mind', background: '#172729', ink: '#e4fba3', accent: '#96d5c6', style: 'sans_bold', family: 'sans-serif', ornament: 'grid' },
  { id: 'rose', name: 'Love notes', tag: 'Say it softly', background: '#f5dce0', ink: '#723247', accent: '#b44865', style: 'italic', family: 'serif', ornament: 'heart' },
  { id: 'ink', name: 'Ink & paper', tag: 'Less, beautifully', background: '#f4f3ed', ink: '#212526', accent: '#6b6b65', style: 'mono', family: 'monospace', ornament: 'lines' },
  { id: 'sunset', name: 'Golden hour', tag: 'Made of warm moments', background: '#f3c6a3', ink: '#673323', accent: '#ae6533', style: 'sans_bold', family: 'sans-serif', ornament: 'sun' },
];
export const FORMATS = {
  square: { label: 'Square', width: 1080, height: 1080 },
  portrait: { label: 'Portrait', width: 1080, height: 1350 },
  story: { label: 'Story', width: 1080, height: 1920 },
};
export const LIMIT = 1200;
export const DEFAULT = {
  text: 'Create something\nwonderful.', theme: 'studio', baseStyle: 'bold',
  overrides: {}, format: 'square', transparent: false, scale: 1, size: 110,
  align: 'center', signature: '',
  ink: null, background: null, frame: 'theme',
};
export const clone = value => JSON.parse(JSON.stringify(value));
export const tokens = text => text.match(/\s+|[^\s]+/gu) || [];
export const themeFor = state => THEMES.find(t => t.id === state.theme) || THEMES[0];
const validStyle = value => STYLES.some(([key]) => key === value);
const validColor = value => /^#[0-9a-f]{6}$/i.test(value);
export function sanitize(input) {
  const state = clone(DEFAULT);
  if (!input || typeof input !== 'object') return state;
  if (typeof input.text === 'string') state.text = input.text.slice(0, LIMIT);
  if (THEMES.some(t => t.id === input.theme)) state.theme = input.theme;
  if (validStyle(input.baseStyle)) state.baseStyle = input.baseStyle;
  if (Object.hasOwn(FORMATS, input.format)) state.format = input.format;
  state.transparent = input.transparent === true;
  state.scale = input.scale === 2 ? 2 : 1;
  if (Number.isFinite(input.size)) state.size = Math.max(40, Math.min(160, input.size));
  if (['left', 'center', 'right'].includes(input.align)) state.align = input.align;
  if (typeof input.signature === 'string') state.signature = input.signature.slice(0, 60);
  if (validColor(input.ink)) state.ink = input.ink;
  if (['theme', 'none', 'line', 'double', 'corners', 'arch'].includes(input.frame)) state.frame = input.frame;
  const background = input.background;
  if (background && ['solid', 'linear'].includes(background.kind) && validColor(background.from) && validColor(background.to)) {
    state.background = { kind: background.kind, from: background.from, to: background.to,
      angle: Number.isFinite(background.angle) ? Math.max(0, Math.min(360, background.angle)) : 135 };
  }
  const parts = tokens(state.text);
  if (input.overrides && typeof input.overrides === 'object') {
    for (const [key, value] of Object.entries(input.overrides)) {
      if (!/^\d+$/.test(key) || !parts[+key]?.trim() || !value || typeof value !== 'object') continue;
      const entry = {};
      if (validStyle(value.style)) entry.style = value.style;
      if (validColor(value.color)) entry.color = value.color;
      if (Object.keys(entry).length) state.overrides[key] = entry;
    }
  }
  return state;
}
export function runsFor(state) {
  const theme = themeFor(state);
  return tokens(state.text).map((raw, index) => {
    const override = state.overrides[index] || {};
    return { raw, text: raw.trim() ? translate(raw, override.style || state.baseStyle) : raw,
      color: override.color || state.ink || theme.ink, index };
  });
}
export const outputFor = state => runsFor(state).map(run => run.text).join('');
// Preserve overrides for unchanged prefix/suffix tokens when words are inserted or removed.
export function editText(state, text) {
  const before = tokens(state.text), after = tokens(text.slice(0, LIMIT));
  let prefix = 0, suffix = 0;
  while (prefix < Math.min(before.length, after.length) && before[prefix] === after[prefix]) prefix++;
  while (suffix < Math.min(before.length, after.length) - prefix && before[before.length - 1 - suffix] === after[after.length - 1 - suffix]) suffix++;
  const overrides = {};
  for (let i = 0; i < prefix; i++) if (state.overrides[i]) overrides[i] = state.overrides[i];
  for (let i = 0; i < suffix; i++) {
    const oldIndex = before.length - 1 - i, newIndex = after.length - 1 - i;
    if (state.overrides[oldIndex]) overrides[newIndex] = state.overrides[oldIndex];
  }
  // Align unchanged tokens inside a replacement too. The input limit bounds this table.
  const rows = before.length - suffix - prefix, columns = after.length - suffix - prefix;
  if (rows && columns && Object.keys(state.overrides).length) {
    const table = Array.from({ length: rows + 1 }, () => new Uint16Array(columns + 1));
    for (let i = rows - 1; i >= 0; i--) for (let j = columns - 1; j >= 0; j--) {
      table[i][j] = before[prefix + i] === after[prefix + j]
        ? 1 + table[i + 1][j + 1] : Math.max(table[i + 1][j], table[i][j + 1]);
    }
    let i = 0, j = 0;
    while (i < rows && j < columns) {
      if (before[prefix + i] === after[prefix + j]) {
        if (state.overrides[prefix + i]) overrides[prefix + j] = state.overrides[prefix + i];
        i++; j++;
      } else if (table[i + 1][j] >= table[i][j + 1]) i++;
      else j++;
    }
  }
  return { ...state, text: text.slice(0, LIMIT), overrides };
}
export function parseLibrary(raw) {
  if (!Array.isArray(raw)) return [];
  return raw.filter(item => item && typeof item.id === 'string' && item.design && typeof item.design.text === 'string')
    .slice(0, 50).map(item => ({ id: item.id, design: sanitize(item.design), savedAt: typeof item.savedAt === 'string' ? item.savedAt : '' }));
}
