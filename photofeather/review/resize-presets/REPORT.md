# Free resize presets — implementation and validation

October 24, 2026 · PhotoFeather **1.0.0 / build 4**.

Implemented Small (**1280 px**), Medium (**1920 px**), Large (**2560 px**) and Custom (**64–8192 whole pixels**) longest-edge controls in the free version. The active choice has a selected state and screen-reader label. Users can type directly into the pixel field; this selects Custom and updates every selected photo's planned width × height immediately. Invalid inputs show an error, suppress the dimensions and disable Preview copies.

The preview preserves aspect ratio, does not enlarge smaller inputs, labels photos already within the limit and explains reductions caused by safe decoding. The Custom mode/value persist across recreation and disk recovery. Resizing can run alone or with a maximum file-size target. Prior prepared copies retain their original settings, and changed resize controls still trigger the existing explicit export review.

## Planned dimensions and measured results

Before encoding, dimensions are labelled **up to** bounds. `ResizePolicy.previewSize` uses the engine's existing eight-megapixel decode policy and bounds both floor/ceiling sampling-rounding possibilities, including oriented photos. A maximum file-size target may shrink the output further. Results continue to report the actual encoded dimensions and bytes, rather than claiming the planned dimensions or file-size targets were achieved.

The image decoder, JPEG compression algorithm, metadata removal, supported formats and export policies were not changed. No Pro gate, billing, backend, ads or analytics was added.

## Fresh verification

**95 automated cases pass in this run**, plus a repeat of the seven resize UI cases for visual capture. These are fresh results on the connected Samsung Galaxy A55 / Android 16, and host JVM tests. The Android 9-only system-folder-picker case is excluded by its SDK annotation; earlier Android 9 evidence remains in the historical [1.0.0 report](../release-1.0.0/REPORT.md) and is not presented as a resize rerun.

| Suite | Passed | Evidence |
|---|---:|---|
| Compression/metadata policy | 12 | [XML](test-results/TEST-com.kreativ.imageengine.PolicyTest.xml) |
| JPEG input structure | 6 | [XML](test-results/TEST-com.kreativ.imageengine.JpegInputTest.xml) |
| New resize preview/input logic | 8 | [XML](test-results/TEST-com.kreativ.imageengine.ResizePreviewTest.xml) |
| Android image engine | 19 | [Log](engine-device-tests.txt) |
| App workflows/regressions, including seven new resize cases | 47 | [Log](app-device-tests.txt) |
| Non-debuggable signed QA release | 2 | [Log](release-smoke-tests.txt) |
| TalkBack-enabled action smoke | 1 | [Log](talkback-tests.txt) |

The **20 new cases** cover preset geometry, portrait/landscape, smaller inputs, large-memory limits, odd PNG/WebP sampling, custom limits/invalid inputs, real output dimensions, original-byte preservation, selection persistence, earlier-preview export protection and 200% text/landscape layouts. The updated release smoke test uses preset/custom controls, processes an **800 × 600** JPEG and verifies saved bytes. The TalkBack-enabled smoke selects Medium, enters Custom **800 px** through accessibility APIs, processes, compares, saves and opens the share sheet without sending.

Both app and engine pass debug/release lint with **zero errors**; advisory warnings remain. [Verification build](verification-build.log) · [QA release build](release-build.log) · [Final build](final-build.log) · [App debug lint](test-results/app-lint-debug.txt) · [App release lint](test-results/app-lint-release.txt) · [Engine debug lint](test-results/image-engine-lint-debug.txt) · [Engine release lint](test-results/image-engine-lint-release.txt).

## Builds and installation

- [Installable debug APK](PhotoFeather-1.0.0-resize-debug.apk): `.debug`, versionCode 4. [Signature](debug-signature.txt).
- [Non-debuggable QA release APK](PhotoFeather-1.0.0-resize-release-QA.apk): `.releasecheck`, signed with the debug certificate for local validation. [Signature](qa-signature.txt).
- [Unsigned production APK](PhotoFeather-1.0.0-resize-unsigned.apk) and [unsigned production AAB](PhotoFeather-1.0.0-resize-unsigned.aab).
- [SHA-256 checksums](SHA256SUMS.txt).

The debug app was updated with `install -r` without clearing app data. [Upgrade check](upgrade-check.txt). Its prior session was empty; original-file preservation was verified with generated-fixture tests. Tests ran in an isolated `.resizeverify` package. Temporary app test/QA packages were removed, and the phone's [original accessibility settings were restored](accessibility-settings-restored.txt).

## Genuine running-app screenshots

[Presets and per-photo dimensions](screenshots/resize-01-presets.png) · [Custom-size preview](screenshots/resize-02-custom.png) · [Resize-only settings](screenshots/resize-03-before-processing.png) · [Measured output](screenshots/resize-04-measured-result.png) · [Large text](screenshots/resize-05-large-text.png) · [TalkBack resize controls](accessibility-screenshots/00-resize-control-focused.png).

Screenshots use generated synthetic fixtures. They are captures from the running app, not mockups.

## Remaining release limits

Production signing, final name/package confirmation and a human TalkBack speech/swipe pass remain the release gates documented in the earlier report. This iteration did not add HEIC support or revalidate older Android/OEM/cloud-provider combinations. The QA certificate is not a production signing key. No store submission, publication or paid-service commitment was made.
