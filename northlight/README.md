# Northlight

An offline Android compass and emergency-light utility from Kreativ.

## What it does

- Shows a live magnetic compass using the device's orientation sensors.
- Controls the camera LED as a lantern, requesting camera permission only when the user selects that feature.
- Provides a full-screen red-light mode for preserving night vision.
- Sends a repeating SOS flash signal.
- Offers brightness control when the device supports variable torch strength.
- Includes a Quick Settings lantern tile after camera access has been granted in the app.
- Handles portrait and landscape orientation, and explains when the compass needs calibration.
- Works without an account, analytics, ads, or tracking. Core compass and light tools work offline; the optional Pro purchase uses Google Play Billing.
- Clearly identifies when a phone lacks a usable compass sensor or camera flash.

## Northlight Pro (1.1.1 / version code 4)

The optional one-time unlock adds up to eight named device-local bearings,
with rename/delete controls and turn guidance, amber and green screen lights,
and an adjustable LED strobe. Compass, lantern,
red light and SOS remain free.

Google Play product ID: `northlight_pro`. Purchase option: `lifetime` (Buy).
EUR checkout price: 2.99, with Play-converted non-EUR regional prices. Purchases are
non-consumable and restored through Google Play. A successful ownership query
caches access locally for offline use; a later successful query reconciles
ownership, including refunds. Pending payments do not unlock Pro.

Before production rollout, verify a license-test purchase, cancellation,
restoration after reinstall, and offline access on a real Play-enabled device.
Also publish the updated privacy policy from
`store-assets/play/privacy-policy/index.html` at the existing policy URL.

On 10 October 2026, version code 3 (1.1.0) was released to Internal testing
and the `lifetime` purchase option was activated in 174 countries/regions.
Production remains on version code 2 (1.0.1). No real purchase/restore test
has been completed yet. Live privacy policy still needs the billing update.

Version code 4 is the local reliability update; it has not been uploaded.
Both compass screens now share rotation compensation, sensor fallback and
accuracy feedback. Naming a bearing freezes the heading at the time of the tap;
the ninth bearing is refused rather than deleting existing data. Night lights
stay awake and remember adjustable brightness. SOS stops on torch failure,
brightness is remembered, and the Quick Settings tile observes hardware state.
All screens handle system bars/cutouts and scroll when needed. UI strings are
available in English and Romanian, with English fallback for other languages.

Billing refreshes ownership at app foreground entry and offers manual retry.
Completed purchases enqueue unique, network-constrained WorkManager jobs with
exponential backoff to acknowledge purchases even after leaving Pro or process
death. Pending payments never grant new access; failed queries preserve cached
access. Background execution still depends on Android scheduling, connectivity,
and users not force-stopping/uninstalling the app; no client can guarantee a
confirmation deadline while the device stays offline.

## Verification

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
./gradlew :app:assembleDebugAndroidTest
# Only with an authorized phone connected (no emulator required):
ANDROID_SERIAL=YOUR_PHONE_SERIAL ./gradlew :app:connectedDebugAndroidTest
```

Debug builds use `com.kreativ.northlight.debug`, leaving production data untouched.
The device regression suite uses a test-only Application and cached entitlement;
it does not contact Play, make purchases or activate flashing lights. It covers
screen launch, landscape/large-text scrolling, night-light wakefulness, frozen bearing
capture, and follow/rename/delete. It is not a substitute for real Billing QA.

On 10 October 2026, all six device regression tests passed on a Samsung Galaxy
A55 (SM-A556B), Android 16/API 36, together with all 18 JVM unit tests. The phone
test exposed a clipped Pro control: safe-area insets now belong to a non-scrolling
outer container, so Android scroll-to-rectangle calculations use the correct
viewport. No emulator, Play payment or flashing-light test was used in this pass.
Lint reports only the advisory that a newer Gradle version is available.

Before release, manually verify:

- Compass consistency in all four rotations, calibration and sensorless devices.
- Layouts with large text, a display cutout, gesture and three-button navigation.
- Flash/SOS failure with the camera busy; brightness after off/on; tile state
  after external torch changes and service recreation.
- Night lights beyond the normal screen timeout and brightness restoration on exit.
- Play license-test purchase, acknowledgement, pending approval/cancellation,
  purchase cancellation, restore after reinstall, offline access and refund
  reconciliation. Use the signed internal-testing build, not the debug package.
- Public privacy-policy update before the paid production rollout.

## Build

Open this folder in Android Studio and run the `app` configuration, or run:

```sh
./gradlew assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Release bundle

Release signing is intentionally kept outside this repository. Set these environment variables before building a Play upload bundle:

```sh
export KREATIV_NORTHLIGHT_KEYSTORE="/path/to/northlight-upload.jks"
export KREATIV_NORTHLIGHT_KEYSTORE_PASSWORD="your-keystore-password"
./gradlew bundleRelease
```

The signed bundle is written to `app/build/outputs/bundle/release/app-release.aab`. Never commit the keystore or its password.

### Northlight’s current upload key

The registered upload key is kept outside the repository at
`~/.android/northlight-upload.jks`, with an encrypted backup in
`~/Documents/Android Signing Backups/Northlight/`. Its password is stored in
macOS Keychain under service `com.kreativ.northlight.upload-key` and account
`northlight-upload`.

On this Mac, build a signed release without exposing the password by running:

```sh
KREATIV_NORTHLIGHT_KEYSTORE="$HOME/.android/northlight-upload.jks" \
KREATIV_NORTHLIGHT_KEYSTORE_PASSWORD="$(security find-generic-password -s 'com.kreativ.northlight.upload-key' -a 'northlight-upload' -w)" \
./gradlew bundleRelease
```

Keep at least one encrypted copy of the `.jks` file off this Mac as disaster
recovery. The Keychain entry and keystore file are both required to sign a
future upload.

The replacement `northlight-upload-2026.jks` was prepared during recovery;
its Play reset request was cancelled after the original password was found.
Use the registered original key above for releases.

## Notes

The compass reports magnetic north. Nearby magnets, metal objects, and some phone cases can affect accuracy. The LED lantern and SOS signal require camera permission because Android treats control of the camera flash as a camera capability.
