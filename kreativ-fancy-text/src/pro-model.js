import { clone, DEFAULT, sanitize } from './model.js';

const template = (id, name, category, text, theme, baseStyle, ink, from, to, frame, format = 'square', align = 'center') => ({
  id, name, category, text,
  look: { theme, baseStyle, ink, background: { kind: from === to ? 'solid' : 'linear', from, to, angle: 135 }, frame, format, align, size: 110, scale: 1, transparent: false },
});
export const TEMPLATES = [
  template('quiet-thought', 'Quiet thought', 'Quotes', 'Less noise.\nMore meaning.', 'ink', 'mono', '#24352b', '#f4f0e6', '#f4f0e6', 'double'),
  template('lilac-letter', 'Lilac letter', 'Names', 'A little\nAlexandra', 'lilac', 'script', '#422b68', '#f4eafb', '#d4c1ed', 'arch'),
  template('golden-note', 'Golden note', 'Greetings', 'You make\nordinary days\nfeel golden.', 'sunset', 'italic', '#673323', '#fff0ca', '#f3b685', 'corners', 'portrait'),
  template('midnight-muse', 'Midnight muse', 'Quotes', 'Make room\nfor your magic.', 'night', 'sans_bold', '#e4fba3', '#162b2b', '#243955', 'line'),
  template('rose-postcard', 'Rose postcard', 'Greetings', 'A little love,\njust for you.', 'rose', 'script', '#723247', '#fce7ed', '#edc9d6', 'double'),
  template('studio-statement', 'Studio statement', 'Social', 'Small words.\nBig energy.', 'studio', 'bold', '#292638', '#eeecdf', '#d5ccec', 'corners'),
  template('ocean-pause', 'Ocean pause', 'Quotes', 'Breathe in.\nBegin again.', 'ink', 'sans', '#153f4d', '#d9f0ee', '#a9cddd', 'line', 'portrait'),
  template('birthday-spark', 'Birthday spark', 'Greetings', 'Here’s to\nyour next\nbeautiful chapter.', 'sunset', 'bold_italic', '#5c283d', '#ffe6c1', '#f0bdd5', 'arch', 'story'),
  template('name-in-bloom', 'Name in bloom', 'Names', 'Isabella', 'lilac', 'script', '#654568', '#fff3ee', '#e4d3eb', 'corners'),
  template('after-hours', 'After-hours note', 'Social', 'Stay curious.\nStay a little wild.', 'night', 'mono', '#f9e6bd', '#282339', '#4d3155', 'double', 'story'),
  template('thank-you', 'A proper thank-you', 'Greetings', 'For showing up.\nFor being you.\nThank you.', 'ink', 'italic', '#364234', '#f5f0df', '#dde3ce', 'line', 'portrait'),
  template('fresh-start', 'Fresh start', 'Social', 'Something good\nis taking shape.', 'studio', 'sans_bold', '#284137', '#e9efcf', '#c5dfd3', 'arch'),
];
export const PRESET_LIMIT = 30;
const LOOK_KEYS = ['theme', 'baseStyle', 'ink', 'background', 'frame', 'format', 'transparent', 'scale', 'size', 'align', 'signature'];
export function requiresPro(design) {
  return !!design.background || !!design.ink || (design.frame != null && design.frame !== 'theme');
}
export function freeLook(design) {
  return sanitize({ ...design, ink: null, background: null, frame: 'theme' });
}
export function lookFrom(design) {
  const clean = sanitize(design);
  return Object.fromEntries(LOOK_KEYS.map(key => [key, clone(clean[key])]));
}
export function applyLook(design, look) {
  // A preset describes a look, never text or positional word overrides.
  const safeLook = lookFrom({ ...DEFAULT, ...look, text: DEFAULT.text, overrides: {} });
  return sanitize({ ...design, ...safeLook, text: design.text, overrides: {} });
}
export function applyTemplate(design, template, keepWords = true) {
  const text = keepWords && design.text.trim() ? design.text : template.text;
  return applyLook({ ...design, text }, { ...template.look, signature: design.signature });
}
export function parsePresets(raw) {
  if (!Array.isArray(raw)) return [];
  const seen = new Set();
  return raw.filter(item => {
    if (!item || typeof item.id !== 'string' || item.id.length > 100 || seen.has(item.id) || typeof item.name !== 'string' || !item.name.trim() || !item.look || typeof item.look !== 'object') return false;
    seen.add(item.id); return true;
  }).slice(0, PRESET_LIMIT).map(item => ({ id: item.id, name: item.name.trim().slice(0, 40), look: lookFrom({ ...DEFAULT, ...item.look }) }));
}
