import { test, expect } from '@playwright/test';
import fs from 'node:fs/promises';
import { PNG } from 'pngjs';
import AxeBuilder from '@axe-core/playwright';

const ownedPreview = 'http://127.0.0.1:5187/';
async function openStudio(page) {
  await page.locator('#pro-studio > summary').click();
  await expect(page.locator('#template-grid button')).toHaveCount(12);
}
async function pngDownload(page) {
  const pending = page.waitForEvent('download');
  await page.locator('#save-png').click();
  return PNG.sync.read(await fs.readFile(await (await pending).path()));
}
test('free users can explore premium templates without changing drafts or unlocking exports', async ({ page }) => {
  await page.goto('/');
  await page.locator('#text-input').fill('Keep my own words');
  await expect(page.locator('#draft-status')).toHaveText('Saved on this device');
  const original = await page.evaluate(() => localStorage.getItem('kreativ-draft-v1'));
  await openStudio(page);
  await page.getByRole('button', { name: 'Preview template: Quiet thought', exact: true }).click();
  await expect(page.locator('#pro-preview')).toBeVisible();
  await expect(page.locator('#apply-pro-look')).toHaveText('Unlock Fancy Text Pro');
  await page.locator('#apply-pro-look').click();
  await expect(page.locator('#pro-dialog')).toBeVisible();
  await expect(page.locator('#purchase-pro')).toBeDisabled();
  await expect(page.locator('#pro-dialog')).toContainText('One-time purchase. No subscription.');
  await page.getByRole('button', { name: 'Close Fancy Text Pro information', exact: true }).click();
  expect(await page.evaluate(() => localStorage.getItem('kreativ-draft-v1'))).toBe(original);
  await expect(page.locator('#text-input')).toHaveValue('Keep my own words');
  const png = await pngDownload(page); expect(png.width).toBe(1080);
  await page.evaluate(() => localStorage.setItem('kreativ-pro-unlocked', 'true'));
  await page.reload();
  await expect(page.locator('.app-header #open-pro')).toHaveCount(0);
  await openStudio(page);
  await page.locator('#open-pro').click();
  await expect(page.locator('#pro-title')).toHaveText('Fancy Text Pro');
  await expect(page.locator('#purchase-pro')).toBeDisabled();
});
test('restored premium designs remain intact when locked and can be converted to a free look', async ({ page }) => {
  await page.addInitScript(() => localStorage.setItem('kreativ-draft-v1', JSON.stringify({
    text: 'My saved words', theme: 'studio', baseStyle: 'bold', background: { kind: 'solid', from: '#112233', to: '#112233', angle: 0 }, ink: '#ffffff', frame: 'line', scale: 2, transparent: true,
  })));
  await page.goto('/');
  await expect(page.locator('#pro-design-note')).toBeVisible();
  await expect(page.locator('#save-png')).toBeDisabled();
  await expect(page.locator('#copy-text')).toBeEnabled();
  await page.locator('#free-look').click();
  await expect(page.locator('#pro-design-note')).toBeHidden();
  await expect(page.locator('#text-input')).toHaveValue('My saved words');
  await expect(page.locator('#transparent')).toBeChecked();
  await expect(page.locator('#resolution')).toHaveValue('2');
  const png = await pngDownload(page); expect(png.width).toBe(2160); expect(png.data[3]).toBe(0);
});
test('owned templates apply to current words, are undoable, and preserve their look after reload', async ({ page }) => {
  await page.goto(ownedPreview);
  await expect(page.locator('.app-header #open-pro')).toHaveCount(0);
  await page.locator('#text-input').fill('My own words');
  await openStudio(page);
  await page.getByRole('button', { name: 'Preview template: Lilac letter', exact: true }).click();
  await expect(page.locator('#apply-pro-look')).toHaveText('Use template');
  await page.locator('#apply-pro-look').click();
  await expect(page.locator('#text-input')).toHaveValue('My own words');
  await expect(page.locator('#save-png')).toBeEnabled();
  await expect(page.locator('#draft-status')).toHaveText('Saved on this device');
  const applied = await page.evaluate(() => JSON.parse(localStorage.getItem('kreativ-draft-v1')));
  expect(applied.background.kind).toBe('linear'); expect(applied.frame).toBe('arch');
  await page.locator('#undo').click();
  await expect.poll(() => page.evaluate(() => JSON.parse(localStorage.getItem('kreativ-draft-v1')).background)).toBe(null);
  await page.locator('#redo').click();
  await expect.poll(() => page.evaluate(() => JSON.parse(localStorage.getItem('kreativ-draft-v1')).frame)).toBe('arch');
  await page.reload();
  await expect(page.locator('#text-input')).toHaveValue('My own words');
  await expect(page.locator('#save-png')).toBeEnabled();
});
test('custom gradient exports decode correctly and named presets preserve new words, save, restore and undo deletion', async ({ page }) => {
  await page.goto(ownedPreview);
  await page.locator('#text-input').fill('Make magic');
  await page.getByRole('button', { name: 'Style word magic', exact: true }).click();
  await page.locator('#word-color').fill('#6554c0');
  await openStudio(page);
  await page.locator('#background-kind').selectOption('linear');
  await page.locator('#background-from').fill('#112233');
  await page.locator('#background-to').fill('#ddeeff');
  await page.locator('#background-angle').selectOption('0');
  await page.locator('#design-ink').fill('#ff0000');
  await page.locator('#design-frame').selectOption('double');
  await page.locator('#preview-background').click();
  await page.locator('#apply-pro-look').click();
  await expect(page.locator('#draft-status')).toHaveText('Saved on this device');
  expect(await page.evaluate(() => JSON.parse(localStorage.getItem('kreativ-draft-v1')).overrides[2].color)).toBe('#6554c0');
  const png = await pngDownload(page);
  expect(png.data[0]).toBeLessThan(30); expect(png.data[1]).toBeLessThan(50); expect(png.data[3]).toBe(255);
  const right = (png.width - 1) * 4; expect(png.data[right]).toBeGreaterThan(200);
  await page.locator('#preset-name').fill('My daily look'); await page.locator('#save-preset').click();
  await expect(page.getByRole('button', { name: 'Apply style preset: My daily look', exact: true })).toBeVisible();
  await page.locator('#text-input').fill('Different words');
  await page.getByRole('button', { name: 'After hours theme', exact: true }).click();
  await page.getByRole('button', { name: 'Apply style preset: My daily look', exact: true }).click();
  await expect(page.locator('#text-input')).toHaveValue('Different words');
  await expect(page.locator('#draft-status')).toHaveText('Saved on this device');
  const draft = await page.evaluate(() => JSON.parse(localStorage.getItem('kreativ-draft-v1')));
  expect(draft.ink).toBe('#ff0000'); expect(draft.background.from).toBe('#112233'); expect(draft.overrides).toEqual({});
  await page.getByRole('button', { name: 'Delete style preset: My daily look', exact: true }).click();
  await expect(page.locator('#preset-list')).not.toContainText('My daily look');
  await page.locator('#undo-preset-delete').click();
  await page.reload(); await openStudio(page);
  await expect(page.getByRole('button', { name: 'Apply style preset: My daily look', exact: true })).toBeVisible();
  await page.locator('#transparent').check();
  const transparent = await pngDownload(page); expect(transparent.data[3]).toBe(0);
  await fs.mkdir('artifacts/pro-screenshots', { recursive: true });
  await page.screenshot({ path: 'artifacts/pro-screenshots/' + test.info().project.name + '-studio.png', fullPage: true });
});
test('Pro panels and purchase dialog remain accessible at narrow widths and 200 percent text', async ({ page }) => {
  await page.goto('/');
  await openStudio(page);
  for (const [width, height, percent] of [[320, 700, 100], [360, 800, 200]]) {
    await page.setViewportSize({ width, height });
    await page.evaluate(p => document.documentElement.style.fontSize = p + '%', percent);
    await expect.poll(() => page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
    const scan = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21aa']).analyze();
    expect(scan.violations.map(v => ({ id: v.id, nodes: v.nodes.map(n => n.target) }))).toEqual([]);
  }
  await page.locator('#open-pro').click();
  await expect.poll(() => page.locator('#pro-dialog').evaluate(el => el.scrollWidth <= el.clientWidth)).toBe(true);
  const scan = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21aa']).analyze();
  expect(scan.violations.map(v => ({ id: v.id, nodes: v.nodes.map(n => n.target) }))).toEqual([]);
});
