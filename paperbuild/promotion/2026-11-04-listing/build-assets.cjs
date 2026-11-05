// Extend the existing Play artwork with genuine 1.2 phone captures. Never repaint UI.
const fs = require('node:fs'), path = require('node:path'), crypto = require('node:crypto'), assert = require('node:assert/strict');
const sharp = require('/Users/andreiolaru/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/sharp');
const root = __dirname, old = path.resolve(root, '../2026-10-18/listing');
const raw = path.resolve(root, '../../store-assets/play/screenshots/1.2/raw');
const p = {bg:'#edf0ff', ink:'#18263e', muted:'#53617a', accent:'#5059dd', line:'#cdd4ef'};
const esc = s => String(s).replaceAll('&','&amp;').replaceAll('<','&lt;').replaceAll('>','&gt;').replaceAll('"','&quot;');
const text = (lines,x,y,size,color,weight=400) => `<text x="${x}" y="${y}" font-family="Arial, sans-serif" font-size="${size}" font-weight="${weight}" fill="${color}">${lines.map((line,i)=>`<tspan x="${x}" dy="${i?size*1.14:0}">${esc(line)}</tspan>`).join('')}</text>`;
const added = [
  {file:'06-edit-pdf-pages.png', source:'editor-pages.png', crop:{left:56,top:208,width:968,height:1670}, title:['Every page.','In its place.'], sub:'Reorder, rotate, duplicate or remove PDF pages.', foot:'Page editing is included in optional paid Pro.', alt:'Actual Pro page editor with a fictional three-page project brief, page list and undo control.'},
  {file:'07-logo-watermarks.png', source:'logo-export-render.png', sourceKind:'Actual app-exported PDF rendered at 180 dpi', crop:{left:0,top:0,width:1489,height:2105}, title:['Your logo.','On every page.'], sub:'Add a logo, text watermark and page numbers.', foot:'Optional paid Pro. Actual exported sample shown.', alt:'Actual app-exported fictional project brief with logo, text watermark and page number.'},
  {file:'08-optional-pro.png', source:'pro-features.png', crop:{left:56,top:310,width:968,height:1730}, title:['More PDF tools.','One optional upgrade.'], sub:'Paid Pro: one-time purchase, no subscription.', foot:'Use your favourite logo again with saved presets.', alt:'Actual Pro feature list including page editing, password protection, logo watermarks and saved presets.'}
];
async function main() {
  fs.mkdirSync(path.join(root,'screenshots'),{recursive:true});
  const baseline=JSON.parse(fs.readFileSync(path.join(root,'baseline-assets.json'),'utf8'));
  const files=[];
  for(const item of baseline) {
    const committed=path.join(root,'screenshots',item.file);
    const bytes=fs.readFileSync(fs.existsSync(committed) ? committed : path.join(old,'screenshots',item.file));
    fs.writeFileSync(path.join(root,'screenshots',item.file),bytes);
    files.push({...item,file:'screenshots/'+item.file,captureVersion:'1.1.0; Free UI unchanged in 1.2',reusedFrom:path.relative(root,path.join(old,'screenshots',item.file)),sha256:crypto.createHash('sha256').update(bytes).digest('hex')});
  }
  for(const s of added) {
    const source=fs.readFileSync(path.join(raw,s.source)); const meta=await sharp(source).metadata();
    if(s.sourceKind) {assert.equal(meta.width,1489);assert.equal(meta.height,2105);} else {assert.equal(meta.width,1080);assert.equal(meta.height,2340);}
    const image=await sharp(source).extract(s.crop).removeAlpha().png().toBuffer();
    const w=Math.min(1256,1830*s.crop.width/s.crop.height),h=w*s.crop.height/s.crop.width,x=(1440-w)/2,y=505+(1830-h)/2;
    const body=`<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="1440" height="2560" viewBox="0 0 1440 2560"><rect width="1440" height="2560" fill="${p.bg}"/><rect x="936" y="563.2" width="864" height="2048" rx="100" fill="#e3e8ff" transform="rotate(12 936 563.2)"/>${text(['PAPERBUILD'],84,99,32,p.accent,700)}${text(s.title,84,225,88,p.ink,700)}${text([s.sub],84,415,36,p.muted)}<rect x="${x-3}" y="${y-3}" width="${w+6}" height="${h+6}" rx="18" fill="${p.line}"/><image x="${x}" y="${y}" width="${w}" height="${h}" preserveAspectRatio="xMidYMid meet" xlink:href="data:image/png;base64,${image.toString('base64')}"/>${text([s.foot],84,2488,29,p.muted)}</svg>`;
    fs.writeFileSync(path.join(root,'screenshots',s.file.replace('.png','.svg')),body);
    const out=path.join(root,'screenshots',s.file);await sharp(Buffer.from(body)).flatten({background:p.bg}).removeAlpha().png().toFile(out);
    files.push({file:'screenshots/'+s.file,captureVersion:'1.2.0 QA / same release UI and processing',sourceKind:s.sourceKind||'Genuine device screenshot',source:path.relative(root,path.join(raw,s.source)),sourceSha256:crypto.createHash('sha256').update(source).digest('hex'),crop:s.crop,alt:s.alt,sha256:crypto.createHash('sha256').update(fs.readFileSync(out)).digest('hex')});
  }
  const copy=JSON.parse(fs.readFileSync(path.join(root,'listing.json'),'utf8'));
  assert.ok(copy.title.length<=30&&copy.shortDescription.length<=80&&copy.fullDescription.length<=4000);
  for(const f of files) {const meta=await sharp(path.join(root,f.file)).metadata();assert.equal(meta.width,1440);assert.equal(meta.height,2560);assert.equal(meta.hasAlpha,false);assert.ok(fs.statSync(path.join(root,f.file)).size<8*1024*1024);}
  const manifest={date:'2026-11-04',app:'PaperBuild: Photo to PDF',release:'6 / 1.2.0',status:'Local release assets; Play status is recorded in REVIEW.md',captureDevice:'Samsung SM-A556B / Android 16, 1080 × 2340',processing:'Crop and proportional scale only. Actual UI/PDF output; no repainted controls, invented readings, prices or unlock state.',generativeImagery:false,iconAndFeatureGraphic:'Existing assets retained unchanged',counts:{title:copy.title.length,shortDescription:copy.shortDescription.length,fullDescription:copy.fullDescription.length,screenshots:files.length},files};
  fs.writeFileSync(path.join(root,'assets-manifest.json'),JSON.stringify(manifest,null,2)+'\n');
  const tiles=[];
  for(let i=0;i<files.length;i++) {const image=await sharp(path.join(root,files[i].file)).resize(270,480).toBuffer();tiles.push({input:image,left:(i%4)*270,top:Math.floor(i/4)*480});}
  await sharp({create:{width:1080,height:960,channels:3,background:p.bg}}).composite(tiles).png().toFile(path.join(root,'screenshots-preview.png'));
  console.log(JSON.stringify(manifest.counts));
}
main().catch(e=>{console.error(e);process.exitCode=1});
