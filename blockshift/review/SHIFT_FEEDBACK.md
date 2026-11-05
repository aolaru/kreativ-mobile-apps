# SHIFT charge clarity — 2026-10-17

Player feedback: it was unclear when SHIFT becomes available.

The meter now shows the charge percentage and the number of full line clears still needed. The disabled button reads “Charging”; a full meter reads “SHIFT READY” and enables “Use SHIFT”. Persistent hints explain that rows and columns charge the meter across turns and that SHIFT is optional. Line-clear feedback reports progress or availability. The tutorial explicitly connects its first line clear to a full meter. No gameplay balance or saved-data format changed.

Validation: 7,049 gameplay assertions and 29 UI assertions passed, including placement without charge, partial charge, four accumulated line clears, configurable per-line gain, the available button, and short portrait layout with larger text. Rendered screenshots 17 and 18 were checked. Android debug APK export, signature, archive exclusions and 16 KB alignment checks passed. Updated APK installed with `adb install -r` and launched on a physical Samsung SM-A556B; Godot main loop started with no script errors in the captured startup log. No data reset was performed. Hands-on usability confirmation remains with the player.

The debug APK and source ZIP include this update. The two AABs remain the previously validated baseline builds; their earlier emulator validation is documented in VALIDATION.md. The complete emulator harness was not rerun for this text/UI update.
