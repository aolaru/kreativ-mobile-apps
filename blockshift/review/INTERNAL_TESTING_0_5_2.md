# BLOCKSHIFT 0.5.2 / code 9 — 7 November 2026

**0.5.2 (9) is active on Google Play internal testing**, released 7 November at 21:24 Europe/Bucharest. The original two-person BLOCKSHIFT tester list remains the only selected list. The phone received an in-place Play update to non-debuggable 0.5.2 / code 9, installer com.android.vending. Fresh progress was backed up and verified first; no uninstall or reset was performed. Hardware listening, cold-restart preference persistence and final progress comparison are in progress. The local-build notes below describe preparation before this rollout.

## Implemented

- Settings → Sound offers Rain, Forest, Ocean and Off, with a text check mark for the selected sound, a description, selected-sound previews and independent ambience/SFX volume controls.
- Selection is stored immediately through the existing save path; volume retains its checkpoint behavior. Off preserves the last volume. Previews respect mute, Off and background focus.
- Two bounded ambience voices provide brief fade transitions. Rapid changes cancel previous transitions safely, and both voices pause when backgrounded. No audio synthesis or asset downloads run in the game.
- Three original 48-second, 32 kHz stereo Vorbis soundscapes are embedded offline (about 1.8 MB combined). They are procedurally authored nature-style audio, not field recordings. Provenance and authoring dependencies are documented in assets/audio/README.md. The legacy tonal pad is removed.
- The additive ambience_sound preference defaults to Rain for older saves while retaining their existing volume. Save version 3, package ID, boards, scores, daily rules/history and gameplay remain unchanged. No SDKs or permissions were added.

## Validation

- Full tools/test.sh --render: **7,334 assertions, zero failures** — 7,132 core, 50 transfer, 91 native UI, 13 transfer UI, 31 reliability and 17 graphics; 80 seeded games / 1,285 placements. Includes all sound selections, previews, persistence without board changes, Off, mute and rapid switching/background transitions. Log: logs/0_5_2-full-tests.log.
- Decoded all three shipped audio files with ffmpeg; level, clipping, quiet-gap, duration, format and loop-boundary checks passed. Levels: Rain −20.6, Forest −21.6 and Ocean −19.8 dBFS RMS.
- Loaded a disposable copy of the user's verified backup through actual save/session services, cycled all four ambience preferences and reloaded them. All three saved modes, statistics, daily history, last mode and volume were preserved; the original backup's bytes remained unchanged. Log: logs/0_5_2-private-backup-verification.log. Private data and signing credentials remain outside the repository.
- Visually inspected the rendered Sound settings screen: review/0_5_2-settings-sound.png. Build and test resources remain excluded from Android exports.
- Signed AAB built successfully with the existing dedicated upload key; JAR signature and certificate identity verified. Official bundletool validation passed. A release QA APK was generated from that exact AAB and passed signature and 16 KB ZIP/ELF alignment checks.
- Inspected both archives: version 0.5.2 / code 9, com.madebykreativ.blockshift, ARM64, minimum API 26 / target API 36, non-debuggable, VIBRATE only. AAB backup is disabled. Logs: logs/0_5_2-bundle-inspection.json and logs/0_5_2-apk-inspection.json.
- Extracted the actual exported Vorbis resources from the AAB and loaded them with Godot: **9 additional assertions, zero failures** for stream type, enabled looping and 48-second duration. All three soundscapes and their manifest are packaged; the old pad is absent. Logs: logs/0_5_2-packaged-audio.log and logs/0_5_2-audio-package-inspection.json.

## Artifacts and next check

- build/blockshift-internal-0.5.2.aab — 28,011,135 bytes; SHA-256 d274be5243eebd819dfe6170e590137af928635eb4a0a48a70c8b373a68b335f.
- build/blockshift-internal-0.5.2.apk — release QA artifact signed with the upload key. It uses a different certificate from the installed Play app and is not an in-place update for that installation. Do not remove the Play app or clear player data to test it.
- store/internal-testing/checksums-0.5.2.txt records both artifact hashes; release-notes.txt is prepared for this version.

Next acceptance check is Play delivery of this candidate and listening on phone speakers/headphones. This turn did not perform an Android installation, a physical audio check, 16 KB runtime testing or public publication. Numerical audio tests and rendered UI tests do not certify hardware loudness, long-term listening comfort, battery life or broad device compatibility.


## Actual Play phone check and listening feedback

The user completed a fresh native export before the in-place update. The file was saved under Android's default name with suffix (1), independently checksum-verified, copied privately to the Mac and verified through the actual save/session services on a disposable copy. All three saved modes were intact; the original backup was unchanged.

0.5.2 (9) was published on the internal track at 21:24 Europe/Bucharest, with the two original tester accounts selected and no other lists. Play reported no blocking errors and the same two nonblocking mapping/native-symbol warnings. It was installed through the phone's Play Store Update button: installer com.android.vending, non-debuggable, code 9. No uninstall, reset or restore was performed.

The user confirmed Rain, Forest, Ocean and Off worked. A subsequent quality report said Forest's white-noise bed was too prominent; the resulting audio correction is version 0.5.3 (10). Forest at 15% and SFX at 50% were visually confirmed before and after a force-stop/cold launch. The initial saved mute setting had also been preserved on upgrade, with correct muted-preview feedback. No matched script/crash errors appeared in the captured process log.

A final native export comparison remains pending: Samsung's game touch-lock covered the picker and then USB disconnected before the export was saved. The verified pre-update backup remains private. The last observed phone build is 0.5.2; see INTERNAL_TESTING_0_5_3.md for the correction's status. Do not claim a completed post-update save comparison yet.
