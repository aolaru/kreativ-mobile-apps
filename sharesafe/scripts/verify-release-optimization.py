#!/usr/bin/env python3
"""Verify a production AAB's R8 output; report sizes, not Play's optimisation score."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import struct
import zipfile


MAPPING_ENTRY = "BUNDLE-METADATA/com.android.tools.build.obfuscation/proguard.map"


def dex_stats(archive):
    with zipfile.ZipFile(archive) as bundle:
        dex = [entry for entry in bundle.infolist()
               if re.fullmatch(r"base/dex/classes\d*\.dex", entry.filename)]
        if not dex:
            raise ValueError("No base DEX files found")
        classes = methods = 0
        for entry in dex:
            data = bundle.read(entry)
            if not data.startswith(b"dex\n") or len(data) < 112:
                raise ValueError(f"Invalid DEX header: {entry.filename}")
            if struct.unpack_from("<I", data, 40)[0] != 0x12345678:
                raise ValueError(f"Unsupported DEX byte order: {entry.filename}")
            classes += struct.unpack_from("<I", data, 96)[0]
            methods += struct.unpack_from("<I", data, 88)[0]
        return {
            "aab_bytes": Path(archive).stat().st_size,
            "dex_files": len(dex),
            "dex_bytes": sum(entry.file_size for entry in dex),
            "dex_class_definitions": classes,
            "dex_method_references": methods,
        }


def verify(archive, mapping_path, configuration_path, baseline=None):
    mapping = Path(mapping_path).read_bytes()
    class_names = re.findall(r"^([^\s#].*?) -> (\S+):$", mapping.decode("utf-8"), re.M)
    renamed = [(source, target) for source, target in class_names if source != target]
    app_renamed = [(source, target) for source, target in renamed
                   if source.startswith("com.kreativ.sharesafe.")]
    if not renamed or not app_renamed:
        raise ValueError("Mapping does not show obfuscated ShareSafe classes")
    for line in Path(configuration_path).read_text().splitlines():
        if line.partition("#")[0].strip() in ("-dontobfuscate", "-dontoptimize", "-dontshrink"):
            raise ValueError(f"R8 optimisation disabled by effective configuration: {line}")
    with zipfile.ZipFile(archive) as bundle:
        if bundle.read(MAPPING_ENTRY) != mapping:
            raise ValueError("Embedded Play mapping differs from standalone mapping.txt")
    result = {
        "aab_sha256": hashlib.sha256(Path(archive).read_bytes()).hexdigest(),
        "optimized": dex_stats(archive),
        "embedded_mapping_matches": True,
        "mapped_classes": len(class_names),
        "renamed_classes": len(renamed),
        "renamed_app_classes": len(app_renamed),
        "note": "Local R8 evidence, not Google's DEX optimisation/obfuscation score or a runtime test.",
    }
    if baseline:
        result["baseline"] = dex_stats(baseline)
        original = result["baseline"]["dex_bytes"]
        result["dex_size_reduction_percent"] = round(
            (original - result["optimized"]["dex_bytes"]) * 100 / original, 2)
    return result


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("aab")
    parser.add_argument("--mapping", required=True)
    parser.add_argument("--configuration", required=True)
    parser.add_argument("--baseline")
    args = parser.parse_args()
    try:
        print(json.dumps(verify(args.aab, args.mapping, args.configuration, args.baseline), indent=2))
    except (OSError, ValueError, KeyError, struct.error, zipfile.BadZipFile) as error:
        parser.exit(1, f"{error}\n")
