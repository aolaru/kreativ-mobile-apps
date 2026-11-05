#!/bin/bash
# Only the disposable 1.2 QA app is updated; never installs over the Play package.
set -euo pipefail
set +x
paperbuild_serial="${1:?Usage: bash scripts/capture-listing-screenshots.sh DEVICE_SERIAL}"
paperbuild_script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd -- "$paperbuild_script_dir/.."
paperbuild_adb="${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}/platform-tools/adb"
paperbuild_code_file="${PAPERBUILD_REVIEW_CODE_FILE:-$HOME/Library/Application Support/Kreativ/PaperBuild/reviewer-access/review-code.txt}"
[[ -f "$paperbuild_code_file" ]] || { echo 'The existing private reviewer code is required.' >&2; exit 1; }
export JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home'
./gradlew -I listing-tools/listing.init.gradle -PpaperbuildQaApplicationIdSuffix=.v120.qa :app:assembleQa :app:assembleQaAndroidTest
"$paperbuild_adb" -s "$paperbuild_serial" install -r app/build/outputs/apk/qa/app-qa.apk
"$paperbuild_adb" -s "$paperbuild_serial" install -r app/build/outputs/apk/androidTest/qa/app-qa-androidTest.apk
paperbuild_capture_code="$(< "$paperbuild_code_file")"
[[ "$paperbuild_capture_code" =~ ^PBR1\.[A-Za-z0-9_-]+$ ]] || { echo 'Private code format is invalid.' >&2; exit 1; }
paperbuild_capture_result="$("$paperbuild_adb" -s "$paperbuild_serial" shell am instrument -w -e reviewCode "$paperbuild_capture_code" com.kreativ.paperbuild.v120.qa.test/androidx.test.runner.AndroidJUnitRunner)"
unset paperbuild_capture_code
printf '%s\n' "$paperbuild_capture_result"
[[ "$paperbuild_capture_result" == *'OK (1 test)'* ]] || exit 1
mkdir -p store-assets/play/screenshots/1.2/raw
"$paperbuild_adb" -s "$paperbuild_serial" pull /sdcard/Android/data/com.kreativ.paperbuild.v120.qa/files/listing-1.2/. store-assets/play/screenshots/1.2/raw/
python3 - "$paperbuild_adb" "$paperbuild_serial" <<'PY'
from pathlib import Path
import subprocess, sys, xml.etree.ElementTree as ET
adb, serial = sys.argv[1:]; pkg = 'com.kreativ.paperbuild.v120.qa'
def read(path): return subprocess.check_output([adb, '-s', serial, 'exec-out', 'run-as', pkg, 'cat', path])
name = next(e.text for e in ET.fromstring(read('shared_prefs/paperbuild_editor.xml')) if e.get('name') == 'source')
assert name and '/' not in name
folder = Path('tmp/pdfs/listing-1.2'); folder.mkdir(parents=True, exist_ok=True)
pdf = folder / 'studio-branded.pdf'; pdf.write_bytes(read('files/paperbuild/editor/' + name))
subprocess.run(['pdftoppm', '-r', '180', '-singlefile', '-png', str(pdf), 'store-assets/play/screenshots/1.2/raw/logo-export-render'], check=True)
PY
