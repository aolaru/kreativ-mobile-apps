#!/usr/bin/env node
// Render the vector source; no painted/AI-generated approximations of the logo.
// Requires sharp (available in the bundled workspace Node runtime).
const fs = require('node:fs/promises');
const path = require('node:path');
const assert = require('node:assert/strict');
const sharp = require('sharp');

async function main() {
  const project = path.resolve(__dirname, '..');
  const assets = path.join(project, 'store-assets');
  const res = path.join(project, 'app/src/main/res');
  const svg = await fs.readFile(path.join(assets, 'sharesafe-icon.svg'), 'utf8');
  const paths = [...svg.matchAll(/\bd="([^"]+)"/g)].map(match => match[1]);
  assert.equal(paths.length, 2);
  const circle = '<circle cx="24" cy="24" r="22" fill="#347463"/>';
  assert.ok(svg.includes(circle), 'Expected the original ShareSafe brand colour and circle');
  for (const file of ['drawable/ic_sharesafe.xml', 'drawable/ic_sharesafe_foreground.xml']) {
    const xml = await fs.readFile(path.join(res, file), 'utf8');
    for (const d of paths) assert.ok(xml.includes(`android:pathData="${d}"`), `${file} differs from the SVG`);
    assert.ok(xml.includes('android:fillType="evenOdd"'), `${file} needs the shield cutout`);
  }
  const foreground = await fs.readFile(path.join(res, 'drawable/ic_sharesafe_foreground.xml'), 'utf8');
  for (const [name, value] of [['scaleX', '1.94'], ['scaleY', '1.94'], ['translateX', '7.44'], ['translateY', '7.44']]) {
    assert.ok(foreground.includes(`android:${name}="${value}"`), 'Update the mask preview if the foreground transform changes');
  }
  const playSvg = svg.replace(circle, '<rect width="48" height="48" fill="#347463"/>');
  // Play adds its own corner mask; the upload asset is an opaque full-bleed square.
  await fs.mkdir(path.join(assets, 'play'), { recursive: true });
  await fs.mkdir(path.join(assets, 'amazon'), { recursive: true });
  await sharp(Buffer.from(playSvg)).resize(512, 512).png().toFile(path.join(assets, 'play/icon-512.png'));
  for (const size of [512, 114]) {
    await sharp(Buffer.from(svg)).resize(size, size).png().toFile(path.join(assets, `amazon/icon-${size}.png`));
  }
  // Preview the real adaptive foreground under two representative launcher masks.
  const logo = svg.match(/<path[\s\S]*?<\/svg>/)[0].replace('</svg>', '');
  const masks = [
    ['Circle', '<circle cx="54" cy="54" r="36"/>'],
    ['Rounded square', '<rect x="18" y="18" width="72" height="72" rx="18"/>'],
  ];
  const tiles = masks.map(([label, mask], i) => `<g transform="translate(${i * 250},0)">
    <svg x="12" y="12" width="216" height="216" viewBox="18 18 72 72">
      <defs><clipPath id="mask${i}">${mask}</clipPath></defs>
      <g clip-path="url(#mask${i})"><rect width="108" height="108" fill="#347463"/>
        <g transform="translate(7.44 7.44) scale(1.94)">${logo}</g>
      </g>
    </svg><text x="120" y="263" text-anchor="middle" font-family="sans-serif" font-size="16" fill="#18302B">${label}</text>
  </g>`).join('');
  const preview = `<svg xmlns="http://www.w3.org/2000/svg" width="500" height="290" viewBox="0 0 500 290">
    <rect width="500" height="290" fill="#F4F7F5"/>${tiles}</svg>`;
  await sharp(Buffer.from(preview)).png().toFile(path.join(assets, 'play/icon-launcher-preview.png'));
  const mono = `<svg xmlns="http://www.w3.org/2000/svg" width="108" height="108" viewBox="0 0 108 108">
    <g transform="translate(7.44 7.44) scale(1.94)">${logo}</g></svg>`;
  const { data, info } = await sharp(Buffer.from(mono)).ensureAlpha().raw().toBuffer({ resolveWithObject: true });
  // A circular safe zone is stricter than the 66x66 central square.
  for (let y = 0; y < info.height; y++) for (let x = 0; x < info.width; x++) {
    if (data[(y * info.width + x) * info.channels + 3] > 32) {
      assert.ok(Math.hypot(x + 0.5 - 54, y + 0.5 - 54) <= 33, 'Logo exceeds the safe zone');
    }
  }
  const play = await sharp(path.join(assets, 'play/icon-512.png')).ensureAlpha().raw().toBuffer();
  for (let i = 3; i < play.length; i += 4) assert.equal(play[i], 255, 'Play icon must be opaque');
  console.log('Rendered Play (512px), Amazon (512px / 114px), and launcher-mask preview.');
  console.log('Android/SVG paths match; adaptive safe zone and opaque Play background verified.');
}

main().catch(error => { console.error(error); process.exitCode = 1; });
