"""Validate the shipped Vorbis soundscapes after decoding, including loop joins."""
from pathlib import Path
import array
import hashlib
import json
import math
import subprocess

root = Path(__file__).resolve().parents[1]
manifest = json.loads((root / 'assets/audio/manifest.json').read_text())
fingerprints = set()
for sound in ['rain', 'forest', 'ocean']:
    path = root / manifest['ambience_' + sound].removeprefix('res://')
    fingerprints.add(hashlib.sha256(path.read_bytes()).hexdigest())
    info = json.loads(subprocess.check_output(['ffprobe', '-v', 'error', '-show_streams', '-of', 'json', str(path)]))['streams'][0]
    assert info['codec_name'] == 'vorbis' and info['channels'] == 2
    rate = int(info['sample_rate'])
    assert rate == 32000 and abs(float(info['duration']) - 48) < .02
    samples = array.array('h', subprocess.check_output(['ffmpeg', '-v', 'error', '-i', str(path), '-f', 's16le', '-acodec', 'pcm_s16le', '-']))
    def rms(values):
        return math.sqrt(sum((v / 32768) ** 2 for v in values) / len(values))
    level = rms(samples)
    assert (.018 < level < .045 if sound == 'forest' else .07 < level < .14), (sound, 'unexpected level', level)
    assert max(abs(v) for v in samples) / 32768 < .85, (sound, 'clipping')
    for start in range(0, len(samples) - rate, rate):
        assert rms(samples[start:start + rate]) > (.009 if sound == 'forest' else .018), (sound, 'silent half-second')
    for channel in range(2):
        assert abs(samples[channel] - samples[-2 + channel]) / 32768 < .30, (sound, 'loop click')
    minimum_join = .009 if sound == 'forest' else .02
    assert rms(samples[:rate // 10]) > minimum_join and rms(samples[-rate // 10:]) > minimum_join, (sound, 'silent loop join')
    if sound == 'forest':
        bed = rms(samples[:2 * rate * 2])  # First bird phrase starts at 3.2 s.
        phrase = max(abs(v) for v in samples[round(3.2 * rate * 2):round(4.2 * rate * 2)]) / 32768
        assert bed < .03 and phrase > .09 and phrase / bed > 4, ('forest birds should stand above a quiet wind bed', bed, phrase)
    assert 'loop=true' in path.with_suffix('.ogg.import').read_text()
    print('Audio %s: 48s stereo, level %.1f dBFS; clipping, continuity and loop checks passed' % (sound, 20 * math.log10(level)))
assert len(fingerprints) == 3, 'soundscapes must be distinct assets'
