# BLOCKSHIFT 0.5.0 — internal-test candidate

Validated on 17 October 2026. Android version code **7**, package `com.madebykreativ.blockshift`, Godot 4.6.3 stable. This edition is free, offline and has no advertising, billing or analytics SDK. The signed bundle has been uploaded to the new Play Console app. Google Play signing is enabled. The internal track is active and available to the two selected testers.

## Changes

- Removed unavailable purchase/restore and premium-theme promotions from Settings; legacy PRO navigation returns safely to About or Home.
- Added accurate, complete in-app privacy text identifying Made by Kreativ / Andrei Olaru, optional local haptics, disabled Android backup, local retention/reset behavior and the confirmed support contact `info@madebykreativ.com`.
- Added public policy HTML, release notes, listing copy and a Play Console handoff under `store/internal-testing/`. The hosted URL remains unset in the game until deployment is verified. The in-app policy is readable without a network connection.
- Created a dedicated RSA-3072 upload key outside the workspace with explicit user authorization. Owner-only recovery files hold the key and password. Signing passes credentials through environment variables; temporary bundletool password files are private and removed after use. No secret is embedded in presets, reports or tester artifacts.
- Added signed-bundle generation, public-certificate matching, AAB inspection, bundle-derived APK generation and reproducible release QA helpers. Private keys are not part of the handoff.
- Updated rendered test helpers to scroll controls into view before clicking the longer privacy page; a 240-second per-check timeout prevents script errors from leaving a test process running indefinitely.

## Automated checks

`tools/test.sh --render`: **7,249 assertions, zero failures**: 7,122 core, 79 UI, 31 reliability, 17 graphics. Stress coverage remains 80 seeded games and 1,285 placements. Audio level/clipping/loop/seam checks passed. `review/logs/internal-0_5_0-tests.log` records this run.

The first reliability attempt exposed an out-of-viewport click in the harness after the privacy text grew. It was fixed by scrolling the target into view, then the entire suite passed. No gameplay-rule or save-envelope version changed.

## Android packaging

`build/blockshift-internal-0.5.0.aab` was exported as a non-debuggable release, signed with the dedicated non-debug upload certificate, and verified by jarsigner with certificate fingerprint comparison. Official bundletool validation passed. Inspection confirms version 0.5.0/code 7, ARM64 only, minimum API 26/target API 36, backup disabled and VIBRATE-only permission. Test/debug/review/store-handoff material is absent from packaged resources. Native ELF LOAD segments meet the 16 KB alignment check.

`build/blockshift-internal-0.5.0.apk` was generated from that exact signed bundle using official bundletool. It passed Android signature verification, release-debug-flag inspection, permission/ABI/SDK checks and 16 KB ZIP/native alignment inspection. Play will sign its delivered APKs with its separate app signing key; Play-delivered installation is still an external check.

The four signing regression checks passed: existing identity overwrite is refused, signed input is rejected, modified signed content fails verification and the original candidate remains unchanged. See `logs/internal-0_5_0-signing-regressions.json`.

Evidence: `logs/internal-0_5_0-signature.json`, `logs/internal-0_5_0-bundle-inspection.json`, `logs/internal-0_5_0-apk-inspection.json`.

## Native runtime scope

Current 0.5.0 full release-APK touch/resume testing is **incomplete**. The bundle-derived APK installed, and a later manual cold launch returned Status ok (7,154 ms, process 2286). The first automated launch had no live process at its checkpoint; a clean rerun then timed out during installation after 180 seconds, and a subsequent package-path query also timed out. The emulator was stopped. These observations do not establish a complete native gameplay pass or a crash-free current release. See `logs/internal-0_5_0-android-smoke.json` and the launch diagnostic. A clean physical-device or Play-delivered smoke test remains a release-test gate. The emulator uses 4 KB memory pages, so this run cannot certify actual 16 KB runtime compatibility. Historical 0.4.1 Galaxy A55 validation remains in `VALIDATION.md`; the phone has not been modified during this task.

The host initially ran short of disk/memory during concurrent emulator and build activity. The emulator was stopped; only this project's disposable generated Gradle outputs were removed. Desktop tests and bundle processing completed before the next emulator attempt. The 1.5 GB/two-CPU hardware-rendered emulator completed its boot. The harness now waits for the app process during cold startup. The full native retry remained incomplete, as recorded above. A transient disk-full interruption also affected an optional signing-tamper test; the compact reproducible test is separate from the gameplay suite.

## External steps and known limits

- Dedicated BLOCKSHIFT internal tester list created and selected, with the two user-confirmed Google accounts.
- Play App Signing is enabled, the signed AAB is uploaded, and release 0.5.0 (7) is available to the two selected internal testers. The invitation page is verified. This developer account predates the new-personal-account closed-test gate.
- Policy published and verified at `https://madebykreativ.com/privacy/blockshift/`. Website commit `d3316fe` passed deployment CI. The URL is saved in Play Console (store-content review is pending). This uploaded binary provides full in-app text and a support email; add the verified hosted link to a subsequent build.
- Complete applicable Console declarations, intended audience/age groups and final listing/artwork before broader distribution. The internal track can start before full app setup is complete.
- The Galaxy A55's existing debug-signed installation cannot be updated in place by a differently signed Play package. Do not uninstall it before progress transfer or an explicitly accepted fresh start. No player-data reset or phone installation was performed here. There is no end-user save export/import UI.
- Actual 16 KB runtime, broader Android/OEM/safe-area/gesture coverage, longer battery/thermal runs, audible ambience/haptics and fresh-player SHIFT comprehension remain QA work. Android system font-scale synchronization and full screen-reader board operation remain limitations.
- Real billing, ads, consent integrations, premium themes and additional customization remain deferred commercial features. Placeholder original audio still needs listening acceptance. Title/rights clearance remains a public-release check.

## Live setup status — 17 October 2026

App `4972101290755310758` was created in the existing developer account after the user explicitly approved both creation declarations. Package identity remains `com.madebykreativ.blockshift`. Bundle 7 (0.5.0), API 26+/target 36, ARM64, is published and marked Available to internal testers (17 October, 20:52 local). No production release was created.

The connected Galaxy A55 remains on its existing 0.4.1 debug installation. A fresh private backup was taken outside the repository, checksums validated, and all three saved modes loaded and round-tripped through the actual save/session code without modifying the backup. Evidence: `logs/current-phone-backup-verification.log`. No installation, uninstall, clear-data or progress reset was performed. The older backup is also verified. Play installation and save transfer remain separate work.

Internal opt-in: https://play.google.com/apps/internaltest/4701679486737881255

Both confirmed accounts are saved in the dedicated list and selected for the track. No other lists are selected. The invitation page displays the correct package and Accept invite; it was inspected without accepting any tester agreement or installing the app. Google shows a temporary package-based name until full store setup/review. Store privacy content is saved but cannot be sent for review until the remaining dashboard requirements are completed. This does not block the active internal release. No production rollout was performed.

Play validation found zero blocking errors and two non-blocking warnings: missing R8/ProGuard deobfuscation file and missing native symbols. Native symbols should be supplied for useful engine crash diagnosis in later builds. The hosted policy deployment and current save-backup verification are complete; Play-delivered runtime and phone migration remain untested.

Proof: `play-internal-active.png`, `play-testers-active.png`, `play-policy-saved.png`, `privacy-policy-live.png`. The existing handoff/source ZIP files remain unchanged snapshots from before online publication; this report and BUILD_MANIFEST.json contain the current status.
