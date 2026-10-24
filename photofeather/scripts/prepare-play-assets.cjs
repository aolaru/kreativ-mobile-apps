#!/usr/bin/env node
// Requires Node.js and sharp. No network calls or generated app screenshots.
const fs = require('node:fs/promises');
const path = require('node:path');
const sharp = require('sharp');
const root = path.resolve(__dirname, '..');
const play = path.join(root, 'store-assets/play');

const screenshots = [
  ['review/release-1.0.0/screenshots/01-home.png', '01-select-photos.png', 'Select photos with on-device processing and no account.'],
  ['review/resize-presets/screenshots/resize-01-presets.png', '02-resize-presets.png', 'Small, Medium, Large and Custom resize presets with planned dimensions.'],
  ['review/resize-presets/screenshots/resize-02-custom.png', '03-custom-resize.png', 'Custom longest-edge resize with live per-photo dimensions.'],
  ['review/resize-presets/screenshots/resize-04-measured-result.png', '04-measured-results.png', 'Measured JPEG size and output dimensions after processing.'],
  ['review/release-1.0.0/screenshots/06-comparison-original.png', '05-compare-photos.png', 'Zoomable original and output comparison before exporting.'],
  ['review/release-1.0.0/screenshots/08-dark-home.png', '06-dark-mode.png', 'PhotoFeather selection screen in dark mode.'],
];
async function main() {
  await fs.mkdir(path.join(play, 'screenshots'), {recursive: true});
  await sharp(path.join(play, 'icon.svg')).resize(512,512).ensureAlpha().png().toFile(path.join(play, 'icon-512.png'));
  await sharp(path.join(play, 'feature-graphic.svg')).flatten({background: '#E3EEE7'}).removeAlpha().png().toFile(path.join(play, 'feature-graphic-1024x500.png'));
  const provenance = [];
  for (const [source, name, alt] of screenshots) {
    const input = path.join(root,source);
    const meta = await sharp(input).metadata();
    if (meta.width !== 1080 || meta.height !== 2340) throw new Error('Unexpected capture dimensions: ' + source);
    // Crop only system status/navigation bars. No scaling, overlays or UI reconstruction.
    const crop = {left:0, top:90, width:1080, height:2110};
    const output = path.join(play, 'screenshots', name);
    const {data:corner}=await sharp(input).extract({left:0,top:90,width:1,height:1}).removeAlpha().raw().toBuffer({resolveWithObject:true});
    const background={r:corner[0],g:corner[1],b:corner[2],alpha:1};
    // Preserve every remaining UI pixel; neutral padding supplies the Console's 9:16 ratio.
    await sharp(input).extract(crop).extend({left:54,right:54,top:1,bottom:1,background}).removeAlpha().png().toFile(output);
    provenance.push({file:'screenshots/'+name, source, crop, padding:{left:54,right:54,top:1,bottom:1}, width:1188, height:2112, alt});
  }
  const alt = {icon:'White feather on a green background.', featureGraphic:'PhotoFeather: Lighter photos. Ready to share. Compress and resize on your device.', screenshots:provenance};
  await fs.writeFile(path.join(play,'assets.json'), JSON.stringify(alt,null,2)+'\n');
  const escapeHtml = value => value.replaceAll('&','&amp;').replaceAll('<','&lt;').replaceAll('>','&gt;').replaceAll('"','&quot;');
  const policy = (await fs.readFile(path.join(root,'app/src/main/res/raw/privacy_policy.txt'),'utf8')).trim().split('\n\n');
  const sections = policy.slice(1).map(part => {
    const lines = part.split('\n');
    return '<section><h2>'+escapeHtml(lines.shift())+'</h2><p>'+escapeHtml(lines.join('\n'))+'</p></section>';
  }).join('\n');
  const page = '<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>PhotoFeather Privacy Policy | Made by Kreativ</title><style>body{font:18px/1.65 system-ui,sans-serif;max-width:760px;margin:48px auto;padding:0 24px;color:#193d38;background:#f5f7f5}h1,h2{line-height:1.25}h2{font-size:1.25em;margin-top:2em}a{color:#226b5a}</style><main><h1>PhotoFeather Privacy Policy</h1><p>Prepared 24 October 2026 · Version 1.0.0</p>'+sections+'<p>Contact: <a href="mailto:info@madebykreativ.com">info@madebykreativ.com</a></p></main></html>\n';
  await fs.writeFile(path.join(play,'privacy-policy.html'),page);
  for (const [file,max] of [['title.txt',30],['short-description.txt',80],['full-description.txt',4000],['release-notes.txt',500]]) {
    const value = (await fs.readFile(path.join(play,'en-US',file),'utf8')).trim();
    if (value.length > max) throw new Error(file+' exceeds '+max+' characters');
    console.log(file+': '+value.length+'/'+max+' characters');
  }
  console.log('Prepared icon, feature graphic and six genuine app captures; system bars cropped and neutral padding added for 9:16.');
}
main().catch(e=>{console.error(e.message);process.exitCode=1;});
