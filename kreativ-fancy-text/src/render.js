import { FORMATS, runsFor, themeFor, outputFor } from './model.js';

export async function loadDesignFonts(state) {
  const text = outputFor(state) || 'Kreativ';
  await Promise.all([
    document.fonts.load('100px "Noto Sans Math"', text),
    document.fonts.load('500 30px "DM Sans"', state.signature || 'Kreativ'),
  ]);
}
const graphemes = new Intl.Segmenter(undefined, { granularity: 'grapheme' });
const fontFor = (size, theme) => `${size}px "Noto Sans Math", ${theme.family}`;

function layout(ctx, runs, width, size, theme) {
  ctx.font = fontFor(size, theme);
  const lines = [{ glyphs: [], width: 0 }];
  let line = lines[0];
  const next = () => { line = { glyphs: [], width: 0 }; lines.push(line); };
  for (const run of runs) {
    // Keep words together whenever possible; split very long words by grapheme.
    const parts = run.text.split(/(\n)/u);
    for (const part of parts) {
      if (part === '\n') { next(); continue; }
      if (!part || (!line.width && !part.trim())) continue;
      const measured = ctx.measureText(part).width;
      if (part.trim() && line.width && line.width + measured > width) next();
      if (!part.trim() && line.width + measured > width) continue;
      if (measured <= width) {
        line.glyphs.push({ text: part, color: run.color, x: line.width });
        line.width += measured;
        continue;
      }
      let chunk = '';
      for (const { segment } of graphemes.segment(part)) {
        if (segment === '\r') continue;
        if (chunk && ctx.measureText(chunk + segment).width + line.width > width) {
          line.glyphs.push({ text: chunk, color: run.color, x: line.width });
          line.width += ctx.measureText(chunk).width;
          next(); chunk = '';
        }
        chunk += segment;
      }
      if (chunk) {
        line.glyphs.push({ text: chunk, color: run.color, x: line.width });
        line.width += ctx.measureText(chunk).width;
      }
    }
  }
  return lines;
}
function decorations(ctx, theme, width, height) {
  const accent = theme.accent;
  ctx.save();
  ctx.strokeStyle = accent; ctx.fillStyle = accent;
  ctx.globalAlpha = 0.22; ctx.lineWidth = 2;
  if (theme.ornament === 'studio') {
    ctx.beginPath(); ctx.arc(width - 36, 12, 220, 0, Math.PI * 2); ctx.stroke();
    ctx.beginPath(); ctx.arc(width - 36, 12, 190, 0, Math.PI * 2); ctx.stroke();
    ctx.fillRect(90, height - 140, 56, 5);
  } else if (theme.ornament === 'grid') {
    ctx.globalAlpha = 0.10;
    for (let x = 0; x < width; x += 90) { ctx.beginPath(); ctx.moveTo(x, 0); ctx.lineTo(x, height); ctx.stroke(); }
    for (let y = 0; y < height; y += 90) { ctx.beginPath(); ctx.moveTo(0, y); ctx.lineTo(width, y); ctx.stroke(); }
  } else if (theme.ornament === 'sun') {
    ctx.globalAlpha = 0.14; ctx.beginPath(); ctx.arc(width * 0.8, height * 0.17, width * 0.25, 0, Math.PI * 2); ctx.fill();
    ctx.beginPath(); ctx.arc(width * 0.12, height, width * 0.35, 0, Math.PI * 2); ctx.fill();
  } else if (theme.ornament === 'petals') {
    for (const [x, y] of [[width - 30, 60], [35, height - 40]]) {
      for (let i = 0; i < 6; i++) {
        ctx.save(); ctx.translate(x, y); ctx.rotate(i * Math.PI / 3);
        ctx.beginPath(); ctx.ellipse(75, 0, 95, 35, 0, 0, Math.PI * 2); ctx.stroke(); ctx.restore();
      }
    }
  } else if (theme.ornament === 'heart') {
    ctx.translate(width - 175, 130); ctx.scale(3, 3);
    ctx.beginPath(); ctx.moveTo(0, 0); ctx.bezierCurveTo(-30, -35, -60, 10, 0, 45);
    ctx.bezierCurveTo(60, 10, 30, -35, 0, 0); ctx.stroke();
  } else {
    ctx.strokeRect(48, 48, width - 96, height - 96);
    ctx.beginPath(); ctx.moveTo(85, 100); ctx.lineTo(170, 100); ctx.stroke();
  }
  ctx.restore();
}
function customFrame(ctx, kind, ink, width, height) {
  if (kind === 'theme' || kind === 'none' || !kind) return;
  ctx.save(); ctx.strokeStyle = ink; ctx.globalAlpha = .55; ctx.lineWidth = 2;
  if (kind === 'line' || kind === 'double') {
    ctx.strokeRect(45, 45, width - 90, height - 90);
    if (kind === 'double') ctx.strokeRect(61, 61, width - 122, height - 122);
  } else if (kind === 'corners') {
    for (const [x,y,sx,sy] of [[45,45,1,1],[width-45,45,-1,1],[45,height-45,1,-1],[width-45,height-45,-1,-1]]) {
      ctx.beginPath(); ctx.moveTo(x,y+sy*100); ctx.lineTo(x,y); ctx.lineTo(x+sx*100,y); ctx.stroke();
    }
  } else if (kind === 'arch') {
    const r = Math.min(width / 2 - 45, height / 4);
    ctx.beginPath(); ctx.moveTo(45,height-45); ctx.lineTo(45,45+r);
    ctx.arcTo(45,45,width/2,45,r); ctx.arcTo(width-45,45,width-45,45+r,r);
    ctx.lineTo(width-45,height-45); ctx.closePath(); ctx.stroke();
  }
  ctx.restore();
}
export function drawDesign(canvas, state, { scale = 1 } = {}) {
  const theme = themeFor(state), format = FORMATS[state.format];
  const { width, height } = format;
  canvas.width = width * scale; canvas.height = height * scale;
  const ctx = canvas.getContext('2d');
  ctx.scale(scale, scale);
  if (!state.transparent) {
    const background = state.background;
    if (background?.kind === 'linear') {
      const radians = background.angle * Math.PI / 180;
      const dx = Math.cos(radians) * width / 2, dy = Math.sin(radians) * height / 2;
      const gradient = ctx.createLinearGradient(width/2-dx,height/2-dy,width/2+dx,height/2+dy);
      gradient.addColorStop(0, background.from); gradient.addColorStop(1, background.to); ctx.fillStyle = gradient;
    } else ctx.fillStyle = background?.from || theme.background;
    ctx.fillRect(0, 0, width, height);
    if (!state.frame || state.frame === 'theme') decorations(ctx, theme, width, height);
  }
  customFrame(ctx, state.frame, state.ink || theme.ink, width, height);
  const margin = 100, maxWidth = width - margin * 2;
  const maxHeight = height - (state.signature ? 340 : 260);
  const runs = runsFor(state);
  let size = state.size, lines;
  do {
    lines = layout(ctx, runs, maxWidth, size, theme);
    if (lines.length * size * 1.5 <= maxHeight || size <= 8) break;
    size -= 2;
  } while (true);
  ctx.font = fontFor(size, theme);
  ctx.textBaseline = 'alphabetic';
  const lineHeight = size * 1.5;
  const fits = lines.length * lineHeight <= maxHeight && lines.every(line => line.width <= maxWidth + 0.01);
  const top = (height - lines.length * lineHeight) / 2 + size;
  ctx.save();
  ctx.beginPath(); ctx.rect(margin, (height - maxHeight) / 2, maxWidth, maxHeight); ctx.clip();
  for (let row = 0; row < lines.length; row++) {
    const line = lines[row];
    const left = state.align === 'left' ? margin : state.align === 'right' ? width - margin - line.width : (width - line.width) / 2;
    for (const glyph of line.glyphs) {
      ctx.fillStyle = glyph.color;
      if (theme.id === 'night') { ctx.shadowColor = glyph.color; ctx.shadowBlur = 9; }
      ctx.fillText(glyph.text, left + glyph.x, top + row * lineHeight);
    }
  }
  ctx.restore();
  ctx.shadowBlur = 0;
  if (state.signature) {
    ctx.fillStyle = state.ink || theme.ink; ctx.globalAlpha = 0.78;
    ctx.font = '500 28px "DM Sans", sans-serif'; ctx.textAlign = 'center';
    ctx.fillText(state.signature, width / 2, height - 110, maxWidth);
  }
  return { width: canvas.width, height: canvas.height, actualSize: size, fits };
}
export async function pngFor(state) {
  await loadDesignFonts(state);
  const canvas = document.createElement('canvas');
  const dimensions = drawDesign(canvas, state, { scale: state.scale });
  if (!dimensions.fits) throw new Error('This message has too many lines for the image. Remove line breaks or choose a taller format.');
  const blob = await new Promise(resolve => canvas.toBlob(resolve, 'image/png'));
  if (!blob) throw new Error('Could not create this image. Try the standard resolution.');
  return { blob, ...dimensions };
}
