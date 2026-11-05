"""Check the phone-oriented pad has useful level, no clipping or silent loop gaps."""
from pathlib import Path
import array, math, wave

path = Path(__file__).resolve().parents[1] / 'assets/audio/ambience/ambience.wav'
with wave.open(str(path), 'rb') as stream:
    assert stream.getsampwidth() == 2 and stream.getnchannels() == 1
    rate = stream.getframerate()
    samples = array.array('h', stream.readframes(stream.getnframes()))
values = [sample / 32768 for sample in samples]
rms = math.sqrt(sum(value * value for value in values) / len(values))
assert .08 < rms < .15, ('unexpected ambience level', rms)
assert max(abs(value) for value in values) < .95, 'clipping'
for start in range(0, len(values), rate // 2):
    window = values[start:start + rate // 2]
    assert math.sqrt(sum(value * value for value in window) / len(window)) > .075, 'silent dip'
assert abs(values[-1] - values[0]) < .06, 'discontinuous loop boundary'
print('Audio: level, clipping, sustained loop and seam checks passed (RMS %.1f dBFS)' % (20 * math.log10(rms)))
