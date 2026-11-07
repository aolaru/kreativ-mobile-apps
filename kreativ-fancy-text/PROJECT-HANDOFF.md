# Kreativ Fancy Text Android app — project handoff

> **Approved and published — verified 7 November 2026:** Google Play Console shows
> production **Active**, **Latest release: 1.1.0 (9)**, and **178 countries/regions**.
> Publishing overview has **no unpublished changes**. The 4 November submission
> is approved and the update is live; no further upload or review submission is
> needed. This is the verification date, not a newly established publication date.
> The release includes R8 optimisation, resource shrinking and AGP **9.0.1**.
> App remains free with optional **Fancy Text Pro**, **€4.99 once** (localised prices).
> Google approval does not add completed-purchase or owned-tool test evidence;
> those native validation limits remain recorded in the production report.
> See [production record](store/review/PRODUCTION_1.1.0.md). Entries below are history.

> **Earlier billing test — 4 November 2026:** Signed **1.1.0 (9)** is installed on the phone.
> Google Play now returns Fancy Text Pro / `buy` at **26.99 RON**; the test checkout
> opens and cancellation is verified. No purchase was completed. Further tests
> stopped at the owner's request; paid unlock/acknowledgement/owned tools remain
> unvalidated. Fixed initial billing setup and purchase/restore message handling;
> an older client now also finds the product, so the setup bug alone did not cause
> the earlier PRODUCT_NOT_FOUND. Catalogue/cache recovery is the likely explanation,
> not a confirmed Google diagnosis. Designs/settings restored; helper removed.
> Internal remains **7**, production **1.0.2 (5)**. No new upload/publication.
> See [billing investigation and result](store/review/BILLING_FIX_1.1.0.md).

> **Earlier code-8 phone retest — 4 November 2026, 09:44 UTC:** Signed **1.1.0 (8)** is now
> installed on the Galaxy A55. The header Pro button is gone and the upgrade is
> named **Fancy Text Pro**. All 12 template previews, solid/gradient previews,
> purchase locks, native copy and Undo/Redo pass focused phone checks. Restore
> reports **no completed Pro purchase**; Play still returns the product unavailable.
> Checkout, acknowledgement and owned native tools remain unvalidated. Original
> designs and phone settings were restored; the temporary helper was removed.
> Internal testing remains **7**, production remains **1.0.2 (5)**, and build 8 has
> not been uploaded. See [phone retest](store/review/PRO_RETEST_1.1.0.md).

> **Earlier name change — 4 November 2026:** The upgrade is now **Fancy Text Pro**.
> Removed its header button; upgrade information and Restore purchase are
> accessible within **Make it yours**. The Google Play product's checkout name
> is saved as Fancy Text Pro, with its original product ID and €4.99 price.
> Signed candidate **1.1.0 (8)** contains this change and is now installed on the
> phone, with focused smoke checks recorded above. Internal testing remains **7**.
> No new upload or production submission was made.
> See [name-change validation](store/review/NAME_CHANGE_1.1.0.md).
> Earlier code-7 artifact hashes below refer to the preserved archive.


Moved on 24 October 2026 from the Fancy Text Generator website/plugin project
into the Utility apps project, alongside the other Android utilities.

## Earlier Pro implementation — 4 November 2026

The owner requested premium templates, custom backgrounds and saved style presets
with one lifetime Pro purchase and explicitly selected **€4.99 once**. Implemented
signed **1.1.0 (7)**. See `store/review/PRO_1.1.0.md` for the earlier implementation status,
exact artifacts/hashes, validation coverage and remaining launch tasks. This
section is historical; the verified publication status at the top takes precedence.

- Production **1.0.2 (5)** is now verified published in Console at **100% rollout**;
  evidence `store/review/play-1.0.2-published-2026-11-04.{png,txt}`. No Pro production
  upload/submission or public rollout was performed.
- Internal track `4701453847955130384`, release 1: build **7**, name
  `1.1.0 (7) — Pro purchase tests`, available to internal testers. Replaced unused
  1.0.0 draft (old draft text archived). Owner's single-account license-test list
  selected for internal access; owner-approved invitation accepted and enrollment verified.
- Alpha `4700669451162278416`, release 1 remains **draft**, now corrected to build
  **7**, not rolled out. No closed testers chosen. Initial uploaded build **6**
  crashed at startup because AGP 9 strict R8 removed the old Room database's
  reflected no-arg constructor. A targeted constructor keep rule fixes it in 7.
  Bad 6 is archived in `artifacts/archive/pro-code6-startup-failure/`; NEVER release it.
- Product **`kreativ_pro_lifetime`**, permanent **`buy`** option is **Active** in
  **174 billing regions**. All 33 EUR rows verified **EUR 4.99**, including tax
  where applicable. Local examples: RON 26.99, USD 4.69, GBP 4.19. No recurring plan,
  rental, discount offer or real payment. Price is always loaded from Play in UI.
- Owner confirmed phone Google Play account `andrei.olaru@gmail.com`, already in
  selected Console license-test list. Native SDK still reports product unavailable;
  live checkout/ack/restore/offline paid access are **not validated**. An unminified
  diagnostic confirms SDK query success but product status 3 PRODUCT_NOT_FOUND;
  propagation is a possible cause. Diagnostic removed; exact signed 7 APK restored,
  original designs hash-verified, settings restored and helper removed again. Do not fake
  ownership or use real payment. Only Google's explicitly no-charge test card.
- New Pro policy matches bundled/local HTML but is **not published** at the hosted
  URL yet. Data safety/app access and listing need Pro review before production.
- 9 model tests +28 browser tests, 3 billing JVM test methods and 20 release checks
  pass. Owned UI is tested in development preview only; release has no dev unlock.
  Signed code 7 free/native checks pass on Galaxy A55/API36, including clipboard,
  both shares cancelled, two PNG saves/cancellation, persistence,200% text/rotation,
  locked Pro previews. Native results explicitly distinguish pending billing tests.
- Fresh pre-Pro private APK/preferences backup:
  `.local/phone-backups/20261104T081415Z/`. Restore only at cold-start boundary;
  never overwrite original preferences while a fixture page is live. Preserve the
  existing upload key and public Play licensing key in `~/.config/kreativ-android/`.
- Local current state is under `proDevelopment` and `localCandidate` in
  `store/play-console-state.json`; root release/upload fields describe production 5.
  The release verifier separately reports internal upload and native billing status.

Next: finish no-charge billing/owned native tests, publish/reverify updated policy,
review app-content/listing, then prepare a production update when requested.
Do not reuse earlier free-only runtime evidence as proof of a paid purchase.

## Earlier 1.0.2 submission — 4 November 2026

The owner resumed phone testing and explicitly requested publication afterwards.
The exact signed **1.0.2 (5)** APK passed native tests on the connected Galaxy A55,
Android 16 / API 36, WebView 156.0.8078.47. Tested native clipboard, text/image share
previews and cancellation, opaque 1080×1080 and transparent 2160×3840 PNG saves,
save cancellation/cache cleanup, Undo/Redo, saved deletion recovery/reopening,
policy/version, normal cold launch/persistence and 200% Android text in portrait
and landscape. No target crashes were observed. Original preferences were restored
and hash-matched, phone settings restored, generated exports/cache removed and
the temporary helper uninstalled. 1.0.2 is left installed/open. Fresh private
backup: `.local/phone-backups/20261104T072357Z/`. See
`store/review/NATIVE_RELEASE_1.0.2.md`; TalkBack and older devices remain untested.

All 18 release checks pass with matching physical-device evidence. Uploaded the
existing AAB (SHA-256 `429065480b63d8e353cec9ba666f75d54caa88bf16f621390c283996c2651030`)
to production release **2**, name **1.0.2 (5)**, with size-optimisation release notes.
Play accepted version 5, API 24+/target 36 and ReTrace mapping, showed Ready to
release with no listed release errors/warnings and no loss of supported devices.
Saved 100% rollout in all targeted countries (existing 178 destinations).
Google estimates new installs at 1.08 MB, 159 KB less than the prior release,
and update download at 454 KB.

**One production change was sent for review and confirmed as Changes in review.**
Automated quick checks are running before forwarding to review; managed
publishing is off. Public 1.0.2 availability/approval and clearing of the historical
R8 recommendation are not yet verified. Last verified live version is 1.0.1.
Evidence: `store/review/play-1.0.2-submitted-2026-11-04.{png,txt}` and
`store/play-console-state.json`. No unpublished changes were shown before this
update; the older listing-refresh record's content/submission was not rechecked.

## Local R8 preparation — 1 November 2026 (historical)

The owner reported Google's recommendation to improve R8 optimisation/resource
shrinking and use AGP 9+. Local candidate **1.0.2 (5)** upgrades AGP 8.13.0 to
9.0.1 and Gradle 8.14.3 to 9.1.0, replaces `proguard-android.txt` (which disables
optimisation) with `proguard-android-optimize.txt`, and retains minification and
resource shrinking. Capacitor 8.5.3 is retained with the documented legacy DSL
and external Kotlin compatibility settings. Runtime annotation types/defaults
are kept explicitly for the reflected Capacitor bridge and plugin callbacks.

The signed APK/AAB build, release lint and Gradle unit-test task pass. All five
model/history tests, 18 browser tests and 18 release verification checks pass.
Packaged AAB metadata records AGP 9.0.1; effective R8 rules have no global
optimisation/shrinking/obfuscation disable flags, and packaged DEX preserves the
plugin registration/method/callback annotations and method defaults. APK size is
down 13.15%, DEX bytes 30.57%, resource-table bytes 15.59% compared with 1.0.1.

See `store/review/R8_1.0.2.md` and `artifacts/play/verification.json` for actual
build evidence. The 1.0.1 binaries, reports, configuration and graphics were
preserved in `artifacts/archive/pre-r8-1.0.1-20261101T190739Z/`.
Do not upload a bundle unless verification passes; native testing of the exact
optimised APK remains pending. Respect the owner's earlier request to defer
phone testing; no device interaction is authorised by this build update.

Last observed public version: **1.0.1 live on 25 October 2026**, with a separate
listing refresh saved but submission unconfirmed. The 1.0.2 candidate is local
only. Console upload/submission fields still describe 1.0.1; do not mark them as
1.0.2 without a real upload. Older review-pending notes below are historical.

## Resumed in Utility apps — 24 October 2026

### Current submission: 1.0.1 (4)

The owner approved the recommended pre-launch improvements. Implemented a compact
mobile editor and persistent Preview/Copy/Save actions; complete-design Undo/Redo
with input grouping and keyboard shortcuts; session-only deletion recovery for
saved designs; larger controls, relative text sizing, stronger label contrast,
Android system font-size support, and reduced-motion navigation. PNG export is
now blocked with a clear warning when extreme line breaks cannot fit the canvas,
while text can still be copied. Empty-line layout work is also reduced.

Signed 1.0.1 APK/AAB and refreshed store assets are prepared locally. The new APK
installed over 1.0.0 using the existing key, preserving both native preference
values exactly. A fresh private backup is in `.local/phone-backups/20261024T150609Z/`.
Phone checks were interrupted by PhotoFeather taking the foreground. Original
designs were restored and hash-verified, and temporary instrumentation removed.
The new build remains installed. The owner explicitly chose to finish the code and
defer phone testing until later. Resume device tests only when requested;
see `store/review/POLISH_1.0.1.md`. Do not reuse 1.0.0 runtime evidence for 1.0.1.

The owner subsequently asked to publish and explicitly selected all available
countries/regions. **1.0.1 (4) is uploaded and submitted for production review**,
with 177 named country/region entries plus Rest of world (178 total), refreshed
screenshots in order 01–05, and the missing Advertising ID declaration saved as No.
Publishing overview confirms **Changes in review** for 11 changes; automated quick
checks are still running and Google will forward the submission when they pass.
Managed publishing is off. Public availability has not been verified; Google
review/approval is pending. Phone testing remains deferred by the owner; do not
mark 1.0.1 as fully phone-tested. The older 1.0.0 internal draft is untouched.
See `store/play-console-state.json` and `store/review/play-production-submitted.jpg`.
Preserve original artifacts; archived reports/graphics are in `artifacts/archive/`.

### Earlier 1.0.0 preparation and validation

The paused work below records the state at transfer time. Google Play preparation
has now resumed: the two planned scripts are implemented, listing/graphics and
policy sources are prepared, and upload-signed 1.0.0 artifacts are built locally.
The full in-app policy and version display are updated, with explicit modern
Android backup/transfer exclusions. See `store/RELEASE_CHECKLIST.md` and
`artifacts/play/verification.json` for current evidence and remaining steps.
The privacy policy is now published and publicly verified at
`https://madebykreativ.com/privacy/kreativ-fancy-text/` (website commit `ebc7c57`,
successful deployment run `40087390720`). The signed bundle is uploaded to an
internal-testing draft; no rollout or app-review submission has occurred.
The original upload key and credentials remain untouched at their original paths.
The owner confirmed the Google Play developer account is personal and was created
on or before 13 November 2023. The closed-test requirement specifically for newer
personal accounts (12 testers continuously for 14 days) therefore does not apply.
Check the account's actual Play Console status before submission.

Signed 1.0.0 (3) has now passed physical Galaxy A55 / Android 16 checks using
the exact verified release APK. With the owner's explicit approval, debug 0.1.1
was backed up and replaced; its original draft and favorite were restored and
verified by matching preference hashes. The signed release is left installed
and open. Temporary instrumentation and generated phone test exports were
removed. See `store/review/NATIVE_RELEASE_1.0.0.md` and
`artifacts/phone-release-1.0.0/native-results.json`. Remaining coverage includes
other devices/older WebViews and accessibility font scaling.

Google Play app `4976006210494663211` is created for `com.kreativfont.studio`.
All 11 initial setup tasks are complete: Free/en-US listing and assets, published
privacy URL, unrestricted access, no ads, Art & design category, support contact,
non-government/no-financial/no-health declarations, target audience matching
PaperBuild (13–15, 16–17, 18+), completed IARC ratings, and Data safety. Owner
explicitly approved the creation declarations and IARC terms, and confirmed a
secure signing backup. The 1.0.0 (3) signed bundle and notes are saved in internal
release draft 1, track `4701453847955130384`. No testers are selected, so its only
warning is that no users can access it. No release was rolled out or sent for
app review. Publishing overview lists unsubmitted changes; review submission
remains gated on the remaining release workflow. See `store/PLAY_SETUP.md` and
`store/play-console-state.json` for exact state before resuming.

## Location and identity

- App directory: `kreativ-fancy-text/` within Utility apps.
- Android Studio project: `kreativ-fancy-text/android/`.
- App ID: `com.kreativfont.studio` (preserved).
- Installed build: upload-signed 1.1.0, version code 7. Free-editor native regression
  passes; real Play billing and owned Pro native flows remain pending. Production
  is 1.0.2 (5), confirmed published in Console.
- Existing `kreativfont/` is a separate type-discovery app and is not this app.

## Tested features

Matching Unicode text and image designs, word-specific styles/colors, six themes,
PNG export with three sizes / 2× resolution / transparent backgrounds, offline
operation, local favorites, native sharing, and Android file saving.

0.1.1 was installed and tested on a Samsung Galaxy A55 running Android 16. Tests
covered native text/image share-sheet previews and cancellation, PNG saves at
1080 × 1080 and transparent 2160 × 3840, temporary-file cleanup, and draft/favorite
restoration after restart. The large-PNG saved-state crash was fixed in 0.1.1.
See `artifacts/VALIDATION.md` and `artifacts/phone/` for the evidence.

## Google Play preparation — state when moved (historical)

The user stopped publishing preparation to relocate the project. Nothing has
been submitted to Google Play.

- The user confirmed this will be a new Play app with no prior upload key.
- Approved public support email: info@madebykreativ.com.
- At transfer time the developer account type was unknown; subsequently confirmed
  as personal (see the resumed-work section above).
- Source metadata was advanced to draft 1.0.0, version code 3. It is not a tested
  release or a completed Play-ready bundle.
- Release signing, minification, resource shrinking, and the release build script
  have been drafted but not run or validated.
- `npm run check:release` and `npm run store:assets` currently point to planned
  scripts that have not been implemented. The store subdirectories are placeholders.
- Full privacy policy, listing copy, graphics, release verification, and signed
  AAB generation remain to be completed before any Play submission.

## Upload key — preserved outside this repository

A new RSA-4096 upload key was generated on 24 October 2026. It is not the debug
key or a Play-managed app-signing key. Its location did not change during the move:

- Keystore: `/Users/andreiolaru/.config/kreativ-android/upload-keystore.p12`
- Private configuration: `/Users/andreiolaru/.config/kreativ-android/signing.json`

Both files are owner-readable/writable only. Do not commit them, print the
passwords, or regenerate/replace this key when resuming work. The release script
reads that external configuration. Securely back up the keystore and credentials
before the first Play upload. No public certificate has yet been exported.

## Working commands

Run from the app directory:

```sh
npm run build
npm test
npm run preview
```

Preview URL: http://127.0.0.1:5174/ . Debug builds use `npm run apk`.
The earlier debug file remains `artifacts/kreativ-0.1.1-debug.apk` for historical
reference. The tested signed release is
`artifacts/play/kreativ-1.0.0-upload-signed-release.apk`; the matching Play upload
candidate is `artifacts/play/kreativ-1.0.0.aab`. Rebuilt binaries require fresh
validation; the verifier accepts native evidence only for the tested APK hash.

WordPress/plugin sources, page assets, and the Chrome extension remain in the
original Kreativ Fancy Text Generator project. This Android app has its own npm
dependencies and Unicode mappings and does not require that project to build.
