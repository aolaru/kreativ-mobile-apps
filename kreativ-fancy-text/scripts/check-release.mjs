import assert from 'node:assert/strict';
import { execFileSync } from 'node:child_process';
import { readFileSync, writeFileSync, readdirSync, existsSync, mkdtempSync, rmSync } from 'node:fs';
import { createHash, X509Certificate } from 'node:crypto';
import { fileURLToPath } from 'node:url';
import { homedir, tmpdir } from 'node:os';
import path from 'node:path';
import { PNG } from 'pngjs';
const root = fileURLToPath(new URL('../', import.meta.url));
process.chdir(root);
const { version } = JSON.parse(readFileSync('package.json', 'utf8'));
const gradle = readFileSync('android/app/build.gradle', 'utf8');
const versionCode = Number(gradle.match(/versionCode\s+(\d+)/)[1]);
const sdk = process.env.ANDROID_HOME || process.env.ANDROID_SDK_ROOT || path.join(homedir(), 'Library/Android/sdk');
const toolsVersion = readdirSync(path.join(sdk, 'build-tools')).filter(v => /^\d+(\.\d+)*$/.test(v))
  .sort((a, b) => a.localeCompare(b, undefined, { numeric: true })).at(-1);
const buildTools = path.join(sdk, 'build-tools', toolsVersion);
const java = process.env.JAVA_HOME || '/Applications/Android Studio.app/Contents/jbr/Contents/Home';
const folder = 'artifacts/play';
const apk = path.join(folder, `kreativ-${version}-upload-signed-release.apk`);
const aab = path.join(folder, `kreativ-${version}.aab`);
const run = (command, args) => execFileSync(command, args, { env: { ...process.env, JAVA_HOME: java }, encoding: 'utf8', maxBuffer: 64 * 1024 * 1024 });
const entry = (file, name) => execFileSync('unzip', ['-p', file, name], { maxBuffer: 30 * 1024 * 1024 });
const sha = bytes => createHash('sha256').update(bytes).digest('hex');
const normalize = fingerprint => fingerprint.replaceAll(':', '').trim().toLowerCase();
const checks = [];
function check(name, fn) { fn(); checks.push(name); console.log(`PASS ${name}`); }
// Remove an earlier success report before checking a new build.
writeFileSync(path.join(folder, 'verification.json'), JSON.stringify({ passed: false, version, checkedAt: new Date().toISOString() }) + '\n');
check('Release metadata is consistent', () => {
  assert.equal(gradle.match(/versionName\s+"([^"]+)"/)[1], version);
  const info = JSON.parse(readFileSync(path.join(folder, 'build-info.json'), 'utf8'));
  assert.equal(info.version, version); assert.equal(info.applicationId, 'com.kreativfont.studio');
  assert.equal(info.uploadSigned, true);
});
check('Packaged AGP and effective R8 configuration enable full optimisation', () => {
  const metadata = entry(aab, 'BUNDLE-METADATA/com.android.tools.build.gradle/app-metadata.properties').toString();
  const agp = metadata.match(/^androidGradlePluginVersion=(.+)$/m)?.[1].trim();
  assert(agp && Number(agp.split('.')[0]) >= 9, 'The bundle must be built with AGP 9 or newer');
  const info = JSON.parse(readFileSync(path.join(folder, 'build-info.json'), 'utf8'));
  assert.equal(info.androidGradlePluginVersion, agp);
  const effective = readFileSync(path.join(folder, `kreativ-${version}-r8-configuration.txt`), 'utf8');
  assert.match(effective, /proguard-android-optimize\.txt/);
  assert(!/^\s*-(?:dontoptimize|dontshrink|dontobfuscate)(?:\s|$)/m.test(effective), 'Global R8 disabling rules are forbidden');
  assert.match(gradle, /minifyEnabled\s+true/);
  assert.match(gradle, /shrinkResources\s+true/);
  const properties = readFileSync('android/gradle.properties', 'utf8');
  assert(!/^\s*android\.(?:enableR8\.fullMode|r8\.optimizedResourceShrinking)\s*=\s*false\s*$/m.test(properties),
    'AGP 9 full-mode and integrated resource shrinking must retain their enabled defaults');
});
check('All delivered release checksums match', () => {
  const files = readdirSync(folder).filter(f => f.endsWith('.sha256'));
  assert(files.length >= 5);
  for (const f of files) {
    const file = f.slice(0, -7);
    assert.equal(sha(readFileSync(path.join(folder, file))), readFileSync(path.join(folder, f), 'utf8').split(/\s/)[0], file);
  }
});
const cert = new X509Certificate(readFileSync('store/upload-certificate.pem'));
check('APK signature verifies against the preserved upload certificate', () => {
  const output = run(path.join(buildTools, 'apksigner'), ['verify', '--verbose', '--print-certs', apk]);
  const fingerprint = output.match(/Signer #1 certificate SHA-256 digest:\s*([a-f0-9]+)/i)?.[1];
  assert(fingerprint, 'Missing signer fingerprint');
  assert.equal(normalize(fingerprint), normalize(cert.fingerprint256));
  assert(!/Android Debug/i.test(output), 'Debug signer is forbidden');
});
check('AAB signature verifies and matches the APK certificate', () => {
  const output = run(path.join(java, 'bin/jarsigner'), ['-J-Duser.language=en', '-verify', aab]);
  assert.match(output, /jar verified\./);
  const key = run(path.join(java, 'bin/keytool'), ['-J-Duser.language=en', '-printcert', '-jarfile', aab]);
  assert.equal(normalize(key.match(/SHA256:\s*([A-F0-9:]+)/i)?.[1] || ''), normalize(cert.fingerprint256));
});
const badging = run(path.join(buildTools, 'aapt2'), ['dump', 'badging', apk]);
check('APK package, version, API target, and release flags are correct', () => {
  assert.match(badging, /package: name='com\.kreativfont\.studio'/);
  assert(badging.includes(`versionCode='${versionCode}'`)); assert(badging.includes(`versionName='${version}'`));
  assert.match(badging, /(?:minSdkVersion|sdkVersion):'24'/); assert.match(badging, /targetSdkVersion:'36'/);
  assert(!badging.includes('application-debuggable'));
  const xml = run(path.join(buildTools, 'aapt2'), ['dump', 'xmltree', apk, '--file', 'AndroidManifest.xml']);
  assert.match(xml, /android:allowBackup[^\n]*0x0/);
  assert.match(xml, /android:fullBackupContent[^\n]*0x0/);
  assert.match(xml, /android:dataExtractionRules/);
  assert(!/android:debuggable[^\n]*(?:0xffffffff|true)/.test(xml));
});
check('Merged APK has only billing/acknowledgement permissions and no internet, tracking or broad storage access', () => {
  const permissions = [...badging.matchAll(/uses-permission: name='([^']+)'/g)].map(m => m[1]);
  assert.deepEqual(permissions.sort(), ['android.permission.ACCESS_NETWORK_STATE', 'android.permission.RECEIVE_BOOT_COMPLETED', 'android.permission.WAKE_LOCK',
    'com.android.vending.BILLING', 'com.kreativfont.studio.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION'].sort());
});
check('Packaged backup rules exclude cloud and device-transfer storage', () => {
  const resources = run(path.join(buildTools, 'aapt2'), ['dump', 'resources', apk]);
  const match = resources.match(/resource (0x[0-9a-f]+) xml\/data_extraction_rules\s+\(\) \(file\) (\S+)/i);
  assert(match, 'Missing packaged extraction rules');
  const manifest = run(path.join(buildTools, 'aapt2'), ['dump', 'xmltree', apk, '--file', 'AndroidManifest.xml']);
  assert(manifest.includes(`android:dataExtractionRules(0x0101063e)=@${match[1]}`) ||
    new RegExp(`android:dataExtractionRules[^\\n]*@${match[1]}`).test(manifest), 'Manifest must reference the checked rules');
  const rules = run(path.join(buildTools, 'aapt2'), ['dump', 'xmltree', apk, '--file', match[2]]);
  for (const section of ['cloud-backup', 'device-transfer']) {
    const part = rules.split(`E: ${section} `)[1]?.split(/\n    E:/)[0];
    assert(part, `Missing ${section} rules`);
    assert(!part.includes('E: include'));
    for (const domain of ['root', 'file', 'database', 'sharedpref', 'external', 'device_root', 'device_file', 'device_database', 'device_sharedpref']) {
      assert(part.includes(`domain="${domain}"`), `Missing ${section}/${domain} exclusion`);
    }
    assert.equal((part.match(/A: path="\."/g) || []).length, 9);
  }
});
function filesUnder(dir) {
  return readdirSync(dir, { withFileTypes: true }).flatMap(f => f.isDirectory() ? filesUnder(path.join(dir, f.name)) : [path.join(dir, f.name)]);
}
for (const [file, prefix] of [[apk, 'assets/'], [aab, 'base/assets/']]) {
  check(`${path.extname(file)} disables bridge logging and WebView debugging`, () => {
    const config = JSON.parse(entry(file, `${prefix}capacitor.config.json`).toString());
    assert.equal(config.appId, 'com.kreativfont.studio'); assert.equal(config.loggingBehavior, 'none');
    assert.equal(config.android.loggingBehavior, 'none'); assert.equal(config.android.webContentsDebuggingEnabled, false);
  });
  check(`${path.extname(file)} contains the exact current web build, policy, and licenses`, () => {
    for (const f of filesUnder('dist')) {
      const relative = path.relative('dist', f).split(path.sep).join('/');
      assert.equal(sha(entry(file, `${prefix}public/${relative}`)), sha(readFileSync(f)), relative);
    }
    assert.equal(entry(file, `${prefix}public/PRIVACY.txt`).toString(), readFileSync('public/PRIVACY.txt', 'utf8'));
    for (const f of ['Capacitor.txt', 'DM-Sans.txt', 'Noto-Sans-Math.txt']) assert(entry(file, `${prefix}public/licenses/${f}`).length > 100);
    const entries = run('unzip', ['-Z1', file]);
    assert(!/\.(?:p12|pfx|jks|keystore|so)$/im.test(entries), 'Unexpected signing material or native library requiring a separate alignment audit');
    assert(!/(?:^|\/)(?:signing\.json|\.env[^/]*|local\.properties)$/m.test(entries));
  });
}
check('Release lint has no errors', () => {
  const lint = readFileSync(path.join(folder, 'lint-results-release.xml'), 'utf8');
  assert.match(lint, /<issues/); assert(!/severity="(?:Error|Fatal)"/.test(lint));
});
check('Shrinking preserves the custom export plugin', () => {
  const mapping = readFileSync(path.join(folder, `kreativ-${version}-mapping.txt`), 'utf8');
  const block = mapping.split('com.kreativfont.studio.ExportFilePlugin -> com.kreativfont.studio.ExportFilePlugin:')[1]?.split(/\n[^\s#]/)[0];
  assert(block, 'Custom export plugin was renamed or removed');
  assert(/savePng\(com\.getcapacitor\.PluginCall\)(?::[\d:]+)? -> savePng/.test(block), 'savePng reflection name must survive shrinking');
  assert(/saveResult\([^\n]+ -> saveResult/.test(block), 'Activity callback must survive shrinking');
});
check('Shrinking preserves the reflected WorkManager Room database constructor', () => {
  const mapping = readFileSync(path.join(folder, `kreativ-${version}-mapping.txt`), 'utf8');
  const block = mapping.split('androidx.work.impl.WorkDatabase_Impl -> androidx.work.impl.WorkDatabase_Impl:')[1]?.split(/\n[^\s#]/)[0];
  assert(block && /void <init>\(\)/.test(block), 'WorkManager startup requires the generated Room database no-argument constructor');
});
check('Packaged DEX retains Capacitor runtime annotations and plugin callbacks', () => {
  const temp = mkdtempSync(path.join(tmpdir(), 'kreativ-release-'));
  try {
    const dexFiles = run('unzip', ['-Z1', apk]).trim().split('\n').filter(f => /^classes\d*\.dex$/.test(f));
    assert(dexFiles.length > 0);
    const classes = new Map();
    for (const file of dexFiles) {
      const extracted = path.join(temp, file);
      writeFileSync(extracted, entry(apk, file));
      const dump = run(path.join(buildTools, 'dexdump'), ['-a', extracted]);
      for (const match of dump.matchAll(/Class #(\d+)\s+-\n\s+Class descriptor\s+: '([^']+)'/g)) {
        const annotationsStart = dump.lastIndexOf(`Class #${match[1]} annotations:\n`, match.index);
        const previousClass = dump.lastIndexOf('\nClass #', match.index - 2);
        const annotations = annotationsStart >= previousClass && annotationsStart >= 0
          ? dump.slice(annotationsStart, match.index) : '';
        classes.set(match[2], annotations);
      }
    }
    const plugins = JSON.parse(entry(apk, 'assets/capacitor.plugins.json').toString()).map(p => p.classpath);
    plugins.push('com.kreativfont.studio.ExportFilePlugin', 'com.kreativfont.studio.KreativBillingPlugin');
    for (const plugin of plugins) {
      assert.match(classes.get(`L${plugin.replaceAll('.', '/')};`) || '',
        /VISIBILITY_RUNTIME Lcom\/getcapacitor\/annotation\/CapacitorPlugin;/, `${plugin}: missing runtime registration annotation`);
    }
    const exported = classes.get('Lcom/kreativfont/studio/ExportFilePlugin;');
    assert.match(exported, /Annotations on method #\d+ 'savePng'\n\s+VISIBILITY_RUNTIME Lcom\/getcapacitor\/PluginMethod;/);
    assert.match(exported, /Annotations on method #\d+ 'saveResult'\n\s+VISIBILITY_RUNTIME Lcom\/getcapacitor\/annotation\/ActivityCallback;/);
    const billing = classes.get('Lcom/kreativfont/studio/KreativBillingPlugin;') || '';
    for (const method of ['getStatus', 'purchase', 'restore']) {
      assert.match(billing, new RegExp(`Annotations on method #\\d+ '${method}'\\n\\s+VISIBILITY_RUNTIME Lcom/getcapacitor/PluginMethod;`), `Missing reflected billing method ${method}`);
    }
    assert.match(classes.get('Lcom/getcapacitor/PluginMethod;') || '',
      /VISIBILITY_SYSTEM Ldalvik\/annotation\/AnnotationDefault;[^\n]*returnType="promise"/,
      'The bridge needs the default plugin-method return type');
  } finally { rmSync(temp, { recursive: true, force: true }); }
});
check('Shipping billing configuration has its public key and no development unlock switch', () => {
  const resources = run(path.join(buildTools, 'aapt2'), ['dump', 'resources', apk]);
  assert.match(resources, /string\/play_billing_public_key/);
  const keyFile = path.join(homedir(), '.config/kreativ-android/billing-public-key.txt');
  const key = (process.env.KREATIV_PLAY_BILLING_PUBLIC_KEY || readFileSync(keyFile, 'utf8')).replace(/\s/g, '');
  assert(key.length > 300 && resources.includes(key), 'The packaged public key must match Play Console configuration');
  const source = filesUnder('dist').filter(f => f.endsWith('.js')).map(f => readFileSync(f, 'utf8')).join('\n');
  assert(!source.includes('VITE_PRO_PREVIEW'), 'The development-only entitlement switch must be compiled out');
});
check('Store text stays within Play character limits', () => {
  for (const [name, max] of [['title', 30], ['short-description', 80], ['full-description', 4000]]) {
    const text = readFileSync(`store/en-US/${name}.txt`, 'utf8').trim();
    assert(text.length > 0 && text.length <= max, `${name}: ${text.length}/${max}`);
  }
});
check('Store graphics match their recorded hashes, dimensions, and PNG formats', () => {
  const manifest = JSON.parse(readFileSync('store/graphics/manifest.json', 'utf8'));
  assert.equal(manifest.version, version); assert(manifest.assets.filter(a => a.file.startsWith('screenshots/')).length >= 4);
  for (const a of manifest.assets) {
    const bytes = readFileSync(path.join('store/graphics', a.file));
    assert.equal(sha(bytes), a.sha256, a.file);
    const png = PNG.sync.read(bytes); assert.deepEqual([png.width, png.height], [a.width, a.height]);
    if (a.file === 'icon-512.png') { assert.deepEqual([png.width, png.height], [512, 512]); assert.equal(bytes[25], 6); assert(bytes.length <= 1024 * 1024); }
    else { assert.equal(bytes[25], 2); assert.deepEqual([png.width, png.height], a.file.startsWith('screenshots/') ? [1080, 1920] : [1024, 500]); }
  }
});
check('Hosted-policy source matches the bundled policy', () => {
  assert.equal(readFileSync('store/privacy/PRIVACY.txt', 'utf8'), readFileSync('public/PRIVACY.txt', 'utf8'));
  assert(existsSync('store/privacy/index.html'));
  const html = readFileSync('store/privacy/index.html', 'utf8');
  for (const line of readFileSync('public/PRIVACY.txt', 'utf8').split('\n').slice(1, 5)) {
    assert(html.includes(line), `Hosted-policy metadata is missing: ${line}`);
  }
});
const nativeEvidencePath = `artifacts/phone-release-${version}/native-results.json`;
let nativeEvidence = null;
if (existsSync(nativeEvidencePath)) {
  const evidence = JSON.parse(readFileSync(nativeEvidencePath, 'utf8'));
  if (evidence.passed && evidence.version === version && evidence.versionCode === versionCode &&
      evidence.apkSha256 === sha(readFileSync(apk)) && evidence.installedApkByteMatch &&
      evidence.originalPreferencesRestored && evidence.temporaryInstrumentationRemoved) nativeEvidence = evidence;
}
const publicationPath = 'store/privacy/publication.json';
let publication = null;
if (existsSync(publicationPath)) {
  const evidence = JSON.parse(readFileSync(publicationPath, 'utf8'));
  if (evidence.verified && evidence.sourcePolicySha256 === sha(readFileSync('public/PRIVACY.txt')) &&
      evidence.url === 'https://madebykreativ.com/privacy/kreativ-fancy-text/') publication = evidence;
}
let uploadKeyBackupConfirmedByOwner = false;
let playConsoleState = null;
if (existsSync('store/play-console-state.json')) {
  const state = JSON.parse(readFileSync('store/play-console-state.json', 'utf8'));
  if (state.packageName === 'com.kreativfont.studio') playConsoleState = state;
  uploadKeyBackupConfirmedByOwner = state.uploadKeyBackupConfirmedByOwner === true &&
    state.uploadCertificateSha256 === cert.fingerprint256;
}
const bundleSha256 = sha(readFileSync(aab));
const matchingTestTrackRelease = playConsoleState?.proDevelopment?.uploadedBundleSha256 === bundleSha256 &&
  playConsoleState.proDevelopment.versionCode === versionCode && playConsoleState.proDevelopment.internalReleaseAvailable === true;
const matchingProductionUpload = playConsoleState?.bundleUploaded === true && playConsoleState.uploadedBundleSha256 === bundleSha256;
const matchingBundleUploadedToPlay = matchingProductionUpload || matchingTestTrackRelease;
const matchingProductionSubmission = matchingProductionUpload && playConsoleState?.submittedForReview === true && playConsoleState.submittedVersionCode === versionCode;
const nativeBillingRuntimeTested = nativeEvidence?.billingPurchaseRestoreAcknowledgementTested === true;
const remainingPlayWorkflow = !matchingBundleUploadedToPlay
  ? `Prepare and submit the validated ${version} update for Google review`
  : matchingProductionSubmission
  ? 'Await Google automated checks/review and verify public availability after approval'
  : matchingTestTrackRelease
    ? `Complete Google Play license tests before preparing the ${version} production release`
  : playConsoleState?.productionReleaseSaved
    ? 'Submit prepared production release and saved changes for Google review'
    : playConsoleState?.initialSetupComplete
      ? 'Select internal testers and authorize rollout, or prepare production release/countries and app-review submission'
      : 'Complete Play Console app-content and release setup';
const report = { passed: true, version, versionCode, applicationId: 'com.kreativfont.studio', checkedAt: new Date().toISOString(),
  uploadCertificateSha256: cert.fingerprint256, checks, nativeReleaseRuntimeTested: !!nativeEvidence,
  nativeRuntimeEvidence: nativeEvidence ? nativeEvidencePath : null,
  nativeBillingRuntimeTested,
  privacyPolicyPublished: !!publication, privacyPolicyUrl: publication?.url || null,
  uploadKeyBackupConfirmedByOwner,
  playConsoleInitialSetupComplete: playConsoleState?.initialSetupComplete === true,
  matchingBundleUploadedToPlay,
  matchingTestTrackRelease,
  matchingProductionSubmission,
  pending: [...(nativeEvidence ? [] : ['Native testing of this signed, shrunk release']), ...(publication ? [] : ['Publish and verify the public privacy-policy URL']),
    ...(nativeBillingRuntimeTested ? [] : ['Verify no-charge Play purchase, cancellation, acknowledgement, restore and offline Pro access on Android']),
    ...(matchingBundleUploadedToPlay ? [] : [`Upload the validated ${version} bundle to Play Console`]),
    remainingPlayWorkflow,
    ...(uploadKeyBackupConfirmedByOwner ? [] : ['Owner-controlled signing backup before upload'])] };
writeFileSync(path.join(folder, 'verification.json'), JSON.stringify(report, null, 2) + '\n');
console.log(`\n${checks.length} release checks passed. ${nativeEvidence ? 'Matching physical-phone evidence recorded.' : 'Native release runtime testing remains pending.'} Play Console steps remain pending.`);
