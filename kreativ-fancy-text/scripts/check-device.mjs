// Explicitly select the phone or emulator authorized for this test.
import { chromium, expect } from '@playwright/test';
import { execFileSync } from 'node:child_process';
import { mkdirSync, writeFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import os from 'node:os';
import path from 'node:path';
const [serial, action = 'inspect'] = process.argv.slice(2);
if (!serial || !/^[\w-]+$/.test(serial)) throw new Error('Usage: node scripts/check-device.mjs AUTHORIZED_SERIAL ACTION');
const root = fileURLToPath(new URL('../', import.meta.url));
const evidence = path.join(root, 'artifacts/phone'); mkdirSync(evidence, { recursive: true });
const adbBinary = path.join(process.env.ANDROID_HOME || path.join(os.homedir(), 'Library/Android/sdk'), 'platform-tools/adb');
const adb = (...args) => execFileSync(adbBinary, ['-s', serial, ...args], { encoding: 'utf8', timeout: 30000 });
const pid = adb('shell', 'pidof', 'com.kreativfont.studio').trim();
if (!pid) throw new Error('Open Kreativ on the authorized device first.');
adb('forward', 'tcp:9223', `localabstract:webview_devtools_remote_${pid}`);
const browser = await chromium.connectOverCDP('http://127.0.0.1:9223');
const page = browser.contexts()[0].pages().find(p => p.url().startsWith('https://localhost'));
if (!page) throw new Error('Kreativ WebView is not ready.');
const errors = []; page.on('pageerror', error => errors.push(error.message));
async function waitForWindow(packageName) {
  for (let i = 0; i < 50; i++) {
    const focus = adb('shell', 'dumpsys', 'window').split('\n').find(line => line.includes('mCurrentFocus=')) || '';
    if (focus.includes(packageName)) return;
    await new Promise(resolve => setTimeout(resolve, 200));
  }
  throw new Error(`Expected native window: ${packageName}`);
}

try {
 await expect(page.locator('#preview')).toHaveJSProperty('width', 1080, { timeout: 20000 });
 if (action === 'inspect') {
   console.log(await page.evaluate(() => ({ title: document.title, width: innerWidth, height: innerHeight,
     safeTop: getComputedStyle(document.documentElement).getPropertyValue('--safe-area-inset-top'),
     safeBottom: getComputedStyle(document.documentElement).getPropertyValue('--safe-area-inset-bottom'),
     previewWidth: document.querySelector('#preview').width, dimensions: document.querySelector('#dimensions').textContent, transparent: document.querySelector('#transparent').checked, text: document.querySelector('#text-input').value, overflow: document.documentElement.scrollWidth > innerWidth,
     savedCount: document.querySelector('#saved-count').textContent,
   })));
 } else if (action === 'prepare') {
   await page.getByLabel('Your text', { exact: true }).fill('Hello from Kreativ');
   await page.getByRole('button', { name: 'Style word Kreativ', exact: true }).click();
   await page.locator('#word-style').selectOption('script');
   await page.locator('#word-color').fill('#6554c0');
   await page.getByRole('button', { name: 'Square', exact: true }).click();
   await page.locator('#resolution').selectOption('1');
   await page.getByLabel('Transparent background').uncheck();
   await page.getByRole('button', { name: 'Save design to favorites' }).click();
   await expect(page.locator('#saved-count')).toHaveText('1');
   await page.getByRole('button', { name: 'Copy text', exact: true }).click();
   await expect(page.locator('#toast')).toContainText('Styled text copied');
   await page.waitForTimeout(600);
   console.log({ prepared: true, nativeClipboard: 'passed', favorites: 1, text: await page.locator('#text-output').textContent() });
 } else if (action === 'share-text' || action === 'share-png') {
   const id = action === 'share-text' ? '#share-text' : '#share-png';
   await expect(page.locator(id)).toBeEnabled(); await page.locator(id).click();
   await waitForWindow('com.android.intentresolver');
   console.log(`${action} chooser is ready; inspect and cancel it.`);
 } else if (action === 'high-res') {
   await page.getByRole('button', { name: 'Story', exact: true }).click();
   await page.locator('#resolution').selectOption('2');
   await page.getByLabel('Transparent background').check();
   await expect(page.locator('#dimensions')).toHaveText('2160 × 3840 px');
   await page.waitForTimeout(500);
   console.log({ transparentStoryReady: true, dimensions: '2160 × 3840' });
 } else if (action === 'save-png') {
   await expect(page.locator('#save-png')).toBeEnabled(); await page.locator('#save-png').click();
   await waitForWindow('com.google.android.documentsui');
   console.log('Android document picker is ready.');
 } else if (action === 'resume') {
   for (const id of ['share-text','share-png','save-png']) await expect(page.locator(`#${id}`)).toBeEnabled({timeout:20000});
   console.log({ controlsEnabled: true, message: await page.locator('#toast').textContent(), errors });
 } else if (action === 'verify-restore') {
   await expect(page.getByLabel('Your text',{exact:true})).toHaveValue('Hello from Kreativ');
   await expect(page.locator('#text-output')).toHaveText('𝐇𝐞𝐥𝐥𝐨 𝐟𝐫𝐨𝐦 𝓚𝓻𝓮𝓪𝓽𝓲𝓿');
   await expect(page.locator('#saved-count')).toHaveText('1');
   console.log({ restoredAfterRestart: true, nativeDraftAndFavorite: 'passed', errors });
 } else if (action === 'restore-favorite') {
   await page.getByRole('button', { name: /Saved designs/ }).click();
   await page.getByRole('button', { name: 'Open saved design: Hello from Kreativ', exact: true }).click();
   await expect(page.locator('#dimensions')).toHaveText('1080 × 1080 px');
   await expect(page.getByLabel('Transparent background')).not.toBeChecked();
   await page.evaluate(() => { document.activeElement?.blur(); scrollTo(0,0); });
   console.log('Saved sample restored, ready for the user.');
 } else if (action === 'capture') {
   await page.evaluate(() => scrollTo(0,0));
   const focus = adb('shell', 'dumpsys', 'window').split('\n').find(line => line.includes('mCurrentFocus=')) || '';
   if (!focus.includes('com.kreativfont.studio/')) throw new Error('Kreativ must be visible for app screenshots.');
   writeFileSync(path.join(evidence,'phone-editor.png'), execFileSync(adbBinary,['-s',serial,'exec-out','screencap','-p']));
   await page.locator('.result').evaluate(element => element.scrollIntoView({ block: 'start' }));
   await page.waitForTimeout(250);
   writeFileSync(path.join(evidence,'phone-preview.png'), execFileSync(adbBinary,['-s',serial,'exec-out','screencap','-p']));
   console.log('Captured the visible Kreativ editor and preview.');
 } else throw new Error('Unknown action');
} finally { await browser.close(); }
