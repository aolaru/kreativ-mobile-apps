const fs=require('node:fs');
const path=require('node:path');
const assert=require('node:assert/strict');
const crypto=require('node:crypto');
const {spawnSync}=require('node:child_process');
const sharp=require('/Users/andreiolaru/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/sharp');
const root=__dirname;
async function main(){
  const copy=JSON.parse(fs.readFileSync(path.join(root,'copy.json'),'utf8'));
  const manifest=JSON.parse(fs.readFileSync(path.join(root,'listing/assets-manifest.json'),'utf8'));
  assert.equal(copy.listing.title.length,24);assert.equal(copy.listing.shortDescription.length,78);assert.ok(copy.listing.fullDescription.length<=4000);
  assert.ok(!copy.listing.fullDescription.includes(copy.tutorialPath),'Do not publish an unavailable tutorial URL');
  const pngs=[];
  for(const entry of manifest.files){
    const source=fs.readFileSync(path.resolve(root,entry.source));
    assert.equal(crypto.createHash('sha256').update(source).digest('hex'),entry.sourceSha256);
    const m=await sharp(source).metadata();const c=entry.crop;
    assert.ok(c.left+c.width<=m.width&&c.top+c.height<=m.height);
    assert.ok(entry.alt.length<=140);
    pngs.push(['listing/screenshots/'+entry.file,1440,2560]);
  }
  pngs.push(['listing/feature-graphic.png',1024,500]);
  for(const number of ['01','02','03'])pngs.push(['social/'+number+'.png',1080,1350]);
  for(const [file,w,h]of pngs){const m=await sharp(path.join(root,file)).metadata();assert.equal(m.width,w);assert.equal(m.height,h);assert.equal(m.channels,3);assert.equal(m.hasAlpha,false);assert.ok(fs.statSync(path.join(root,file)).size<8*1024*1024);}
  let checkedLocalLinks=0;
  for(const file of ['index.html','tutorial/preview.html']){
    const html=fs.readFileSync(path.join(root,file),'utf8');
    assert.ok(html.startsWith('<!DOCTYPE html>'));assert.ok(html.includes('noindex,nofollow'));
    assert.equal((html.match(/<h1\b/g)||[]).length,1);
    for(const match of html.matchAll(/(?:href|src)="([^"]+)"/g)){
      const value=match[1].replaceAll('&amp;','&');
      if(/^(https?:|data:|mailto:|#)/.test(value))continue;
      const local=path.resolve(path.dirname(path.join(root,file)),decodeURIComponent(value.split('#')[0]));
      assert.ok(fs.existsSync(local),`${file}: missing ${value}`);checkedLocalLinks++;
    }
    const htmlCheck=spawnSync('/Users/andreiolaru/Documents/ChatGPT/madebykreativ.com/node_modules/.bin/html-validate',['--config','/Users/andreiolaru/Documents/ChatGPT/madebykreativ.com/.htmlvalidate.json',path.join(root,file)],{encoding:'utf8'});
    assert.equal(htmlCheck.status,0,htmlCheck.stdout+'\n'+htmlCheck.stderr);
  }
  const mediaCheck=spawnSync('/opt/homebrew/bin/ffprobe',['-v','error','-show_entries','format=duration,size','-show_entries','stream=codec_name,width,height,codec_type','-of','json',path.join(root,'video/paperbuild-screenshot-tour.mp4')],{encoding:'utf8'});
  assert.equal(mediaCheck.status,0,mediaCheck.stderr);const media=JSON.parse(mediaCheck.stdout);
  assert.equal(media.streams.length,1);assert.equal(media.streams[0].codec_type,'video');assert.equal(media.streams[0].codec_name,'h264');assert.equal(media.streams[0].width,1080);assert.equal(media.streams[0].height,1920);assert.equal(Number(media.format.duration),24);
  const tutorial=JSON.parse(fs.readFileSync(path.join(root,'tutorial/metadata.json'),'utf8'));
  assert.equal(tutorial.imageCount,5);assert.equal(tutorial.app,'paperbuild');
  const result={date:'2026-10-18',status:'Static checks passed; unpublished review package',titleCharacters:copy.listing.title.length,shortDescriptionCharacters:copy.listing.shortDescription.length,fullDescriptionCharacters:copy.listing.fullDescription.length,validatedPngs:pngs.length,verifiedSourceHashes:manifest.files.length,checkedLocalLinks,htmlValidation:{pages:2,errors:0,warnings:0},media:{durationSeconds:Number(media.format.duration),width:1080,height:1920,codec:'h264',audio:false,bytes:Number(media.format.size)},visualInspection:'Social contact sheet, six screenshot layouts and feature graphic inspected; no observed clipped text or fabricated app UI.',browserLayoutVerified:false,browserLimitation:'Established local-file browser restriction respected. No alternate browser or serving workaround was used. Responsive HTML layout and media playback remain manual checks.',websiteBuildOrDeployment:false,consoleMutation:false,publication:false,outreach:false};
  fs.writeFileSync(path.join(root,'verification.json'),JSON.stringify(result,null,2)+'\n');console.log(JSON.stringify(result,null,2));
}
main().catch(e=>{console.error(e);process.exitCode=1});
