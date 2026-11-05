// Mechanical crops of complete, existing UI panels. Never paint over app content.
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const sharp = require('/Users/andreiolaru/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/sharp');
const original = path.join(__dirname,'../tutorial/screenshots');
const output = path.join(__dirname,'v2/captures');
const crops = [
  {source:'04-before-after.png',file:'01-metadata.png',left:52,top:176,width:976,height:1040},
  {source:'01-choose-photos.png',file:'02-originals.png',left:52,top:130,width:976,height:1274},
  {source:'03-export-options.png',file:'03-options.png',left:52,top:176,width:976,height:1578},
  {source:'05-ready-to-share.png',file:'04-sharing.png',left:52,top:176,width:976,height:1058},
];
async function main() {
  fs.mkdirSync(output,{recursive:true});
  for (const {source,file,...rect} of crops) {
    const meta=await sharp(path.join(original,source)).metadata();
    assert.equal(meta.width,1080); assert.equal(meta.height,2340);
    await sharp(path.join(original,source)).extract(rect).png().toFile(path.join(output,file));
  }
  fs.writeFileSync(path.join(__dirname,'v2/capture-crops.json'),JSON.stringify({sourceDirectory:original,device:'Galaxy A55',version:'1.2.0 Play debug; only release-shared UI visible',date:'2026-10-17',note:'Complete UI panels cropped from verified tutorial captures. Status/navigation bars and adjacent partial cards excluded; no UI pixels repainted or rearranged.',crops},null,2)+'\n');
  console.log('Prepared four complete real UI panel crops. Original captures unchanged.');
}
main().catch(error=>{console.error(error);process.exitCode=1;});
