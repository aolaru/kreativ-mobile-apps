# PaperBuild

An offline Android document-maker from Kreativ.

## What the app does

- Uses the system document picker to add one or more images without requesting broad gallery access.
- Opens the user's camera app to capture a page without declaring direct camera permission.
- Lets the user reorder, remove, and rotate individual pages.
- Crops pages without overwriting the source image; restore the full page at any time.
- Saves the current draft privately, including page order, rotation, name, and export settings.
- Exports A4 or US Letter PDFs in original colour, grayscale, or high-contrast document mode.
- Creates PDFs locally, then lets the user open, share, or save them to `Downloads/PaperBuild`.
- Works without an account, ads, analytics, tracking, or internet permission.
- Keeps completed documents in a private Recent PDFs library, with previews, rename, open, share, save, and reversible removal.

## PaperBuild Pro

PaperBuild Pro is a permanent one-time unlock, not a subscription. Its base price is €2.99, with automatic Google Play country pricing; the app shows the local Play price. It adds smaller PDF exports, unlimited pages, PDF merge and page-range extraction, password-protected exports, custom text watermarks, page numbers, and named export presets. Existing Pro ownership unlocks the new features without another purchase. Merge/extraction preserves page content and sizes rather than converting pages into images. Free documents support up to 10 pages; cropping and Recent PDFs are free.

Version 1.1.0 (version code 4) has been committed and its signed App Bundle uploaded and validated. Device review covers camera capture, Gmail attachment hand-off, password protection, overlays, presets, compact export, merge/extraction and an eleven-page export. It is prepared for owner-approved Google review submission, not submitted or published. Android 8/9 permission testing was explicitly skipped; Play payment/restore testing remains waived and unverified. See `RELEASE_CHECKLIST.md` for evidence and approval status. Pro options apply to image exports, merging, and extraction. Export passwords are 8–64 characters and are never written to drafts or presets; protected presets require a fresh password before exporting. Encrypted library entries do not generate previews. Forgotten export passwords cannot be recovered.

Documents are processed offline. Google Play handles purchase connectivity. The merged app manifest explicitly removes internet/network-state permissions added by billing dependencies.

Start a new document to clear the private draft. Completed 1.1 exports are copied atomically into app-private Recent PDFs storage, separate from disposable cache. Removing a PDF moves it to Removed PDFs, where it can be restored or permanently deleted with confirmation. The library is removed when the app is uninstalled; export copies to Downloads or another location to retain them. Legacy cached exports other than the latest are cleaned after seven days. Saved Downloads are never removed by the app.

PDF tools do not accept password-protected input documents; use an unlocked working copy. Pro can password-protect the output. Editing invalidates digital signatures; extracted PDFs discard document bookmarks and page labels. Make an unsigned working copy when necessary. Watermarks and page numbers are visible additions, not redaction or digital signatures.

## Verification

Run `./gradlew testDebugUnitTest lintDebug assembleDebug`. Device regression tests use `./gradlew connectedQaAndroidTest`: the QA variant has application ID `com.kreativ.paperbuild.qa` and does not replace the installed release app. Google Play purchase/restore testing must use the release package installed from the internal track.

## Build

Open this folder in Android Studio and run the `app` configuration, or run:

```sh
./gradlew assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.
