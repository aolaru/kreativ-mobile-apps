#!/usr/bin/env python3
"""Install the matching official Android source template and apply local build limits."""
from pathlib import Path
import argparse, os, platform, subprocess, zipfile

def main():
 parser=argparse.ArgumentParser()
 parser.add_argument('--godot',default=os.environ.get('GODOT_BIN','godot'))
 parser.add_argument('--templates-dir',type=Path)
 args=parser.parse_args()
 version=subprocess.check_output([args.godot,'--version'],text=True).strip()
 identifier='.'.join(version.split('.')[:4])
 if identifier!='4.6.3.stable':
  raise SystemExit(f'Validated with 4.6.3.stable; found {identifier}. Review upgrade compatibility before exporting.')
 home=Path.home()
 if platform.system()=='Darwin': base=home/'Library/Application Support/Godot/export_templates'
 elif platform.system()=='Windows': base=Path(os.environ['APPDATA'])/'Godot/export_templates'
 else: base=Path(os.environ.get('XDG_DATA_HOME',str(home/'.local/share')))/'godot/export_templates'
 template=(args.templates_dir or base/identifier)/'android_source.zip'
 project=Path(__file__).resolve().parents[1]
 android=project/'android'; build=android/'build'
 marker=android/'.build_version'
 if marker.exists() and marker.read_text().strip()!=identifier:
  raise SystemExit('Existing Android template differs. Back up any native changes and reinstall it from Godot.')
 if not (build/'build.gradle').exists():
  if not template.exists(): raise SystemExit(f'Install official {identifier} export templates in Godot first: {template}')
  build.mkdir(parents=True,exist_ok=True)
  with zipfile.ZipFile(template) as archive:
   for member in archive.infolist():
    if not (build/member.filename).resolve().is_relative_to(build.resolve()):
     raise SystemExit('Unsafe template archive path.')
   archive.extractall(build)
  marker.write_text(identifier+'\n')
 (android/'.gdignore').touch()
 if platform.system()!='Windows': (build/'gradlew').chmod(0o755)
 props=build/'gradle.properties'
 overrides={'android.enableJetifier':'false','org.gradle.jvmargs':'-Xmx1536m -Dfile.encoding=UTF-8',
            'org.gradle.workers.max':'2','org.gradle.parallel':'false','org.gradle.daemon':'false'}
 lines=[line for line in props.read_text().splitlines() if line.split('=',1)[0].strip() not in overrides and line != '# BLOCKSHIFT: AndroidX-only project; bounded local build resources.']
 while lines and not lines[-1].strip(): lines.pop()
 lines+=['', '# BLOCKSHIFT: AndroidX-only project; bounded local build resources.']
 lines += [f'{key}={value}' for key,value in overrides.items()]
 props.write_text('\n'.join(lines)+'\n')
 gradle=build/'build.gradle'
 branding="apply from: '../../tools/android_branding.gradle'"
 if branding not in gradle.read_text():
  gradle.write_text(gradle.read_text()+'\n'+branding+'\n')
 print(f'Android template ready: {identifier}; generated files remain ignored by Git.')

if __name__=='__main__': main()
