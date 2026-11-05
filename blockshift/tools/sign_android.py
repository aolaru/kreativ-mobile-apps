#!/usr/bin/env python3
"""Create a private upload key outside this checkout; sign and verify release AABs.

Passwords go to child processes through environment variables, never command-line
arguments, Godot presets or build logs. The private JSON is intentionally not a
project file. Back up its directory in a private encrypted vault.
"""
import argparse
import base64
import hashlib
import json
import os
from pathlib import Path
import re
import secrets
import stat
import subprocess
import tempfile
import zipfile

PROJECT = Path(__file__).resolve().parents[1]
DEFAULT_PRIVATE = Path.home() / 'Library/Application Support/Made by Kreativ/signing/blockshift'


def run(java_sdk, tool, args, env=None):
    result = subprocess.run([str(java_sdk / 'bin' / tool), '-J-Duser.language=en', *map(str, args)],
                            capture_output=True, text=True, env=env)
    if result.returncode:
        raise RuntimeError(f'{tool} failed: {result.stdout}\n{result.stderr}')
    return result.stdout + result.stderr


def certificate_digest(pem):
    match = re.search(r'-----BEGIN CERTIFICATE-----\s*(.*?)\s*-----END CERTIFICATE-----', pem, re.S)
    if not match:
        raise RuntimeError('No signing certificate found')
    return hashlib.sha256(base64.b64decode(match[1])).hexdigest()


def private_location(folder):
    folder = folder.expanduser().resolve()
    if folder.is_relative_to(PROJECT.parent):
        raise RuntimeError('Signing keys must be outside the workspace')
    return folder


def create_key(folder, java_sdk):
    folder = private_location(folder)
    folder.mkdir(parents=True, exist_ok=True, mode=0o700)
    folder.chmod(0o700)
    if any(folder.iterdir()):
        raise RuntimeError('Private directory is not empty; refusing to replace a signing identity')
    password = secrets.token_urlsafe(36)
    env = os.environ.copy()
    env['BLOCKSHIFT_KEY_PASSWORD'] = password
    key = folder / 'upload.p12'
    alias = 'blockshift-upload'
    run(java_sdk, 'keytool', ['-genkeypair', '-keystore', key, '-storetype', 'PKCS12',
        '-alias', alias, '-keyalg', 'RSA', '-keysize', '3072', '-validity', '10000',
        '-dname', 'CN=BLOCKSHIFT Upload, O=Made by Kreativ',
        '-storepass:env', 'BLOCKSHIFT_KEY_PASSWORD', '-keypass:env', 'BLOCKSHIFT_KEY_PASSWORD'], env)
    key.chmod(0o600)
    certificate = run(java_sdk, 'keytool', ['-exportcert', '-rfc', '-keystore', key,
        '-alias', alias, '-storepass:env', 'BLOCKSHIFT_KEY_PASSWORD'], env)
    (folder / 'upload-certificate.pem').write_text(certificate)
    credentials = folder / 'credentials.json'
    # umask protects credentials at creation, before chmod can run.
    with os.fdopen(os.open(credentials, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600), 'w') as stream:
        json.dump({'keystore': str(key), 'alias': alias, 'password': password,
                   'certificate_sha256': certificate_digest(certificate)}, stream, indent=2)
    (folder / 'BACKUP.txt').write_text('Private BLOCKSHIFT upload signing identity.\n'
        'Back up this entire folder in an encrypted vault. Never share credentials.json or upload.p12,\n'
        'commit them to source control, or put them in the upload/testing package.\n'
        'Only upload-certificate.pem is public. Google Play App Signing uses a separate app signing key.\n')
    print(f'Created dedicated upload key. Private recovery directory: {folder}')
    print('Password was saved locally with owner-only access; it was not printed.')


def sign_bundle(folder, java_sdk, source, target, report):
    folder = private_location(folder)
    credentials_path = folder / 'credentials.json'
    if stat.S_IMODE(credentials_path.stat().st_mode) & 0o077:
        raise RuntimeError('Credentials file must have owner-only permissions (chmod 600)')
    credentials = json.loads(credentials_path.read_text())
    key = Path(credentials['keystore']).resolve()
    if key.is_relative_to(PROJECT.parent) or stat.S_IMODE(key.stat().st_mode) & 0o077:
        raise RuntimeError('Keystore must be outside the workspace with owner-only permissions')
    with zipfile.ZipFile(source) as archive:
        if any(n.upper().endswith(('.SF', '.RSA', '.DSA', '.EC')) and n.startswith('META-INF/') for n in archive.namelist()):
            raise RuntimeError('Expected an unsigned release bundle')
        if 'BundleConfig.pb' not in archive.namelist():
            raise RuntimeError('Input is not an Android App Bundle')
    env = os.environ.copy()
    env['BLOCKSHIFT_KEY_PASSWORD'] = credentials['password']
    target.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(dir=target.parent) as temp:
        signed = Path(temp) / target.name
        run(java_sdk, 'jarsigner', ['-keystore', key, '-storetype', 'PKCS12',
            '-storepass:env', 'BLOCKSHIFT_KEY_PASSWORD', '-keypass:env', 'BLOCKSHIFT_KEY_PASSWORD',
            '-sigalg', 'SHA256withRSA', '-digestalg', 'SHA-256', '-signedjar', signed,
            source, credentials['alias']], env)
        verification = run(java_sdk, 'jarsigner', ['-verify', '-verbose', '-certs', signed])
        if 'jar verified.' not in verification:
            raise RuntimeError('JAR signature verification did not succeed')
        certificate = run(java_sdk, 'keytool', ['-printcert', '-jarfile', signed, '-rfc'])
        digest = certificate_digest(certificate)
        if digest != credentials['certificate_sha256']:
            raise RuntimeError('Bundle signer differs from the dedicated upload certificate')
        os.replace(signed, target)
    result = {'file': str(target), 'bytes': target.stat().st_size,
              'sha256': hashlib.sha256(target.read_bytes()).hexdigest(),
              'signature_verified': True, 'certificate_sha256': digest,
              'signing_identity': 'dedicated non-debug upload key',
              'play_app_signing_checked_by_this_tool': False}
    report.parent.mkdir(parents=True, exist_ok=True)
    report.write_text(json.dumps(result, indent=2) + '\n')
    print(json.dumps(result, indent=2))


def main():
    version = re.search(r'config/version="([^"]+)"', (PROJECT / 'project.godot').read_text()).group(1)
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--java-sdk', type=Path, default=Path('/Applications/Android Studio.app/Contents/jbr/Contents/Home'))
    parser.add_argument('--private-dir', type=Path, default=DEFAULT_PRIVATE)
    parser.add_argument('--create-key', action='store_true')
    parser.add_argument('--input', type=Path, default=PROJECT / 'build/blockshift-release-unsigned.aab')
    parser.add_argument('--output', type=Path, default=PROJECT / f'build/blockshift-internal-{version}.aab')
    parser.add_argument('--report', type=Path, default=PROJECT / f'review/logs/internal-{version.replace(".", "_")}-signature.json')
    args = parser.parse_args()
    if args.create_key:
        create_key(args.private_dir, args.java_sdk)
    else:
        sign_bundle(args.private_dir, args.java_sdk, args.input, args.output, args.report)


if __name__ == '__main__':
    main()
