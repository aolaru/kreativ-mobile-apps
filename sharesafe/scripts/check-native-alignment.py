#!/usr/bin/env python3
"""Read an APK/AAB's 64-bit ELF load alignment; also check uncompressed APK offsets."""
import struct
import sys
import zipfile


def check(archive, strict_relro=False):
    checked = 0
    relro_warnings = 0
    with zipfile.ZipFile(archive) as bundle:
        with open(archive, "rb") as raw:
            for entry in bundle.infolist():
                if not entry.filename.endswith(".so") or not any(
                    f"/{abi}/" in entry.filename for abi in ("arm64-v8a", "x86_64")
                ):
                    continue
                data = bundle.read(entry)
                if data[:5] != b"\x7fELF\x02":
                    raise ValueError(f"Not a 64-bit ELF: {entry.filename}")
                endian = "<" if data[5] == 1 else ">"
                offset = struct.unpack_from(endian + "Q", data, 32)[0]
                size, count = struct.unpack_from(endian + "HH", data, 54)
                aligns = []
                for index in range(count):
                    ph = offset + index * size
                    kind = struct.unpack_from(endian + "I", data, ph)[0]
                    if kind == 1:
                        aligns.append(struct.unpack_from(endian + "Q", data, ph + 48)[0])
                    elif kind == 0x6474E552:  # PT_GNU_RELRO
                        address = struct.unpack_from(endian + "Q", data, ph + 16)[0]
                        memory_size = struct.unpack_from(endian + "Q", data, ph + 40)[0]
                        if (address + memory_size) % 16384:
                            message = f"RELRO end not on a 16 KB boundary: {entry.filename}; validate on a 16 KB runtime"
                            if strict_relro:
                                raise ValueError(message)
                            print(f"WARNING: {message}")
                            relro_warnings += 1
                if not aligns or min(aligns) < 16384:
                    raise ValueError(f"ELF load alignment below 16 KB: {entry.filename}: {aligns}")
                if str(archive).endswith(".apk") and entry.compress_type == zipfile.ZIP_STORED:
                    raw.seek(entry.header_offset + 26)
                    name_length, extra_length = struct.unpack("<HH", raw.read(4))
                    data_offset = entry.header_offset + 30 + name_length + extra_length
                    if data_offset % 16384:
                        raise ValueError(f"Uncompressed APK entry not 16 KB aligned: {entry.filename}")
                print(f"Load/ZIP alignment OK: {entry.filename}")
                checked += 1
    if not checked:
        raise ValueError("No 64-bit native libraries found; this is not a native alignment pass")
    print(f"Checked {checked} 64-bit libraries; {relro_warnings} RELRO boundary warnings.")
    print("This is a static check, not proof of runtime compatibility. Re-run on the signed release and test a 16 KB runtime.")


if __name__ == "__main__":
    if len(sys.argv) not in (2, 3) or (len(sys.argv) == 3 and sys.argv[2] != "--strict-relro"):
        sys.exit("Usage: python3 scripts/check-native-alignment.py path/to/app.apk-or.aab [--strict-relro]")
    try:
        check(sys.argv[1], len(sys.argv) == 3)
    except (OSError, ValueError, struct.error, zipfile.BadZipFile) as error:
        sys.exit(str(error))
