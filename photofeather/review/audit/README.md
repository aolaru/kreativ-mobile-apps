# PhotoFeather follow-up review — October 22, 2026

**Historical audit: these four bugs were fixed in 0.1.1. See [the fix report and passing regressions](../FIXES.md).** The evidence below records the original failures; the passing MVP suite had not covered them.

Review used an isolated package, `com.kreativ.photofeather.review`, on the connected Samsung Galaxy A55 / Android 16. Its production Java/resources are identical to the delivered project; only the application ID suffix and added review tests differ. No production implementation was changed. The original debug app and its session were left intact.

## 1. [P1] A successful saved copy can be deleted after a journal-write failure

**Trigger:** A JPEG is saved, verified and published to the gallery, but the subsequent local session-state write fails. This was reproduced by injecting `IOException` into `AtomicFile.startWrite()` after the pending destination had been durably recorded; the device was not filled with data.

**Observed:** The destination opened successfully and the item had a saved URI. After recovery from the last durable journal, the destination no longer existed. The original input remains untouched.

**Cause:** `Session.save()` clears `pending` and ignores the result of `persist()`. If that write fails, the on-disk journal still lists the completed URI as pending. `Session.restore()` unconditionally rolls it back on the next startup, even though `ExportManager` already published it. UI can show “Saved and verified” based on the in-memory saved URI.

**Locations:** [Session.java](../../app/src/main/java/com/kreativ/photofeather/Session.java), lines 408–419 and 124–127; [ExportManager.java](../../app/src/main/java/com/kreativ/photofeather/ExportManager.java), lines 76–82.

**Required fix:** Separate pending and committed exports durably. Recovery must distinguish a verified/published destination from an incomplete write. Handle failed commit persistence explicitly; do not delete a completed copy merely because the journal is stale.

## 2. [P2] Truncated JPEGs are silently accepted as successful photos

**Trigger:** A valid 506,607-byte JPEG was truncated to 253,303 bytes, then selected and processed normally.

**Observed:** The app reported **Ready**, produced a 49,290-byte JPEG, and allowed export. The output contains the upper portion of the image and a solid black lower portion. [Actual accepted output](review-evidence/accepted-output.jpg).

**Cause:** Input validation checks bounds and whether Android returns a bitmap. Android can recover a bitmap from an incomplete JPEG without returning null. Validating the newly encoded output then verifies the recovered/black pixels, without detecting that the source was damaged.

**Location:** [ImageEngine.java](../../image-engine/src/main/java/com/kreativ/imageengine/ImageEngine.java), lines 141–146; input inspection at lines 75–104.

**Required fix:** Add input integrity validation that supports the declared input formats. Reject damaged/truncated input or clearly flag a recovered partial image instead of presenting it as an ordinary successful result.

## 3. [P2] Cancelling a new preview loses access to the previous successful previews

**Trigger:** Generate a successful preview, start another preview with a different target, then cancel immediately.

**Observed:** The previous JPEG still existed privately, but the item's output path was emptied and it was no longer available to save or share. The current result became Cancelled. Saved gallery photos and originals were not deleted.

**Cause:** Every existing output reference and saved status is cleared before any replacement encoding succeeds.

**Location:** [Session.java](../../app/src/main/java/com/kreativ/photofeather/Session.java), lines 292–300.

**Required fix:** Stage replacement results separately and swap them into the session only after successful processing. Preserve the previous result and its settings when the replacement is cancelled or fails.

## 4. [P2] Changing settings resets the screen to the top

**Trigger:** Scroll to settings and open Advanced JPEG quality.

**Observed:** Scroll position changed from **1,000 px to 0 px**. Users must find their place again. The same whole-screen rendering pattern is used for presets and other controls, and when returning to the main activity.

**Cause:** Each render creates a fresh ScrollView and replaces the activity's content view; scroll/focus state is not restored. The old UI tests invoked buttons programmatically, so they did not catch this usability problem.

**Locations:** [MainActivity.java](../../app/src/main/java/com/kreativ/photofeather/MainActivity.java), lines 86–100 and 356–365; [Ui.java](../../app/src/main/java/com/kreativ/photofeather/Ui.java), page construction.

**Required fix:** Update controls/results in place, or explicitly preserve scroll and focus. A completed preview should bring the results into view.

## Evidence and test method

- [Four regression probes and their failing assertions](regression-probes.txt)
- [Observed values from the device](probe-observations.txt)
- [Reproduction test source](ReviewProbeTest.java), stored as review evidence, not wired into the product's test source set
- [Build/lint log](build-and-lint.log)
- [Running app showing the accepted damaged image](truncated-comparison.png)
- [Damaged input](review-evidence/truncated-input.jpg), [accepted output](review-evidence/accepted-output.jpg)

The exported-copy failure was simulated at the journal I/O boundary. The other three used the actual app/session/engine paths with synthetic fixtures. The app's production source was not patched to manufacture failures. None of these tests sent an image to another person.

Older Android versions, the older folder-export path and a full manual TalkBack audit remain validation gaps, separate from these confirmed bugs.

## Baseline recheck

The 12 existing JVM tests and 13 engine device tests passed again. The app baseline passed 15/16 initially; the share-sheet test encountered the sleeping/locked phone (`com.android.systemui`). After waking and dismissing the keyguard, that share-sheet test passed on rerun. In the same awake rerun, the truncated-JPEG regression still failed and was captured in the running app. This separates the device-lock test-environment failure from the four reproducible app bugs.

[Unit baseline](unit-baseline.xml) · [Engine baseline](engine-baseline.log) · [App baseline](app-baseline.txt) · [Awake recheck](awake-recheck.txt) · [Unchanged production-source checksums](source-checksums.txt)
