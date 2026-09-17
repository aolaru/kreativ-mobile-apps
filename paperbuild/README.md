# PaperBuild

An offline Android document-maker from Kreativ.

## What the first version does

- Uses the system document picker to add one or more images without requesting broad gallery access.
- Opens the user's camera app to capture a page without declaring direct camera permission.
- Lets the user reorder, remove, and rotate individual pages.
- Exports A4 or US Letter PDFs in original colour, grayscale, or high-contrast document mode.
- Creates PDFs locally, then lets the user open, share, or save them to `Downloads/PaperBuild`.
- Works without an account, ads, analytics, tracking, or internet permission.

## Planned Pro edition

PaperBuild Pro is planned as a one-time unlock, not a subscription. Planned features: merge and split PDFs, compression, signatures, permanent redaction, password protection, folders, and on-device OCR.

## Build

Open this folder in Android Studio and run the `app` configuration, or run:

```sh
./gradlew assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.
