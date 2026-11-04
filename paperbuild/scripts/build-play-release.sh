#!/bin/bash
set -euo pipefail
set +x

for paperbuild_arg in "$@"; do
    case "$paperbuild_arg" in
        -PpaperbuildIsolatedTest*)
            echo 'Isolated R8 test packages cannot be built by the Play release script.' >&2
            exit 1
            ;;
    esac
done
paperbuild_script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
paperbuild_project_dir="$(cd -- "$paperbuild_script_dir/.." && pwd)"
export PAPERBUILD_STORE_FILE="${PAPERBUILD_STORE_FILE:-$HOME/Library/Application Support/Kreativ/PaperBuild/PaperBuild-release.jks}"
export PAPERBUILD_KEY_ALIAS="${PAPERBUILD_KEY_ALIAS:-paperbuild}"
if [[ ! -f "$PAPERBUILD_STORE_FILE" ]]; then
    echo 'PaperBuild upload keystore is missing.' >&2
    exit 1
fi
if [[ -z "${PAPERBUILD_STORE_PASSWORD:-}" ]]; then
    PAPERBUILD_STORE_PASSWORD="$(/usr/bin/security find-generic-password -a PaperBuild-release-key -s PaperBuild-release-key -w)"
fi
export PAPERBUILD_STORE_PASSWORD
export PAPERBUILD_KEY_PASSWORD="${PAPERBUILD_KEY_PASSWORD:-$PAPERBUILD_STORE_PASSWORD}"
if [[ -x '/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/java' ]]; then
    export JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home'
fi
cd -- "$paperbuild_project_dir"
exec ./gradlew :app:assembleRelease :app:bundleRelease "$@"
