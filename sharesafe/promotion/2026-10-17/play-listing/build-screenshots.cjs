const fs = require('node:fs');
const path = require('node:path');
const sharp = require('/Users/andreiolaru/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/sharp');

// Store-sized vector layouts around genuine app captures. No app UI is drawn,
// retouched, cropped or rearranged. Source screenshot bytes remain unchanged.
const root = __dirname;
const source = path.join(root, '..', 'tutorial', 'screenshots');
const output = path.join(root, 'screenshots');
const slides = [
  { file: '01-privacy-check.png', capture: '04-before-after.png', title: ['Check. Clean.', 'Compare.'], subtitle: 'See selected EXIF checks before and after.', footer: 'Selected fields only. Not an anonymity guarantee.' },
  { file: '02-keep-originals.png', capture: '01-choose-photos.png', title: ['Keep originals.', 'Share fresh copies.'], subtitle: 'Choose photos. Create new JPEG copies.', footer: 'Photo processing happens on your device.' },
  { file: '03-export-options.png', capture: '03-export-options.png', title: ['Choose how', 'you share.'], subtitle: 'JPEG quality, individual photos or one ZIP.', footer: 'Metadata cleaning and Privacy Check are free.' },
  { file: '04-ready-to-share.png', capture: '05-ready-to-share.png', title: ['Your copy is', 'ready to share.'], subtitle: 'Save to your gallery or choose where to send it.', footer: 'Review visible details before sharing.' },
];
const esc = text => text.replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;');

async function main() {
  fs.mkdirSync(output, { recursive: true });
  const files = [];
  for (const slide of slides) {
    const capture = fs.readFileSync(path.join(source, slide.capture));
    const meta = await sharp(capture).metadata();
    if (meta.width !== 1080 || meta.height !== 2340) throw Error('Unexpected capture dimensions');
    const svg = `<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="1440" height="2560" viewBox="0 0 1440 2560">
      <rect width="1440" height="2560" fill="#f3f7f5"/>
      <text x="96" y="84" font-family="Arial, sans-serif" font-size="31" font-weight="700" letter-spacing="5" fill="#347463">SHARESAFE</text>
      <text x="96" y="183" font-family="Arial, sans-serif" font-size="86" font-weight="700" letter-spacing="-2" fill="#193d35">${esc(slide.title[0])}</text>
      <text x="96" y="278" font-family="Arial, sans-serif" font-size="86" font-weight="700" letter-spacing="-2" fill="#193d35">${esc(slide.title[1])}</text>
      <text x="96" y="341" font-family="Arial, sans-serif" font-size="35" fill="#465e56">${esc(slide.subtitle)}</text>
      <rect x="229" y="375" width="982" height="2114" rx="5" fill="#d9e5de"/>
      <image x="235" y="381" width="970" height="2102" preserveAspectRatio="xMidYMid meet" xlink:href="data:image/png;base64,${capture.toString('base64')}"/>
      <text x="720" y="2532" text-anchor="middle" font-family="Arial, sans-serif" font-size="29" fill="#465e56">${esc(slide.footer)}</text>
    </svg>`;
    fs.writeFileSync(path.join(output, slide.file.replace('.png', '.svg')), svg);
    const png = path.join(output, slide.file);
    await sharp(Buffer.from(svg)).flatten({ background: '#f3f7f5' }).removeAlpha().png().toFile(png);
    const rendered = await sharp(png).metadata();
    if (rendered.hasAlpha || rendered.width !== 1440 || rendered.height !== 2560) throw Error('Invalid Play asset');
    files.push({ file: slide.file, source: slide.capture, width: rendered.width, height: rendered.height, hasAlpha: rendered.hasAlpha });
  }
  await sharp({ create: { width: 1440, height: 640, channels: 3, background: '#e6eee9' } })
    .composite(await Promise.all(slides.map(async (slide, i) => ({ input: await sharp(path.join(output, slide.file)).resize(360, 640).toBuffer(), left: i * 360, top: 0 }))))
    .png().toFile(path.join(root, 'screenshots-preview.png'));
  fs.writeFileSync(path.join(root, 'screenshots-manifest.json'), JSON.stringify({ appVersion: '1.2.0', captureBuild: 'Play debug; shared release UI only; no test-only controls visible', sourceDate: '2026-10-17', policyReference: 'https://support.google.com/googleplay/android-developer/answer/9866151', files }, null, 2) + '\n');
  console.log(JSON.stringify(files));
}
main().catch(error => { console.error(error); process.exitCode = 1; });
