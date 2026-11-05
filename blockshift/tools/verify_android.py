#!/usr/bin/env python3
"""Inspect exported archives; validates permissions, exclusions, ABI and ELF alignment."""
from pathlib import Path
import argparse, json, re, struct, subprocess, zipfile, os
import xml.etree.ElementTree as ET

def verify_archive(path):
 with zipfile.ZipFile(path) as archive:
  names=archive.namelist()
  leaked=[n for n in names if '/assets/' in '/'+n and any(x in n for x in ['scripts/debug/','tests/','review/','tools/','store/','/build/'])]
  if leaked: raise SystemExit(f'Development resources leaked into {path}: {leaked}')
  libs=[n for n in names if '/lib/' in '/'+n and n.endswith('.so')]
  if not libs or any('/arm64-v8a/' not in '/'+n for n in libs): raise SystemExit(f'Unexpected ABIs: {libs}')
  for name in libs:
   raw=archive.read(name)
   if raw[:5]!=b'\x7fELF\x02': raise SystemExit(f'Expected ELF64: {name}')
   phoff=struct.unpack_from('<Q',raw,32)[0]
   phsize,phcount=struct.unpack_from('<HH',raw,54)
   alignments=[]
   for i in range(phcount):
    entry=struct.unpack_from('<IIQQQQQQ',raw,phoff+i*phsize)
    if entry[0]==1: alignments.append(entry[-1])
   if not alignments or any(a<16384 for a in alignments): raise SystemExit(f'16 KB ELF alignment failure: {name}: {alignments}')
  return {'file':str(path),'bytes':path.stat().st_size,'native_libraries':libs,'elf_load_alignment_at_least':16384,'development_resources_absent':True}

def main():
 parser=argparse.ArgumentParser()
 parser.add_argument('archive',type=Path)
 parser.add_argument('--build-tools',type=Path)
 parser.add_argument('--java-sdk',type=Path)
 parser.add_argument('--bundletool',type=Path)
 parser.add_argument('--expect-release',action='store_true')
 args=parser.parse_args()
 result=verify_archive(args.archive)
 if args.archive.suffix=='.aab' and args.bundletool:
  java=(args.java_sdk/'bin/java') if args.java_sdk else Path('java')
  def bundle(*cmd):
   return subprocess.check_output([str(java),'-jar',str(args.bundletool),*map(str,cmd)],text=True,stderr=subprocess.STDOUT)
  validation=bundle('validate','--bundle='+str(args.archive))
  manifest=bundle('dump','manifest','--bundle='+str(args.archive),'--module=base')
  document=ET.fromstring(manifest)
  android='{http://schemas.android.com/apk/res/android}'
  sdk=document.find('uses-sdk'); app=document.find('application')
  if document.get('package')!='com.madebykreativ.blockshift': raise SystemExit('Wrong AAB identifier')
  if sdk is None or sdk.get(android+'minSdkVersion')!='26' or sdk.get(android+'targetSdkVersion')!='36': raise SystemExit('Wrong AAB SDK levels')
  if app is None or app.get(android+'debuggable','false')!='false': raise SystemExit('Expected a non-debuggable release AAB')
  if app.get(android+'allowBackup')!='false': raise SystemExit('Expected backup disabled')
  permissions=sorted(e.get(android+'name') for e in document.findall('uses-permission'))
  if permissions!=['android.permission.VIBRATE']: raise SystemExit(f'Unexpected AAB permissions: {permissions}')
  version=re.search(r'config/version="([^"]+)"',Path('project.godot').read_text()).group(1)
  code=re.search(r'version/code=(\d+)',Path('export_presets.cfg').read_text()).group(1)
  if document.get(android+'versionName')!=version or document.get(android+'versionCode')!=code: raise SystemExit('AAB version differs from current project')
  with zipfile.ZipFile(args.archive) as archive:
   release_paths=[n for n in archive.namelist() if n.endswith('/assets/config/release.json')]
   if len(release_paths)!=1: raise SystemExit('Expected exactly one release configuration in base or install-time assets')
   release=json.loads(archive.read(release_paths[0]))
   if release.get('support_email')!='info@madebykreativ.com' or release.get('channel')!='internal': raise SystemExit('Missing internal-test publisher configuration')
  result.update({'bundletool_validated':True,'package':document.get('package'),'version':version,'version_code':int(code),
    'min_api':26,'target_api':36,'debuggable':False,'backup_enabled':False,'permissions':permissions})
 if args.archive.suffix=='.apk' and args.build_tools:
  env=os.environ.copy()
  if args.java_sdk: env['JAVA_HOME']=str(args.java_sdk)
  def run(*cmd): return subprocess.check_output([str(v) for v in cmd],text=True,stderr=subprocess.STDOUT,env=env)
  badging=run(args.build_tools/'aapt2','dump','badging',args.archive)
  if args.expect_release and 'application-debuggable' in badging: raise SystemExit('Expected a non-debuggable release APK')
  for field,expected in [('minSdkVersion','26'),('targetSdkVersion','36')]:
   if f"{field}:'{expected}'" not in badging: raise SystemExit(f'Manifest {field} differs from {expected}')
  if "package: name='com.madebykreativ.blockshift'" not in badging: raise SystemExit('Wrong application identifier')
  permissions=run(args.build_tools/'aapt2','dump','permissions',args.archive)
  requested=re.findall(r"uses-permission(?:-sdk-\d+)?: name='([^']+)'",permissions)
  if sorted(requested)!=['android.permission.VIBRATE']: raise SystemExit(f'Unexpected permissions: {requested}')
  run(args.build_tools/'apksigner','verify','--verbose',args.archive)
  run(args.build_tools/'zipalign','-c','-P','16','4',args.archive)
  package_line=next(line for line in badging.splitlines() if line.startswith('package:'))
  result.update({'package':'com.madebykreativ.blockshift','min_api':26,'target_api':36,'permissions':requested,
    'version':re.search(r"versionName='([^']+)'",package_line).group(1),
    'version_code':int(re.search(r"versionCode='(\d+)'",package_line).group(1)),
    'debuggable':'application-debuggable' in badging,'signature_verified':True,'zip_16kb_alignment_verified':True})
 print(json.dumps(result,indent=2))

if __name__=='__main__': main()
