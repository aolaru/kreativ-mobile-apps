#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
PHOTOFEATHER_ADB="${ANDROID_HOME:-$HOME/Library/Android/sdk}/platform-tools/adb"
PHOTOFEATHER_SUFFIX=".verify"
PHOTOFEATHER_PACKAGE="com.kreativ.photofeather${PHOTOFEATHER_SUFFIX}"
# This package owns only generated test fixtures. Never clear or instrument the user's .debug app.
./gradlew -PphotoFeatherTestSuffix="$PHOTOFEATHER_SUFFIX" :app:assembleDebug :app:assembleDebugAndroidTest --console=plain
"$PHOTOFEATHER_ADB" install -r app/build/outputs/apk/debug/app-debug.apk
"$PHOTOFEATHER_ADB" install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
"$PHOTOFEATHER_ADB" shell am instrument -w -r "$PHOTOFEATHER_PACKAGE.test/androidx.test.runner.AndroidJUnitRunner" | tee review/device-workflows.txt
rg -q '^OK \([0-9]+ tests\)' review/device-workflows.txt
mkdir -p review/device-captures
"$PHOTOFEATHER_ADB" pull "/sdcard/Android/data/$PHOTOFEATHER_PACKAGE/files/screenshots/." review/device-captures/
