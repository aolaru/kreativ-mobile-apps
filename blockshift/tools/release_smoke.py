#!/usr/bin/env python3
"""Black-box touch/save checks of the non-debuggable bundle-derived APK.

Requires Pillow and the dedicated 1080x2400 ARM64 emulator. Never accesses app
private files or permits a physical-device serial. Tutorial captures also need
visual review; a Play-delivered installation still requires its own smoke test.
"""
import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess
import time
from PIL import Image, ImageChops

ROOT = Path(__file__).resolve().parents[1]
PACKAGE = 'com.madebykreativ.blockshift'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--adb', default='adb')
    parser.add_argument('--serial', default='emulator-5554')
    parser.add_argument('--apk', type=Path, default=ROOT / 'build/blockshift-internal-0.5.0.apk')
    parser.add_argument('--reset-emulator-app', action='store_true')
    args = parser.parse_args()
    if not args.serial.startswith('emulator-') or not args.reset_emulator_app:
        raise SystemExit('Use --reset-emulator-app with a dedicated emulator; physical devices are forbidden.')
    layout = json.loads((ROOT / 'review/android-layout.json').read_text())
    output = ROOT / 'review/internal-0_5_0'
    output.mkdir(exist_ok=True)

    def adb(*parts, timeout=40, check=True):
        return subprocess.run([args.adb, '-s', args.serial, *map(str, parts)],
                              capture_output=True, timeout=timeout, check=check)

    def text(*parts, **kwargs):
        return adb(*parts, **kwargs).stdout.decode(errors='replace').strip()

    deadline = time.monotonic() + 180
    while time.monotonic() < deadline:
        try:
            if text('shell', 'getprop', 'sys.boot_completed', timeout=4) == '1':
                break
        except subprocess.SubprocessError:
            pass
        time.sleep(1)
    else:
        raise RuntimeError('Emulator did not boot; this is not an app startup result.')
    if '1080x2400' not in text('shell', 'wm', 'size'):
        raise RuntimeError('Use the 1080x2400 validation emulator.')
    if 'arm64' not in text('shell', 'getprop', 'ro.product.cpu.abi'):
        raise RuntimeError('ARM64 emulator required.')
    adb('shell', 'input', 'keyevent', 'KEYCODE_WAKEUP')
    adb('shell', 'wm', 'dismiss-keyguard')
    adb('uninstall', PACKAGE, check=False)  # Only this game's disposable emulator data.
    adb('install', '--no-incremental', args.apk, timeout=180)
    package = text('shell', 'dumpsys', 'package', PACKAGE)
    if not re.search(r'versionName=' + re.escape(layout['version']) + r'\s', package):
        raise RuntimeError('Installed version differs from the generated touch layout.')
    if 'DEBUGGABLE' in package:
        raise RuntimeError('This must exercise a release APK.')
    sx, sy = 1080 / layout['viewport'][0], 2400 / layout['viewport'][1]

    def xy(point):
        return round(point[0] * sx), round(point[1] * sy)

    def board_point(geometry, x, y, lift=False):
        return xy([geometry['origin'][0] + x * geometry['cell'],
                   geometry['origin'][1] + y * geometry['cell'] + (geometry['lift'] if lift else 0)])

    def launch():
        adb('shell', 'am', 'start', '-W', '-n', PACKAGE + '/com.godot.game.GodotAppLauncher')
        deadline = time.monotonic() + 35
        while time.monotonic() < deadline:
            if text('shell', 'pidof', PACKAGE, check=False):
                break
            time.sleep(1)
        else:
            raise RuntimeError('Release app process did not start; inspect Android runtime logs.')
        time.sleep(4)

    def capture(name):
        time.sleep(1)
        path = output / (name + '.png')
        path.write_bytes(adb('exec-out', 'screencap', '-p').stdout)
        return Image.open(path).convert('RGB')

    def tap(point):
        adb('shell', 'input', 'tap', *xy(point))
        time.sleep(0.6)

    def swipe(start, finish):
        adb('shell', 'input', 'swipe', *start, *finish, 800)
        time.sleep(1)

    def occupied(image, geometry):
        colors = [image.getpixel(board_point(geometry, x + 0.5, y + 0.5)) for y in range(8) for x in range(8)]
        return sum(max(color) > 100 and max(color) - min(color) > 45 for color in colors)

    launch()
    pid = text('shell', 'pidof', PACKAGE)
    adb('shell', 'input', 'tap', 925, 500)  # Dismiss Android's initial fullscreen tip.
    capture('01-home')
    tap(layout['home']['shift'])
    counts = [occupied(capture('02-tutorial-empty-meter'), layout['tutorial'])]
    for line in range(4):
        swipe(xy(layout['tutorial']['tray']), board_point(layout['tutorial'], 7, 7.5 - line, True))
        image = capture('03-charge-' + str(line + 1))
        counts.append(occupied(image, layout['tutorial']))
    # First three line clears remove exactly six prefilled cells apiece.
    assert counts[:4] == [24, 18, 12, 6], counts
    tap(layout['shift_button'])
    swipe(board_point(layout['armed'], 4, 3.5), board_point(layout['armed'], 5.5, 3.5))
    capture('04-shift-preview')
    tap(layout['shift_apply'])
    capture('05-tutorial-result')
    tap(layout['tutorial_menu'])
    capture('06-home-after-tutorial')
    tap(layout['home']['classic'])
    empty = capture('07-classic-empty')
    assert occupied(empty, layout['classic']) == 0
    swipe(xy(layout['classic']['tray']), board_point(layout['classic'], 2.5, 2.5, True))
    placed = capture('08-classic-placed')
    assert occupied(placed, layout['classic']) > 0
    adb('shell', 'input', 'keyevent', 4)
    capture('09-pause')
    first_log = text('logcat', '-d', '--pid=' + pid)
    adb('shell', 'am', 'force-stop', PACKAGE)
    launch()
    tap(layout['resume'])
    restored = capture('10-classic-restored')
    origin, cell = layout['classic']['origin'], layout['classic']['cell']
    board_box = (*xy(origin), *xy([origin[0] + 8 * cell, origin[1] + 8 * cell]))
    tray_y = layout['classic']['tray'][1]
    tray_box = (*xy([30, tray_y - 55]), *xy([450, tray_y + 55]))
    assert ImageChops.difference(placed.crop(board_box), restored.crop(board_box)).getbbox() is None, 'Board pixels changed after restart'
    assert ImageChops.difference(placed.crop(tray_box), restored.crop(tray_box)).getbbox() is None, 'Hand pixels changed after restart'
    runtime = first_log + '\n' + text('logcat', '-d', '--pid=' + text('shell', 'pidof', PACKAGE))
    (output / 'runtime.log').write_text(runtime)
    assert not re.search(r'SCRIPT ERROR|E godot\s*: ERROR:|FATAL EXCEPTION|Fatal signal', runtime)
    result = {'version': layout['version'], 'apk': str(args.apk),
              'apk_sha256': hashlib.sha256(args.apk.read_bytes()).hexdigest(),
              'non_debuggable_bundle_derived_apk': True, 'emulator_only': True,
              'tutorial_prefilled_cell_counts': counts, 'tutorial_capture_review_required': True,
              'classic_placement': True, 'pause_force_stop_resume': True,
              'exact_board_and_hand_pixels_restored': True, 'runtime_errors': 0,
              'private_save_payload_accessed': False, 'play_delivered_build_tested': False}
    (ROOT / 'review/logs/internal-0_5_0-android-smoke.json').write_text(json.dumps(result, indent=2) + '\n')
    print(json.dumps(result, indent=2), flush=True)


if __name__ == '__main__':
    main()
