#!/usr/bin/env python3
"""Release signing guards and content-tamper rejection; needs a built candidate."""
import hashlib
import json
from pathlib import Path
import struct
import subprocess
import sys
import tempfile
import zipfile

ROOT = Path(__file__).resolve().parents[1]
BUNDLE = ROOT / 'build/blockshift-internal-0.5.0.aab'
JARSIGNER = Path('/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/jarsigner')


def main():
    original = hashlib.sha256(BUNDLE.read_bytes()).hexdigest()
    results = {}
    command = [sys.executable, str(ROOT / 'tools/sign_android.py')]
    result = subprocess.run(command + ['--create-key'], capture_output=True, text=True, timeout=30)
    results['existing_key_cannot_be_overwritten'] = result.returncode != 0 and 'refusing to replace' in result.stderr
    with tempfile.TemporaryDirectory(prefix='blockshift-signing-test-') as folder:
        target = Path(folder) / 'must-not-exist.aab'
        result = subprocess.run(command + ['--input', str(BUNDLE), '--output', str(target)],
                                capture_output=True, text=True, timeout=30)
        results['already_signed_input_rejected'] = result.returncode != 0 and 'Expected an unsigned release bundle' in result.stderr and not target.exists()
        # Flip one byte inside a signed entry; no decompression/recompression or
        # large duplicate intermediates are required on a low-disk-space host.
        with zipfile.ZipFile(BUNDLE) as archive:
            entry = next(item for item in archive.infolist() if item.filename.endswith('/assets/config/release.json'))
        raw = bytearray(BUNDLE.read_bytes())
        name_length, extra_length = struct.unpack_from('<HH', raw, entry.header_offset + 26)
        offset = entry.header_offset + 30 + name_length + extra_length + entry.compress_size // 2
        raw[offset] ^= 1
        tampered = Path(folder) / 'tampered.aab'
        tampered.write_bytes(raw)
        result = subprocess.run([str(JARSIGNER), '-J-Xmx128m', '-verify', str(tampered)],
                                capture_output=True, text=True, timeout=120)
        results['tampered_signed_content_rejected'] = result.returncode != 0
    results['original_signed_bundle_unchanged'] = hashlib.sha256(BUNDLE.read_bytes()).hexdigest() == original
    assert all(results.values()), results
    (ROOT / 'review/logs/internal-0_5_0-signing-regressions.json').write_text(json.dumps(results, indent=2) + '\n')
    print(json.dumps(results, indent=2))


if __name__ == '__main__':
    main()
