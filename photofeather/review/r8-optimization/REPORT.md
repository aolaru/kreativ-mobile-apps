# PhotoFeather — R8 optimisation build checks

1 November 2026. Source version remains 1.0.0 / versionCode 4 for local validation; this is not a new Play release.

## Changes

- Release builds enable R8 code shrinking, optimisation and obfuscation, plus resource shrinking, using `proguard-android-optimize.txt`. Debug builds remain unminified. The reusable engine is optimised as part of the final app.
- Application rules are documented in `app/proguard-rules.pro`, without blanket class keeps or suppressed production missing-class diagnostics.
- Release instrumentation initially failed because AndroidX Test references `CanIgnoreReturnValue` and `MustBeClosed` annotations absent from its runtime dependency graph. Adding the annotation library introduced another compiler-only JDK reference (`javax.lang.model.element.Modifier`), so that approach was discarded. The final fix uses two exact R8-generated `-dontwarn` rules in `app/proguard-test-rules.pro`, applied only through `testProguardFiles`. No dependency was added. Verification confirms these exclusions are absent from the production R8 configuration.

## Checks completed

The following QA command passed, including shrinking the release instrumentation APK:

```sh
./gradlew -PphotoFeatherQaRelease -PphotoFeatherTestBuildType=release \
  :app:assembleRelease :app:assembleReleaseAndroidTest \
  :app:lintRelease :image-engine:lintRelease :image-engine:testReleaseUnitTest
```

The production command also passed with all four production signing environment variables unset, producing **unsigned** APK/AAB artifacts:

```sh
./gradlew :app:assembleRelease :app:bundleRelease \
  :app:lintRelease :image-engine:lintRelease
```

- **26 JVM tests passed**: JPEG validation 6, resize preview 8 and policy 12. XML results are in `unit-test-results/`.
- **Zero lint errors/fatal issues**; 8 app and 2 engine advisory warnings remain. XML reports are retained here.
- Production APK manifest still identifies `com.kreativ.photofeather`, version 1.0.0 / code 4, minSdk 26 / targetSdk 36; it is non-debuggable, retains its application/activity/provider entry points, and requests no `android.permission.*` permissions.
- The complete offline policy is still packaged byte-for-byte. The AAB's `BUNDLE-METADATA/com.android.tools.build.obfuscation/proguard.map` matches the generated production `mapping.txt`.
- QA app and instrumentation APK signatures verify with the QA debug certificate.
- Archived signed build 4 APK/AAB SHA-256 values remain unchanged. No private signing key was accessed, no bundle uploaded and no Google Play release/submission setting changed.

See `verification.json`, `production-apk-badging.txt`, signature reports, `qa-build-final.log` and `production-build.log`. `initial-missing-annotations.log` records the initial failed build; it is not a successful test run. `baseline-build.log` records the initial unoptimised QA build.

## Measured sizes

| Artifact | Bytes |
| --- | ---: |
| Archived signed, unoptimised build 4 APK | 1,757,721 |
| Optimised unsigned production APK | 123,100 |
| Optimised unsigned production AAB, including mapping | 214,234 |
| Original uncompressed DEX payload | 5,069,136 |
| Optimised uncompressed DEX payload | 187,716 |

The DEX payload is **96.30% smaller**. This comparison excludes signing overhead. The APK totals use different signing states and are not a controlled installed/download-size or startup-speed comparison. No runtime performance improvement is claimed.

## Limits and next release

The owner explicitly requested **build checks only**. No Android device was connected; release instrumentation was compiled and shrunk but **not executed**. UI behaviour, image processing, saving/sharing and recreation must still be checked on a device with the optimised release before uploading it. The 26 JVM tests are not a replacement for those runtime checks.

Generated QA artifacts are in the ignored `build/r8-checks/` folder. Production artifacts and the mapping are in Gradle's normal ignored output folders. Do not upload these local validation artifacts with versionCode 4: that code is already used by the previously submitted bundle. Prepare a higher versionCode for a future upload, sign it through the existing signing helper, and archive its matching mapping with the exact released artifact. The previous build 4 submission remains unchanged.

References: [Android R8 configuration](https://developer.android.com/topic/performance/app-optimization/enable-app-optimization), [test ProGuard configuration](https://developer.android.com/reference/tools/gradle-api/8.10/com/android/build/api/dsl/TestBuildType).
