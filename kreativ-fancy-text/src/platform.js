import { Capacitor, registerPlugin } from '@capacitor/core';
import { Preferences } from '@capacitor/preferences';
import { Clipboard } from '@capacitor/clipboard';
import { Filesystem, Directory } from '@capacitor/filesystem';
import { Share } from '@capacitor/share';

const ExportFile = registerPlugin('ExportFile');
export const isNative = Capacitor.isNativePlatform();
export async function readSaved(key) {
  try {
    const value = isNative ? (await Preferences.get({ key })).value : localStorage.getItem(key);
    return value ? JSON.parse(value) : null;
  } catch { return null; }
}
export async function writeSaved(key, value) {
  const text = JSON.stringify(value);
  if (isNative) await Preferences.set({ key, value: text });
  else localStorage.setItem(key, text);
}
export async function copyText(text) {
  if (isNative) await Clipboard.write({ string: text });
  else await navigator.clipboard.writeText(text);
}
export async function shareText(text) {
  if (isNative) await Share.share({ text, dialogTitle: 'Share your Kreativ text' });
  else if (navigator.share) await navigator.share({ text });
  else { await copyText(text); return 'Text copied. Paste it into your favorite app.'; }
  return 'Share sheet opened.';
}
const base64For = blob => new Promise((resolve, reject) => {
  const reader = new FileReader(); reader.onerror = reject;
  reader.onload = () => resolve(reader.result.split(',')[1]); reader.readAsDataURL(blob);
});
export const filenameFor = () => `kreativ-${new Date().toISOString().replace(/[:.]/g, '-')}.png`;
async function cacheImage(blob, filename, folder) {
  try {
    const { files } = await Filesystem.readdir({ path: folder, directory: Directory.Cache });
    await Promise.all(files.filter(file => (file.mtime || 0) < Date.now() - 86_400_000)
      .map(file => Filesystem.deleteFile({ path: `${folder}/${file.name}`, directory: Directory.Cache })));
  } catch { /* Folder is created by its first export. */ }
  return Filesystem.writeFile({ path: `${folder}/${filename}`, directory: Directory.Cache,
    data: await base64For(blob), recursive: true });
}
export async function saveImage(blob) {
  const filename = filenameFor();
  if (isNative) {
    // Android persists pending activity-call options. Keep large image bytes on disk,
    // rather than putting base64 data in its size-limited saved-state Bundle.
    const { uri } = await cacheImage(blob, filename, 'kreativ-export');
    try {
      const result = await ExportFile.savePng({ filename, path: uri });
      return result.cancelled ? null : 'PNG saved to your selected folder.';
    } finally {
      try { await Filesystem.deleteFile({ path: `kreativ-export/${filename}`, directory: Directory.Cache }); }
      catch { /* Native completion also removes the cached source. */ }
    }
  }
  const url = URL.createObjectURL(blob), link = document.createElement('a');
  link.href = url; link.download = filename; link.click();
  setTimeout(() => URL.revokeObjectURL(url), 30_000);
  return 'PNG downloaded.';
}
export async function shareImage(blob) {
  const filename = filenameFor();
  if (isNative) {
    const { uri } = await cacheImage(blob, filename, 'kreativ-share');
    await Share.share({ files: [uri], dialogTitle: 'Share your Kreativ image' });
    return 'Share sheet opened.';
  }
  const file = new File([blob], filename, { type: 'image/png' });
  if (navigator.canShare?.({ files: [file] })) {
    await navigator.share({ files: [file] }); return 'Share sheet opened.';
  }
  await saveImage(blob); return 'PNG downloaded. You can share the file from your downloads.';
}
