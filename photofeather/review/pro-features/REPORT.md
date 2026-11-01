# PhotoFeather — initial Pro capabilities

2 November 2026. The features are available locally without billing, subscriptions, accounts or a paywall. Existing free compression, resize and batch functionality remains available.

## Implemented

- Named presets save, apply and delete the byte target, resize edge, quality, background and mode in private app preferences. Presets survive session clearing; the app excludes its private data from backup.
- Batch naming supports a prefix, starting number and prefix-first or number-first templates. A unique identifier prevents collisions, and version outputs include their requested edge. Names apply to saved JPEGs, shared JPEG display names and ZIP entries. Original source filenames are not used.
- Selected prepared photos can produce separate 1280, 1920 and 2560 px versions. All versions use the current byte target and quality, respect the no-upscaling and larger-copy safeguards and report measured results individually. Sources are shared private imports; cleanup retains an import while any result references it. The existing 100-item session limit includes versions. Cancellation retains completed results; queued work interrupted by process death becomes retryable with current settings.
- ZIP sharing verifies selected JPEGs, checks available space, creates the archive on the session worker, checks every archived entry against a SHA-256 fingerprint, and deletes an incomplete archive after cancellation or failure. Android's share sheet receives a read-only URI. Temporary archives follow the existing cleanup policy. There is no separate Save ZIP picker.
- Fixed a render race: busy state and item availability are snapshotted when constructing the screen, preventing the action dock from becoming null when a worker finishes during rendering.

The in-app privacy policy and the prepared HTML draft now describe locally saved presets, temporary ZIPs and user-chosen filename prefixes. The public website still describes the earlier build and **must be updated before releasing these features**.

## Build and tests

- Debug app/test APKs, unsigned R8 production APK/AAB, isolated non-debuggable R8 QA release and instrumented QA APK all build successfully. The installable local debug APK is at `build/pro-checks/PhotoFeather-debug.apk`; its application ID is `com.kreativ.photofeather.debug`.
- **26 engine JVM tests passed.** Release lint has **zero errors**; the app has 9 and the engine 2 advisory warnings.
- On a Samsung Galaxy A55 / Android 16, **51 device tests passed**, including the four new Pro tests. After that full run, the numbering logic was tightened to count selected outputs and the ZIP share name was made readable; the final focused four-test run passed against those latest changes.
- The focused tests cover preset save/apply, both naming templates, shared URI display name, a named MediaStore export and byte-for-byte reopening, three differently sized results, no upscaling, shared-import retention and original-import fingerprint, version cancellation, ZIP content/fingerprint, ZIP cancellation and simulated low storage. They do not constitute a human TalkBack pass or cover every provider/device.
- **3 R8 QA release smoke tests passed** for existing import/resize/save/share, permissions and offline policy. The instrumented QA build uses additional QA-only keep rules so AndroidX Test and direct test calls can link across APKs. These rules do not apply to production. A separate production-like R8 QA APK without those rules successfully cold-started on the same device; the four new workflows have not been exercised on that production-like APK.
- During testing an initial full run exposed a busy/ready render crash, fixed before the passing full run. A large-text regression test also failed once and passed on isolated retry and the subsequent full runs. Initial R8 test-runner linking failures were addressed with the QA-only rules; the production R8 configuration retains optimisation and has no such QA keeps.
- The unsigned production bundle contains the matching R8 mapping, the updated offline policy and no Android network/storage permissions. The previously submitted signed build 4 APK and AAB hashes remain unchanged. See `artifact-verification.json`.

The device logs and build logs in this directory record the runs. No new Play upload or release was made. The source still carries versionCode 4 for local validation; use a higher code for a future Play release. Update the public privacy policy and perform broader device/provider and accessibility checks before publishing the new functionality.
