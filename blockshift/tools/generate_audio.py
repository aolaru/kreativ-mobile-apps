"""Reproducible original placeholder tones. No samples or external dependencies."""
from pathlib import Path
import json, math, wave, struct
ROOT=Path(__file__).resolve().parents[1]/'assets/audio'
def tone(name, frequencies, duration, gain=.18, ambience=False):
 rate=22050; frames=round(rate*duration); target=ROOT/('ambience' if ambience else 'sfx')/(name+'.wav')
 with wave.open(str(target),'wb') as out:
  out.setparams((1,2,rate,frames,'NONE','not compressed'))
  data=bytearray()
  for i in range(frames):
   t=i/rate; phase=t/duration
   envelope=(math.sin(math.pi*phase)**2 if ambience else min(1,t/.012)*math.exp(-5*phase)*(1-phase))
   sample=sum(math.sin(2*math.pi*f*t) for f in frequencies)/len(frequencies)
   data+=struct.pack('<h',int(32767*gain*envelope*sample))
  out.writeframes(data)
 return 'res://'+str(target.relative_to(ROOT.parent.parent))

manifest={key: value for key, value in json.loads((ROOT/'manifest.json').read_text()).items() if key.startswith('ambience_')}
for name,freqs,sec in [('menu',[580],.09),('pickup',[440,660],.12),('place',[260,520],.16),('invalid',[180],.1),('clear',[660,880,1100],.4),('combo',[660,990,1320],.55),('shift',[220,440,660,880],.5),('achievement',[523.25,659.25,783.99],.8),('game_over',[220,277.18,329.63],.8)]:
 manifest[name]=tone(name,freqs,sec)
(ROOT/'manifest.json').write_text(json.dumps(manifest,indent=2)+'\n')
