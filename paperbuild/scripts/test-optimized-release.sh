#!/bin/bash
# Run only after the owner makes this device available. Never targets the Play app.
set -euo pipefail
set +x
paperbuild_serial="${1:?Usage: bash scripts/test-optimized-release.sh DEVICE_SERIAL}"
paperbuild_script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
paperbuild_project_dir="$(cd -- "$paperbuild_script_dir/.." && pwd)"
paperbuild_adb="${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}/platform-tools/adb"
paperbuild_package='com.kreativ.paperbuild.r8test'
paperbuild_code_file="${PAPERBUILD_REVIEW_CODE_FILE:-$HOME/Library/Application Support/Kreativ/PaperBuild/reviewer-access/review-code.txt}"
if [[ ! -f "$paperbuild_code_file" ]]; then
    echo 'The existing private reviewer code is required. Never add it to source or logs.' >&2
    exit 1
fi
if [[ -n "$("$paperbuild_adb" -s "$paperbuild_serial" shell pm path "$paperbuild_package")" ]]; then
    echo 'The isolated test package already exists. Review its test data before explicitly resetting it; no automatic deletion is performed.' >&2
    exit 1
fi
export JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home'
cd -- "$paperbuild_project_dir"
./gradlew -PpaperbuildIsolatedTest :app:assembleRelease :app:assembleReleaseAndroidTest
"$paperbuild_adb" -s "$paperbuild_serial" install app/build/isolated-r8/outputs/apk/release/app-release.apk
"$paperbuild_adb" -s "$paperbuild_serial" install -r app/build/isolated-r8/outputs/apk/androidTest/release/app-release-androidTest.apk
paperbuild_review_code="$(< "$paperbuild_code_file")"
if [[ ! "$paperbuild_review_code" =~ ^PBR1\.[A-Za-z0-9_-]+$ ]]; then
    echo 'Private reviewer code format is invalid.' >&2
    exit 1
fi
paperbuild_test_output="$("$paperbuild_adb" -s "$paperbuild_serial" shell am instrument -w \
    -e reviewCode "$paperbuild_review_code" "$paperbuild_package.test/com.kreativ.paperbuild.OptimizedReleaseRunner")"
unset paperbuild_review_code
printf '%s\n' "$paperbuild_test_output"
if [[ "$paperbuild_test_output" != *'OK (4 tests)'* ]]; then exit 1; fi
mkdir -p tmp/pdfs/r8-1.2.0
"$paperbuild_adb" -s "$paperbuild_serial" pull \
    "/sdcard/Android/data/$paperbuild_package/files/optimized-review/." tmp/pdfs/r8-1.2.0/
echo 'Device workflows passed. Independently validate/render the exported PDFs before release approval.'
