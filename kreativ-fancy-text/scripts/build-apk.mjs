import { spawnSync } from 'node:child_process';
import { existsSync, mkdirSync, writeFileSync, copyFileSync, readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import path from 'node:path';
import os from 'node:os';
import { createHash } from 'node:crypto';
const root = fileURLToPath(new URL('../', import.meta.url));
const android = path.join(root, 'android');
const studioJava = '/Applications/Android Studio.app/Contents/jbr/Contents/Home';
const sdk = process.env.ANDROID_HOME || process.env.ANDROID_SDK_ROOT || path.join(os.homedir(), 'Library/Android/sdk');
const env = { ...process.env };
if (!env.JAVA_HOME && existsSync(studioJava)) env.JAVA_HOME = studioJava;
if (existsSync(sdk)) {
  env.ANDROID_HOME = sdk;
  writeFileSync(path.join(android, 'local.properties'), `sdk.dir=${sdk.replaceAll('\\', '\\\\')}\n`);
}
const result = spawnSync(process.platform === 'win32' ? 'gradlew.bat' : './gradlew', [':app:assembleDebug', '--no-daemon', '--max-workers=2'], { cwd: android, env, stdio: 'inherit' });
if (result.status !== 0) process.exit(result.status || 1);
const source = path.join(android, 'app/build/outputs/apk/debug/app-debug.apk');
const artifacts = path.join(root, 'artifacts'); mkdirSync(artifacts, { recursive: true });
const { version } = JSON.parse(readFileSync(path.join(root, 'package.json'), 'utf8'));
const target = path.join(artifacts, `kreativ-${version}-debug.apk`); copyFileSync(source, target);
const checksum = createHash('sha256').update(readFileSync(target)).digest('hex');
writeFileSync(`${target}.sha256`, `${checksum}  ${path.basename(target)}\n`);
console.log(`\nInstallable test APK: ${target}\nSHA-256: ${checksum}`);
