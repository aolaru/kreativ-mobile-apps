#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
engine="${GODOT_BIN:-godot}"
mkdir -p build
touch build/.gdignore
python3 tools/test_audio.py
task_logs="$(mktemp -d)"
trap 'rm -rf "$task_logs"' EXIT
run_check() {
  local log="$task_logs/$1.log"
  shift
  python3 -c 'import subprocess,sys; sys.exit(subprocess.run(sys.argv[1:], timeout=240).returncode)' "$engine" "$@" >"$log" 2>&1 || { cat "$log"; exit 1; }
  if rg -q 'SCRIPT ERROR|^ERROR:|FAIL:' "$log"; then cat "$log"; exit 1; fi
  rg 'assertions|Stress:' "$log" || true
}
run_check import --headless --path . --editor --import --quit
run_check core --headless --path . --script tests/run_tests.gd
run_check transfer --headless --path . --script tests/transfer_tests.gd
if [[ "${1:-}" == "--render" ]]; then
  run_check ui --always-on-top --path . --script tests/ui_smoke.gd
  run_check transfer_ui --always-on-top --path . --script tests/transfer_ui_smoke.gd
  run_check reliability --always-on-top --path . --script tests/reliability_smoke.gd
  run_check graphics --always-on-top --path . --script tests/graphics_smoke.gd
  run_check layout --always-on-top --path . --script tests/export_android_layout.gd
else
  echo 'UI: skipped (use --render for native input and pixel/layout assertions)'
fi
