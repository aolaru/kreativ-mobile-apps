"""Talk to the temporary same-signer release harness; never enables WebView debugging."""
import base64, json, os, re, shlex, subprocess, sys
from pathlib import Path
ADB = str(Path(os.environ.get('ANDROID_HOME', Path.home() / 'Library/Android/sdk')) / 'platform-tools/adb')
def broadcast(serial, command, source=None):
    if not re.fullmatch(r'[\w-]+', serial): raise ValueError('An explicitly selected device serial is required')
    args=['am','broadcast','-a','com.kreativfont.studio.RELEASE_CHECK','-p','com.kreativfont.studio','--es','command',command]
    if source is not None: args += ['--es','source',base64.b64encode(source.encode()).decode()]
    output=subprocess.check_output([ADB,'-s',serial,'shell',shlex.join(args)],text=True,timeout=30)
    match=re.search(r'Broadcast completed: result=(\d+)(?:, data="(.*)")?', output, re.S)
    if not match or match[1]!='0' or match[2] is None: raise RuntimeError(output)
    raw=match[2].strip()
    try: return json.loads(raw)
    except json.JSONDecodeError: return raw
if __name__ == '__main__':
    if len(sys.argv) != 3 or sys.argv[2] not in {'eval', 'info', 'finish'}:
        raise SystemExit('Usage: release-device.py AUTHORIZED_SERIAL eval|info|finish (private snapshots must be saved directly)')
    print(json.dumps(broadcast(sys.argv[1],sys.argv[2],sys.stdin.read() if sys.argv[2]=='eval' else None),ensure_ascii=False,indent=2))
