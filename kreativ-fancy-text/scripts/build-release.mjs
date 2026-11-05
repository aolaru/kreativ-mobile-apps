import { spawnSync, execFileSync } from 'node:child_process';
import { existsSync, mkdirSync, writeFileSync, copyFileSync, readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import path from 'node:path';
import os from 'node:os';
import { createHash } from 'node:crypto';
const root = fileURLToPath(new URL('../', import.meta.url));
const android = path.join(root, 'android');
const unsigned = process.argv.includes('--unsigned');
const { version } = JSON.parse(readFileSync(path.join(root, 'package.json'), 'utf8'));
const env = { ...process.env };
const studioJava = '/Applications/Android Studio.app/Contents/jbr/Contents/Home';
if (!env.JAVA_HOME && existsSync(studioJava)) env.JAVA_HOME = studioJava;
const sdk = env.ANDROID_HOME || env.ANDROID_SDK_ROOT || path.join(os.homedir(), 'Library/Android/sdk');
if (existsSync(sdk)) {
  env.ANDROID_HOME = sdk;
  writeFileSync(path.join(android, 'local.properties'), `sdk.dir=${sdk.replaceAll('\\', '\\\\')}\n`);
}
const configPath = env.KREATIV_SIGNING_CONFIG || path.join(os.homedir(), '.config/kreativ-android/signing.json');
if (!unsigned) {
  if (existsSync(configPath)) {
    const config = JSON.parse(readFileSync(configPath, 'utf8'));
    env.KREATIV_UPLOAD_KEYSTORE ||= config.keystore;
    env.KREATIV_UPLOAD_KEY_ALIAS ||= config.alias;
    env.KREATIV_UPLOAD_STORE_PASSWORD ||= config.storePassword;
    env.KREATIV_UPLOAD_KEY_PASSWORD ||= config.keyPassword;
  }
  for (const name of ['KREATIV_UPLOAD_KEYSTORE','KREATIV_UPLOAD_KEY_ALIAS','KREATIV_UPLOAD_STORE_PASSWORD','KREATIV_UPLOAD_KEY_PASSWORD']) {
    if (!env[name]) throw new Error(`Missing ${name}. Configure an upload key before building a Play bundle. Do not use a debug key.`);
  }
  if (!existsSync(env.KREATIV_UPLOAD_KEYSTORE)) throw new Error('Configured upload keystore does not exist.');
} else for (const name of ['KREATIV_UPLOAD_KEYSTORE','KREATIV_UPLOAD_KEY_ALIAS','KREATIV_UPLOAD_STORE_PASSWORD','KREATIV_UPLOAD_KEY_PASSWORD']) delete env[name];
const config = JSON.parse(readFileSync(path.join(root, 'capacitor.config.json'), 'utf8'));
config.loggingBehavior = 'none';
config.android = { ...config.android, loggingBehavior: 'none', webContentsDebuggingEnabled: false };
const releaseAssets = path.join(android, 'app/src/release/assets');
mkdirSync(releaseAssets, { recursive: true });
writeFileSync(path.join(releaseAssets, 'capacitor.config.json'), JSON.stringify(config, null, 2) + '\n');
const result = spawnSync(process.platform === 'win32' ? 'gradlew.bat' : './gradlew',
  // AGP 9 creates unit tests only for the tested build type (debug by default).
  // Use the aggregate task; signed-release verification is performed separately.
  [':app:bundleRelease', ':app:assembleRelease', ':app:lintRelease', ':app:test', '--no-daemon', '--max-workers=2'],
  { cwd: android, env, stdio: 'inherit' });
if (result.status !== 0) process.exit(result.status || 1);
const destination = path.join(root, 'artifacts/play'); mkdirSync(destination, { recursive: true });
const files = [
  [path.join(android, 'app/build/outputs/bundle/release/app-release.aab'), `kreativ-${version}${unsigned ? '-unsigned' : ''}.aab`],
  [path.join(android, `app/build/outputs/apk/release/app-release${unsigned ? '-unsigned' : ''}.apk`), `kreativ-${version}-${unsigned ? 'unsigned' : 'upload-signed'}-release.apk`],
  [path.join(android, 'app/build/outputs/mapping/release/mapping.txt'), `kreativ-${version}-mapping.txt`],
  [path.join(android, 'app/build/outputs/mapping/release/configuration.txt'), `kreativ-${version}-r8-configuration.txt`],
  [path.join(android, 'app/build/outputs/mapping/release/resources.txt'), `kreativ-${version}-resources.txt`],
  [path.join(android, 'app/build/reports/lint-results-release.html'), 'lint-results-release.html'],
  [path.join(android, 'app/build/reports/lint-results-release.xml'), 'lint-results-release.xml'],
];
for (const [source, filename] of files) {
  if (!existsSync(source)) throw new Error(`Required release output missing: ${source}`);
  const target = path.join(destination, filename); copyFileSync(source, target);
  const checksum = createHash('sha256').update(readFileSync(target)).digest('hex');
  writeFileSync(`${target}.sha256`, `${checksum}  ${filename}\n`);
}
if (!unsigned) {
  const certPath = path.join(root, 'store/upload-certificate.pem');
  execFileSync(path.join(env.JAVA_HOME, 'bin/keytool'), ['-exportcert', '-rfc', '-keystore', env.KREATIV_UPLOAD_KEYSTORE,
    '-alias', env.KREATIV_UPLOAD_KEY_ALIAS, '-storepass:env', 'KREATIV_UPLOAD_STORE_PASSWORD', '-file', certPath], { env, stdio: ['ignore','ignore','ignore'] });
}
const bundleMetadata = execFileSync('unzip', ['-p', files[0][0], 'BUNDLE-METADATA/com.android.tools.build.gradle/app-metadata.properties'], { encoding: 'utf8' });
const androidGradlePluginVersion = bundleMetadata.match(/^androidGradlePluginVersion=(.+)$/m)?.[1].trim();
if (!androidGradlePluginVersion) throw new Error('Bundle is missing its Android Gradle Plugin version.');
const gradleVersion = readFileSync(path.join(android, 'gradle/wrapper/gradle-wrapper.properties'), 'utf8').match(/gradle-([\d.]+)-(?:bin|all)\.zip/)?.[1];
writeFileSync(path.join(destination, 'build-info.json'), JSON.stringify({ version, applicationId: config.appId, uploadSigned: !unsigned,
  debuggable: false, webViewDebugging: false, logging: 'none', shrinkResources: true, minify: true,
  androidGradlePluginVersion, gradleVersion, r8Configuration: `kreativ-${version}-r8-configuration.txt`,
  signingConfigLocation: unsigned ? null : configPath, builtAt: new Date().toISOString() }, null, 2) + '\n');
console.log(`\nRelease artifacts: ${destination}\n${unsigned ? 'Unsigned review build — cannot be submitted to Google Play.' : 'Upload-signed bundle built. Play App Signing will use a separate app-signing key for delivery.'}`);
