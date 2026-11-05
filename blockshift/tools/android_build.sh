#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
engine="${GODOT_BIN:-godot}"
python3 tools/prepare_android.py --godot "$engine"
case "${1:-apk}" in
  apk) preset="Android Debug"; kind="--export-debug"; target="build/blockshift-debug.apk" ;;
  aab-dev) preset="Android AAB"; kind="--export-debug"; target="build/blockshift-development.aab" ;;
  aab-release-check|aab-internal) preset="Android Release Validation"; kind="--export-release"; target="build/blockshift-release-unsigned.aab" ;;
  *) echo 'Usage: android_build.sh [apk|aab-dev|aab-release-check|aab-internal]' >&2; exit 2 ;;
esac
mkdir -p build review/logs
touch build/.gdignore
rm -f "$target" "$target.idsig"
"$engine" --headless --path . "$kind" "$preset" "$target" 2>&1 | tee "review/logs/android-build.log"
if rg -q 'SCRIPT ERROR|^ERROR:|BUILD FAILED' review/logs/android-build.log || [[ ! -s "$target" ]]; then
  echo 'Android export did not pass.' >&2
  exit 1
fi
printf 'Built %s\n' "$target"
if [[ "${1:-}" == "aab-internal" ]]; then
  python3 tools/sign_android.py
fi
