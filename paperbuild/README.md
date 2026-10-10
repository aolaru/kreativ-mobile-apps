# PaperBuild

An offline Android document-maker from Kreativ.

## What the first version does

- Uses the system document picker to add one or more images without requesting broad gallery access.
- Opens the user's camera app to capture a page without declaring direct camera permission.
- Lets the user reorder, remove, and rotate individual pages.
- Exports A4 or US Letter PDFs in original colour, grayscale, or high-contrast document mode.
- Creates PDFs locally, then lets the user open, share, or save them to `Downloads/PaperBuild`.
- Works without an account, ads, analytics, tracking, or internet permission.

## PaperBuild Pro

PaperBuild Pro is a permanent €2.99 unlock, not a subscription. It adds smaller PDF exports, unlimited pages, and PDF merge and page-range extraction. Free documents support up to 10 pages.

## Build

Open this folder in Android Studio and run the `app` configuration, or run:

```sh
./gradlew assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.
