import { test, expect } from '@playwright/test';
import fs from 'node:fs/promises';
import path from 'node:path';
import AxeBuilder from '@axe-core/playwright';
import { createRequire } from 'node:module';
const require = createRequire(import.meta.url);
const { PNG } = require('pngjs');
async function downloadPng(page) {
  const promise = page.waitForEvent('download');
  await page.getByRole('button', { name: 'Save PNG', exact: true }).click();
  const download = await promise;
  return PNG.sync.read(await fs.readFile(await download.path()));
}
async function ready(page) {
  await page.goto('/');
  await expect(page.locator('#preview')).toHaveJSProperty('width', 1080);
}
test('mixed word styling, matching output and full favorite persist across reload', async ({ page }) => {
  const errors = []; page.on('pageerror', error => errors.push(error.message));
  await ready(page);
  await page.getByLabel('Your text', { exact: true }).fill('Hello wonderful world');
  await page.getByRole('button', { name: 'Ink & paper theme', exact: true }).click();
  await page.getByRole('button', { name: 'Style whole text', exact: true }).click();
  await page.locator('#word-style').selectOption('plain');
  await page.getByRole('button', { name: 'Style word wonderful', exact: true }).click();
  await page.locator('#word-style').selectOption('script');
  await page.locator('#word-color').fill('#aa2255');
  await expect(page.locator('#text-output')).toHaveText('Hello 𝔀𝓸𝓷𝓭𝓮𝓻𝓯𝓾𝓵 world');
  await page.getByRole('button', { name: 'Story', exact: true }).click();
  await page.getByLabel('Transparent background').check();
  await page.locator('#resolution').selectOption('2');
  await page.getByRole('button', { name: 'Save design to favorites' }).click();
  await expect(page.locator('#saved-count')).toHaveText('1');
  await page.waitForTimeout(400);
  await page.reload();
  await expect(page.locator('#text-output')).toHaveText('Hello 𝔀𝓸𝓷𝓭𝓮𝓻𝓯𝓾𝓵 world');
  await expect(page.getByLabel('Transparent background')).toBeChecked();
  await expect(page.locator('#dimensions')).toHaveText('2160 × 3840 px');
  await page.getByRole('button', { name: 'Clear', exact: true }).click();
  await page.getByRole('button', { name: /Saved designs/ }).click();
  await page.getByRole('button', { name: 'Open saved design: Hello wonderful world' }).click();
  await expect(page.locator('#text-output')).toHaveText('Hello 𝔀𝓸𝓷𝓭𝓮𝓻𝓯𝓾𝓵 world');
  await page.getByRole('button', { name: /Saved designs/ }).click();
  await page.getByRole('button', { name: 'Delete saved design: Hello wonderful world' }).click();
  await expect(page.locator('#saved-count')).toHaveText('0');
  expect(errors).toEqual([]);
});
test('all PNG formats decode at the requested size, with transparency and custom word color', async ({ page }) => {
  await ready(page);
  await page.getByLabel('Your text', { exact: true }).fill('Make magic');
  await page.getByRole('button', { name: 'Style word magic', exact: true }).click();
  await page.locator('#word-color').fill('#ff0000');
  for (const [format, height] of [['Square', 1080], ['Portrait', 1350], ['Story', 1920]]) {
    await page.getByRole('button', { name: format, exact: true }).click();
    const png = await downloadPng(page);
    expect([png.width, png.height]).toEqual([1080, height]);
    expect(png.data[3]).toBe(255);
  }
  await page.getByLabel('Transparent background').check();
  await page.locator('#resolution').selectOption('2');
  const png = await downloadPng(page);
  expect([png.width, png.height]).toEqual([2160, 3840]);
  expect(png.data[3]).toBe(0);
  let coloredPixels = 0, painted = 0;
  for (let i = 0; i < png.data.length; i += 4) {
    if (png.data[i + 3] > 100) painted++;
    if (png.data[i] > 200 && png.data[i + 1] < 20 && png.data[i + 2] < 20 && png.data[i + 3] > 200) coloredPixels++;
  }
  expect(painted).toBeGreaterThan(1000); expect(coloredPixels).toBeGreaterThan(500);
  await fs.mkdir('artifacts/screenshots', { recursive: true });
  await fs.writeFile(`artifacts/screenshots/${test.info().project.name}-transparent.png`, PNG.sync.write(png));
});
test('editing, favorites and export work with the network offline and no third-party requests', async ({ page, context }) => {
  const requests = []; page.on('request', request => requests.push(request.url()));
  await ready(page);
  await context.setOffline(true);
  await page.getByLabel('Your text', { exact: true }).fill('Made offline');
  await page.getByRole('button', { name: 'Lilac daydream theme' }).click();
  await expect(page.locator('#text-output')).toHaveText('𝓜𝓪𝓭𝓮 𝓸𝓯𝓯𝓵𝓲𝓷𝓮');
  await page.getByRole('button', { name: 'Save design to favorites' }).click();
  const png = await downloadPng(page);
  expect(png.width).toBe(1080);
  expect(requests.every(url => url.startsWith('http://127.0.0.1:5174/'))).toBe(true);
});
test('long messages fit, user text stays inert, and the mobile page does not overflow', async ({ page }) => {
  await ready(page);
  await page.getByLabel('Your text', { exact: true }).fill('<img src=x onerror=alert(1)> Ștefan 🌸 ' + 'wonderful '.repeat(90));
  await expect(page.locator('#text-output img')).toHaveCount(0);
  await expect(page.locator('#fit-note')).not.toBeEmpty();
  await expect.poll(() => page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  await page.getByLabel('Transparent background').check();
  const png = await downloadPng(page); expect(png.height).toBe(1080);
  for (let y = 0; y < png.height; y++) for (const x of [0, 1079]) {
    expect(png.data[(y * png.width + x) * 4 + 3]).toBe(0);
  }
  await page.getByRole('button', { name: 'Clear', exact: true }).click();
  await expect(page.getByRole('button', { name: 'Save PNG', exact: true })).toBeDisabled();
});
test('visual evidence and privacy dialog', async ({ page }) => {
  await ready(page);
  await page.getByRole('button', { name: 'Style word wonderful.', exact: true }).click();
  await page.locator('#word-style').selectOption('script');
  await page.locator('#word-color').fill('#6554c0');
  await page.waitForTimeout(250);
  await fs.mkdir('artifacts/screenshots', { recursive: true });
  await page.screenshot({ path: path.resolve(`artifacts/screenshots/${test.info().project.name}-editor.png`), fullPage: true });
  await page.getByRole('button', { name: 'Offline by design' }).click();
  await expect(page.getByRole('dialog')).toContainText('Your words are yours.');
  await page.getByText('Read the full privacy policy', { exact: true }).click();
  await expect(page.locator('#privacy-policy-text')).toBeVisible();
  await expect(page.locator('#privacy-policy-text')).toContainText('com.kreativfont.studio');
  await expect(page.getByRole('dialog')).toContainText('info@madebykreativ.com');
  await expect.poll(() => page.evaluate(() => {
    const dialog = document.querySelector('#privacy-dialog');
    return dialog.scrollWidth <= dialog.clientWidth;
  })).toBe(true);
  await page.getByRole('button', { name: 'Back to creating' }).click();
});

test('undo and redo recover text, word styling, theme and export settings without losing the draft', async ({ page }) => {
  await ready(page);
  await page.getByLabel('Your text', { exact: true }).fill('Keep this design');
  await page.getByRole('button', { name: 'Style word design', exact: true }).click();
  await page.locator('#word-style').selectOption('script');
  await page.locator('#word-color').fill('#aa2255');
  await page.getByRole('button', { name: 'Story', exact: true }).click();
  await page.getByLabel('Transparent background').check();
  await page.getByRole('button', { name: 'Clear', exact: true }).click();
  await expect(page.locator('#copy-text')).toBeDisabled();
  await page.getByRole('button', { name: 'Undo', exact: true }).click();
  await expect(page.getByLabel('Your text', { exact: true })).toHaveValue('Keep this design');
  await expect(page.locator('#text-output')).toHaveText('𝐊𝐞𝐞𝐩 𝐭𝐡𝐢𝐬 𝓭𝓮𝓼𝓲𝓰𝓷');
  await expect(page.getByLabel('Transparent background')).toBeChecked();
  await expect(page.locator('#dimensions')).toHaveText('1080 × 1920 px');
  await page.getByRole('button', { name: 'Redo', exact: true }).click();
  await expect(page.getByLabel('Your text', { exact: true })).toHaveValue('');
  await page.keyboard.press('Control+z');
  await expect(page.getByLabel('Your text', { exact: true })).toHaveValue('Keep this design');
  await page.getByRole('button', { name: 'After hours theme' }).click();
  await page.keyboard.press('Control+z');
  await expect(page.getByRole('button', { name: 'The original theme' })).toHaveAttribute('aria-pressed', 'true');
  await expect(page.locator('#text-output')).toHaveText('𝐊𝐞𝐞𝐩 𝐭𝐡𝐢𝐬 𝓭𝓮𝓼𝓲𝓰𝓷');
  await expect(page.locator('#draft-status')).toHaveText('Saved on this device');
  await page.reload();
  await expect(page.getByLabel('Your text', { exact: true })).toHaveValue('Keep this design');
  await expect(page.locator('#text-output')).toHaveText('𝐊𝐞𝐞𝐩 𝐭𝐡𝐢𝐬 𝓭𝓮𝓼𝓲𝓰𝓷');
});

test('multiple saved deletions can be undone in order and the restored collection persists', async ({ page }) => {
  await ready(page);
  for (const text of ['First design', 'Second design']) {
    await page.getByLabel('Your text', { exact: true }).fill(text);
    await page.getByRole('button', { name: 'Save design to favorites' }).click();
    await expect(page.locator('#favorite')).toBeEnabled();
  }
  await page.getByRole('button', { name: /Saved designs/ }).click();
  await page.getByRole('button', { name: 'Delete saved design: Second design' }).click();
  await expect(page.locator('#saved-count')).toHaveText('1');
  await page.getByRole('button', { name: 'Delete saved design: First design' }).click();
  await expect(page.locator('#saved-count')).toHaveText('0');
  await expect(page.getByRole('button', { name: 'Undo delete', exact: true })).toBeFocused();
  await page.getByRole('button', { name: 'Undo delete', exact: true }).click();
  await expect(page.locator('#saved-count')).toHaveText('1');
  await page.getByRole('button', { name: 'Undo delete', exact: true }).click();
  await expect(page.locator('#saved-count')).toHaveText('2');
  await expect(page.locator('.saved-title')).toHaveText(['Second design', 'First design']);
  await expect(page.locator('#delete-recovery')).toBeHidden();
  await page.reload();
  await page.getByRole('button', { name: /Saved designs/ }).click();
  await expect(page.locator('.saved-title')).toHaveText(['Second design', 'First design']);
});

test('unfittable messages block image exports, remain copyable, and recover in a taller format', async ({ page }) => {
  await ready(page);
  await page.getByLabel('Your text', { exact: true }).fill('Line\n'.repeat(90));
  await expect(page.locator('#fit-note')).toContainText('Too many lines');
  await expect(page.locator('#save-png')).toBeDisabled();
  await expect(page.locator('#share-png')).toBeDisabled();
  await expect(page.locator('#copy-text')).toBeEnabled();
  await page.getByRole('button', { name: 'Story', exact: true }).click();
  await expect(page.locator('#save-png')).toBeEnabled();
  await expect(page.locator('#fit-note')).toContainText('Your full message is included');
  const png = await downloadPng(page);
  expect([png.width, png.height]).toEqual([1080, 1920]);
  await page.getByLabel('Your text', { exact: true }).fill('A' + '\n'.repeat(1198) + 'B');
  await expect(page.locator('#save-png')).toBeDisabled();
  await expect(page.locator('#fit-note')).toContainText('Too many lines');
  await page.getByRole('button', { name: 'Undo', exact: true }).click();
  await expect(page.locator('#save-png')).toBeEnabled();
});

test('small screens and 200 percent text keep controls reachable and meet accessibility checks', async ({ page }) => {
  await ready(page);
  for (const [width, height, percent] of [[320, 568, 100], [360, 640, 200], [640, 360, 200], [800, 600, 200]]) {
    await page.setViewportSize({ width, height });
    await page.evaluate(percent => { document.documentElement.style.fontSize = `${percent}%`; }, percent);
    await expect.poll(() => page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
    const undersized = await page.evaluate(() => [...document.querySelectorAll('button, select, input, textarea, summary, .brand')]
      .filter(el => el.getClientRects().length && !el.closest('[hidden]') && el.type !== 'checkbox')
      .filter(el => { const r = el.getBoundingClientRect(); return r.width < 47.9 || r.height < 47.9; })
      .map(el => el.id || el.textContent));
    expect(undersized).toEqual([]);
    const scan = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21aa']).analyze();
    expect(scan.violations.map(v => ({ id: v.id, nodes: v.nodes.map(n => n.target) }))).toEqual([]);
  }
  await fs.mkdir('artifacts/screenshots', { recursive: true });
  await page.setViewportSize({ width: 360, height: 800 });
  await page.screenshot({ path: `artifacts/screenshots/${test.info().project.name}-large-text-viewport.png` });
  await page.screenshot({ path: `artifacts/screenshots/${test.info().project.name}-large-text.png`, fullPage: true });
  await page.getByRole('button', { name: 'Offline by design' }).click();
  const scan = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21aa']).analyze();
  expect(scan.violations.map(v => v.id)).toEqual([]);
  await page.getByRole('button', { name: 'Back to creating' }).click();
  await page.evaluate(() => { document.documentElement.style.fontSize = '100%'; });
  await page.getByRole('button', { name: 'Save design to favorites' }).click();
  await page.getByRole('button', { name: /Saved designs/ }).click();
  await expect(page.locator('.saved-title')).toHaveCount(1);
  const savedScan = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21aa']).analyze();
  expect(savedScan.violations.map(v => ({ id: v.id, nodes: v.nodes.map(n => n.target) }))).toEqual([]);
});
