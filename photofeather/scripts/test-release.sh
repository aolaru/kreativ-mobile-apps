#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
PHOTOFEATHER_ADB="${ANDROID_HOME:-$HOME/Library/Android/sdk}/platform-tools/adb"
# ANDROID_SERIAL chooses the device when multiple devices are connected.
# QA uses the debug certificate and a separate package; production credentials are never used.
./gradlew -PphotoFeatherQaRelease -PphotoFeatherTestBuildType=release :app:assembleRelease :app:assembleReleaseAndroidTest :app:lintRelease :image-engine:lintRelease --console=plain
"$PHOTOFEATHER_ADB" install -r app/build/outputs/apk/release/app-release.apk
"$PHOTOFEATHER_ADB" install -r app/build/outputs/apk/androidTest/release/app-release-androidTest.apk
mkdir -p review/release-1.0.0
"$PHOTOFEATHER_ADB" shell am instrument -w -r com.kreativ.photofeather.releasecheck.test/androidx.test.runner.AndroidJUnitRunner | tee review/release-1.0.0/qa-release-smoke.txt
rg -q '^OK \([0-9]+ tests\)' review/release-1.0.0/qa-release-smoke.txt
