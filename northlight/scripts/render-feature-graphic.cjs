// Regenerate the Play banner from the editable vector, using the Field identity.
const path = require('node:path');
const sharp = require('sharp');
const assets = path.join(__dirname, '../store-assets/play');
sharp(path.join(assets, 'northlight-feature-graphic.svg'))
  .resize(1024, 500).flatten({ background: '#0b1320' }).removeAlpha().png()
  .toFile(path.join(assets, 'northlight-feature-graphic.png'))
  .then(() => console.log('Rendered Northlight feature graphic (1024 × 500).'))
  .catch(error => { console.error(error.message); process.exitCode = 1; });
