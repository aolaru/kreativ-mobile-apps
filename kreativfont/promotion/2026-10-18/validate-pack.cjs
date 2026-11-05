const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict'),crypto=require('node:crypto');
const {spawnSync}=require('node:child_process');
const sharp=require('/Users/andreiolaru/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/sharp');
const root=__dirname,hash=b=>crypto.createHash('sha256').update(b).digest('hex');
async function main(){
 const copy=JSON.parse(fs.readFileSync(path.join(root,'copy.json'),'utf8')),manifest=JSON.parse(fs.readFileSync(path.join(root,'listing/assets-manifest.json'),'utf8'));
 assert.equal(copy.listing.title,copy.listing.currentTitle);assert.ok(copy.listing.title.length<=30);assert.ok(copy.listing.shortDescription.length<=80);assert.ok(copy.listing.fullDescription.length<=4000);
 assert.ok(!copy.listing.fullDescription.includes(copy.tutorialPath),'Unavailable tutorial must stay out of Play copy');
 for(const phrase of ['not a font installer or text editor','app is free','prices and licence','hosting logs','no cloud sync','in English'])assert.ok(copy.listing.fullDescription.includes(phrase),phrase);
 assert.equal(manifest.files.length,5);const pngs=[];
 for(const entry of manifest.files){
  const raw=fs.readFileSync(path.resolve(root,entry.source));assert.equal(hash(raw),entry.sourceSha256);
  const m=await sharp(raw).metadata(),c=entry.crop;assert.ok(c.left>=0&&c.top>=0&&c.left+c.width<=m.width&&c.top+c.height<=m.height);assert.ok(entry.alt.length<=140);
  const expected=await sharp(raw).extract(c).removeAlpha().png().toBuffer(),actual=fs.readFileSync(path.join(root,'tutorial/screenshots',entry.file));assert.equal(hash(actual),hash(expected),'Tutorial must be an unaltered source crop');
  pngs.push(['listing/screenshots/'+entry.file,1440,2560]);
 }
 pngs.push(['listing/feature-graphic.png',1024,500]);for(const number of ['01','02','03'])pngs.push(['social/'+number+'.png',1080,1350]);
 for(const [file,w,h]of pngs){const m=await sharp(path.join(root,file)).metadata();assert.equal(m.width,w);assert.equal(m.height,h);assert.equal(m.channels,3);assert.equal(m.hasAlpha,false);assert.ok(fs.statSync(path.join(root,file)).size<8*1024*1024);}
 let checkedLocalLinks=0;const htmlResults=[];
 const validator=process.env.KREATIV_HTML_VALIDATE_BIN||'/Users/andreiolaru/.npm/_npx/1b843fc40880c85c/node_modules/html-validate/bin/html-validate.mjs';
 assert.ok(fs.existsSync(validator),'Set KREATIV_HTML_VALIDATE_BIN to an installed html-validate CLI');
 for(const file of ['index.html','tutorial/preview.html']){
  const html=fs.readFileSync(path.join(root,file),'utf8');assert.ok(html.startsWith('<!DOCTYPE html>'));assert.ok(html.includes('noindex,nofollow'));assert.equal((html.match(/<h1\b/g)||[]).length,1);
  for(const match of html.matchAll(/(?:href|src)="([^"]+)"/g)){const value=match[1].replaceAll('&amp;','&');if(/^(https?:|data:|mailto:|#)/.test(value))continue;assert.ok(fs.existsSync(path.resolve(path.dirname(path.join(root,file)),decodeURIComponent(value.split('#')[0]))),file+': '+value);checkedLocalLinks++;}
  const r=spawnSync(process.execPath,[validator,'--config','/Users/andreiolaru/Documents/ChatGPT/madebykreativ.com/.htmlvalidate.json',path.join(root,file)],{encoding:'utf8'});assert.equal(r.status,0,r.stdout+'\n'+r.stderr);htmlResults.push(file);
 }
 const tutorial=fs.readFileSync(path.join(root,'tutorial/discover-fonts-android-kreativ-font.md'),'utf8');assert.ok(tutorial.includes('app: "kreativ-font"'));assert.equal((tutorial.match(/\[!\[/g)||[]).length,5);
 const links=[...tutorial.matchAll(/\]\((https:\/\/play\.google\.com\/[^)]+)\)/g)].map(m=>new URL(m[1]));assert.equal(links.length,2);for(const link of links){assert.equal(link.searchParams.get('id'),'com.kreativ.fontapp');assert.equal(link.searchParams.get('utm_campaign'),copy.campaign);}
 for(const p of copy.posts){const link=new URL(copy.store);link.searchParams.set('referrer',new URLSearchParams({utm_source:p.source,utm_medium:p.medium,utm_campaign:copy.campaign}).toString());assert.equal(new URLSearchParams(link.searchParams.get('referrer')).get('utm_campaign'),copy.campaign);}
 const r=spawnSync('/opt/homebrew/bin/ffprobe',['-v','error','-show_entries','format=duration,size','-show_entries','stream=codec_name,width,height,codec_type','-of','json',path.join(root,'video/kreativfont-screenshot-tour.mp4')],{encoding:'utf8'});assert.equal(r.status,0,r.stderr);const media=JSON.parse(r.stdout);assert.equal(media.streams.length,1);assert.equal(media.streams[0].codec_type,'video');assert.equal(media.streams[0].codec_name,'h264');assert.equal(media.streams[0].width,1080);assert.equal(media.streams[0].height,1920);assert.equal(Number(media.format.duration),20);
 const result={date:copy.date,status:'Static checks passed; unpublished review pack',titleCharacters:copy.listing.title.length,shortDescriptionCharacters:copy.listing.shortDescription.length,fullDescriptionCharacters:copy.listing.fullDescription.length,validatedPngs:pngs.length,verifiedSourceHashRecords:manifest.files.length,verifiedUnalteredTutorialCrops:manifest.files.length,checkedLocalLinks,htmlValidation:{pages:htmlResults.length,errors:0,warnings:0},tutorialImages:5,tutorialPlayLinks:2,media:{seconds:20,width:1080,height:1920,codec:'h264',audio:false,bytes:Number(media.format.size)},visualInspection:'Listing/social contact sheets, feature graphic and tutorial crops inspected. No repainted UI, clipped outer copy or AI badges observed. Source carousel partial cards and ellipses preserved.',browserLayoutVerified:false,browserLimitation:'Known local-file browser restriction respected; no alternate-browser or server workaround. Responsive HTML layout, keyboard/screen-reader interaction and video playback remain manual review.',consoleMutation:false,websiteBuildOrDeployment:false,publication:false,outreach:false};
 fs.writeFileSync(path.join(root,'verification.json'),JSON.stringify(result,null,2)+'\n');console.log(JSON.stringify(result,null,2));
}
main().catch(e=>{console.error(e);process.exitCode=1});
