import { DEFAULT, clone, sanitize, themeFor } from './model.js';
import { TEMPLATES, PRESET_LIMIT, applyLook, applyTemplate, lookFrom, parsePresets, requiresPro } from './pro-model.js';
import { drawDesign, loadDesignFonts } from './render.js';
import { billingState, observeBilling, startBilling, purchasePro, restorePro } from './billing.js';

const $ = selector => document.querySelector(selector);
const make = (tag, cls, text) => { const e = document.createElement(tag); e.className = cls; if (text !== undefined) e.textContent = text; return e; };
export function mountProStudio({ getDesign, changeDesign, persist, initialPresets, toast, statusChanged }) {
  let presets = parsePresets(initialPresets), removedPreset = null, candidate = null, lastLook = '', savedBusy = false;
  const section = make('details', 'pro-studio'); section.id = 'pro-studio';
  section.innerHTML = `<summary><span>Make it yours</span><span class="pro-badge">PRO</span></summary>
    <div class="pro-studio-body"><p class="hint">A starting point, a fresh background, a look to use again.</p><button id="open-pro" class="text-button">About Fancy Text Pro</button>
      <h3>Premium templates</h3><div id="template-grid" class="template-grid"></div>
      <h3>Custom background</h3><div class="background-fields">
        <label>Background<select id="background-kind"><option value="solid">Solid colour</option><option value="linear">Gradient</option></select></label>
        <label>First colour<input id="background-from" type="color" value="#eeecdf"></label>
        <label id="background-to-label">Second colour<input id="background-to" type="color" value="#d5ccec"></label>
        <label id="background-angle-label">Gradient angle<select id="background-angle"><option value="0">Across</option><option value="90">Down</option><option value="135">Diagonal</option><option value="225">Reverse diagonal</option></select></label>
        <label>Text colour<input id="design-ink" type="color" value="#2e302c"></label>
        <label>Frame<select id="design-frame"><option value="none">No frame</option><option value="line">Fine line</option><option value="double">Double line</option><option value="corners">Corners</option><option value="arch">Arch</option></select></label>
      </div><p class="hint">Word-specific colours stay as you set them. Transparent export hides the background.</p>
      <button id="preview-background" class="secondary-button">Preview background</button>
      <h3>Your style presets</h3><p class="hint">Save the look, then apply it to new words. Presets include your signature and export settings.</p>
      <form id="preset-form" class="preset-form"><label for="preset-name">Preset name</label><input id="preset-name" maxlength="40" placeholder="My everyday look" required><button id="save-preset" class="secondary-button" type="submit">Save style preset</button></form>
      <p id="preset-status" class="hint" role="status"></p><div id="preset-list" class="preset-list"></div><button id="undo-preset-delete" class="text-button" hidden>Undo preset deletion</button>
    </div>`;
  $('.fine-tune').before(section);
  const note = make('div', 'pro-design-note'); note.id = 'pro-design-note'; note.hidden = true;
  note.innerHTML = '<p>This design uses a Pro look. Your words are still copyable.</p><button id="design-unlock" class="secondary-button">Unlock Fancy Text Pro</button><button id="free-look" class="text-button">Use free look</button>';
  $('.preview-mat').before(note);
  const preview = make('dialog', 'pro-preview'); preview.id = 'pro-preview'; preview.setAttribute('aria-labelledby', 'pro-preview-title');
  preview.innerHTML = '<button class="icon-button modal-close" aria-label="Close design preview">×</button><span class="pro-kicker">A LITTLE INSPIRATION</span><h2 id="pro-preview-title">Design preview</h2><div class="pro-preview-mat"><canvas id="pro-preview-canvas" role="img" aria-label="Preview of the selected Pro design"></canvas></div><label class="keep-words"><input id="keep-template-words" type="checkbox" checked>Keep my current words</label><p class="hint">Your draft changes only when you apply this look. You can undo it.</p><button id="apply-pro-look" class="primary-button">Unlock Fancy Text Pro</button>';
  const paywall = make('dialog', 'pro-paywall'); paywall.id = 'pro-dialog'; paywall.setAttribute('aria-labelledby', 'pro-title');
  paywall.innerHTML = '<button class="icon-button modal-close" aria-label="Close Fancy Text Pro information">×</button><span class="pro-kicker">MORE ROOM TO CREATE</span><h2 id="pro-title">Fancy Text Pro</h2><p>Make it unmistakably yours. Unlock with one purchase.</p><ul><li>12 premium starting templates</li><li>Custom colours, gradients and frames</li><li>Named style presets for your next design</li></ul><p class="pro-promise">One-time purchase. No subscription. Your existing free features stay free.</p><p id="pro-billing-status" role="status"></p><button id="purchase-pro" class="primary-button" disabled>Google Play unavailable</button><button id="restore-pro" class="text-button">Restore purchase</button><button class="text-button pro-done">Back to creating</button>';
  document.body.append(preview, paywall);
  for (const dialog of [preview, paywall]) {
    dialog.querySelector('.modal-close').onclick = () => dialog.close();
    dialog.addEventListener('click', e => { if (e.target === dialog) { const r = dialog.getBoundingClientRect(); if (e.clientX < r.left || e.clientX > r.right || e.clientY < r.top || e.clientY > r.bottom) dialog.close(); } });
  }
  paywall.querySelector('.pro-done').onclick = () => paywall.close();
  function openPaywall() { if (preview.open) preview.close(); if (!paywall.open) paywall.showModal(); }
  $('#open-pro').onclick = $('#design-unlock').onclick = openPaywall;
  $('#purchase-pro').onclick = () => purchasePro(); $('#restore-pro').onclick = () => restorePro();
  function renderBilling(status) {
    $('#open-pro').textContent = status.preview ? 'Fancy Text Pro preview' : status.owned ? 'Fancy Text Pro is active' : 'About Fancy Text Pro';
    $('#pro-billing-status').textContent = status.preview ? 'Development preview. No payment can be taken here.' : status.message;
    $('#purchase-pro').disabled = status.preview || status.owned || !status.available || status.pending || status.purchasing;
    $('#purchase-pro').textContent = status.owned ? 'Fancy Text Pro is active' : status.purchasing ? 'Opening Google Play…' : status.pending ? 'Payment pending' : status.available && status.price ? `Unlock Fancy Text Pro — ${status.price}` : 'Google Play unavailable';
    $('#restore-pro').disabled = !status.native;
    $('#apply-pro-look').textContent = status.owned ? candidate?.kind === 'template' ? 'Use template' : 'Apply background' : 'Unlock Fancy Text Pro';
    note.hidden = status.owned || !requiresPro(getDesign());
    renderPresets(); statusChanged();
  }
  observeBilling(renderBilling);
  function previewDesign() { return candidate.kind === 'template' ? applyTemplate(getDesign(), candidate.template, $('#keep-template-words').checked) : sanitize({ ...getDesign(), ...candidate.look }); }
  let previewGeneration = 0;
  async function drawCandidate() {
    const design = previewDesign(), generation = ++previewGeneration;
    await loadDesignFonts(design); if (generation !== previewGeneration) return;
    drawDesign($('#pro-preview-canvas'), design, { scale: .35 });
  }
  function openPreview(value) {
    candidate = value; $('#pro-preview-title').textContent = value.kind === 'template' ? value.template.name : 'Your custom background';
    $('.keep-words').hidden = value.kind !== 'template'; $('#keep-template-words').checked = !!getDesign().text.trim();
    $('#apply-pro-look').textContent = billingState().owned ? value.kind === 'template' ? 'Use template' : 'Apply background' : 'Unlock Fancy Text Pro';
    drawCandidate().catch(() => toast('Could not render this preview. Try again.')); preview.showModal();
  }
  $('#keep-template-words').onchange = () => drawCandidate().catch(() => toast('Could not render this preview.'));
  $('#apply-pro-look').onclick = () => {
    if (!billingState().owned) { openPaywall(); return; }
    changeDesign(previewDesign()); preview.close(); toast(candidate.kind === 'template' ? 'A fresh look for your words.' : 'Your background is ready.');
  };
  for (const template of TEMPLATES) {
    const button = make('button', 'template-card'); button.setAttribute('aria-label', `Preview template: ${template.name}`);
    const canvas = make('canvas', 'template-canvas'); canvas.setAttribute('aria-hidden', 'true');
    const label = make('span', 'template-name', template.name), category = make('span', 'template-category', template.category);
    button.append(canvas, label, category); $('#template-grid').append(button); button.onclick = () => openPreview({ kind: 'template', template });
    const design = applyTemplate(DEFAULT, template, false);
    loadDesignFonts(design).then(() => drawDesign(canvas, design, { scale: .18 })).catch(() => {});
  }
  function backgroundFieldsVisibility() {
    const gradient = $('#background-kind').value === 'linear'; $('#background-to-label').hidden = $('#background-angle-label').hidden = !gradient;
  }
  $('#background-kind').onchange = backgroundFieldsVisibility;
  $('#preview-background').onclick = () => openPreview({ kind: 'background', look: { background: { kind: $('#background-kind').value, from: $('#background-from').value, to: $('#background-to').value, angle: Number($('#background-angle').value) }, ink: $('#design-ink').value, frame: $('#design-frame').value } });
  function renderPresets() {
    $('#preset-status').textContent = `${presets.length} / ${PRESET_LIMIT} style presets`;
    const list = $('#preset-list'); list.replaceChildren();
    if (!presets.length) list.append(make('p', 'hint', 'Your favourite looks will live here.'));
    for (const preset of presets) {
      const row = make('div', 'preset-row'), apply = make('button', 'preset-apply', preset.name), remove = make('button', 'text-button', 'Delete');
      apply.setAttribute('aria-label', `Apply style preset: ${preset.name}`); remove.setAttribute('aria-label', `Delete style preset: ${preset.name}`);
      apply.onclick = () => { if (!billingState().owned) { openPaywall(); return; } changeDesign(applyLook(getDesign(), preset.look)); toast('Your saved look is back.'); };
      remove.onclick = async () => {
        if (savedBusy) return; savedBusy = true;
        const index = presets.indexOf(preset), next = presets.filter(p => p.id !== preset.id);
        if (await persist('kreativ-style-presets-v1', next)) { presets = next; removedPreset = { preset, index }; $('#undo-preset-delete').hidden = false; renderPresets(); $('#undo-preset-delete').focus(); }
        savedBusy = false;
      };
      row.append(apply, remove); list.append(row);
    }
  }
  $('#preset-form').onsubmit = async event => {
    event.preventDefault();
    if (!billingState().owned) { openPaywall(); return; }
    if (savedBusy) return;
    const name = $('#preset-name').value.trim();
    if (!name) { $('#preset-name').focus(); return; }
    if (presets.length >= PRESET_LIMIT) { toast('Your preset collection is full. Remove one to make room.'); return; }
    if (presets.some(p => p.name.toLocaleLowerCase() === name.toLocaleLowerCase())) { toast('That preset name is already in use. Choose another name.'); return; }
    savedBusy = true; const next = [...presets, { id: crypto.randomUUID(), name, look: lookFrom(getDesign()) }];
    if (await persist('kreativ-style-presets-v1', next)) { presets = next; $('#preset-name').value = ''; renderPresets(); toast('Style preset saved on this device.'); }
    savedBusy = false;
  };
  $('#undo-preset-delete').onclick = async () => {
    if (!removedPreset || savedBusy) return; savedBusy = true;
    if (presets.length >= PRESET_LIMIT) { savedBusy = false; toast('Your preset collection is full. Remove one before restoring this preset.'); return; }
    const next = [...presets]; next.splice(Math.min(removedPreset.index, next.length), 0, removedPreset.preset);
    if (await persist('kreativ-style-presets-v1', next)) { presets = next; removedPreset = null; $('#undo-preset-delete').hidden = true; renderPresets(); }
    savedBusy = false;
  };
  startBilling();
  return { owned: () => billingState().owned, openPaywall, refresh() {
    const design = getDesign(), look = JSON.stringify(lookFrom(design));
    note.hidden = billingState().owned || !requiresPro(design);
    if (look !== lastLook) {
      lastLook = look; const theme = themeFor(design), bg = design.background;
      $('#background-kind').value = bg?.kind || 'solid'; $('#background-from').value = bg?.from || theme.background; $('#background-to').value = bg?.to || '#d5ccec';
      $('#background-angle').value = String(bg?.angle ?? 135); $('#design-ink').value = design.ink || theme.ink; $('#design-frame').value = design.frame === 'theme' ? 'none' : design.frame; backgroundFieldsVisibility();
    }
  } };
}
