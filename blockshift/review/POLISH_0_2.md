# BLOCKSHIFT 0.2.0 — UI and configuration polish

## Implemented

- Home is shorter, with one clear primary action and a saved-board card showing mode, score and cleared lines. Daily completion is visible; PRO is reachable through About.
- Board and hand form one centered group instead of leaving a large gap. Tray pieces fit both height and width, including tall shapes. Unplaceable shapes have a text indicator as well as dimming.
- Held pieces, previews, validation and landing use the same configurable finger offset. Drag height is adjustable under Settings → Play. Existing saves default to the previous 88 logical units.
- SHIFT has named row/column direction buttons and a full selected-line outline. The four-line charge explanation and remaining-clear count remain visible. Compact portrait and larger text layouts keep all controls in view.
- Settings is grouped into Play, Sound and About. Toggles have consistent padding and original vector on/off artwork. Sliders show meaningful values. Restore defaults requires an explicit action and keeps the active board, scores and tutorial completion.
- Battery saver caps gameplay at 30 fps; normal play uses 60 fps and menus use 30 fps. Unnecessary per-frame pickup processing was removed. Battery-life gains are not measured.
- In-game help, clear pause/game-over explanations, scrolling modal sheets and parent-aware Back navigation improve recovery and learning. First-time SHIFT entry from mode selection also reaches the tutorial.
- Phone testing exposed duplicated Android Back delivery. A 180 ms guard prevents one physical press from opening and immediately closing Pause. The final APK passes the repeated-window-event regression check and real ADB Back key checks on the Galaxy A55.
- Version is 0.2.0 / Android code 2. Gameplay balance, generator catalog, daily rules version and save envelope version are unchanged.

## Verification

The final rendered test command passes 7,054 gameplay assertions and 51 UI assertions, plus audio level/clipping/loop checks. Eighty seeded stress games complete 1,285 placements. Tests cover old-save settings defaults, bounds/nonfinite values, restoring settings without losing a board, changed drag-height targets, battery frame budget after navigation, compact armed column controls, actual parent-page Back behavior and duplicate Back events.

All three Android artifacts were rebuilt from the final source: signed development APK, signed development AAB and unsigned release AAB. Package inspection checks ARM64, 16 KB native/ZIP alignment where applicable, API 26/36, VIBRATE-only permission and absence of developer resources. The release bundle is validated with official bundletool and remains unsigned and non-debuggable.

The final APK was installed over the existing game on Samsung Galaxy A55 / SM-A556B. The saved Classic board, remaining pieces, score, RNG and move count are preserved. All prior preference values remain intact; new controls receive defaults. Real touches exercised Continue, Pause and the Play/Sound settings tabs. Injected Android Back opens Pause; a second press resumes. Runtime logs contain no script errors. Physical drag/SHIFT play, audio listening, gesture diversity and sustained battery behavior are not certified by these checks.

The emulator harness now obtains touch targets from the real Godot UI through tests/export_android_layout.gd and review/android-layout.json, and checks the fixture version against the APK. Fixture export and Python syntax checks pass. The complete emulator reset/tutorial/restart harness was last run on the previous 0.1.0 baseline; it was not rerun for 0.2.0. Current Android runtime evidence comes from the physical Galaxy A55.

Evidence: logs/polish-tests.log, logs/polish-phone.json, logs/apk-inspection.json, logs/development-inspection.json, logs/release-inspection.json, logs/bundle-validation.log; rendered screenshots 01–24 and phone-polish-*.png. Artifact hashes are in BUILD_MANIFEST.json.

Commercial billing/ads remain disabled adapters. Production signing, publisher/name clearance, broad device/accessibility testing and final audio remain release work.
