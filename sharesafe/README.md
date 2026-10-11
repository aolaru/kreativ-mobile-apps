# ShareSafe

An Android photo-privacy utility. Select photos, choose a JPEG quality level, and export fresh JPEG copies individually or in a ZIP. Photo processing stays on-device; optional Google Play Pro purchases use Google's network services.

## What the first version does

- Uses Android's system document/photo selection interface; it does not request broad gallery access.
- Redraws selected images as new JPEGs, removing embedded location, camera, and timestamp metadata.
- Handles all eight EXIF orientations, including mirrored photos.
- Replaces source filenames with neutral names. Large images are reduced to at most 4096 pixels per side / 8 megapixels; transparency becomes white and animations export one frame.
- Processes photos entirely on-device. Only the Play variant includes Google Play Billing and its network permissions.
- Lets people share clean JPEGs directly or package them into a ZIP.
- Saves clean JPEGs to the device gallery in `Pictures/ShareSafe`.
- Preserves selections, completed exports, and editor masks across recreation; running jobs survive rotation.
- Removes private export/redaction cache entries older than 24 hours on a new session (not a strict expiry timer).

## Pro and store separation

Google Play Pro (`sharesafe_pro`) is a permanent, non-consumable unlock. Cover is the default privacy tool; blur/pixelation are optional effects, not guaranteed anonymisation. Editing includes zoom, pan, undo, reset, accessible percentage-based area entry, and flattened JPEG output. The Play public verification key is in `app/src/play/res/values/billing.xml`; it is public, not the private release key.

The Play implementation verifies signed receipts and package/product identity, refreshes ownership on foreground/restore, clears access on a successful empty purchase query, preserves previously verified access when offline, and uses WorkManager for durable acknowledgement retries. There is no Kreativ receipt server. Client-side checks cannot prevent a modified/rooted app from bypassing payment; a backend would be a separate product decision.

Samsung and Amazon are free-only until their own billing integrations are implemented. They neither ship Google's billing SDK nor show Pro purchase controls. Cross-store purchase portability is not promised.

### Google Play reviewer access

The Play edition provides complimentary access under **Privacy & About → Reviewer
access**. The reusable, non-expiring code is supplied privately in Play Console's
Sign-in details, not in this repository. Only its SHA-256 digest is compiled into
the app. Review access works offline, survives app restarts, and unlocks the same
editor/export features without creating or modifying a Google Play purchase.
Disable it from the same screen; reinstalling requires re-entering the code.
Other store editions cannot enable this access.

The private code on this Mac is stored outside Git at
`~/Library/Application Support/Kreativ/ShareSafe/reviewer-access-code` (mode 600).
Pass it as `KREATIV_SHARESAFE_REVIEWER_CODE` for the positive unit test, or as the
instrumentation argument `reviewerCode` for `ReviewerAccessWorkflowTest`. Those
positive tests are skipped when the private code is not supplied. Never place the
raw code in source, release notes, public listings or screenshots.

## Build and install

Open this folder in Android Studio and run the `playDebug` configuration on an Android 8.0+ device, or build a debug APK:

```sh
./gradlew assemblePlayDebug
```

The APK is written to `app/build/outputs/apk/play/debug/app-play-debug.apk`.
Debug builds install separately as `com.kreativ.sharesafe.debug`, named **ShareSafe Test**, so the live app is preserved. The Play debug build includes a synthetic demo editor; that entry point and activity do not exist in release builds.

To install it over USB with Android Debug Bridge enabled:

```sh
adb install app/build/outputs/apk/play/debug/app-play-debug.apk
```

## Before a Play Store submission

Use the existing release key to generate a signed Android App Bundle, prepare the store listing and privacy information, then complete Google Play's required testing/review steps. Do not publish the debug APK.

### Release signing on this Mac

Build a signed Google Play bundle with:

```sh
bash scripts/build-play-release.sh
```

The helper reads the existing key from
`~/Library/Application Support/Kreativ/ShareSafe/sharesafe-release.jks` and its
password from macOS Keychain (service `Kreativ ShareSafe Release Key`, account
`sharesafe-release`). It does not store the password in this repository. Explicit
`KREATIV_SHARESAFE_KEYSTORE` and `KREATIV_SHARESAFE_KEYSTORE_PASSWORD` environment
variables can override those defaults.

The encrypted key backup is at
`~/Documents/Android Signing Backups/ShareSafe/sharesafe-release.jks`.
The password remains in Keychain; copying the key alone does not copy its password.
The SHA-256 upload certificate fingerprint verified against Play Console is:
`9D:64:E7:84:F2:8E:3F:24:86:86:1B:B0:44:13:B1:BB:14:79:80:51:20:4A:05:09:47:C8:6B:78:03:A7:F2:B5`.

The signed bundle is written to `app/build/outputs/bundle/playRelease/app-play-release.aab`.

## Store builds

The project has distinct store variants so future billing SDKs can be added without
shipping one store's payment code to another:

- Google Play: `./gradlew assemblePlayRelease` / `bundlePlayRelease`
- Samsung Galaxy Store: `./gradlew assembleSamsungRelease`
- Amazon Appstore (Fire tablets): `./gradlew assembleAmazonRelease`

Release builds require a signing configuration before they can be submitted. The
store submission checklist is in [`STORE_SUBMISSION.md`](STORE_SUBMISSION.md).
