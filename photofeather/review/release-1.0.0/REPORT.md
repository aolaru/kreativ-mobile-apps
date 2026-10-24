# PhotoFeather 1.0.0 — free release candidate

October 22, 2026 · versionName **1.0.0**, versionCode **3**.

The four requested free-version improvements are implemented. This is a locally validated release candidate. Nothing has been published, submitted or purchased. The name/package remain provisional, and the production artifacts are unsigned pending the owner's signing setup.

## What changed

1. **Clear failed-target results.** A failed target leads with “Target not reached,” requested maximum and the actual measured final attempt. It explicitly says the attempt cannot be saved/shared and offers Change target. Failed results do not display a celebratory percentage-saved headline. It says final attempt, not smallest possible file, because the search is bounded.
2. **Shorter workflow.** Separate Settings and Results views, fixed Preview/Save/Share actions, expandable result details and shorter explanations. Large-text/short windows use Export selected and a scrollable Save/Share chooser with the metadata notice. Settings/Results choice, details, drafts, scroll and focus survive activity recreation.
3. **Per-photo controls.** Remove one photo, retry one or only failed photos, and select successful outputs for export. Retry snapshots IDs and preserves healthy previews/saved status. Export selection is persisted; folder requests retain the chosen IDs across recreation. Picker results wait for session restoration or an active job instead of being silently dropped. Removal commits the journal before pruning the private import; originals, saved destinations and recent shared outputs are retained.
4. **Broader release validation.** Android 9/API 28 emulator at 720 × 1280 and Samsung Galaxy A55/Android 16/API 36, including the actual Android 9 system folder picker, 200% text, landscape, dark mode and TalkBack-enabled accessibility actions. Non-debuggable QA release APKs run on both environments, including real MediaStore/folder exports and destination verification.

Provider fault testing also found and fixed a stale pending-record problem after confirmed export rollback. The journal now clears a confirmed deletion; an in-memory rollback confirmation survives a failed journal write until it can be retried. Completed-copy fingerprint recovery remains intact.

All features remain free. No HEIC expansion, billing, ads, analytics, backend, network permission or changes to other Made by Kreativ apps were introduced.

## Automated results

**76 distinct automated cases pass across the combined suites.** Android-specific cases are selected by SDK: one gallery-pending case runs on Android 16; one system folder-picker case runs on Android 9. Each platform therefore runs 40 of the 41 app cases. Both modules pass debug/release lint with zero errors; dependency-update advisories remain.

| Suite | Android 9 | Android 16 / host | Evidence |
|---|---:|---:|---|
| Compression/resize/metadata policy | — | 12/12 host | [XML](test-results/TEST-com.kreativ.imageengine.PolicyTest.xml) |
| JPEG structure | — | 6/6 host | [XML](test-results/TEST-com.kreativ.imageengine.JpegInputTest.xml) |
| Image engine | 14/14 | 14/14 | [Android 9](android9-engine-tests.txt), [phone](phone-engine-tests.txt) |
| App workflows and regressions | 40/40 | 40/40 | [Android 9](android9-app-tests.txt), [phone](phone-app-tests.txt) |
| Non-debuggable signed QA release | 2/2 | 2/2 | [Android 9](android9-release-smoke.txt), [phone](phone-release-smoke.txt) |
| TalkBack-enabled action smoke | — | 1/1 | [Log](talkback-workflow.txt) |

The final compact-export/folder-restoration checks are also recorded in [Android 9 targeted validation](android9-compact-export.txt). They verify that Save and Share can be scrolled into reach at 200% text in landscape. The full device suites cover the four earlier bugs, new selective operations, failed targets, cancellation, provider failure, journal failure, rotation, large text, system selection/sharing and original-file preservation. The engine suite covers orientation, large images, transparency, corrupt input, measured target bytes, metadata and injected low storage. Low-storage failures are simulated; the user's device was not filled.

TalkBack remained enabled during its action smoke test. The test focused and activated controls through Android accessibility APIs, compared images, saved, and opened the system share sheet without sending. The phone's prior accessibility settings were [restored and verified](accessibility-settings-restored.txt). This does not substitute for a human listening to speech output or testing swipe gestures end to end.

Builds: [QA release](qa-release-build.log), [final debug/unsigned release](final-build.log). Lint: [app debug](test-results/app-lint-debug.txt), [app release](test-results/app-lint-release.txt), [engine debug](test-results/image-engine-lint-debug.txt), [engine release](test-results/image-engine-lint-release.txt).

## Artifacts

- [Installable debug APK](PhotoFeather-1.0.0-debug.apk): `com.kreativ.photofeather.debug`, version 1.0.0. [Signature](debug-signature.txt).
- [Installable non-debuggable QA release APK](PhotoFeather-1.0.0-release-QA.apk): `com.kreativ.photofeather.releasecheck`, signed with the debug certificate for local validation only. It contains no debug fixture activities/provider. [Signature](qa-signature.txt).
- [Unsigned production APK](PhotoFeather-1.0.0-unsigned.apk) and [unsigned production AAB](PhotoFeather-1.0.0-unsigned.aab): provisional `com.kreativ.photofeather`; require production signing before distribution.
- [SHA-256 checksums](SHA256SUMS.txt).
- [Upgrade/session preservation](upgrade-check.txt) and [real cold-restart check](cold-restart-check.txt).

## Running-app screenshots

Synthetic fixtures only; these are captured from the app on the device/emulator, not mockups.

[Settings](screenshots/03-settings.png) · [Results](screenshots/04-result.png) · [Failed target](screenshots/13-target-not-reached.png) · [Dark mode](screenshots/08-dark-home.png) · [200% text](screenshots/14-large-text-results.png) · [Android 9 folder save](android9-screenshots/15-android9-folder-saved.png) · [Android 9 landscape export chooser](android9-screenshots/16-large-text-export-choice.png) · [TalkBack comparison focus](accessibility-screenshots/02-comparison-focused.png).

## Remaining release gates and limits

- **Production signing:** no PhotoFeather production key was supplied or created. Configure the four external PHOTOFEATHER_* signing variables documented in the project README, then rebuild/sign. The QA APK's debug certificate is not a store upload key.
- **Name/package confirmation:** exact-name indexed searches did not establish availability. Owner confirmation and Play Console/package/name validation remain outstanding. Nothing was reserved or submitted.
- **Human accessibility sign-off:** the automated TalkBack-enabled action smoke passed; a human speech/listening and swipe-navigation pass remains.
- **Coverage:** Android 8/API 26, Android 10–15, other OEMs, real cloud-provider downloads, physically exhausted storage and the full range of assistive technologies were not validated here. Android 9 and 16 cover the two export implementations; this is not an every-device guarantee.
- The engine remains JPEG/PNG/static-WebP input and lossy JPEG output, with an eight-megapixel decode cap and bounded attempts. Interrupted jobs retain completed previews and require explicit retry; this is not a durable background queue. Metadata removal does not anonymise visible content.

## Reproduce

JDK 17 and SDK 36; set ANDROID_SERIAL when several devices are connected.

```sh
./gradlew :image-engine:testDebugUnitTest :image-engine:connectedDebugAndroidTest :app:lintDebug :image-engine:lintDebug
ANDROID_SERIAL=<device-id> bash scripts/test-device.sh
ANDROID_SERIAL=<device-id> bash scripts/test-release.sh
# Opt-in: enable TalkBack first; this script does not change device accessibility settings.
ANDROID_SERIAL=<device-id> bash scripts/test-accessibility.sh
```

Relevant platform guidance: [accessibility testing](https://developer.android.com/guide/topics/ui/accessibility/testing), [Storage Access Framework](https://developer.android.com/training/data-storage/shared/documents-files), [app signing](https://developer.android.com/studio/publish/app-signing).
