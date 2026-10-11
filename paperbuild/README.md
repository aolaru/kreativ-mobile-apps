# PaperBuild

An offline Android document-maker from Kreativ.

## What the first version does

- Uses the system document picker to add one or more images without requesting broad gallery access.
- Opens the user's camera app to capture a page without declaring direct camera permission.
- Lets the user reorder, remove, and rotate individual pages.
- Saves the current draft privately, including page order, rotation, name, and export settings.
- Exports A4 or US Letter PDFs in original colour, grayscale, or high-contrast document mode.
- Creates PDFs locally, then lets the user open, share, or save them to `Downloads/PaperBuild`.
- Works without an account, ads, analytics, tracking, or internet permission.

## PaperBuild Pro

PaperBuild Pro is a permanent one-time unlock, not a subscription. Its base price is €2.99, with automatic Google Play country pricing; the app shows the local Play price. It adds smaller PDF exports, unlimited pages, and PDF merge and page-range extraction. Merge/extraction preserves page content and sizes rather than converting pages into images. Free documents support up to 10 pages.

Documents are processed offline. Google Play handles purchase connectivity. The merged app manifest explicitly removes internet/network-state permissions added by billing dependencies.

Start a new document to clear the private draft. Unreferenced temporary exports older than seven days are removed on startup; the most recent export is retained while available. Saved Downloads are never removed by the app.

PDF tools do not support password-protected documents. Editing invalidates digital signatures; extracted PDFs discard document bookmarks and page labels. Make an unsigned working copy when necessary.

## Verification

Run `./gradlew testDebugUnitTest lintDebug assembleDebug`. Device regression tests use `./gradlew connectedQaAndroidTest`: the QA variant has application ID `com.kreativ.paperbuild.qa` and does not replace the installed release app. Google Play purchase/restore testing must use the release package installed from the internal track.

## Build

Open this folder in Android Studio and run the `app` configuration, or run:

```sh
./gradlew assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.
