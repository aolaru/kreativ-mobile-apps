# BLOCKSHIFT 0.5.1 / code 8 — 26 October 2026

The signed local candidate is ready. It is **uploaded and saved as an internal-test draft**, not rolled out. The active internal release remains 0.5.0 (7), available to the two selected tester accounts. No production release exists.

## Implemented and verified

- All four mode saves, statistics, daily history and preferences can be exported as one versioned, checksummed private file. Native Android SAF needs Android 10+ and no storage permission. Exports are read back before success is reported.
- Restore validates size, format, version, checksum, run state and counters before preview. Confirmation stages and verifies a save transaction, preserves previous progress for Undo, and only then replaces the main save and live session. Future-version saves remain read-only.
- The final source passed **7,311 assertions, zero failures**: 7,122 core, 49 transfer, 79 UI, 13 transfer UI, 31 reliability and 17 graphics. Audio level/clipping/loop/seam checks passed. Stress coverage: 80 seeded games / 1,285 placements. Evidence: `logs/0_5_1-final-tests.log`.
- Dedicated upload-key signature and certificate comparison passed. Official bundletool validation passed. The exact signed AAB produced a release QA APK; APK signature and 16 KB ZIP alignment passed. Both archives are ARM64, minimum API 26/target 36, release/non-debuggable and VIBRATE-only. Debug/test material is excluded; native ELF load alignment meets 16 KB. Evidence: `logs/0_5_1-signature.json`, `logs/0_5_1-bundle-inspection.json`, `logs/0_5_1-apk-inspection.json`, `BUILD_MANIFEST_0_5_1.json`.
- The policy is live at https://madebykreativ.com/privacy/blockshift/ with portable-backup disclosure, dated 26 October. Website commit 8f68174 passed deployment run 40347363346. Proof: `0_5_1-policy-live.png`.
- Play Console saved the 13+ audience, no login restrictions, no ads, Data safety, no government/financial/health features, Puzzle category and public support contacts. With explicit user agreement, IARC was completed: PEGI 3 / ESRB Everyone. Content ratings and intended audience are different declarations.
- The default English listing was saved with text, original icon, feature graphic and five actual game UI screenshots. Assets carry the created/edited-using-AI disclosure. These are controlled offscreen game renders, not physical-phone captures. Store changes are saved for review; no review submission was performed yet.

## Phone progress and actual runtime scope

The Galaxy A55 received an in-place debug-certificate development update to 0.5.1; it was not uninstalled, reset or replaced with a differently signed package. Private raw backups and the original installed APK remain outside the repository. A later backup preserves progress newer than the initial export. Its conversion and restoration through the actual save/session services passed with the original unchanged and all three saved modes intact (`logs/0_5_1-latest-private-backup.log`). No player backup is included in store assets or public artifacts.

Native export to Downloads succeeded and was independently read back and compared. The correct `.blockshift` file opened a valid restore preview on the phone. Mirrored coordinate input had selected an unrelated image earlier; it was safely rejected without changing progress. Keyboard navigation selected the correct file. Temporary diagnostic prints were removed from the final source.

**Native Apply restore, Undo, restart and the actual Play-installed gameplay pass remain unverified.** The Mac locked and the phone disconnected before this sequence could finish. The installed diagnostic development build and the final signed local candidate differ only by removal of debug read diagnostics plus packaging metadata; do not describe the signed candidate as tested on the phone. No native restore was applied and no game data was cleared. Audible ambience, hardware haptics and Play-signed tutorial/background/save behavior still need direct testing.

## Resume steps

1. Unlock the Mac and reconnect the phone with USB debugging allowed. Take another fresh private backup.
2. Cancel the older preview, export the latest progress and verify the selected file. Restore that latest file, then verify Undo and cold restart across modes. Keep both private backups.
3. Upload the verified 0.5.1 AAB to internal testing only, retain the selected tester list, and inspect Play validation.
4. Before removing the debug installation, have a verified external backup and explicit approval for the signature switch. Install from Play, restore latest progress and complete tutorial, mode-save, sound and background/resume checks.
5. Inspect Publishing overview and send only the intended store/declaration changes for review. Keep production inactive.

Broader release limitations remain documented in `docs/KNOWN_LIMITATIONS.md`. Successful packaging checks do not certify 16 KB runtime, broad device support, battery life or store review approval.

## Resumed check after Mac unlock

The phone briefly reconnected. A fresh private raw backup was taken and verified; conversion and restore through real save/session services passed with all three saved modes and original data unchanged (`logs/0_5_1-fresh-backup-transfer.log`). The menu still showed the latest Zen progress. USB dropped again during the next native export attempt, before any restore, Undo or uninstall. Native confirmation/Undo/restart remain pending.

The exact signed 0.5.1 (8) AAB was uploaded, processed and saved as the internal-test draft “0.5.1 (8) — Progress backup”. Reopening the track verified draft 8 separately from active release 7. Play validation reported zero blocking errors and two nonblocking warnings: missing R8/ProGuard mapping and native debug symbols. No rollout occurred. Proof: `0_5_1-play-draft-saved.png` and `0_5_1-play-validation.png`.

Publishing overview lists the saved store/declaration changes but disables Send app for review and links to dashboard requirements. Dashboard setup cards are complete; wider release tasks remain. No production or closed-testing release was created to bypass the internal-only scope. The temporary unreviewed app name remains expected until a reviewable release is submitted and approved.
