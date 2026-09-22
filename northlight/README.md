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
- Works without an account, analytics, ads, tracking, or internet permission.
- Clearly identifies when a phone lacks a usable compass sensor or camera flash.

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

## Notes

The compass reports magnetic north. Nearby magnets, metal objects, and some phone cases can affect accuracy. The LED lantern and SOS signal require camera permission because Android treats control of the camera flash as a camera capability.
