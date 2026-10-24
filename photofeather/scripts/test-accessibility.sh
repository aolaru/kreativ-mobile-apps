#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
PHOTOFEATHER_ADB="${ANDROID_HOME:-$HOME/Library/Android/sdk}/platform-tools/adb"
# Enable TalkBack before invoking this opt-in check; the script does not change system settings.
# Uses an isolated package that owns generated fixtures only. Set ANDROID_SERIAL as needed.
./gradlew -PphotoFeatherTestSuffix=.accessverify -PphotoFeatherAccessibilityTest :app:assembleDebug :app:assembleDebugAndroidTest --console=plain
"$PHOTOFEATHER_ADB" install -r app/build/outputs/apk/debug/app-debug.apk
"$PHOTOFEATHER_ADB" install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
mkdir -p review/release-1.0.0
"$PHOTOFEATHER_ADB" shell am instrument -w -r com.kreativ.photofeather.accessverify.test/androidx.test.runner.AndroidJUnitRunner | tee review/release-1.0.0/talkback-recheck.txt
rg -q '^OK \([0-9]+ test' review/release-1.0.0/talkback-recheck.txt
