# PaperBuild

An offline Android document-maker from Kreativ.

## Version 1.2 submitted for review — 4 November 2026

Version code **6 / 1.2.0** includes the previously prepared optimisation work
and two new lifetime Pro features:

- **Existing-PDF page editor:** preview, reorder (including move to a position),
  rotate, duplicate, remove and undo up to 20 edits. A separate persisted editor
  draft leaves the photo draft intact. Exports go to Recent PDFs as new copies.
  Open the editor from PDF tools or a Recent PDF's More options menu.
- **Logo watermarks:** private, immutable PNG copies, transparency and EXIF
  orientation support, a logo preview in export options, and reuse through saved
  presets. Logos render at the visible top right, including rotated/cropped PDF
  pages, on image, merge, extraction and editor exports. Text watermarks and page
  numbers can be used alongside the logo. Passwords still stay out of storage.

The editor preserves page text/vector content and geometry. Local page links
are remapped to retained pages; links to removed pages are dropped. Signed,
fillable and password-protected inputs are rejected before replacing a draft.
Undo history survives Activity recreation; persisted drafts restore the page
plan after process death, without restoring undo history. Existing presets
without a logo remain compatible. Price, product ID and permissions are unchanged.

**Local verification:** signed release APK/AAB built; 16 JVM tests per debug and
release build type pass with no failures/skips; release lint has 0 errors and
39 warnings. APK certificate matches the existing upload key. The candidate AAB
is **7,518,062 bytes**, 34.92% smaller than v4, with mapping embedded and APK/AAB
DEX identical. Local renamed-class coverage is 1,314/1,356 (96.90%), not Play's
own optimisation metric. Retained artifacts: `app/build/outputs/releases/1.2.0/`.

**Device verification:** the 24-test QA suite (six new 1.2 regressions) passes
on Samsung SM-A556B / Android 16. Four release-equivalent black-box workflows
also pass, including a native thumbnail check that rejects blank page content.
Independent pypdf checks and Poppler renders verify output geometry, content,
links, encryption, transparency and overlays; Apple Quartz confirms logo output.
See `release-evidence/2026-11-04/1.2-VERIFICATION.md` for evidence and limits.
Source commit `3dcae85` is pushed to `codex/paperbuild-1-2` and `main`. Google
Play production draft **1.2.0 — PDF page editor and logo watermarks** is saved
with v6, the embedded ReTrace mapping and en-GB notes. Preview reports **Ready to
release**, no blocking issues and no devices lost. Play estimates 6.55 MB for a
new install (4.86 MB less than v4) and a 689 KB update. Final listing review is
complete: revised full description and 8/8 gallery are saved/reviewed in Play,
with the release submitted together as three changes after explicit owner
approval. Publishing overview confirms **Changes in review** for production
1.2.0, the en-GB full description and phone screenshots. Google restarted its
automated quick checks after submission; their completion is not yet confirmed.
Managed publishing is off, so approval may publish automatically. Approval and
publication remain unconfirmed. See `release-evidence/2026-11-04/1.2-submitted-for-review.png` and the review
pack at `promotion/2026-11-04-listing/review.html` and its `REVIEW.md`.

To test on an available unlocked device, using new isolated app packages:

```sh
bash scripts/test-qa-release.sh DEVICE_SERIAL
bash scripts/test-optimized-release.sh DEVICE_SERIAL
```

Both scripts refuse to replace an existing test installation automatically.
Run Gradle build variants sequentially: AGP shares task history between the
production and isolated output directories, so concurrent builds can invalidate
each other's resource intermediates.
QA uses `.v120.qa`, while the non-debuggable R8 build uses `.r8test`; production
and the owner's old QA app are preserved. The private signed reviewer code is
read at invocation and never committed. Independently reopen/render the pulled
PDFs, check page order/rotation/geometry, logo alpha/placement, encryption and
source preservation, and review the phone UI before preparing a Play draft.
Real purchase testing and Android 8/9 testing remain waived and unverified.

## Historical 1.1.1 optimisation candidate — superseded by 1.2

Version code 5 / 1.1.1 enables R8 code optimisation/obfuscation and resource
shrinking. The signed candidate bundle is 7,492,092 bytes (35.14% smaller than
v4), with its deobfuscation mapping embedded. Local analysis of the actual DEX
finds 1,290 of 1,331 defined classes renamed (96.92%); this is **not** Google's
proprietary optimisation metric and does not claim its warning has cleared.

Local build, 12 JVM tests per debug/release build type, and release lint pass
(0 errors / 28 warnings). **Not ready to submit:** device regressions and
independent PDF review remain pending because the owner is using the phone
for another app. No 1.1.1 APK has been installed and no Play draft/upload or
submission has been made. Existing Play/QA app data is untouched.

Reproducible preparation:

```sh
bash scripts/build-play-release.sh :app:testDebugUnitTest :app:testReleaseUnitTest :app:lintRelease
python3 scripts/verify-optimized-release.py app/build/outputs/apk/release/app-release.apk app/build/outputs/bundle/release/app-release.aab
```

The signing script reads the existing upload key password from macOS Keychain,
never from Git. It rejects the isolated-test flag. Production artifacts remain
under `app/build/outputs/`; isolated test artifacts use `app/build/isolated-r8/`.

When the owner explicitly makes the phone available, run:

```sh
bash scripts/test-optimized-release.sh DEVICE_SERIAL
```

This uses the **release** build type with the same R8/resource-shrinking rules,
but a separate `.r8test` application ID and the debug signing key so it can
coexist with Play. It is non-debuggable. Its platform-only black-box runner
references no private app types and adds no test-only app keep rules. The
existing private reviewer code is supplied at invocation, never committed or
printed. The runner covers Free crop/export/recovery, protected export/presets,
compact output, merge/extraction and a thirteen-page export. This is a
release-equivalent isolated APK, **not** the exact production package/billing
installation. Real purchase testing remains separate and waived.

The deeper QA suite can also use `-PpaperbuildQaApplicationIdSuffix=.v111.qa`
to preserve an existing `.qa` installation. Its APKs have been compiled but
not installed or run. The new release-equivalent suite is compiled but likewise
unrun; do not count compilation as device-test success. After device tests,
pull/render/reopen the generated PDFs independently and verify encryption,
page sizes/counts and overlays before uploading the release candidate.

## Marketing and listing preparation on 18 October 2026

**Latest Play status:** Original listing refresh published and publicly verified.
A follow-up adds the live tutorial link to the full description (3153/4000).
Submitted with user approval; Console shows **Changes in review** with quick
checks still running. See `promotion/2026-10-18/listing/TUTORIAL-LINK-SUBMISSION.txt`.
Earlier review-status notes below are historical.

**Website publication update:** The screenshot-led tutorial is live at
https://madebykreativ.com/news/photos-to-pdf-android-paperbuild/ and linked from
PaperBuild's existing app page. Five genuine screenshots, Updates, both RSS feeds,
sitemap and the app-page cross-link were publicly verified. The full GitHub
quality gate and deployment passed. See
`promotion/2026-10-18/tutorial/PUBLISHING-NOTES.txt`. Social/outreach remain drafts;
the preparation notes below are historical, not the current website state.

**Listing update:** The user approved **PaperBuild: Photo to PDF**, the copy,
six screenshots, feature graphic and review submission. Those listing changes
are uploaded and saved; all eight AI-label flags are OFF, including the icon.
Quick checks passed. After the user's manual final Send on 18 October 2026,
Console confirms **Changes in review**. Approval/publication remains pending.
See `promotion/2026-10-18/listing/SUBMISSION-STATUS.txt` for the current state.
Do not treat the earlier preparation notes below as the current Console state.

Google Play's public About this app confirms **1.1.0 is live**, updated
16 October. Earlier submission notes below are historical. A local draft pack
is ready in `promotion/2026-10-18/`; open `index.html` for its review board.
It proposes **PaperBuild: Photo to PDF**, revised listing copy, six layouts from
genuine app captures, a feature graphic, three social graphics, a 24-second silent
screenshot tour, captions, unsent pitches and a five-screen website tutorial.
The free 10-page limit and optional one-time paid Pro are explicit.

No UI was reconstructed and no AI marks were baked into the assets. No Console
AI declarations were inspected or changed. The website currently lacks a
PaperBuild app entry; the pack documents app/schema/privacy-link prerequisites.
No listing, website, app code, price, permissions or account settings were changed
by this preparation, and no post or message was sent. Browser layout/playback
checks remain manual; raster inspection, static HTML, asset and media checks
passed. See the pack's `QA.txt` and `verification.json`.

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

Version 1.1.0 (version code 4) has been committed and its signed App Bundle uploaded and validated. Device review covers camera capture, Gmail attachment hand-off, password protection, overlays, presets, compact export, merge/extraction and an eleven-page export. Submitted with owner approval on 16 October 2026: Play shows Changes in review, with automated quick checks still running; approval/publication is not yet confirmed. Managed publishing is off, so approval may publish automatically. Android 8/9 permission testing was explicitly skipped; Play payment/restore testing remains waived and unverified. See `RELEASE_CHECKLIST.md` for evidence and approval status. Pro options apply to image exports, merging, and extraction. Export passwords are 8–64 characters and are never written to drafts or presets; protected presets require a fresh password before exporting. Encrypted library entries do not generate previews. Forgotten export passwords cannot be recovered.

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
