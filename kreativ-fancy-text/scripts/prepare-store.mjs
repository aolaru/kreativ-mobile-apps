import { chromium } from '@playwright/test';
import { preview } from 'vite';
import { readFileSync, writeFileSync, mkdirSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import path from 'node:path';
import { createHash } from 'node:crypto';
import { PNG } from 'pngjs';
const root = fileURLToPath(new URL('../', import.meta.url));
process.chdir(root);
const dir = path.join(root, 'store/graphics');
mkdirSync(path.join(dir, 'screenshots'), { recursive: true });
const policy = readFileSync('public/PRIVACY.txt', 'utf8');
mkdirSync('store/privacy', { recursive: true });
writeFileSync('store/privacy/PRIVACY.txt', policy);
const escapeHtml = text => text.replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;').replaceAll('"', '&quot;');
const policyParts = policy.trim().split('\n\n');
const policyBody = [policyParts[0].split('\n').slice(1).join('\n'), ...policyParts.slice(1)]
  .map(part => `<p>${escapeHtml(part).replaceAll('\n', ' ')}</p>`).join('\n');
writeFileSync('store/privacy/index.html', `<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Kreativ Fancy Text — Privacy Policy</title><style>body{font:17px/1.7 system-ui,sans-serif;max-width:760px;margin:48px auto;padding:0 24px;color:#292a30;background:#fffdf8}h1{font-size:32px;line-height:1.2}p{white-space:normal;overflow-wrap:anywhere}</style><main><h1>Kreativ Fancy Text — Privacy Policy</h1>${policyBody}</main></html>\n`);
const server = await preview({ preview: { host: '127.0.0.1', port: 5186, strictPort: true, open: false } });
let browser;
const manifest = [];
function record(filename, source) {
  const bytes = readFileSync(path.join(dir, filename));
  const png = PNG.sync.read(bytes);
  manifest.push({ file: filename, width: png.width, height: png.height, source,
    sha256: createHash('sha256').update(bytes).digest('hex') });
}
function rgb(filename) {
  const file = path.join(dir, filename);
  const png = PNG.sync.read(readFileSync(file));
  writeFileSync(file, PNG.sync.write(png, { colorType: 2 }));
}
try {
  browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({ viewport: { width: 432, height: 768 }, deviceScaleFactor: 2.5,
    isMobile: true, hasTouch: true, locale: 'en-US', colorScheme: 'light' });
  const page = await context.newPage();
  const errors = []; page.on('pageerror', error => errors.push(error.message));
  const outsideRequests = [];
  await page.route('**/*', route => {
    if (new URL(route.request().url()).origin !== 'http://127.0.0.1:5186') {
      outsideRequests.push(route.request().url()); return route.abort();
    }
    return route.continue();
  });
  await page.goto('http://127.0.0.1:5186/');
  await page.waitForFunction(() => document.querySelector('#preview')?.width === 1080);
  await page.evaluate(() => document.fonts.ready);
  async function settle() {
    await page.waitForTimeout(450);
    if (await page.evaluate(() => document.documentElement.scrollWidth > innerWidth)) throw new Error('Screenshot layout overflows');
    await page.locator('#toast').evaluate(el => el.classList.remove('visible'));
  }
  async function capture(name, selector) {
    if (selector) await page.locator(selector).evaluate(el => window.scrollTo(0, el.getBoundingClientRect().top + scrollY - document.querySelector('.view-tabs').offsetHeight - 24));
    else await page.evaluate(() => window.scrollTo(0, 0));
    await settle();
    const filename = `screenshots/${name}.png`;
    await page.screenshot({ path: path.join(dir, filename), animations: 'disabled' });
    rgb(filename); record(filename, 'Actual bundled web interface in Chromium, 432×768 CSS px at 2.5×; no Android system chrome.');
  }
  await page.locator('#text-input').fill('Make something\nwonderful.');
  await page.getByRole('button', { name: 'Style word wonderful.', exact: true }).click();
  await page.locator('#word-style').selectOption('script');
  await page.locator('#word-color').fill('#6554c0');
  await capture('01-create');
  await capture('02-word-styles', '.word-heading');
  await page.getByRole('button', { name: 'Lilac daydream theme', exact: true }).click();
  await capture('03-matching-image', '.result');
  await page.locator('#favorite').click();
  await page.getByRole('button', { name: 'Golden hour theme', exact: true }).click();
  await page.locator('#text-input').fill('A little hello.');
  await page.locator('#favorite').click();
  await page.getByRole('button', { name: 'Story', exact: true }).click();
  await page.getByLabel('Transparent background').check();
  await page.locator('#resolution').selectOption('2');
  await capture('04-png-options', '.text-result');
  await page.locator('#saved-tab').click();
  await capture('05-saved-designs');
  // Use the app's renderer for the artwork displayed in the feature graphic.
  const art = await page.evaluate(async () => {
    // Rendering has already occurred in the real UI. Reuse its saved-design canvases.
    return [...document.querySelectorAll('.saved-canvas')].map(canvas => canvas.toDataURL('image/png'));
  });
  const iconSource = readFileSync('public/icon.svg', 'utf8');
  const icon = await browser.newPage({ viewport: { width: 512, height: 512 }, deviceScaleFactor: 1 });
  await icon.setContent(`<style>html,body{margin:0;width:512px;height:512px}svg{width:512px;height:512px;display:block}</style>${iconSource.replace('rx="26"', 'rx="0"')}`);
  await icon.screenshot({ path: path.join(dir, 'icon-512.png') });
  const iconPath = path.join(dir, 'icon-512.png');
  writeFileSync(iconPath, PNG.sync.write(PNG.sync.read(readFileSync(iconPath)), { colorType: 6 }));
  record('icon-512.png', 'Existing public/icon.svg, full-bleed square for the Play icon mask.');
  await icon.close();
  const font = readFileSync('node_modules/@fontsource/dm-sans/files/dm-sans-latin-500-normal.woff2').toString('base64');
  const feature = await browser.newPage({ viewport: { width: 1024, height: 500 }, deviceScaleFactor: 1 });
  await feature.setContent(`<!doctype html><style>
    @font-face{font-family:DM;src:url(data:font/woff2;base64,${font})}*{box-sizing:border-box}body{margin:0;background:#eeecdf;color:#2e302c;font-family:DM,sans-serif;width:1024px;height:500px;overflow:hidden}
    main{position:relative;width:100%;height:100%;padding:64px}header{display:flex;align-items:center;gap:12px;font-size:22px;color:#6554c0}header svg{width:46px;height:46px}h1{font-size:49px;line-height:1.08;letter-spacing:-2px;font-weight:500;margin:35px 0 20px}h1 em{color:#6554c0;font-family:Georgia,serif;font-weight:400}p{font-size:17px;line-height:1.7;color:#646951;margin:0}.art{position:absolute;right:52px;top:96px;width:298px;height:298px;transform:rotate(6deg);box-shadow:0 15px 35px #292b2226;border:8px solid #fffefa}.back{right:96px;top:70px;transform:rotate(-10deg)}.star{position:absolute;right:58px;bottom:30px;color:#6554c0;font:75px Georgia}
    </style><main><header>${iconSource}<span>Kreativ Fancy Text</span></header><h1>Make your words<br><em>feel like you.</em></h1><p>Style text. Create PNGs.<br>Made on your device.</p><img class="art back" src="${art[1]}"><img class="art" src="${art[0]}"><span class="star">✳</span></main>`);
  await feature.evaluate(() => document.fonts.ready);
  await feature.screenshot({ path: path.join(dir, 'feature-1024x500.png') });
  rgb('feature-1024x500.png'); record('feature-1024x500.png', 'Brand vector, bundled font, and artwork rendered by the app; no stock imagery.');
  await feature.close();
  if (errors.length || outsideRequests.length) throw new Error(JSON.stringify({ errors, outsideRequests }));
  const { version } = JSON.parse(readFileSync('package.json', 'utf8'));
  writeFileSync(path.join(dir, 'manifest.json'), JSON.stringify({ version, generatedAt: new Date().toISOString(), assets: manifest }, null, 2) + '\n');
  console.log(`Prepared ${manifest.length} assets in ${dir}`);
} finally {
  await browser?.close();
  await new Promise(resolve => server.httpServer.close(resolve));
}
