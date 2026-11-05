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

def ambience_pad():
 # Integer-cycle harmonics and periodic modulation make the loop seamless.
 # Upper midrange partials stay audible on small phone speakers.
 rate=22050; duration=12; frames=rate*duration
 target=ROOT/'ambience/ambience.wav'
 partials=[(262,.12),(330,.18),(392,.20),(524,.15),(660,.20),(784,.15)]
 data=bytearray()
 for i in range(frames):
  t=i/rate
  envelope=.91+.09*math.cos(2*math.pi*t/duration)
  sample=.36*envelope*sum(weight*math.sin(2*math.pi*frequency*t) for frequency,weight in partials)
  data+=struct.pack('<h',round(32767*sample))
 with wave.open(str(target),'wb') as out:
  out.setparams((1,2,rate,frames,'NONE','not compressed'))
  out.writeframes(data)
 return 'res://assets/audio/ambience/ambience.wav'
manifest={}
for name,freqs,sec in [('menu',[580],.09),('pickup',[440,660],.12),('place',[260,520],.16),('invalid',[180],.1),('clear',[660,880,1100],.4),('combo',[660,990,1320],.55),('shift',[220,440,660,880],.5),('achievement',[523.25,659.25,783.99],.8),('game_over',[220,277.18,329.63],.8)]:
 manifest[name]=tone(name,freqs,sec)
manifest['ambience']=ambience_pad()
(ROOT/'manifest.json').write_text(json.dumps(manifest,indent=2)+'\n')
import_file=ROOT/'ambience/ambience.wav.import'
if import_file.exists():
 import re
 import_file.write_text(re.sub(r'edit/loop_mode=\d+', 'edit/loop_mode=2', import_file.read_text()))
else:
 import_file.write_text('''[remap]\nimporter="wav"\ntype="AudioStreamWAV"\n\n[deps]\nsource_file="res://assets/audio/ambience/ambience.wav"\n\n[params]\nedit/loop_mode=2\nedit/loop_begin=0\nedit/loop_end=-1\n''')
