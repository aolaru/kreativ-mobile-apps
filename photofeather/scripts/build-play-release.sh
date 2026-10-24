#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

# Passwords and private keys stay outside Git and build logs.
export PHOTOFEATHER_KEYSTORE="${PHOTOFEATHER_KEYSTORE:-$HOME/Library/Application Support/Kreativ/PhotoFeather/photofeather-upload.jks}"
export PHOTOFEATHER_KEY_ALIAS="${PHOTOFEATHER_KEY_ALIAS:-photofeather-upload}"
if [[ ! -f "$PHOTOFEATHER_KEYSTORE" ]]; then
  echo 'PhotoFeather upload keystore is missing. See docs/PLAY_RELEASE.md.' >&2
  exit 1
fi
if [[ -z "${PHOTOFEATHER_STORE_PASSWORD:-}" ]]; then
  PHOTOFEATHER_STORE_PASSWORD="$(/usr/bin/security find-generic-password \
    -s 'com.kreativ.photofeather.upload-key' -a 'photofeather-upload' -w)"
fi
export PHOTOFEATHER_STORE_PASSWORD
export PHOTOFEATHER_KEY_PASSWORD="${PHOTOFEATHER_KEY_PASSWORD:-$PHOTOFEATHER_STORE_PASSWORD}"
if [[ -z "$PHOTOFEATHER_STORE_PASSWORD" || -z "$PHOTOFEATHER_KEY_PASSWORD" ]]; then
  echo 'Release signing passwords are unavailable.' >&2
  exit 1
fi
if [[ -x '/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/java' ]]; then
  export JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home'
fi
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
./gradlew :app:bundleRelease :app:assembleRelease :app:lintRelease :image-engine:lintRelease --console=plain "$@"
bundle_verification="$("$JAVA_HOME/bin/jarsigner" -J-Duser.language=en -J-Duser.country=US -verify app/build/outputs/bundle/release/app-release.aab 2>&1)"
printf '%s\n' "$bundle_verification"
if [[ "$bundle_verification" != *'jar verified.'* ]]; then
  echo 'The app bundle is not verifiably signed. Do not upload it.' >&2
  exit 1
fi
"$ANDROID_HOME/build-tools/36.1.0/apksigner" verify --verbose --print-certs app/build/outputs/apk/release/app-release.apk
package_info="$("$ANDROID_HOME/build-tools/36.1.0/aapt" dump badging app/build/outputs/apk/release/app-release.apk)"
if [[ "$package_info" != "package: name='com.kreativ.photofeather' "* ]]; then
  echo 'The APK does not have the production PhotoFeather package. Do not upload it.' >&2
  exit 1
fi
