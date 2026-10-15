// Rasterize the editable vector; never hand-edit the generated Play icon.
const path = require('node:path');
const sharp = require('sharp');
const assets = path.join(__dirname, '../store-assets/play');
sharp(path.join(assets, 'northlight-icon.svg'))
  .resize(512, 512).png().toFile(path.join(assets, 'northlight-icon-512.png'))
  .then(() => console.log('Rendered Northlight Play icon (512 × 512).'))
  .catch(error => { console.error(error.message); process.exitCode = 1; });
