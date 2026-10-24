# PhotoFeather MVP — October 22, 2026

> **Latest work — 1.0.0:** The top four free-version improvements are implemented. See [1.0.0 validation and release limitations](release-1.0.0/REPORT.md). The 0.1.x history below is retained as historical evidence.

> **Current status — 0.1.1:** All four follow-up audit bugs are fixed. **59 tests pass**, both modules pass lint, and the updated debug APK has been installed with the existing session preserved. See [fix details, screenshots and validation](FIXES.md). The initial MVP history below is retained for context.

Built as the separate `photofeather/` Android Studio/Gradle project. Java 17/native Android views follow the existing workspace conventions. Existing apps and their local changes were left untouched. No store publication, listing, backend, billing or paid-service commitment was made.

## Delivered

- [Installable, signed debug APK](PhotoFeather-debug.apk), version **0.1.0**, package `com.kreativ.photofeather.debug`, approximately 2.3 MB. Installed and run on the connected Samsung Galaxy A55 / Android 16 (API 36).
- System photo selection, incoming image shares, multi-photo imports, explicit unsupported/unreadable-file errors and no broad storage access.
- File size presets/custom target, longest-edge resizing, resize-only mode and advanced JPEG quality. Measured bytes, dimensions and percentage savings; zoomable comparison; honest unsuccessful-target and no-benefit results.
- New-copy gallery/folder export, destination byte verification, Android share sheet, per-image batch outcomes, cancellation, partial failures and recovery after interruption.
- JPEG/PNG/static WebP inputs; JPEG output; transparency/background confirmation, all eight EXIF orientations and verified metadata stripping. No anonymisation or lossless claims.
- Separate reusable image-engine module, bounded memory/encoding, storage checks, atomic state/export journal and private-file cleanup. No internet permission, advertising or analytics SDKs.
- Light/dark views, 200% text layouts, accessible labels/selection states, non-gesture zoom controls and landscape scrolling.

## Validation

**41 tests pass; none skipped.**

| Suite | Result | Evidence |
|---|---:|---|
| Pure-Java policy/metadata tests | 12/12 | [JUnit XML](test-results/unit-tests.xml) |
| Android image-engine tests | 13/13 | [Device JUnit XML](test-results/engine-device-tests.xml) |
| Android app workflows | 16/16 | [Instrumentation output](device-workflows.txt) |
| App / engine lint | 0 errors | [App](test-results/app-lint.txt), [engine](test-results/engine-lint.txt) |

Coverage includes portrait/landscape, independent corner checks for all eight EXIF orientations, a 24-megapixel input, transparent PNG/background consent, already-small/no-benefit files, an impossible target, corrupt input, animated PNG rejection, static WebP, exact encoded byte counts, unchanged source hashes, reopened outputs and removed EXIF/GPS/camera/comment metadata. App tests cover failed batch members, cancelled batch results, low storage via injection, cancelled/pending-export rollback, saved bytes, incoming shares, native picker launch/cancel, native share-sheet launch without sending, rotation, recovery, dark mode and large text/landscape controls.

An additional real force-stop and cold restart restored both selected photos and results: [launch evidence](process-restart.txt), [UI hierarchy](restart-ui.xml), [screenshot](screenshots/13-recovered-session.png). The APK passes [signature verification](apk-verification.txt); its [SHA-256](apk-sha256.txt) is recorded. Lint's remaining advisories concern newer dependency versions; the project deliberately reuses the workspace's tested versions.

## Genuine screenshots

Captured from the running app using Android instrumentation/screencap, without mockups or compositing. Image content is a deterministic synthetic mountain fixture, not a personal photo or a claim about typical savings.

[Home](screenshots/01-home.png) · [Selection](screenshots/02-selected.png) · [Settings](screenshots/03-settings.png) · [Measured result](screenshots/04-result.png) · [Output comparison](screenshots/05-comparison-output.png) · [Original comparison](screenshots/06-comparison-original.png) · [Zoom](screenshots/07-comparison-zoom.png) · [Dark](screenshots/08-dark-home.png) · [200% text](screenshots/09-large-text.png) · [Large settings](screenshots/10-large-text-settings.png) · [Landscape](screenshots/11-landscape-large-comparison.png) · [Landscape controls](screenshots/12-landscape-zoom-controls.png)

The sample produced **187,357 bytes** from **2,198,921 bytes** at the 200,000-byte target. Actual photo results vary.

## Remaining issues and coverage

At the end of the initial MVP run, the existing automated suite passed. The subsequent audit above supersedes the earlier assessment of known issues. Android 8–12 runtime behaviour, the Android 8–9 folder-save path, broader OEM/cloud-provider selection and a complete manual TalkBack audit remain unverified. Low-storage failure paths were simulated; the user's device was not filled. The decoder/codec and provider calls are cooperatively cancellable between operations, not instantly interruptible.

HEIC/AVIF/GIF/animations are outside v1. Large images may be reduced to eight megapixels; comparisons decode to at most 2,048 px. Jobs interrupted by process death require an explicit retry. Provider refusal can prevent deletion of its own incomplete export. See [technical notes](../docs/TECHNICAL_NOTES.md) for complete limits and cleanup behaviour.

The name remains **provisional**: initial indexed searches found no exact compressor match, but Play Console/package validation and fuller name checks remain open. Availability is not claimed.

## Free / Pro suggestion — not implemented

Keep the entire delivered MVP free, including basic batches, custom size targets, metadata removal and comparison. A future optional Pro edition could add named saved presets, export profiles and advanced batch organisation. This is a product suggestion only: no feature gate, billing code, product, price or subscription has been created.
