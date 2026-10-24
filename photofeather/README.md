# PhotoFeather

**Lighter photos. Ready to share.** A native Android app by Made by Kreativ that compresses and resizes photos on-device. Play title: **PhotoFeather: Photo Compressor**. The owner has chosen the name. Play accepted the signed `com.kreativ.photofeather` bundle on 24 October 2026. Version 1.0.0 (build 4), the listing and declarations were submitted for production review on 24 October 2026, with publication authorised after Google approval. Play Console shows Changes in review and managed publishing off. Internal testing remains inactive; the app is not yet confirmed live. Version **1.0.0** (build 4) adds free resize presets and live dimension previews alongside the release improvements: clear failed-target results, separate Settings/Results views, fixed primary actions and per-photo controls. See [production submission](review/production-submission/REPORT.md), [production preparation](review/production-preparation/REPORT.md), [latest resize validation](review/resize-presets/REPORT.md), [signed build and listing preparation](docs/PLAY_RELEASE.md) and [1.0.0 release limitations](review/release-1.0.0/REPORT.md).

## Run

Requires **JDK 17**, Android SDK 36 and an Android **8.0/API 26+** device. Open this folder in Android Studio, or set `JAVA_HOME` to JDK 17 and `sdk.dir` in `local.properties`:

```sh
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.kreativ.photofeather.debug/com.kreativ.photofeather.MainActivity
```

A ready-to-install 1.0.0 build is at [review/resize-presets/PhotoFeather-1.0.0-resize-debug.apk](review/resize-presets/PhotoFeather-1.0.0-resize-debug.apk). The `.debug` package is separate from other apps.

## Workflow

Select photos → choose target/pixel dimensions → preview measured results → save new copies or share.

- System photo picker on Android 13+; system document-picker fallback on Android 8–12. Accepts images shared through granted content URIs. No broad photo/storage permission.
- **Small (1280 px), Medium (1920 px), Large (2560 px)** and **Custom (64–8192 px)** resize choices use the longest edge, preserving aspect ratio without enlargement. A live per-photo dimension preview accounts for safe decoding and shows when a photo is already within the limit or constrained by memory.
- **200 KB / 500 KB / 1 MB / custom 1–50,000 KB**, optional JPEG quality 10–100, and resize-only mode. KB = 1,000 bytes; MB = 1,000,000 bytes. Planned resize dimensions are upper bounds; a byte target may reduce them further. Results retain final measured dimensions and bytes.
- Exact output bytes/dimensions, percentage saved and zoomable original/output comparison. Larger or same-sized copies require explicit opt-in; unsuccessful targets cannot be exported. Cancelled, failed or unreachable retries keep the previous usable preview. Each result records its own settings; exporting older or mixed settings requires explicit review.
- Save new JPEGs to `Pictures/PhotoFeather` on Android 10+, or a chosen folder on Android 8–9. Save verifies destination bytes. Share through Android's share sheet.
- Separate **Settings / Results** views, expandable technical details and fixed Preview / Save / Share actions. Large-text and short landscape layouts use a compact Export choice with the same metadata disclosure.
- Remove individual photos, retry one or only failed photos, and select successful outputs to save/share. Export selection and pending folder-save selection survive recreation. Removal deletes only the private imported copy; saved photos and recent shared files stay untouched.
- Batch progress/cancellation, per-image failures, retained successes, rotation support and interrupted-session recovery.
- Dark mode, scalable text, labelled controls and scrollable comparison layouts.

## Architecture

`image-engine/` is a reusable Android library containing input inspection, orientation, resize policy, bounded JPEG encoding and metadata verification. It has no screen, export, branding or network dependencies. `app/Session` owns the serial background worker and atomic recovery journal; `ExportManager` owns new-file export and rollback. Activities own presentation only. The UI adapts existing Made by Kreativ conventions without modifying those apps.

## Formats and limitations

Inputs: **JPEG, PNG and static WebP**. Output: **lossy JPEG**. HEIC/AVIF/GIF and animations are rejected. PNG/WebP conversion asks for a background because JPEG has no transparency. All eight EXIF orientations are applied. JPEG codestream completeness is checked before accepting an input, including progressive JPEGs; truncated inputs are rejected.

Decoding is bounded to eight megapixels; large inputs may be reduced. Compression uses at most 35 attempts and stops at quality 40 (or the user's lower maximum) and a 320 px edge (or the smaller starting edge). It reports failure honestly; there is no lossless, optimal-compression or visual-quality guarantee. Preview decoding is limited to a 2,048 px edge. Maximum batch: 100 images; maximum input: 200 MB each.

Originals are never written or deleted. Fresh JPEGs have all APP/comment metadata segments stripped and verified before export; this does not anonymise photos or hide visible sensitive details. Working files are private and excluded from backups. Clear session removes imports; unused exports older than 24 hours are cleaned on a later launch. Completed previews recover after process death; interrupted jobs require a retry. Codec/provider calls finish before cancellation can take effect.

There is **no backend, internet permission, billing, ads or analytics SDK**. The system picker may retrieve a cloud-provider image at the user's request.

The full offline privacy policy is available from **About → Privacy policy**. The matching [public privacy policy](https://madebykreativ.com/privacy/photofeather/) is live, effective 24 October 2026, and saved in Play Console. The website source is maintained separately in `aolaru/madebykreativ.com`; `store-assets/play/privacy-policy.html` remains the standalone prepared source. Play listing copy, graphics and sourced screenshots are in `store-assets/play/`.

## Tests and handoff

```sh
./gradlew :image-engine:testDebugUnitTest :image-engine:connectedDebugAndroidTest :app:lintDebug :image-engine:lintDebug
bash scripts/test-device.sh
```

Device tests run under a separate `.verify` package and leave your `.debug` session intact. Set `ANDROID_SERIAL` when more than one device is connected.

```sh
ANDROID_SERIAL=<device-id> bash scripts/test-release.sh
```

The release smoke script builds the **non-debuggable `.releasecheck` QA variant**, signed with the debug certificate for local validation. It does not produce a production-signed store upload. A normal `:app:assembleRelease :app:bundleRelease` is unsigned unless all four external variables are supplied: `PHOTOFEATHER_KEYSTORE`, `PHOTOFEATHER_STORE_PASSWORD`, `PHOTOFEATHER_KEY_ALIAS`, `PHOTOFEATHER_KEY_PASSWORD`. Keep credentials and keys outside this repository. Nothing is uploaded by these commands.

See [completion report and screenshots](review/REPORT.md), [technical details](docs/TECHNICAL_NOTES.md), and the separately proposed free/Pro boundary in the report. Device tests generate synthetic fixtures and do not send photos to anyone.
