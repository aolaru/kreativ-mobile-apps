import '@fontsource/dm-sans/latin-400.css';
import '@fontsource/dm-sans/latin-500.css';
import '@fontsource/dm-sans/latin-600.css';
import '@fontsource/dm-sans/latin-700.css';
import './style.css';
import './editor.css';
import './pro.css';
import { version } from '../package.json';
import privacyPolicy from './privacy-policy.txt?raw';
import { DEFAULT, STYLES, THEMES, FORMATS, LIMIT, clone, sanitize, tokens, themeFor, outputFor, runsFor, editText, parseLibrary } from './model.js';
import { drawDesign, loadDesignFonts, pngFor } from './render.js';
import { readSaved, writeSaved, copyText, shareText, saveImage, shareImage } from './platform.js';
import { createHistory } from './history.js';
import { mountProStudio } from './pro-ui.js';
import { requiresPro, freeLook } from './pro-model.js';

const paths = {
  sparkle: '<path d="m12 3 2.4 6.6L21 12l-6.6 2.4L12 21l-2.4-6.6L3 12l6.6-2.4L12 3Z"/>',
  heart: '<path d="M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.7l-1.1-1.1a5.5 5.5 0 0 0-7.8 7.8L12 21l8.8-8.6a5.5 5.5 0 0 0 0-7.8Z"/>',
  arrow: '<path d="M5 12h14m-6-6 6 6-6 6"/>',
  copy: '<rect x="8" y="8" width="12" height="13" rx="2"/><path d="M16 8V5a2 2 0 0 0-2-2H5a2 2 0 0 0-2 2v9a2 2 0 0 0 2 2h3"/>',
  download: '<path d="M12 3v12m-5-5 5 5 5-5M4 16v5h16v-5"/>',
  share: '<circle cx="18" cy="5" r="3"/><circle cx="6" cy="12" r="3"/><circle cx="18" cy="19" r="3"/><path d="m9 10 6-4m-6 8 6 4"/>',
  check: '<path d="m5 12 4 4L19 6"/>',
  close: '<path d="m6 6 12 12M6 18 18 6"/>',
  shield: '<path d="m12 3 8 3v6c0 5-8 9-8 9s-8-4-8-9V6l8-3Z"/><path d="m8 12 3 3 5-6"/>',
  trash: '<path d="M3 6h18M9 6V3h6v3M5 6l1 15h12l1-15M10 10v7m4-7v7"/>',
  undo: '<path d="M9 5 4 10l5 5M4 10h10a6 6 0 0 1 0 12"/>',
  redo: '<path d="m15 5 5 5-5 5m5-5H10a6 6 0 0 0 0 12"/>',
};
const icon = name => `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${paths[name]}</svg>`;
const $ = selector => document.querySelector(selector);
const make = (tag, className, text) => { const el = document.createElement(tag); el.className = className; if (text !== undefined) el.textContent = text; return el; };
const scrollBehavior = () => matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth';
let state = sanitize(await readSaved('kreativ-draft-v1'));
let library = parseLibrary(await readSaved('kreativ-favorites-v1'));
const initialPresets = await readSaved('kreativ-style-presets-v1');
let proUI;
const history = createHistory(state);
const removedDesigns = [];
let selection = null, generation = 0, saveTimer, toastTimer, writeQueue = Promise.resolve(), busy = false;
let previewPending = true, previewFits = true;

$('#app').innerHTML = `
  <header class="app-header"><a class="brand" href="#" aria-label="Kreativ home"><span class="brand-mark">k<span>✦</span></span><span>kreativ<span class="brand-dot">.</span></span></a>
    <span class="edition">YOUR POCKET TYPOGRAPHY STUDIO</span><button id="privacy" class="offline-pill">${icon('shield')}<span>Offline by design</span></button></header>
  <main>
    <section class="intro"><div><p class="eyebrow">A LITTLE TEXT. A LOT OF YOU.</p><h1>Make your words<br> <em>feel like you.</em><span class="headline-star">✳</span></h1><p class="intro-copy">From a little thought to something worth sharing.</p></div><div class="intro-note"><span class="scribble">Aa</span><span>Type it. Style it. Make it yours.</span></div></section>
    <nav class="view-tabs" aria-label="Workspace"><button id="create-tab" class="active" aria-current="page">${icon('sparkle')}Create</button><button id="saved-tab">${icon('heart')}Saved designs <span id="saved-count">0</span></button><span class="local-note">Made here. Stays here.</span><button id="jump-preview" class="jump-preview">Preview ↓</button></nav>
    <div id="create-view" class="workspace">
      <section class="editor" aria-label="Design editor">
        <div class="section-heading"><span class="step">01</span><h2>Your words</h2><button id="clear" class="text-button">Clear</button></div>
        <label class="sr-only" for="text-input">Your text</label><textarea id="text-input" maxlength="${LIMIT}" placeholder="A thought, a name, a little hello…" spellcheck="false"></textarea>
        <div class="input-meta"><span id="draft-status">Saved on this device</span><span id="char-count"></span></div>
        <div class="history-actions" role="group" aria-label="Edit history"><button id="undo" class="text-button" aria-keyshortcuts="Control+Z Meta+Z" disabled>${icon('undo')}Undo</button><button id="redo" class="text-button" aria-keyshortcuts="Control+Shift+Z Meta+Shift+Z Control+Y" disabled>${icon('redo')}Redo</button></div>
        <div class="word-heading"><p>Make a word your own</p><button id="all-words" class="text-button">Style whole text</button></div>
        <p class="hint">Tap a word to give it its own style or color.</p><div id="word-chips" class="word-chips" aria-label="Select a word to style"></div>
        <div class="style-controls"><div class="field grow"><label for="word-style" id="style-label">Text style</label><select id="word-style">${STYLES.map(([value, label]) => `<option value="${value}">${label}</option>`).join('')}</select></div><div class="field color-field"><label for="word-color">Word color</label><input type="color" id="word-color" value="#6961c9" disabled></div><button id="reset-word" class="icon-button reset-word" title="Reset selected word" aria-label="Reset selected word" disabled>${icon('close')}</button></div>
        <p class="color-hint hint" id="selection-hint">Select a word to change its color.</p>
        <div class="section-heading theme-heading"><span class="step">02</span><h2>Find your feeling</h2><span class="small-note">6 curated looks</span></div>
        <div id="themes" class="theme-grid" aria-label="Design themes"></div>
        <details class="fine-tune"><summary>Make it just right <span>Size, alignment & signature</span></summary><div class="fine-fields"><label for="font-size">Text size <output id="size-value"></output></label><input id="font-size" type="range" min="40" max="160" step="2"><div class="field"><label for="alignment">Alignment</label><select id="alignment"><option value="left">Left</option><option value="center">Center</option><option value="right">Right</option></select></div><div class="field"><label for="signature">Your signature <span class="muted">Optional</span></label><input id="signature" maxlength="60" placeholder="@yourname or a little sign-off"></div></div></details>
      </section>
      <section class="result" aria-label="Matching text and image">
        <div class="section-heading"><span class="step">03</span><h2>Your little masterpiece</h2><button id="favorite" class="icon-button" aria-label="Save design to favorites" title="Save design">${icon('heart')}</button></div>
        <div class="preview-mat"><div class="preview-frame"><canvas id="preview" role="img" aria-label="Image preview of your design"></canvas></div><span class="preview-label">WORDS, WITH PERSONALITY.</span></div>
        <div class="text-result"><div class="text-result-title"><span>THE MATCHING TEXT</span><span class="text-only-tag">Copy & paste</span></div><p id="text-output" dir="auto"></p><div class="text-actions"><button id="copy-text" class="text-button">${icon('copy')}Copy text</button><button id="share-text" class="text-button">${icon('share')}Share text</button></div></div>
        <p class="output-note">Letters copy with your text. Colors and artwork stay in the image.</p>
        <div class="export-panel"><div class="export-heading"><h3>Ready for the world</h3><span id="dimensions"></span></div><div id="formats" class="format-options" aria-label="Image format"></div>
          <div class="export-options"><label class="toggle-label"><input type="checkbox" id="transparent"><span class="switch"></span>Transparent background</label><label class="resolution-label" for="resolution">Resolution<select id="resolution"><option value="1">Standard · 1×</option><option value="2">High · 2×</option></select></label></div>
          <p class="hint" id="fit-note" role="status" aria-live="polite"></p><div class="export-actions"><button id="save-png" class="primary-button" aria-describedby="fit-note">${icon('download')}Save PNG</button><button id="share-png" class="secondary-button" aria-describedby="fit-note">${icon('share')}Share image</button></div>
        </div>
      </section>
    </div>
    <section id="saved-view" hidden><div class="saved-heading"><div><h2>A collection of you.</h2><p>Your favorite designs, ready for another moment.</p></div><span class="small-note">Stored only on this device</span></div><div id="delete-recovery" class="recovery-banner" hidden><p id="delete-status" role="status" aria-live="polite"></p><button id="undo-delete" class="text-button">${icon('undo')}Undo delete</button><button id="dismiss-recovery" class="icon-button" aria-label="Dismiss deletion recovery">${icon('close')}</button></div><div id="saved-grid" class="saved-grid"></div></section>
    <footer><span>Small words. Endless possibilities.</span><span>Kreativ <span class="footer-flower">✳</span> Made for expression.</span></footer>
  </main>
  <nav id="quick-actions" class="quick-actions" aria-label="Quick design actions"><button id="quick-preview" class="text-button">Preview ${icon('arrow')}</button><button id="quick-copy" class="secondary-button" aria-label="Copy styled text">${icon('copy')}Copy text</button><button id="quick-save" class="primary-button" aria-label="Save design as PNG" aria-describedby="fit-note">${icon('download')}Save PNG</button></nav>
  <dialog id="privacy-dialog" aria-labelledby="privacy-title"><button class="icon-button dialog-close" aria-label="Close privacy information">${icon('close')}</button><span class="dialog-symbol">${icon('shield')}</span><h2 id="privacy-title">Your words are yours.</h2><p>Text, styling, and PNG creation work entirely on this device. Fonts and artwork are bundled with the app. No account, ads, analytics, or network connection needed.</p><p>Your draft and favorites stay on this device. Sharing sends only the text or image you choose to the app you select. Uninstalling the app removes its saved designs.</p><details class="privacy-policy"><summary>Read the full privacy policy</summary><pre id="privacy-policy-text"></pre></details><p>Support: <a href="mailto:info@madebykreativ.com">info@madebykreativ.com</a></p><p class="hint">Kreativ Fancy Text · ${version}</p><button class="primary-button dialog-done">Back to creating</button></dialog>
`;
$('#privacy-policy-text').textContent = privacyPolicy;
function toast(message) {
  if (!message) return;
  $('#toast').textContent = message; $('#toast').classList.add('visible');
  clearTimeout(toastTimer); toastTimer = setTimeout(() => $('#toast').classList.remove('visible'), 4500);
}
function persist(key, value) {
  const snapshot = clone(value);
  writeQueue = writeQueue.then(async () => {
    try { await writeSaved(key, snapshot); return true; }
    catch {
      $('#draft-status').textContent = 'Could not save on this device';
      toast('Device storage is unavailable or full. Your current design is still open.');
      return false;
    }
  });
  return writeQueue;
}
function saveDraft() {
  $('#draft-status').textContent = 'Saving…';
  clearTimeout(saveTimer);
  saveTimer = setTimeout(async () => {
    const saved = await persist('kreativ-draft-v1', state);
    if (saved && $('#draft-status').textContent === 'Saving…') $('#draft-status').textContent = 'Saved on this device';
  }, 250);
}
function renderWords() {
  const container = $('#word-chips'); container.replaceChildren();
  tokens(state.text).forEach((word, index) => {
    if (!word.trim()) return;
    const button = make('button', `word-chip${selection === index ? ' selected' : ''}${state.overrides[index] ? ' customized' : ''}`, word);
    button.setAttribute('aria-pressed', String(selection === index));
    button.setAttribute('aria-label', `Style word ${word}`);
    button.onclick = () => { selection = selection === index ? null : index; renderWords(); renderControls(); };
    container.append(button);
  });
  if (!state.text.trim()) container.append(make('span', 'empty-words', 'Your words will appear here.'));
}
function renderControls() {
  const part = selection === null ? null : tokens(state.text)[selection];
  $('#style-label').textContent = part ? `Style: ${part}` : 'Text style';
  $('#word-style').value = (part && state.overrides[selection]?.style) || state.baseStyle;
  $('#word-color').disabled = !part;
  $('#reset-word').disabled = !part || !state.overrides[selection];
  $('#word-color').value = (part && state.overrides[selection]?.color) || state.ink || themeFor(state).ink;
  $('#selection-hint').textContent = part ? `Styling “${part}” only. Colors appear in the image.` : 'Select a word to change its color.';
  $('#all-words').classList.toggle('is-selected', !part);
  $('#font-size').value = state.size; $('#size-value').textContent = state.size;
  $('#alignment').value = state.align; $('#signature').value = state.signature;
  $('#transparent').checked = state.transparent; $('#resolution').value = state.scale;
  $('#char-count').textContent = `${state.text.length} / ${LIMIT}`;
  for (const button of $('#themes').children) button.setAttribute('aria-pressed', String(button.dataset.theme === state.theme));
  for (const button of $('#formats').children) button.setAttribute('aria-pressed', String(button.dataset.format === state.format));
  const format = FORMATS[state.format];
  $('#dimensions').textContent = `${format.width * state.scale} × ${format.height * state.scale} px`;
  const match = library.some(item => JSON.stringify(item.design) === JSON.stringify(state));
  $('#favorite').classList.toggle('is-saved', match); $('#favorite').setAttribute('aria-pressed', String(match));
  $('#saved-count').textContent = library.length;
  const empty = !state.text.trim();
  for (const id of ['copy-text', 'share-text', 'quick-copy', 'favorite']) $(`#${id}`).disabled = busy || empty;
  updateImageControls();
  $('#undo').disabled = busy || !history.canUndo; $('#redo').disabled = busy || !history.canRedo;
  for (const button of document.querySelectorAll('.restore-design, .remove-design, #undo-delete')) button.disabled = busy;
  const text = $('#text-output'); text.replaceChildren();
  for (const run of runsFor(state)) { const span = make('span', '', run.text); text.append(span); }
  if (empty) text.textContent = 'A little inspiration starts with a word.';
  $('#preview').setAttribute('aria-label', `Image preview: ${state.text || 'Empty design'}`);
  proUI?.refresh();
}
function updateImageControls() {
  for (const id of ['save-png', 'share-png', 'quick-save']) $(`#${id}`).disabled = busy || !state.text.trim() || previewPending || !previewFits || (requiresPro(state) && !proUI?.owned());
}
async function renderPreview() {
  const id = ++generation, snapshot = clone(state);
  await loadDesignFonts(snapshot);
  if (id !== generation) return;
  const result = drawDesign($('#preview'), snapshot);
  previewPending = false; previewFits = result.fits;
  $('#fit-note').textContent = !result.fits ? 'Too many lines for this image. Remove some line breaks or choose a taller format. You can still copy your text.' : result.actualSize < state.size ? 'Text was reduced to fit the image. Your full message is included.' : '';
  $('#fit-note').classList.toggle('fit-warning', !result.fits);
  updateImageControls();
}
function refresh({ words = false, save = true } = {}) {
  previewPending = true;
  if (words) renderWords();
  renderControls(); renderPreview().catch(() => {
    previewPending = false; previewFits = false; updateImageControls();
    toast('Could not render the preview. Try a shorter message.');
  });
  if (save) saveDraft();
}
function changeDesign(next, group = null) {
  state = history.commit(next, group);
  refresh({ words: true });
}
function restoreHistory(direction) {
  if (busy || !(direction === 'undo' ? history.canUndo : history.canRedo)) return;
  state = history[direction](); selection = null;
  $('#text-input').value = state.text;
  refresh({ words: true });
  toast(direction === 'undo' ? 'Change undone.' : 'Change redone.');
}
$('#undo').onclick = () => restoreHistory('undo');
$('#redo').onclick = () => restoreHistory('redo');
document.addEventListener('keydown', event => {
  if (event.defaultPrevented || event.isComposing || document.querySelector('dialog[open]') || $('#create-view').hidden) return;
  if ((event.ctrlKey || event.metaKey) && !event.altKey) {
    const key = event.key.toLowerCase();
    if (key === 'z' || (key === 'y' && !event.shiftKey)) {
      event.preventDefault(); restoreHistory(event.shiftKey || key === 'y' ? 'redo' : 'undo');
    }
  }
});
$('.editor').addEventListener('focusout', () => history.breakGroup());
$('.editor').addEventListener('beforeinput', event => {
  if (event.inputType === 'historyUndo' || event.inputType === 'historyRedo') {
    event.preventDefault(); restoreHistory(event.inputType === 'historyUndo' ? 'undo' : 'redo');
  }
});
THEMES.forEach(theme => {
  const button = make('button', 'theme-card'); button.dataset.theme = theme.id;
  button.setAttribute('aria-label', `${theme.name} theme`);
  const swatch = make('span', `theme-swatch swatch-${theme.id}`); swatch.style.background = theme.background; swatch.style.color = theme.ink;
  swatch.append(make('span', 'swatch-letters', theme.id === 'lilac' ? '𝓐𝓪' : theme.id === 'ink' ? '𝙰𝚊' : 'Aa'), make('span', 'theme-check', '✓'));
  const label = make('span', 'theme-name', theme.name); const tag = make('span', 'theme-tag', theme.tag);
  button.append(swatch, label, tag);
  button.onclick = () => changeDesign({ ...freeLook(state), theme: theme.id, baseStyle: theme.style });
  $('#themes').append(button);
});
Object.entries(FORMATS).forEach(([key, format]) => {
  const button = make('button', 'format-button'); button.dataset.format = key;
  const shape = make('span', `format-shape shape-${key}`); button.append(shape, document.createTextNode(format.label));
  button.onclick = () => changeDesign({ ...state, format: key }); $('#formats').append(button);
});
$('#text-input').value = state.text;
$('#text-input').addEventListener('input', event => { selection = null; changeDesign(editText(state, event.target.value), 'typing'); });
$('#clear').onclick = () => { selection = null; $('#text-input').value = ''; changeDesign(editText(state, '')); $('#text-input').focus(); };
$('#all-words').onclick = () => { selection = null; renderWords(); renderControls(); };
$('#word-style').onchange = event => {
  const next = clone(state);
  if (selection === null) next.baseStyle = event.target.value;
  else next.overrides[selection] = { ...next.overrides[selection], style: event.target.value };
  changeDesign(next);
};
$('#word-color').oninput = event => {
  if (selection === null) return;
  const next = clone(state);
  next.overrides[selection] = { ...next.overrides[selection], color: event.target.value };
  changeDesign(next, `color-${selection}`);
};
$('#reset-word').onclick = () => { const next = clone(state); delete next.overrides[selection]; changeDesign(next); };
$('#font-size').oninput = event => changeDesign({ ...state, size: +event.target.value }, 'size');
$('#alignment').onchange = event => changeDesign({ ...state, align: event.target.value });
$('#signature').oninput = event => changeDesign({ ...state, signature: event.target.value }, 'signature');
$('#transparent').onchange = event => changeDesign({ ...state, transparent: event.target.checked });
$('#resolution').onchange = event => changeDesign({ ...state, scale: +event.target.value });
async function action(fn, focusAfter = null) {
  if (busy) return;
  busy = true; renderControls();
  try { toast(await fn()); }
  catch (error) {
    if (error?.name !== 'AbortError' && !/cancel/i.test(error?.message || '')) toast(error?.message || 'That did not work. Please try again.');
  } finally { busy = false; renderControls(); if (focusAfter) $(focusAfter)?.focus(); }
}
$('#copy-text').onclick = () => action(async () => { await copyText(outputFor(state)); return 'Styled text copied. Make someone’s day.'; });
$('#share-text').onclick = () => action(() => shareText(outputFor(state)));
$('#save-png').onclick = () => { const snapshot = clone(state); if (requiresPro(snapshot) && !proUI?.owned()) { proUI.openPaywall(); return; } action(async () => saveImage((await pngFor(snapshot)).blob)); };
$('#share-png').onclick = () => { const snapshot = clone(state); if (requiresPro(snapshot) && !proUI?.owned()) { proUI.openPaywall(); return; } action(async () => shareImage((await pngFor(snapshot)).blob)); };
$('#quick-copy').onclick = () => $('#copy-text').click();
$('#quick-save').onclick = () => $('#save-png').click();
$('#favorite').onclick = () => action(async () => {
  if (library.some(item => JSON.stringify(item.design) === JSON.stringify(state))) { toast('This design is already in your favorites.'); return; }
  if (library.length >= 50) { toast('Your collection is full. Remove a saved design to make room.'); return; }
  const next = [{ id: crypto.randomUUID(), design: clone(state), savedAt: new Date().toISOString() }, ...library];
  if (await persist('kreativ-favorites-v1', next)) { library = next; return 'A keeper. Saved to your designs.'; }
});
function renderRecovery() {
  $('#delete-recovery').hidden = removedDesigns.length === 0;
  $('#delete-status').textContent = removedDesigns.length === 1 ? 'Saved design removed.' : `${removedDesigns.length} saved designs removed. Undo restores the most recent.`;
}
$('#undo-delete').onclick = () => action(async () => {
  if (!removedDesigns.length) return;
  if (library.length >= 50) return 'Your collection is full. Remove another saved design before restoring this one.';
  const { item, index } = removedDesigns.at(-1), next = [...library];
  next.splice(Math.min(index, next.length), 0, item);
  if (await persist('kreativ-favorites-v1', next)) {
    library = next; removedDesigns.pop(); renderRecovery();
    await renderLibrary(); $('#saved-tab').focus();
    return 'Saved design restored.';
  }
});
$('#dismiss-recovery').onclick = () => { removedDesigns.length = 0; renderRecovery(); $('#saved-tab').focus(); };
let libraryGeneration = 0;
async function renderLibrary() {
  const id = ++libraryGeneration, grid = $('#saved-grid'); grid.replaceChildren();
  if (!library.length) {
    const empty = make('div', 'empty-library');
    empty.innerHTML = `${icon('heart')}<h3>Keep a little inspiration.</h3><p>Tap the heart on a design to save it here.</p><button class="primary-button">Create your first favorite ${icon('arrow')}</button>`;
    empty.querySelector('button').onclick = () => switchView(false); grid.append(empty); return;
  }
  for (const item of library) {
    const card = make('article', 'saved-card'), restore = make('button', 'restore-design');
    restore.setAttribute('aria-label', `Open saved design: ${item.design.text}`);
    const canvas = make('canvas', 'saved-canvas'); restore.append(canvas);
    const caption = make('span', 'saved-title', item.design.text); restore.append(caption);
    restore.onclick = () => {
      selection = null; $('#text-input').value = item.design.text;
      changeDesign(sanitize(item.design)); switchView(false); $('#create-tab').focus();
      window.scrollTo({ top: 0, behavior: scrollBehavior() }); toast('Your design is back on the canvas.');
    };
    const bottom = make('div', 'saved-card-bottom'); bottom.append(make('span', 'small-note', themeFor(item.design).name));
    const remove = make('button', 'icon-button remove-design'); remove.innerHTML = icon('trash'); remove.setAttribute('aria-label', `Delete saved design: ${item.design.text}`);
    remove.onclick = () => action(async () => {
      const index = library.findIndex(entry => entry.id === item.id);
      if (index < 0) return;
      const next = library.filter(entry => entry.id !== item.id);
      if (await persist('kreativ-favorites-v1', next)) {
        library = next; removedDesigns.push({ item, index }); renderRecovery();
        await renderLibrary();
        return 'Removed from saved designs. Undo delete is available.';
      }
    }, '#undo-delete');
    bottom.append(remove); card.append(restore, bottom); grid.append(card);
    await loadDesignFonts(item.design);
    if (id !== libraryGeneration) return;
    drawDesign(canvas, item.design, { scale: 0.3 });
  }
}
function switchView(saved) {
  $('#create-view').hidden = saved; $('#saved-view').hidden = !saved;
  $('#create-tab').classList.toggle('active', !saved); $('#saved-tab').classList.toggle('active', saved);
  $('#create-tab').setAttribute('aria-current', saved ? 'false' : 'page'); $('#saved-tab').setAttribute('aria-current', saved ? 'page' : 'false');
  $('#jump-preview').hidden = saved;
  $('#quick-actions').hidden = saved;
  if (saved) renderLibrary().catch(() => toast('Could not load a saved preview. You can still reopen the design.'));
}
$('#jump-preview').onclick = () => {
  const atPreview = $('.result').getBoundingClientRect().top < 150;
  (atPreview ? $('.editor') : $('.result')).scrollIntoView({ behavior: scrollBehavior(), block: 'start' });
};
$('#quick-preview').onclick = () => {
  $('.result').scrollIntoView({ behavior: scrollBehavior(), block: 'start' });
  $('.result').focus({ preventScroll: true });
};
$('.result').tabIndex = -1;
new ResizeObserver(() => {
  document.documentElement.style.setProperty('--quick-actions-height', `${$('#quick-actions').offsetHeight}px`);
}).observe($('#quick-actions'));
new IntersectionObserver(entries => {
  $('#jump-preview').textContent = entries[0].isIntersecting ? 'Edit ↑' : 'Preview ↓';
}, { threshold: 0.1 }).observe($('.result'));
$('#create-tab').onclick = () => switchView(false); $('#saved-tab').onclick = () => switchView(true);
$('.brand').onclick = event => { event.preventDefault(); switchView(false); window.scrollTo({ top: 0, behavior: scrollBehavior() }); };
$('#privacy').onclick = () => $('#privacy-dialog').showModal();
$('.dialog-close').onclick = $('.dialog-done').onclick = () => $('#privacy-dialog').close();
// Flush a draft when Android backgrounds the WebView, as well as after normal editing.
document.addEventListener('visibilitychange', () => { if (document.hidden) { clearTimeout(saveTimer); persist('kreativ-draft-v1', state); } });
proUI = mountProStudio({ getDesign: () => state, changeDesign: next => { selection = null; $('#text-input').value = next.text; changeDesign(next); }, persist, initialPresets, toast, statusChanged: updateImageControls });
$('#free-look').onclick = () => { changeDesign(freeLook(state)); toast('Your words are ready with a free look.'); };
refresh({ words: true, save: false });
