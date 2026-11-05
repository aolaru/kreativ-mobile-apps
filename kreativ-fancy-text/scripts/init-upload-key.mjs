import { randomBytes } from 'node:crypto';
import { existsSync, mkdirSync, writeFileSync, chmodSync, unlinkSync } from 'node:fs';
import { spawnSync } from 'node:child_process';
import { homedir } from 'node:os';
import path from 'node:path';
const folder = path.join(homedir(), '.config', 'kreativ-android');
const configPath = path.join(folder, 'signing.json');
if (existsSync(configPath)) { console.log(`Existing signing configuration retained: ${configPath}`); process.exit(0); }
mkdirSync(folder, { recursive: true, mode: 0o700 }); chmodSync(folder, 0o700);
const keystore = path.join(folder, 'upload-keystore.p12');
if (existsSync(keystore)) throw new Error('Keystore already exists. Refusing to replace it.');
const password = randomBytes(32).toString('base64url');
const javaHome = process.env.JAVA_HOME || '/Applications/Android Studio.app/Contents/jbr/Contents/Home';
const env = { ...process.env, KREATIV_KEYGEN_PASSWORD: password };
const config = { keystore, alias: 'kreativ-upload', storePassword: password, keyPassword: password };
writeFileSync(configPath, JSON.stringify(config, null, 2) + '\n', { flag: 'wx', mode: 0o600 });
const keytool = path.join(javaHome, 'bin', 'keytool');
const result = spawnSync(keytool, ['-genkeypair', '-storetype', 'PKCS12', '-keystore', keystore,
  '-alias', config.alias, '-keyalg', 'RSA', '-keysize', '4096', '-sigalg', 'SHA256withRSA',
  '-validity', '10000', '-dname', 'CN=Kreativ Android Upload Key',
  '-storepass:env', 'KREATIV_KEYGEN_PASSWORD', '-keypass:env', 'KREATIV_KEYGEN_PASSWORD', '-noprompt'], { env, encoding: 'utf8' });
if (result.status !== 0) { unlinkSync(configPath); throw new Error('Upload key generation failed; no signing configuration was retained.'); }
chmodSync(keystore, 0o600);
console.log(`Created upload keystore: ${keystore}\nPrivate credentials: ${configPath}\nBoth are outside source control with owner-only permissions. Back them up securely before uploading to Play.`);
