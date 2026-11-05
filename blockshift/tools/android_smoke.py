#!/usr/bin/env python3
"""Touch smoke test for the dedicated 1080x2400 Android ARM64 emulator.
Resets only BLOCKSHIFT's emulator data when --reset-qa-data is explicitly supplied.
Never targets physical devices. Requires an already booted emulator.
"""
from pathlib import Path
import argparse,json,re,subprocess,time

ROOT=Path(__file__).resolve().parents[1]
PKG='com.madebykreativ.blockshift'

def main():
 parser=argparse.ArgumentParser()
 parser.add_argument('--adb',default='adb')
 parser.add_argument('--serial',default='emulator-5554')
 parser.add_argument('--apk',type=Path,default=ROOT/'build/blockshift-debug.apk')
 parser.add_argument('--reset-qa-data',action='store_true',help='Erase only this game’s test data on the emulator.')
 args=parser.parse_args()
 if not args.serial.startswith('emulator-'): raise SystemExit('This harness only permits an emulator serial.')
 layout_path=ROOT/'review/android-layout.json'
 if not layout_path.exists():raise SystemExit('Run tools/test.sh --render to generate current UI touch targets first.')
 layout=json.loads(layout_path.read_text())
 scale_x=1080/layout['viewport'][0];scale_y=2400/layout['viewport'][1]
 def xy(point):return round(point[0]*scale_x),round(point[1]*scale_y)
 def board_point(geometry,x,y,lift=False):
  return xy([geometry['origin'][0]+x*geometry['cell'],geometry['origin'][1]+y*geometry['cell']+(geometry['lift'] if lift else 0)])
 def run(*parts,**kwargs):
  timeout=kwargs.pop('timeout',50)
  return subprocess.run([args.adb,'-s',args.serial,*map(str,parts)],check=True,timeout=timeout,**kwargs)
 def text(*parts):return run(*parts,stdout=subprocess.PIPE,stderr=subprocess.PIPE,text=True).stdout
 if '1080x2400' not in text('shell','wm','size'):raise SystemExit('Use the 1080x2400 validation emulator.')
 if 'arm64' not in text('shell','getprop','ro.product.cpu.abi'):raise SystemExit('ARM64 emulator required.')
 (ROOT/'review/logs').mkdir(parents=True,exist_ok=True)
 def progress():
  env=json.loads(text('shell','run-as',PKG,'cat','files/progress.json'))
  return env,json.loads(env['payload'])
 def wait_for(predicate,seconds=35):
  deadline=time.monotonic()+seconds
  while time.monotonic()<deadline:
   try:
    value=progress()
    if predicate(value):return value
   except (ValueError,subprocess.SubprocessError):pass
   time.sleep(.3)
  raise RuntimeError('Android checkpoint timed out; inspect the emulator and logs.')
 def tap(x,y):run('shell','input','tap',x,y);time.sleep(.5)
 def swipe(x,y,dx,dy):run('shell','input','swipe',x,y,dx,dy,700);time.sleep(.5)
 def capture(name):
  time.sleep(1)
  with (ROOT/'review'/name).open('wb') as out:run('exec-out','screencap','-p',stdout=out)
 def launch():run('shell','am','start','-W','-n',PKG+'/com.godot.game.GodotAppLauncher',stdout=subprocess.DEVNULL)
 # Cold emulator package optimization can outlast ordinary shell commands.
 run('install','--no-incremental','-r',args.apk,stdout=subprocess.DEVNULL,timeout=180)
 installed=re.search(r'versionName=([^\s]+)',text('shell','dumpsys','package',PKG))
 if not installed or installed[1]!=layout['version']:raise SystemExit('APK and generated UI fixture versions differ; regenerate with tools/test.sh --render.')
 if not args.reset_qa_data:raise SystemExit('Use --reset-qa-data on the dedicated emulator to run a clean tutorial test.')
 print('Resetting BLOCKSHIFT test data on '+args.serial,flush=True)
 run('shell','am','force-stop',PKG)
 run('shell','pm','clear',PKG,stdout=subprocess.DEVNULL)
 started=time.monotonic()
 launch()
 # No touch at all until a menu checkpoint appears: catches Android timer stalls.
 env,data=wait_for(lambda v:v[0]['sequence']>0,seconds=45)
 cold_start=round(time.monotonic()-started,2)
 tap(925,500) # Dismiss Android's first-use fullscreen tip; empty hero area otherwise.
 capture('android-home.png')
 baseline=env['sequence']
 tap(*xy(layout['home']['shift'])) # Play SHIFT -> real interactive tutorial.
 env,_=wait_for(lambda v:v[0]['sequence']>baseline)
 capture('android-tutorial.png')
 for line in range(4):
  baseline=env['sequence']
  swipe(*xy(layout['tutorial']['tray']),*board_point(layout['tutorial'],7,7.5-line,True))
  env,_=wait_for(lambda v:v[0]['sequence']>baseline)
 time.sleep(0.8) # Let the fourth clear finish before the movement practice board.
 capture('android-shift-ready.png')
 tap(*xy(layout['shift_button']))
 swipe(*board_point(layout['armed'],4,3.5),*board_point(layout['armed'],5.5,3.5))
 tap(*xy(layout['shift_apply']))
 env,data=wait_for(lambda v:v[1]['settings'].get('tutorial_done') is True)
 assert not data['runs'] and all(s.get('games',0)==0 for s in data['stats'].values())
 time.sleep(1.0) # Commit precedes the wrap/clear animation and completion sheet.
 capture('android-tutorial-complete.png')
 baseline=env['sequence']
 tap(*xy(layout['tutorial_menu']))
 wait_for(lambda v:v[0]['sequence']>baseline)
 tap(*xy(layout['home']['classic']))
 _,data=wait_for(lambda v:v[1]['runs'].get('classic',{}).get('mode')=='classic')
 piece=data['runs']['classic']['pieces'][0]['cells']
 width=max(c[0] for c in piece)+1;height=max(c[1] for c in piece)+1
 target=board_point(layout['classic'],width/2,height/2,True)
 swipe(*xy(layout['classic']['tray']),*target)
 env,data=wait_for(lambda v:v[1]['runs'].get('classic',{}).get('moves')==1)
 assert data['runs']['classic']['score']['total']==len(piece)
 assert sum(c!=0 for c in data['runs']['classic']['board'])==len(piece)
 capture('android-classic.png')
 run('shell','input','keyevent',4)
 capture('android-pause.png')
 old_env,paused=progress();checkpoint=paused['runs']['classic']
 first_pid=text('shell','pidof',PKG).strip()
 first_log=text('logcat','-d','--pid='+first_pid)
 run('shell','am','force-stop',PKG)
 launch()
 wait_for(lambda v:v[0]['sequence']>old_env['sequence'],seconds=45)
 tap(*xy(layout['resume']))
 _,resumed=wait_for(lambda v:v[1]['runs'].get('classic',{}).get('elapsed',0)>checkpoint['elapsed']+1,seconds=25)
 for key in ['board','pieces','score','rng_state','moves']:assert resumed['runs']['classic'][key]==checkpoint[key],key
 capture('android-resumed.png')
 second_pid=text('shell','pidof',PKG).strip()
 runtime=first_log+'\n'+text('logcat','-d','--pid='+second_pid)
 (ROOT/'review/logs/android-runtime.log').write_text(runtime)
 assert not re.search(r'SCRIPT ERROR|E godot\s*: ERROR:|FATAL EXCEPTION',runtime),'Runtime error in Android logs'
 report={'device':'Android 16 / API 36.1 ARM64 emulator','resolution':'1080x2400','apk':args.apk.name,'apk_version':installed[1],
  'cold_menu_without_touch':True,'emulator_cold_start_seconds':cold_start,'tutorial_touch_completed':True,
  'tutorial_charging_clears':4,'tutorial_preview_then_apply':True,
  'tutorial_did_not_change_player_stats':True,'classic_touch_placement':True,'classic_score':len(piece),
  'pause_and_force_stop_recovery':True,'restored_fields':['board','pieces','score','rng_state','moves'],
  'idle_playtime_autosave_after_resume':True,'runtime_script_errors':0}
 (ROOT/'review/logs/android-smoke.json').write_text(json.dumps(report,indent=2)+'\n')
 print(json.dumps(report,indent=2),flush=True)

if __name__=='__main__':main()
