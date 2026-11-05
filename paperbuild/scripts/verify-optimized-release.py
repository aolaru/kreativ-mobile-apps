#!/usr/bin/env python3
"""Check the shipped DEX, not just the presence of a mapping file.

Local renamed-class coverage is diagnostic, not Google's proprietary Play metric.
No device access, credentials, or external dependencies are required.
"""
import argparse
import hashlib
import json
from pathlib import Path
import re
import struct
import zipfile


def dex_classes(data):
    if not data.startswith(b"dex\n"):
        raise ValueError("Expected an Android DEX file")
    def uint(offset):
        return struct.unpack_from("<I", data, offset)[0]
    strings, types = uint(60), uint(68)
    count, classes = uint(96), uint(100)
    for index in range(count):
        type_index = uint(classes + 32 * index)
        string_index = uint(types + 4 * type_index)
        offset = uint(strings + 4 * string_index)
        while data[offset] & 0x80:
            offset += 1
        offset += 1  # Skip the final byte of the string's UTF-16 length.
        descriptor = data[offset:data.index(b"\0", offset)].decode("utf-8")
        yield descriptor[1:-1].replace("/", ".")


def inspect(apk, bundle):
    with zipfile.ZipFile(bundle) as archive, zipfile.ZipFile(apk) as application:
        mapping_name = "BUNDLE-METADATA/com.android.tools.build.obfuscation/proguard.map"
        mapping = archive.read(mapping_name).decode("utf-8")
        if "# compiler: R8" not in mapping:
            raise ValueError("Bundle has no R8 mapping")
        original_names = {}
        for line in mapping.splitlines():
            match = re.fullmatch(r"(\S+) -> (\S+):", line)
            if match:
                original_names.setdefault(match[2], set()).add(match[1])
        dex_names = sorted(name for name in application.namelist() if re.fullmatch(r"classes\d*\.dex", name))
        if not dex_names:
            raise ValueError("APK has no DEX")
        classes = set()
        dex_bytes = 0
        for name in dex_names:
            data = application.read(name)
            if data != archive.read("base/dex/" + name):
                raise ValueError("APK and App Bundle contain different DEX bytes")
            dex_bytes += len(data)
            classes.update(dex_classes(data))
        renamed = {name for name in classes if name in original_names and name not in original_names[name]}
        coverage = 100 * len(renamed) / len(classes)
        if coverage < 25:
            raise ValueError(f"Local renamed-class coverage unexpectedly low: {coverage:.2f}%")
        return {
            "apk_bytes": apk.stat().st_size,
            "bundle_bytes": bundle.stat().st_size,
            "bundle_sha256": hashlib.sha256(bundle.read_bytes()).hexdigest(),
            "dex_files": len(dex_names),
            "dex_bytes": dex_bytes,
            "defined_classes": len(classes),
            "renamed_defined_classes": len(renamed),
            "local_renamed_class_percent": round(coverage, 2),
            "mapping_embedded": True,
            "apk_bundle_dex_identical": True,
            "note": "Local class coverage is not Play's DEX optimisation percentage.",
        }


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("apk", type=Path)
    parser.add_argument("bundle", type=Path)
    arguments = parser.parse_args()
    print(json.dumps(inspect(arguments.apk, arguments.bundle), indent=2))
