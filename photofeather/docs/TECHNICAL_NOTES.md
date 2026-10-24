# PhotoFeather — technical notes

**Lighter photos. Ready to share.** A separate, native Android app by Made by Kreativ. Working Play title: **PhotoFeather: Photo Compressor** (30 characters). Version 1.0.0 / build 4; the name and package are provisional. Nothing has been published.

## Build and run

Requires JDK 17, Android SDK 36 and Android 8.0/API 26 or later. Open this directory as a project in Android Studio, or set `JAVA_HOME` to JDK 17 and point `local.properties` at your Android SDK:

```sh
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.kreativ.photofeather.debug/com.kreativ.photofeather.MainActivity
```

The packaged 1.0.0 debug APK is also in `../review/resize-presets/PhotoFeather-1.0.0-resize-debug.apk`. It uses the distinct `.debug` application ID; it cannot replace another Made by Kreativ app. No release key, backend, network permission, billing, advertising or analytics SDK is included. AndroidX adds a private signature permission for receiver protection; it grants no network or photo-library access.

## What works

Select one or many images with Android's system photo picker on Android 13+, or its system document-picker fallback on Android 8–12. Supported images shared by other apps are imported through their granted content URIs. Only selected files are read; no broad storage access is requested. Files are copied into private working storage so an expiring source permission does not destroy an imported session.

Choose 200 KB, 500 KB, 1 MB or a custom **1–50,000 whole KB** target. Units are decimal: KB = 1,000 bytes, MB = 1,000,000 bytes. Set a longest edge of **64–8,192 pixels**, without upscaling or changing aspect ratio. Resize-only mode has no byte target. Advanced maximum JPEG quality accepts 10–100, default 85.

Preview actual encoded byte counts, original/output dimensions and percentage saved. Compare original and output at the same zoom/centre, using pinch/pan, double-tap or labelled zoom buttons. Preview decoding is limited to a 2,048-pixel edge; it is not a promise of full-resolution inspection. The first three result cards also show bounded thumbnails.

Save **new** copies into `Pictures/PhotoFeather` on Android 10+, or a user-selected folder on Android 8–9. Save verifies the destination against the prepared JPEG with SHA-256 before reporting success. Share verified private copies through Android's share sheet. Selecting a receiving app or recipient remains the user's action. Existing originals are never written, overwritten or deleted.

Batch progress, cancellation, per-image errors and partial success are supported. Larger/equal-sized JPEGs are blocked by default and require a per-image opt-in. Targets not reached show the final measured attempt and prevent its export. Each committed preview stores its own target/edge/quality settings. Current-control mismatches require explicit confirmation before exporting the shown copies; no result is relabelled with newer settings. Scroll, editor focus and position across comparison/rotation are restored, and newly completed previews bring results into view.

## Formats, metadata and limits

- Inputs: **JPEG, PNG, static WebP**. HEIC/HEIF, AVIF, GIF, animated PNG/WebP and unreadable inputs are rejected individually. Input detection checks decoded data, rather than trusting a filename or declared MIME type.
- Output: **lossy JPEG**. PNG/WebP conversion always asks for an opaque white, warm-paper or black background because transparency may be present. Originals retain transparency. Colour profiles/HDR data and source metadata are not preserved.
- EXIF orientations 1–8, including mirrored variants, are applied to pixels. The decoder uses power-of-two sampling to keep the decoded image within **eight megapixels**. Requested dimensions may therefore be reduced for large inputs.
- At most **35 encodes**: up to five resolution levels, with bounded quality search. Automatic reduction stops at a 320-pixel longest edge, or the smaller initial edge. Quality stops at 40, or the user's lower maximum. A failed target is not called a successful compression. The search is bounded and does not promise a globally optimal quality/size combination.
- JPEG input codestreams are checked for complete marker/scan structure and end-of-image before decoding. Progressive and multiple-scan JPEGs are supported; the check is not a proof of the photo’s authenticity or a guarantee against every possible entropy-level corruption.
- The encoder receives pixels only. All JPEG APP0–APP15 and comment segments are stripped; a strict baseline-JPEG parser verifies headers, scan data, end-of-image and absence of trailing data. The JPEG is reopened and its dimensions checked. No unchecked output is offered for export. Gallery/filesystem records will still have newly generated names and timestamps.
- Metadata removal does **not** anonymise a photo, redact visible information or guarantee visual quality. No lossless claim is made.
- Maximum batch: **100 images**, maximum source: **200 MB per image**. Processing is sequential on a worker; previews load on separate workers. Storage is checked before imports, encodes and exports. Cancellation is cooperative between stream chunks and platform decode/encode calls; an in-progress platform codec/provider call cannot be interrupted immediately.
- Work continues across rotation. Process death retains completed previews and marks incomplete work interrupted; the user retries explicitly. Replacement outputs are staged: cancellation, processing failure and unreachable replacement targets preserve the previous usable result and its saved status/settings. Each successfully verified replacement commits per image, so a batch can retain earlier previews beside newly completed ones. It does not run a durable background queue. A pending export is journalled with its item ID and the prepared JPEG fingerprint. On recovery, a matching completed destination is adopted (and published if needed), an incomplete unpublished gallery destination is removed, and unknown/unverifiable published files are retained. Failure to clear the recovery record is reported and never causes a verified published copy to be deleted. Old URI-only journals migrate by matching the neutral output filename and prepared bytes. Provider access failures are reported per image; a provider that refuses deletion can prevent rollback of its new incomplete document.
- Imported working copies and the current session stay private and are excluded from cloud backup/device transfer. Clear session removes imported copies. Unreferenced export files older than 24 hours are cleaned on a later session/app start, preserving recent files for share recipients. This is lazy cleanup, not a timer. A session older than 24 hours is discarded on a cold launch. Saved gallery/folder copies remain until the user removes them.
- All processing is on-device. Android's picker can offer cloud-provider photos; retrieval is controlled by the chosen provider. PhotoFeather has no image upload path.

## Architecture

- `image-engine`: reusable Android library, independent of screens, content providers, export destinations and branding. Immutable `CompressionOptions`, pure-Java `ResizePolicy`, `ImageEngine` for decoding/encoding, and `JpegMetadata` for stripping/verification. Cancellation and available storage are injectable.
- `app/Session`: serial worker, imported-file ownership, per-image state and atomic JSON recovery journal. The application owns it so activity recreation does not restart work. Processing snapshots an optional set of item IDs, allowing selective retries without touching healthy previews/saved status. Save similarly snapshots selected IDs. Individual removal commits journal state before pruning the private import; journal failure restores the item. Export selection is durable per photo.
- Export rollback informs the session when deletion was confirmed, clearing a stale pending record. If the journal write also fails, an in-memory confirmation survives until persistence can be retried; completed saved files still follow the fingerprint-based recovery path. Unknown destinations remain conservatively retained.
- `MainActivity` separates Settings/Results, stores page/expanded details across rotation, and supplies a fixed action area via `Ui.page`. High font scales/short windows use one Export action with accessible Save/Share choices and a metadata disclosure. Folder picker requests snapshot IDs across activity recreation, so a later result cannot export newly selected images accidentally. Picker/folder results wait for an opening session or an active job to finish, preserving the selected URI/ID payload rather than dropping the action when the worker is busy.
- `app/ExportManager`: new-file creation, pending-destination rollback, cancellation and byte verification. FileProvider exposes only the managed `exports/` directory with temporary read grants.
- `MainActivity`, `PreviewActivity`, `Ui`: native views, system light/dark palettes, scalable text, accessibility labels/selected states, 48dp+ controls and scrollable large-text/landscape layouts. Preview zoom geometry was reused from Kreativ Font; card/inset conventions were adapted from ShareSafe and PaperBuild. Those projects were read only.
- Three non-exported debug-only activities provide dark/200% font configuration overrides for tests, without changing device settings. A debug-only document provider protected by MANAGE_DOCUMENTS tests folder/provider faults without advertising a root in the picker. These components do not ship in release builds. Separate release-test and opt-in accessibility-test source sets validate release code and TalkBack-enabled accessibility actions. Sample images are generated by instrumentation, not bundled in the product.

## Verify

```sh
./gradlew :image-engine:testDebugUnitTest :image-engine:connectedDebugAndroidTest :app:lintDebug
# Run app tests directly to retain captured screenshots and the installed debug app:
bash scripts/test-device.sh
```

See [the completion report](../review/REPORT.md) for actual results, screenshots, remaining coverage and the separate free/Pro suggestion. Device tests create their own synthetic inputs and gallery copies; saved test gallery rows are deleted in cleanup. They do not send anything through the share sheet or use personal photos.

## Naming and reference checks — October 22, 2026

Read the existing workspace Android projects and the [launched app directory](https://madebykreativ.com/apps/), including the [ShareSafe listing](https://play.google.com/store/apps/details?id=com.kreativ.sharesafe) and [Northlight listing](https://play.google.com/store/apps/details?id=com.kreativ.northlight). Native implementation follows Android's [photo picker](https://developer.android.com/training/data-storage/shared/photo-picker) and [shared media](https://developer.android.com/training/data-storage/shared/media) guidance.

Initial exact-name web and indexed Google Play searches for PhotoFeather did not surface an exact compressor match. This is not confirmation of availability. Play Console/package validation and fuller name/trademark checks remain open. No store entry, listing submission, account, price or subscription was created.

## 1.0.0 release validation

See [the 1.0.0 report](../review/release-1.0.0/REPORT.md) for the actual Android 9/16 matrix, signed QA release checks, screenshots and remaining signing/name/manual-listening gates. The QA release package is `.releasecheck`; its debug signing certificate is for local verification only. Production signing uses external PHOTOFEATHER_* variables documented in the README.

## Resize presets — build 4

All resizing remains free. Small/Medium/Large select longest-edge limits of 1280/1920/2560 pixels; Custom accepts whole numbers from 64 to 8192. Editing the pixel field selects Custom and updates every imported photo’s dimension preview immediately. Invalid text suppresses the planned dimensions and disables Preview copies; it does not replace the last valid persisted edge. The Custom selection (including a value equal to a preset) persists in the atomic session journal and migrates older non-preset edges to Custom.

`ResizePolicy.previewSize` shares the engine’s safe-decode sampling policy, handles oriented dimensions and bounds both floor/ceiling codec-rounding possibilities before fitting the longest edge. The UI therefore calls these dimensions **up to** bounds, not encoded results. It discloses memory reductions and no-upscaling. A maximum file-size target can reduce resolution further; actual encoded dimensions and bytes remain measured in Results. No decoder/encoder algorithm, output format or metadata policy was changed.

See [resize implementation and validation](../review/resize-presets/REPORT.md) for fresh test evidence and running-app screenshots.
