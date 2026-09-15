# ShareSafe

An offline Android photo-cleaning utility. Select one or more photos, choose a JPEG quality level, and ShareSafe exports a ZIP containing fresh JPEG copies with embedded EXIF metadata removed.

## What the first version does

- Uses Android's system photo picker; it does not request broad gallery access.
- Redraws selected images as new JPEGs, removing embedded location, camera, and timestamp metadata.
- Applies EXIF orientation before exporting so common camera photos remain upright.
- Processes entirely on-device and does not declare internet access.
- Packages cleaned copies into a ZIP that can be shared using Android's share sheet.

## Build and install

Open this folder in Android Studio and run the `app` configuration on an Android 8.0+ device, or build a debug APK:

```sh
./gradlew assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

To install it over USB with Android Debug Bridge enabled:

```sh
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Before a Play Store submission

Create a release signing key outside this repository, generate a signed Android App Bundle, prepare the store listing and privacy information, then complete Google Play's required testing/review steps. Do not publish the debug APK.
