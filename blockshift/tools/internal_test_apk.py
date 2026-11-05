#!/usr/bin/env python3
"""Build a universal QA APK from the signed bundle using official bundletool.

This APK is signed with the upload key. Play delivery will use the separate app
signing key. Password files exist only in a private temporary directory.
"""
import argparse
import json
import os
import re
from pathlib import Path
import subprocess
import tempfile
import zipfile
from sign_android import DEFAULT_PRIVATE, PROJECT, private_location


def main():
    version = re.search(r'config/version="([^"]+)"', (PROJECT / 'project.godot').read_text()).group(1)
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--bundletool', type=Path, required=True)
    parser.add_argument('--java-sdk', type=Path, default=Path('/Applications/Android Studio.app/Contents/jbr/Contents/Home'))
    parser.add_argument('--private-dir', type=Path, default=DEFAULT_PRIVATE)
    parser.add_argument('--bundle', type=Path, default=PROJECT / f'build/blockshift-internal-{version}.aab')
    parser.add_argument('--output', type=Path, default=PROJECT / f'build/blockshift-internal-{version}.apk')
    args = parser.parse_args()
    credentials = json.loads((private_location(args.private_dir) / 'credentials.json').read_text())
    with tempfile.TemporaryDirectory(prefix='blockshift-bundle-') as directory:
        password_file = Path(directory) / 'password'
        with os.fdopen(os.open(password_file, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600), 'w') as stream:
            stream.write(credentials['password'])
        archive = Path(directory) / 'universal.apks'
        subprocess.run([str(args.java_sdk / 'bin/java'), '-Xmx768m', '-jar', str(args.bundletool),
            'build-apks', '--bundle=' + str(args.bundle), '--output=' + str(archive), '--mode=universal',
            '--ks=' + credentials['keystore'], '--ks-key-alias=' + credentials['alias'],
            '--ks-pass=file:' + str(password_file), '--key-pass=file:' + str(password_file)], check=True)
        with zipfile.ZipFile(archive) as apk_set:
            args.output.parent.mkdir(parents=True, exist_ok=True)
            with args.output.open('wb') as output:
                output.write(apk_set.read('universal.apk'))
    print(f'Created release QA APK from the exact signed AAB: {args.output}')


if __name__ == '__main__':
    main()
