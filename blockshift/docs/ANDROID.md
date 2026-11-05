# Android build and export

Validated engine: **Godot 4.6.3 stable, standard GDScript**. App ID: `com.madebykreativ.blockshift`. Portrait. ARM64 only. Both Gradle APK and AAB presets use minimum API 26 and target API 36. This identifier is a proposed permanent choice; verify it and the working title before publication.

## Toolchain setup

1. Install Godot 4.6.3 and its **matching official export templates** from the editor's Export Template Manager.
2. Install a supported Java SDK (Godot recommends JDK 17; this environment uses Android Studio's bundled JBR) and the Android SDK. Set both paths in Godot → Editor Settings → Export → Android. Paths are machine-local and are not committed.
3. Install Android platform 36, build-tools 36.1.0, platform-tools and Android SDK command-line tools, and accept SDK licenses. The official engine template supplies Gradle and its prebuilt engine AAR; native recompilation is not required for this project.
4. Run the helper. It installs the official Android source ZIP into ignored `android/build/`, sets the matching version marker, and limits Gradle to 1.5 GB and two workers. Jetifier is disabled because the current dependencies are already AndroidX. Revisit that choice if adding an old native plugin. It also installs the versioned `tools/android_branding.gradle` task, which supplies a real navy color for Android’s native splash background before resource merging.

```sh
cd blockshift
export GODOT_BIN=/absolute/path/to/godot
python3 tools/prepare_android.py
./tools/android_build.sh apk
./tools/android_build.sh aab-dev
./tools/android_build.sh aab-release-check
./tools/android_build.sh aab-internal
```

On macOS a downloaded application's binary is `Godot.app/Contents/MacOS/Godot`. `prepare_android.py` requires Python 3.9 or newer. The generated Android build is ignored by Git and can also be installed through Godot's Project → Install Android Build Template menu. The helper does not overwrite native source changes in an existing matching template; it updates only its documented Gradle properties.

Outputs:

| Output | Purpose | Signing |
| --- | --- | --- |
| `build/blockshift-debug.apk` | Sideloadable development build | Local debug key |
| `build/blockshift-development.aab` | Development bundle, not a store release | Unsigned by the current exporter |
| `build/blockshift-release-unsigned.aab` | Release variant and packaging validation | Deliberately unsigned |
| `build/blockshift-internal-0.5.0.aab` | Internal-track upload candidate, version code 7 | Dedicated non-debug upload key |
| `build/blockshift-internal-0.5.0.apk` | QA APK generated from that exact bundle | Dedicated upload key; Play delivery uses its app signing key |

The last bundle exercises release compilation and strips debug flags; **it cannot be submitted as a signed production release**. The helper rejects logged Godot/Gradle errors even when the engine happens to exit with status 0. Build results and emulator coverage are recorded in `review/VALIDATION.md`.

## Install and inspect

```sh
adb install -r build/blockshift-debug.apk
adb shell monkey -p com.madebykreativ.blockshift -c android.intent.category.LAUNCHER 1
adb logcat -s godot AndroidRuntime
# Run from the installed SDK build-tools directory or use absolute paths.
aapt2 dump badging build/blockshift-debug.apk
apksigner verify --verbose build/blockshift-debug.apk
zipalign -c -P 16 -v 4 build/blockshift-debug.apk
```

Check actual output for API 36, ARM64 only, portrait, backup disabled and only the optional haptic VIBRATE permission. No internet, network-state, storage, location or account permissions are required. Native input is tested with Android touch events; emulators cannot certify hardware haptics, battery life, OEM layouts or real audio routes.

## Emulator regression harness

The dedicated `Medium_Phone_API_36.1` ARM64 emulator was exercised at 1080×2400. On this Apple Silicon host, use two virtual CPUs and hardware graphics. The one-CPU software-rendered profile restarted Android during the 0.4.1 check; the hardware profile completed the test. Start the dedicated emulator, then run:

```sh
emulator -avd Medium_Phone_API_36.1 -cores 2 -gpu host
GODOT_BIN=/path/to/godot ./tools/test.sh --render
python3 tools/android_smoke.py --adb /path/to/adb --reset-qa-data
```

This explicit flag clears **only BLOCKSHIFT's test data on the emulator**. The harness refuses a physical-device serial. It waits for startup before injecting any touch, completes the real SHIFT tutorial, verifies that tutorial play did not change statistics, places a random Classic piece, pauses, force-stops, resumes, and waits for the active-time autosave. Screenshots and a JSON report are written under `review/`. On a Google emulator, dismiss unrelated system ANR dialogs before running; the original validation run temporarily disabled and then restored a malfunctioning Digital Wellbeing service. The rendered test command also runs `tests/export_android_layout.gd`, generating touch targets from real controls in `review/android-layout.json`; the Android harness checks this fixture's version against the installed APK. The fixture targets the dedicated 1080×2400 validation emulator; use the Godot UI harness for other sizes. See the validation report for which APK version actually completed the emulator run.

## Production signing

Create an upload key **outside the repository**, back it up privately and use Google Play App Signing. Do not commit a key, password, credentials file or machine path. The production **Android AAB** export preset is separate from the unsigned validation preset. Supply these supported Godot environment variables through your local shell/secret manager without echoing secrets:

- `GODOT_ANDROID_KEYSTORE_RELEASE_PATH`
- `GODOT_ANDROID_KEYSTORE_RELEASE_USER`
- `GODOT_ANDROID_KEYSTORE_RELEASE_PASSWORD`

Then export with `godot --headless --path . --export-release "Android AAB" build/blockshift.aab`. Confirm the exact variable names against the installed engine if upgrading. Alternatively fill the release keystore fields in the editor; Godot's generated `.godot/export_credentials.cfg` is ignored. The keystore and key passwords must match for the standard exporter. For 0.5.0, a dedicated RSA-3072 upload key was created with explicit user authorization outside the workspace. `tools/sign_android.py` signs the unsigned release AAB using `jarsigner`, verifies its signature and checks its certificate fingerprint. The private key, locally saved password and recovery details are under `~/Library/Application Support/Made by Kreativ/signing/blockshift/` with owner-only access. Back up that entire directory privately; never put it in the tester ZIP. `--create-key` refuses to overwrite any existing identity. `android_build.sh aab-internal` uses this signing path and never sends the password to Godot or command-line arguments.

The signed AAB uses the upload key; Google-managed Play App Signing is already enabled for this app. Play-delivered APKs use a different certificate from the existing development installation. Before switching, use Settings → About → Back up & restore progress to export and verify the latest progress file on Android 10+. Verify a restore and keep an additional private copy outside the phone. Never uninstall the existing installation until the transfer is verified and the user authorizes the switch. A clean test device or separate Android profile avoids this signature conflict.

To reproduce the bundle-derived QA APK, run `python3 tools/internal_test_apk.py --bundletool /path/to/bundletool.jar`, then verify it with `tools/verify_android.py --expect-release` and the installed SDK build-tools. Password files exist only in a private temporary directory. The Play-installed version still needs its own internal-track smoke test.

Before publication, rerun tests, inspect the release variant, verify the AAB with official bundletool, and complete the [release checklist](RELEASE_CHECKLIST.md). Keep SDKs disabled until real billing/consent/advertising integrations have passed store testing.

## Policy references

Checked 2026-10-16 against [Google Play target API requirements](https://developer.android.com/google/play/requirements/target-sdk): new Android phone/tablet apps and updates require Android 16 / API 36. Recheck on the actual publication date. [Godot Android export documentation](https://docs.godotengine.org/en/stable/tutorials/export/exporting_for_android.html) covers tool paths, icons, signing and AAB export. Check [16 KB page-size requirements](https://developer.android.com/guide/practices/page-sizes) for the engine **and every future native plugin**. Build success by itself is not Play approval.

## Build troubleshooting

The first AAB attempt ran out of disk space while a legacy Jetifier task copied the large engine AAR. Keep several GB free, use the bounded Gradle settings above and remove only this project's disposable generated build outputs if necessary. The official template may log an Android Gradle Plugin/compile SDK recommendation or unused manifest replacement warning. Record such warnings and review them on engine upgrades; do not silently suppress failures.

## Internal candidate checks

The 0.5.0 release helpers use the local Java SDK and official bundletool; no Play service account or store credentials are embedded. Run `python3 tools/verify_android.py build/blockshift-internal-0.5.0.aab --java-sdk /path/to/jdk --bundletool /path/to/bundletool.jar` to inspect SDK, version, permissions, backup/debug flags, ARM64, native alignment and packaged release metadata. Godot puts resources in an install-time asset module; the verifier supports that layout.

After building the candidate, `python3 tests/signing_regressions.py` checks identity overwrite protection, refusal to re-sign an already signed input and rejection of modified content. Private credentials never enter the report.

`python3 tools/release_smoke.py --adb /path/to/adb --reset-emulator-app` exercises the non-debuggable bundle-derived APK using real Android touch events. It requires Pillow and the dedicated 1080x2400 ARM64 emulator. It intentionally removes only BLOCKSHIFT test data on that emulator to replace any incompatible debug-signed package. Physical-device serials are refused. It checks tutorial clear counts, placement, pause/force-stop/resume and exact board/hand screenshots; it does not inspect private saves. Review tutorial preview/Apply/result captures manually. No Play-delivered or physical-phone result is implied by a local emulator pass.

On a host with little free memory/disk, finish the desktop tests and Gradle/bundle operations before booting the emulator. Keep at least several GB free. Do not delete player data to make room. The current initial cold emulator attempt ran short of host resources; the final report records the actual successful or pending scope.
