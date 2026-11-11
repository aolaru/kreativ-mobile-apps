# BLOCKSHIFT 0.5.3 / code 10 — 7 November 2026

**0.5.3 (10) — Softer Forest is active on Google Play internal testing**, released at 21:37 Europe/Bucharest. The original two-person BLOCKSHIFT list remains the only selected tester list. Public production is inactive. The phone was last verified on Play-installed 0.5.2 (9); USB disconnected before the correction could be installed. Final phone listening and post-update save comparison are pending reconnection.

## Forest correction

The user confirmed all three ambience choices and Off worked on the actual Play-installed 0.5.2 build, then reported that Forest's white-noise bed overwhelmed the birds. Forest now uses a much lower wind bed, reduced high-frequency hiss and bird phrases that stand more clearly above it. It is deliberately not normalised back up to the previous background level. Decoded overall RMS is −32.1 dBFS, compared with −21.6 dBFS in 0.5.2. The average level is lower; the bird phrases have greater contrast with the bed.

Rain and Ocean are byte-identical to their 0.5.2 exported resources. The change adds no gameplay, save-format, SDK, permission or commerce changes. The existing ambience selection and volume are retained. Forest is still original procedural nature-style audio, not a field recording.

## Verified locally and in Console

- Full tools/test.sh --render: **7,334 assertions, zero failures** (7,132 core, 50 transfer, 91 UI, 13 transfer UI, 31 reliability, 17 graphics). Stress: 80 seeded games / 1,285 placements. Log: logs/0_5_3-full-tests.log.
- All three decoded audio files passed duration, format, clipping, quiet-gap and loop checks. New Forest-specific checks confirm the pre-bird noise bed is below 0.03 RMS and the first bird phrase rises above it. Rain/Ocean levels remain −20.6/−19.8 dBFS RMS.
- Signed AAB and derived release QA APK passed bundletool/signature, version, permission and archive checks. Both are ARM64, non-debuggable, minimum API 26 / target API 36, VIBRATE only, with no development resources. Native ELF and APK ZIP alignment passed 16 KB checks; this is not actual 16 KB runtime certification.
- Loaded the three actual sound resources extracted from the AAB in Godot: **9 additional assertions, zero failures**, confirming Vorbis type, enabled looping and 48-second duration. Verified Rain/Ocean are unchanged and Forest differs in the exported bundle. Evidence: logs/0_5_3-packaged-audio.log and logs/0_5_3-audio-package-inspection.json.
- Play validation reported no blocking errors and two nonblocking warnings: absent deobfuscation mapping and native debug symbols. No supported-device loss was shown. Only the internal track was published. Proof: 0_5_3-play-internal-active.png and 0_5_3-play-both-testers.png.

## Progress safety and remaining phone checks

A fresh native 0.5.1 backup was exported before the first update, copied privately to the Mac, checksum-verified and tested on a disposable copy through real save/session services. All three saved modes remained intact. The original raw and portable backups were unchanged. The 0.5.2 update was installed in place through Google Play; no uninstall, reset or restore was performed. The user confirmed playback of Rain, Forest, Ocean and Off. Forest at 15% and SFX at 50% survived a verified force-stop/cold launch on 0.5.2.

The final export dialog was not completed before Samsung's game touch-lock appeared and USB disconnected. A Classic run had been resumed between backup and testing, then paused at 20 points before updating. The final comparison must preserve any legitimate elapsed-time/last-mode changes rather than blindly restore the older backup. No moves were made by the agent. Do not claim exact post-update save equality until the new export is available.

On reconnection: install 0.5.3 using the existing Play Store installation, confirm the Forest mix on the physical speaker, verify the chosen sound/volume after restart and export current progress for comparison. Do not sideload the upload-key QA APK over the differently signed Play installation, uninstall the game or clear player data.

## Artifacts

- build/blockshift-internal-0.5.3.aab — 27,978,665 bytes, SHA-256 878add9f5fe5b0d013a48d1bd88a230155e27b7c9e4ab9cf050a0a937578c528.
- build/blockshift-internal-0.5.3.apk — release QA artifact signed with the existing dedicated upload key. Hashes: store/internal-testing/checksums-0.5.3.txt.
- Signing credentials and player backups remain private outside the repository. No tester email was sent.
