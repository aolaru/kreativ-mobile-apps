# PhotoFeather 0.1.1 — four-bug fix validation

> Historical fix report. For the latest free-version release work, see [1.0.0 validation](release-1.0.0/REPORT.md).

**All four audited bugs are fixed.** Version 0.1.1 / code 2 was built, signed, installed over the existing debug app and launched on the connected Samsung Galaxy A55 / Android 16. Existing selected-photo IDs were preserved across the update: [upgrade check](fix-session-preservation.txt).

## Changes

1. **Safe export recovery.** The journal now records the owning item and the prepared JPEG's SHA-256 fingerprint. Recovery verifies the destination: completed matching copies are kept/adopted, incomplete unpublished gallery rows are cleaned up, and published or inaccessible copies are retained when verification is uncertain. A failed journal commit is reported without deleting the saved photo. Recovery is idempotent, and older URI-only journals migrate using the neutral filename and prepared bytes.
2. **Reject truncated JPEGs.** Input codestream completeness is checked before a JPEG is accepted, including all scans and the end marker. Valid progressive JPEGs remain supported. The check detects structural truncation; it does not prove authenticity or guarantee detection of every possible damaged entropy stream.
3. **Preserve previous previews on retry.** Results are replaced per image only after a replacement succeeds. Cancellation, processing failure and an unreachable replacement target preserve a previous usable preview, its saved status and its original settings. Mixed batches retain each result's settings. Exporting results that differ from the current controls requires explicit confirmation to export the shown copies.
4. **Preserve screen position.** Settings changes, comparison return and rotation preserve scroll and editor focus/selection. Completing a preview brings its results into view.

No Pro features, billing, backend, network permission, ads or analytics were added. Other Made by Kreativ apps were not changed.

## Results

**59 automated tests pass; none skipped. Both modules pass lint with zero errors.**

| Suite | Result | Evidence |
|---|---:|---|
| Compression/resize/metadata policy | 12/12 | [XML](fix-test-results/policy-tests.xml) |
| JPEG input structure | 6/6 | [XML](fix-test-results/jpeg-input-tests.xml) |
| Android image engine | 14/14 | [XML](fix-test-results/engine-device-tests.xml) |
| Android workflows and regressions | 27/27 | [Full suite: 26 tests](fix-device-tests.txt), [supplemental journal-error test](fix-journal-error-test.txt) |
| App / engine lint | 0 errors | [App](fix-test-results/app-lint.txt), [engine](fix-test-results/engine-lint.txt) |

The eleven new app regressions cover journal-commit failure followed by repeated recovery, accurate reporting when a journal failure prevents saving, incomplete pending-gallery cleanup, truncated JPEG rejection, cancelled/failed/unreachable retries, saved-status preservation, mixed-result settings across recovery, scroll/focus preservation, comparison return, rotation and automatic scrolling to completed previews. A real progressive-JPEG fixture is tested for both successful processing and rejection when truncated. The app suite passed 26 tests together; the additional journal-error case passed separately after the final error-message refinement.

Fault injection simulates journal I/O failure; the user's device was not filled. App tests ran under the separate `.fixverify` package, so they did not clear the user's `.debug` session. The ordinary device-test script now uses a separate `.verify` package as well.

[Build and verification log](fix-verification-build.log) · [Final distributable build](fix-final-build.log) · [Signature verification](apk-verification.txt) · [APK checksum](apk-sha256.txt) · [Installable APK](PhotoFeather-debug.apk)

## Running-app screenshots

Captured from the isolated test package running the same 0.1.1 production code, with generated synthetic images:

[Home](fix-screenshots/01-home.png) · [Settings](fix-screenshots/03-settings.png) · [Measured result and stored settings](fix-screenshots/04-result.png) · [Comparison](fix-screenshots/05-comparison-output.png) · [Dark mode](fix-screenshots/08-dark-home.png) · [Large text](fix-screenshots/09-large-text.png) · [Landscape controls](fix-screenshots/12-landscape-zoom-controls.png)

The original [audit and failing-case evidence](audit/README.md) is retained as historical evidence. Older Android/OEM/provider runtime coverage and a complete manual TalkBack audit remain separate validation gaps. Nothing was published.
