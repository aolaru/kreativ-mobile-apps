import { Capacitor, registerPlugin } from '@capacitor/core';
const NativeBilling = registerPlugin('KreativBilling');
// This preview is compiled out of production builds. No browser flag or saved
// boolean can grant an Android entitlement; that comes from signed Play receipts.
const developmentPreview = import.meta.env.DEV && import.meta.env.VITE_PRO_PREVIEW === '1';
let current = { owned: developmentPreview, available: false, native: Capacitor.isNativePlatform(), preview: developmentPreview,
  price: null, pending: false, message: developmentPreview ? 'Development preview. Purchases are disabled.' : 'Purchases and restore are available in the Android app.' };
const subscribers = new Set();
function receive(update) {
  current = { ...current, ...update, owned: update.owned === true, preview: developmentPreview };
  for (const listener of subscribers) listener(current);
}
export const billingState = () => ({ ...current });
export function observeBilling(listener) { subscribers.add(listener); listener(billingState()); return () => subscribers.delete(listener); }
export async function startBilling() {
  if (!current.native) return;
  try {
    await NativeBilling.addListener('billingStateChanged', receive);
    receive(await NativeBilling.getStatus());
  } catch { receive({ owned: false, available: false, message: 'Google Play is unavailable. You can keep using the free editor.' }); }
}
export async function purchasePro() {
  if (!current.native || developmentPreview || !current.available || current.owned) return;
  try { receive(await NativeBilling.purchase()); }
  catch { receive({ ...current, message: 'Could not open Google Play. Please try again.' }); }
}
export async function restorePro() {
  if (!current.native) return;
  try { receive(await NativeBilling.restore()); }
  catch { receive({ ...current, message: 'Could not restore right now. Check Google Play and try again.' }); }
}
