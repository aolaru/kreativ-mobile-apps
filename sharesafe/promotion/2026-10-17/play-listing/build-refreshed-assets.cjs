// Code-native layouts around genuine app captures. UI pixels are never reconstructed.
// Captures omit system bars and adjacent partial cards; full originals are retained.
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const sharp = require('/Users/andreiolaru/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/sharp');
const root = __dirname;
const output = path.join(root, 'v2');
const sources = path.join(output, 'captures');
const esc = s => s.replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;');
const slides = [
  {file:'01-remove-metadata.png', source:'01-metadata.png', title:['Remove hidden', 'photo metadata.'], subtitle:'Check selected location, date and camera fields.', footer:'Selected fields only. Not an anonymity guarantee.', alt:'Privacy Check shows selected location, date and camera fields found before cleaning and absent from the new JPEG copy.'},
  {file:'02-keep-originals.png', source:'02-originals.png', title:['Keep your originals.', 'Create fresh copies.'], subtitle:'Choose photos. Clean a copy on your device.', footer:'Your original files are not changed.', alt:'ShareSafe’s photo chooser explains on-device processing and confirms that original photos stay untouched.'},
  {file:'03-export-options.png', source:'03-options.png', title:['Your photos.', 'Your export options.'], subtitle:'Choose JPEG quality, individual photos or a ZIP.', footer:'Large images may be resized. Copies are JPEGs.', alt:'Export options offer three JPEG quality levels, individual photos or a ZIP file, with image-conversion limits.'},
  {file:'04-ready-to-share.png', source:'04-sharing.png', title:['Clean a copy.', 'Choose where it goes.'], subtitle:'Share it directly or save it to your gallery.', footer:'Review visible details before sharing.', alt:'A successful export shows one cleaned copy, selected metadata checks passed, and sharing and gallery-save controls.'},
  {file:'05-pro-cover.png', source:'05-pro-after.png', title:['Cover visible', 'private details.'], subtitle:'Optional Pro · One-time purchase', footer:'Actual Cover tool · Fictional address · Review every copy.', alt:'Optional paid Pro editor before and after using solid Cover over a fictional address in an illustrative bicycle listing.'},
].filter(slide => !process.argv.includes('--free-only') || !slide.file.startsWith('05'));
const base = (width, height, body) => `<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}">${body}</svg>`;
const font = 'font-family="Arial, sans-serif"';
async function image(file, x, y, width, height) {
  const data = fs.readFileSync(path.join(sources, file));
  return `<image x="${x}" y="${y}" width="${width}" height="${height}" preserveAspectRatio="xMidYMid meet" xlink:href="data:image/png;base64,${data.toString('base64')}"/>`;
}
async function png(svg, filename, width, height) {
  fs.writeFileSync(filename.replace('.png','.svg'), svg);
  await sharp(Buffer.from(svg)).flatten({background:'#f3f7f5'}).removeAlpha().png().toFile(filename);
  const meta = await sharp(filename).metadata();
  assert.equal(meta.width,width); assert.equal(meta.height,height); assert.equal(meta.channels,3); assert.equal(meta.hasAlpha,false);
  assert.ok(fs.statSync(filename).size < 8 * 1024 * 1024);
}
async function renderScreenshots() {
  const manifest = [];
  for (const slide of slides) {
    let content = `<rect width="1440" height="2560" fill="#f3f7f5"/>
      <circle cx="1435" cy="2100" r="500" fill="#e6f0e9"/>
      <text x="88" y="100" ${font} font-size="30" font-weight="700" letter-spacing="5" fill="#347463">SHARESAFE</text>
      <text x="88" y="227" ${font} font-size="101" font-weight="700" letter-spacing="-3" fill="#193d35">${esc(slide.title[0])}</text>
      <text x="88" y="340" ${font} font-size="101" font-weight="700" letter-spacing="-3" fill="#193d35">${esc(slide.title[1])}</text>
      <text x="88" y="428" ${font} font-size="40" fill="#465e56">${esc(slide.subtitle)}</text>`;
    if (slide.file.startsWith('05')) {
      // Two complete, unaltered app canvas cards: before and after the real Cover action.
      for (const [index, capture] of ['05-pro-before.png','05-pro-after.png'].entries()) {
        const meta = await sharp(path.join(sources,capture)).metadata();
        const width = Math.min(1160, 770 * meta.width / meta.height);
        const height = width * meta.height / meta.width;
        const x = (1440-width)/2, y = 600 + index*920;
        content += `<text x="${x}" y="${y-28}" ${font} font-size="34" font-weight="700" letter-spacing="3" fill="#347463">${index ? 'AFTER SOLID COVER' : 'BEFORE'}</text>`;
        content += await image(capture,x,y,width,height);
      }
    } else {
      const meta = await sharp(path.join(sources,slide.source)).metadata();
      const width = Math.min(1264, 1740 * meta.width / meta.height);
      const height = width * meta.height/meta.width;
      const x = (1440-width)/2, y = 570 + (1740-height)/2;
      content += `<rect x="${x-4}" y="${y-4}" width="${width+8}" height="${height+8}" rx="22" fill="#d9e5de"/>`;
      content += await image(slide.source,x,y,width,height);
    }
    content += `<text x="720" y="2496" text-anchor="middle" ${font} font-size="31" fill="#465e56">${esc(slide.footer)}</text>`;
    const filename = path.join(output,'screenshots',slide.file);
    await png(base(1440,2560,content),filename,1440,2560);
    assert.ok(slide.alt.length<=140);
    manifest.push({...slide,width:1440,height:2560,hasAlpha:false});
  }
  return manifest;
}
async function renderFeature() {
  // An extension of the established green identity, not an enlarged duplicate icon.
  const feature = base(1024,500,`<rect width="1024" height="500" fill="#dfeee4"/>
    <circle cx="950" cy="260" r="300" fill="#c6dfd0"/>
    <text x="72" y="111" ${font} font-size="24" font-weight="700" letter-spacing="4" fill="#347463">SHARESAFE</text>
    <text x="72" y="202" ${font} font-size="54" font-weight="700" letter-spacing="-1.5" fill="#193d35">Remove hidden</text>
    <text x="72" y="266" ${font} font-size="54" font-weight="700" letter-spacing="-1.5" fill="#193d35">photo metadata.</text>
    <text x="74" y="328" ${font} font-size="23" fill="#465e56">On-device cleaning.</text>
    <text x="74" y="363" ${font} font-size="23" fill="#465e56">Originals unchanged.</text>
    <g transform="translate(690 100) rotate(8 105 150)"><rect width="210" height="280" rx="20" fill="#94bda7"/></g>
    <g transform="translate(662 112) rotate(-8 105 140)"><rect width="210" height="280" rx="20" fill="#f3f7f5"/>
      <rect x="16" y="16" width="178" height="195" rx="12" fill="#c9dfcf"/>
      <circle cx="148" cy="62" r="20" fill="#f4d790"/>
      <path d="M16 170 L71 87 L132 172 L162 132 L194 177 V199 Q194 211 182 211 H28 Q16 211 16 199 Z" fill="#347463"/>
      <path d="M35 243 H175" stroke="#b5cebf" stroke-width="9" stroke-linecap="round"/>
    </g>`);
  await png(feature,path.join(output,'feature-graphic.png'),1024,500);
}
async function main() {
  fs.mkdirSync(path.join(output,'screenshots'), {recursive:true});
  await renderFeature();
  if (process.argv.includes('--feature-only')) return;
  const manifest = await renderScreenshots();
  await sharp({create:{width:slides.length*360,height:640,channels:3,background:'#e6eee9'}})
    .composite(await Promise.all(slides.map(async (slide,i)=>({input:await sharp(path.join(output,'screenshots',slide.file)).resize(360,640).toBuffer(),left:i*360,top:0}))))
    .png().toFile(path.join(output,'screenshots-preview.png'));
  const copy=JSON.parse(fs.readFileSync(path.join(root,'listing-copy.json'),'utf8'));
  assert.equal(copy.title.length,27); assert.ok(copy.shortDescription.length<=80); assert.ok(copy.fullDescription.length<=4000);
  fs.writeFileSync(path.join(output,'assets-manifest.json'),JSON.stringify({version:'1.2.0',date:'2026-10-17',capture:'Free panels: verified 17 October Galaxy A55 Play debug captures, release-shared UI and synthetic EXIF. See capture-crops.json. Pro is included only after a real capture is available.',proExample:slides.length===5?'Real Pro Cover capture with fictional address':'Not captured: phone disconnected; emulator installer blocked and host storage full',processing:'Proportional scaling and crops of complete real UI panels only. No UI redrawing, substitution, or retouching. Full source screens retained.',featureGraphic:{file:'feature-graphic.png',width:1024,height:500,hasAlpha:false,alt:'ShareSafe. Remove hidden photo metadata. On-device cleaning and unchanged originals, with a green photo-card illustration.'},title:copy.title,shortDescription:copy.shortDescription,shortDescriptionCharacters:copy.shortDescription.length,files:manifest},null,2)+'\n');
  fs.writeFileSync(path.join(output,'preview.html'),`<!doctype html>
    <html lang="en-GB"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
    <meta name="robots" content="noindex,nofollow"><title>ShareSafe — listing refresh preview</title>
    <style>*{box-sizing:border-box}body{margin:0;background:#f3f7f5;color:#193d35;font:17px/1.6 system-ui,sans-serif}main{max-width:1180px;margin:auto;padding:40px 24px 80px}h1{font-size:clamp(30px,4vw,48px);line-height:1.15;margin:16px 0}.label{letter-spacing:.12em;text-transform:uppercase;font-size:12px;font-weight:700;color:#347463}.notice{padding:16px 20px;border:1px solid #d3e1d7;border-radius:14px;background:#e5f0e8}h2{margin-top:44px;font-size:24px}.feature{display:block;width:100%;max-width:1024px;border-radius:16px}.copy{padding:20px;background:#fff;border-radius:16px}.gallery{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:20px}figure{margin:0}figure img{display:block;width:100%;border:1px solid #d3e1d7;border-radius:12px}figcaption{font-size:14px;margin-top:10px;color:#465e56}a{color:#23664f}@media(max-width:780px){.gallery{grid-template-columns:repeat(2,minmax(0,1fr))}}@media(max-width:480px){.gallery{grid-template-columns:1fr}}</style></head>
    <body><main><div class="label">Google Play · English (United Kingdom)</div><h1>${esc(copy.title)}</h1>
    <p class="notice">Local asset preview. Check Play Console for the current review and publication status.</p>
    <h2>Short description</h2><p class="copy">${esc(copy.shortDescription)}<br><small>${copy.shortDescription.length}/80 characters</small></p>
    <h2>Feature graphic</h2><img class="feature" src="feature-graphic.png" alt="ShareSafe: remove hidden photo metadata, with on-device cleaning and unchanged originals.">
    <h2>Phone screenshots · proposed order</h2><div class="gallery">${slides.map((slide,i)=>`<figure><a href="screenshots/${slide.file}"><img src="screenshots/${slide.file}" alt="${esc(slide.alt)}"></a><figcaption>${i+1}. ${esc(slide.title.join(' '))}</figcaption></figure>`).join('')}</div>
    <h2>What changed</h2><p>Clearer opening benefit, larger focused real UI captures without system-bar clutter or partial cards, a consistent green feature graphic, and simpler short-description copy.</p>
    <p class="notice">${slides.length===5?'Includes an actual Pro Cover comparison with a fictional address.':'Pro before/after capture is still pending. The phone needs to be reconnected; no invented editor screen is included.'}</p>
    <p>Pro is a paid, one-time optional unlock. Full source captures and asset provenance are retained. Existing privacy and SDK disclosures in the full description are unchanged.</p>
    </main></body></html>`);
  console.log(JSON.stringify({screenshots:manifest.length,featureGraphic:'1024x500 RGB',shortDescriptionCharacters:copy.shortDescription.length}));
}
main().catch(error=>{console.error(error);process.exitCode=1;});
