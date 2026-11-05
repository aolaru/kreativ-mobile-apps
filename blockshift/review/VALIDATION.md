> Historical 0.4.1 evidence. The current 0.5.0 internal candidate is documented in [INTERNAL_TESTING_0_5_0.md](INTERNAL_TESTING_0_5_0.md). Do not attribute these physical-phone results to 0.5.0.

# BLOCKSHIFT validation — 0.4.1 / 2026-10-17

Current project: **0.4.1 / Android code 6**, Godot 4.6.3 stable. Save envelope version 3 and Daily rules version 2 are unchanged. This is a playable development release; production billing, advertising and release signing remain unfinished.

## Fixed and verified

- Failed checkpoints propagate to a persistent storage notice. Native Retry remains clickable under pause/confirmation sheets; transient errors also retry automatically. Pause/results only claim successful persistence after a successful write. Tests verify recovery of the latest board without duplicate statistics and compact/larger-text warning layouts.
- Progress reset uses a separate verified stage. Read-only/staging/commit failures preserve live progress and recovery material. Successful reset survives reload, removes old recovery copies and retains preferences.
- Dialogs own their Back behavior. Settings, Privacy and PRO confirmations dismiss in place even when a finished game remains in memory. Results and lesson completion explicitly return home.
- Daily/terminal state is committed and saved immediately, while the final placement or SHIFT wrap/clear plays before results. Further input is rejected. Reduced motion skips the delay. Both placement and SHIFT completion paths are covered.
- Voluntary finish records a backward-compatible manual end reason and displays a truthful explanation after reload.
- SHIFT practice starts with zero energy, earns charge over four separate real line clears, highlights targets and preserves the final clear before movement practice. The meter explains charging while score, line and combo feedback remains visible. Early use during the lesson transition is rejected.

## Automated tests

**7,245 assertions; zero failures:** 7,122 core, 75 rendered UI, 31 reliability regressions and 17 graphics assertions. Eighty seeded stress games cover 1,285 placements. Audio level, clipping, sustained-loop and seam checks passed. The renderer recorded 150 controlled demonstration frames. See `logs/reliability-tests.log`; reproduce with `tools/test.sh --render`.

The rendered harness uses viewport-local touch events for board gestures and native desktop mouse hit testing for buttons. Isolated saves and controlled host focus prevent player-data changes; focus/audio lifecycle is also exercised explicitly. This is not a physical-phone frame-rate or battery benchmark.

A disposable copy of the captured legacy phone save migrated and round-tripped successfully, with the original unchanged. Gameplay fields, preferences, statistics and Daily history remained consistent. See `logs/reliability-migration-fixture.log`.

## Android builds and native checks

Debug APK, development AAB and unsigned release AAB exported successfully. APK inspection confirms version 0.4.1/code 6, package `com.madebykreativ.blockshift`, min API 26, target API 36, ARM64 only, VIBRATE-only permissions, valid debug signature and 16 KB ZIP/native ELF alignment. Tests, tools, review material and debug boards are absent from exports. Both bundles passed bundletool validation and native-library inspection. The development AAB is debuggable and unsigned by this exporter; the release AAB is non-debuggable and deliberately unsigned. See `logs/reliability-*-inspection.json` and `logs/reliability-bundle-validation.json`.

The **0.4.1 APK passed the Android 16 / API 36.1 ARM64 emulator harness at 1080×2400**: cold menu without touch, four charging clears, preview/Apply, tutorial isolation from player statistics, Classic touch placement, force-stop/resume, exact board/hand/score/RNG/move preservation and idle playtime autosave. Its two runtime log windows contained no script errors or fatal exceptions. Additional native Back-to-Pause behavior was visually verified in `android-back-pause-0_4_1.png`. See `logs/reliability-android-smoke.json` and `logs/android-runtime.log`.

The initial one-CPU/software-rendered emulator profile became unresponsive and Android system_server restarted; its failed attempts are retained in logs. The successful run used the same AVD with two CPUs and host graphics (Apple M1), with no game-code change. The emulator was launched without audio, so this run does not certify audible output or haptics.

**0.4.1 is installed and tested on the physical Samsung Galaxy A55 / SM-A556B**, Android 16 / API 36, 1080×2340. The installed APK SHA-256 matches the validated artifact. Upgrade from 0.3.1 preserved the save file byte-for-byte before first launch; migration preserved the entire legacy SHIFT snapshot, settings, statistics and Daily history.

Phone touch checks completed four real charging clears (0→25→50→75→ready), pause/resume with retained partial charge, preview, Cancel with exact restored board pixels, then Apply with row wrap and column clear. The complete saved player payload was unchanged by the tutorial. Classic and Zen received distinct one-placement test runs and resumed their own board/hand/score/energy/RNG/move state. Cancelling New game preserved both. Backgrounding, force-stop and relaunch preserved the entire saved payload exactly; idle playtime autosave also advanced after resume. Resuming added no games. All prior high scores, settings, Daily history and the original completed 928-point SHIFT snapshot are unchanged. The two test runs remain available, each at 3 points; their real starts added one game to Classic and Zen statistics. No app-data reset or save-file injection/restoration was used.

Both phone runtime-log windows contain zero script errors, fatal exceptions or fatal signals. The phone was left at the main menu with Continue Zen and an independently saved Classic board. See `logs/phone-0_4_1.json` and `phone-0_4_1/` screenshots. Raw player backups are excluded from the source ZIP. Storage-failure/corruption injection, long sessions, audible output, haptics and actual Back gesture recognition were not certified on the phone. Earlier physical-phone recordings remain 0.3.1 material; `logs/android-smoke-0_1_0.json` retains the old emulator baseline.

## Deliverables and limits

`build/blockshift-debug.apk` is the sideloadable development update. Both bundle variants, refreshed source ZIP and current renderer preview are available under build. `BUILD_MANIFEST.json` records versions, scope, sizes and exact SHA-256 hashes. Raw player-save fixtures, caches and signing credentials are excluded from the source archive.

Screens 41–46 document the revised charge lesson, final clear, storage warning, compact layout, retained combo feedback and voluntary ending. Broad physical-device coverage, long sessions, accessibility services, system font scaling, final audio, real purchases/ads and commercial store submission remain outside this validation. See `../docs/KNOWN_LIMITATIONS.md`.
