#!/bin/bash
set -euo pipefail

# Keep credentials in macOS Keychain; never print or save the password to the repo.
script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
project_dir="$(cd -- "$script_dir/.." && pwd)"

export KREATIV_SHARESAFE_KEYSTORE="${KREATIV_SHARESAFE_KEYSTORE:-$HOME/Library/Application Support/Kreativ/ShareSafe/sharesafe-release.jks}"
if [[ ! -f "$KREATIV_SHARESAFE_KEYSTORE" ]]; then
    echo 'ShareSafe release keystore is missing. See README.md for its backup location.' >&2
    exit 1
fi
if [[ -z "${KREATIV_SHARESAFE_KEYSTORE_PASSWORD:-}" ]]; then
    KREATIV_SHARESAFE_KEYSTORE_PASSWORD="$(/usr/bin/security find-generic-password \
        -a sharesafe-release -s 'Kreativ ShareSafe Release Key' -w)"
fi
export KREATIV_SHARESAFE_KEYSTORE_PASSWORD

studio_jdk='/Applications/Android Studio.app/Contents/jbr/Contents/Home'
if [[ -x "$studio_jdk/bin/java" ]]; then
    export JAVA_HOME="$studio_jdk"
fi

cd -- "$project_dir"
exec ./gradlew :app:bundlePlayRelease "$@"
