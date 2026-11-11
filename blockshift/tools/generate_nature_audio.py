#!/usr/bin/env python3
"""Original nature-style synthesis; no field recordings or third-party samples.

Authoring only: Python + NumPy 2.x + SoundFile. Runtime needs none of them.
Periodic noise and envelopes avoid an artificial fade-to-silence at loop joins.
"""
from pathlib import Path
import argparse
import json
import numpy as np
import soundfile as sf

ROOT = Path(__file__).resolve().parents[1] / 'assets/audio'
RATE, SECONDS = 32000, 48
COUNT = RATE * SECONDS
TIME = np.arange(COUNT) / RATE
RNG = np.random.default_rng(20261107)


def noise(low, high, slope=0):
    frequencies = np.fft.rfftfreq(COUNT, 1 / RATE)
    spectrum = np.fft.rfft(RNG.standard_normal(COUNT))
    band = (1 - np.exp(-(frequencies / low) ** 4)) / (1 + (frequencies / high) ** 6)
    band *= np.maximum(frequencies, low) ** (-slope)
    band[0] = 0
    result = np.fft.irfft(spectrum * band, n=COUNT)
    return result / np.sqrt(np.mean(result ** 2))


def create(sound, write_asset=True):
    wind = .78 + .14 * np.sin(2 * np.pi * TIME / SECONDS + .8) + .08 * np.sin(6 * np.pi * TIME / SECONDS)
    channels = []
    for channel in range(2):
        if sound == 'rain':
            # Fine rain, leaf patter and a low, diffuse wash; no thunder.
            bed = .70 * noise(420, 6500, .12) + .24 * noise(120, 1400, .35)
            patter = noise(1000, 4800) * (.5 + .5 * np.sin(2 * np.pi * 73 * TIME / SECONDS + channel) ** 2)
            result = bed * wind + .11 * patter
        elif sound == 'forest':
            # Keep the wind genuinely quiet instead of normalising it back up.
            # Softer high frequencies leave room for the distant bird phrases.
            result = .032 * noise(150, 1600, .65) * wind + .004 * noise(1400, 4000)
            # Quiet, spaced, distant bird phrases, with short diffuse echoes.
            birds = np.zeros(COUNT)
            for start in [3.2, 8.1, 15.5, 23.4, 29.8, 38.6, 43.2]:
                for note in range(3):
                    duration = .13 + .025 * note
                    t = np.arange(round(duration * RATE)) / RATE
                    phase = 2 * np.pi * ((1850 + 120 * note) * t + 520 * t * t / duration + 32 * np.sin(2 * np.pi * 7 * t) / (2 * np.pi * 7))
                    song = .14 * np.sin(np.pi * t / duration) ** 2 * (np.sin(phase) + .10 * np.sin(2 * phase))
                    at = round((start + note * .23 + .007 * channel) * RATE)
                    birds[at:at + len(song)] += song * (1 if channel == 0 else .75)
            result += birds + .22 * np.roll(birds, round(.12 * RATE)) + .10 * np.roll(birds, round(.27 * RATE))
        else:
            # Five rounded incoming waves; a quieter, brighter receding wash.
            phase = 2 * np.pi * 5 * TIME / SECONDS
            swell = (.5 + .5 * np.sin(phase - .6)) ** 2
            retreat = (.5 + .5 * np.sin(phase - 1.7)) ** 3
            result = noise(110, 3100, .3) * (.22 + .85 * swell) + .19 * noise(900, 5800) * retreat
        channels.append(result)
    stereo = np.column_stack(channels)
    # A correlated centre keeps the quiet bed audible on a mono phone speaker.
    stereo = .72 * stereo + .28 * stereo.mean(axis=1, keepdims=True)
    if sound != 'forest':
        target_rms = {'rain': .095, 'ocean': .105}[sound]
        stereo *= target_rms / np.sqrt(np.mean(stereo ** 2))
    assert np.max(np.abs(stereo)) < .85
    if not write_asset:
        return
    target = ROOT / 'ambience' / (sound + '.ogg')
    sf.write(target, stereo, RATE, format='OGG', subtype='VORBIS')
    settings = target.with_suffix('.ogg.import')
    if not settings.exists():
        settings.write_text('[remap]\nimporter="oggvorbisstr"\ntype="AudioStreamOggVorbis"\n\n[deps]\nsource_file="res://assets/audio/ambience/' + sound + '.ogg"\n\n[params]\nloop=true\nloop_offset=0.0\n')
    print(f'{sound}: {SECONDS}s stereo, {target.stat().st_size:,} bytes')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--only', choices=['rain', 'forest', 'ocean'], help='Rewrite only this asset; retain the fixed RNG sequence for all three.')
    args = parser.parse_args()
    for sound in ['rain', 'forest', 'ocean']:
        create(sound, args.only is None or args.only == sound)
    manifest = json.loads((ROOT / 'manifest.json').read_text())
    manifest.pop('ambience', None)
    for sound in ['rain', 'forest', 'ocean']:
        manifest['ambience_' + sound] = 'res://assets/audio/ambience/' + sound + '.ogg'
    (ROOT / 'manifest.json').write_text(json.dumps(manifest, indent=2) + '\n')
