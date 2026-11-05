# Kreativ Fancy Text for Android — Pro 1.1.0 submitted for review

> **Submitted — 4 November 2026:** Exact **1.1.0 (9)** is under **Changes in review**
> in Google Play. All 3 changes (production, English Pro description and rating)
> sent, together with the saved free reviewer-access instructions. Rollout is set
> to **100% across all 178 targeted countries**; managed publishing is off, so it
> publishes automatically after approval. Current approved production remains
> **1.0.2 (5)**. Hosted Pro policy is live and verified. Brazil rating is now 14+;
> other ratings remain Everyone/PEGI 3 or equivalent with In-app purchases.
> See [production record](store/review/PRODUCTION_1.1.0.md). Earlier entries below
> describe implementation/test history, not current submission status.

> **Current — 4 November 2026:** Signed **1.1.0 (9)** is installed on the phone.
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


> **Earlier code-7 implementation record — 4 November 2026:** Fancy Text Pro is implemented in signed
> **1.1.0 (7)**: 12 premium templates, custom solid/gradient backgrounds and frames,
> and 30 named style presets. The owner selected **€4.99 once, no subscription**.
> Google Play product `kreativ_pro_lifetime`, permanent purchase option `buy`, is
> active; all 33 EUR regions show €4.99, with localized prices elsewhere.
> Build 7 is available only on the internal test track; Alpha holds a saved build-7
> draft. **No Pro production submission.** Production **1.0.2 (5)** is confirmed
> published in Console at 100% rollout. The older submission notes below are history.
>
> All **37 model/browser tests**, **3 native billing unit tests**, and **20 release
> checks** pass. The exact signed APK passes Galaxy A55 free-editor regression,
> non-destructive locked Pro previews and 200% Android text checks. Live Play
> purchase/acknowledgement/restore and owned Pro phone features remain pending:
> Play currently returns product unavailable on the confirmed owner test account.
> The updated Pro privacy policy is prepared locally and must be published before
> production. See [Pro implementation and release status](store/review/PRO_1.1.0.md).

## Pro development

The six original themes, all 17 text styles, word styling, saved designs,
transparent PNG and 2× export remain free. Pro adds new design tools, with a
preview before applying and Undo/Redo afterwards. Presets save appearance,
signature and export options while preserving the next design's words.

Purchases and restore use Google Play Billing 9.1.0. Shipping Android Pro access
requires a signed Play purchase for this package/product; local Boolean flags
cannot unlock it. Signed receipt caching supports offline access, verified empty
ownership queries revoke it, and WorkManager retries acknowledgement. There is
no billing backend; local receipt verification does not provide the tamper
resistance of server-side validation.

Use `VITE_PRO_PREVIEW=1 npm run dev:pro` for an owned visual preview at
http://127.0.0.1:5187. This development-only mode cannot take payment and is
compiled out of the release. Normal web previews remain free. The public Play
licensing key is read at build time from
`~/.config/kreativ-android/billing-public-key.txt` or
`KREATIV_PLAY_BILLING_PUBLIC_KEY`; private upload credentials stay outside the repo.


> **Earlier 4 November 2026 submission:** Signed **1.0.2 (5)** passed Galaxy A55 / Android 16 tests,
> including native clipboard/sharing, PNG save/cancel, Undo/Redo, saved designs,
> cold-start persistence and Android 200% text in both orientations. Original
> designs and device settings were restored; temporary instrumentation removed.
> All 18 release checks pass with matching phone evidence. The exact bundle is
> uploaded and submitted for 100% production rollout in all 178 targeted
> countries/regions. Publishing overview confirms **Changes in review** while
> automated checks run; managed publishing is off. Approval/public 1.0.2
> availability remains pending. See `store/review/NATIVE_RELEASE_1.0.2.md` and
> `store/review/play-1.0.2-submitted-2026-11-04.png`.

> **1 November 2026:** Prepared signed 1.0.2 (5) to address Google's R8 recommendation:
> AGP 9.0.1, Gradle 9.1.0, `proguard-android-optimize.txt`, code minification and
> integrated resource shrinking. Explicit keep rules protect Capacitor's runtime
> annotations and callbacks. All 23 model/browser tests and 18 release checks
> pass; the local APK is 13.15% smaller. See `store/review/R8_1.0.2.md` for validation.
> At preparation time this was local only and phone testing was deferred.
> The 4 November test/submission status above supersedes that earlier state.

> **Listing refresh, 25 October:** Approved en-US short/full copy, five new
> screenshot layouts and feature graphic saved. Name/icon retained; AI labels
> verified off. Short-description promotional-wording warning addressed.
> Automated Send confirmations did not register at that time. On 4 November,
> Console showed no unpublished changes before the app update; the exact live
> listing content was not rechecked. Historical evidence:
> `promotion/2026-10-25/listing/SUBMISSION-STATUS.txt`. Other marketing remains draft.

> **25 October 2026:** Public Google Play listing and its About dialog confirm
> version **1.0.1** is live. Prepared a promotion/listing pack in
> `promotion/2026-10-25/`; open `index.html`. New graphics, copy, tutorial and
> outreach remain drafts. Corrected the existing code-rendered icon/feature AI
> declarations at the owner's request; saved **Don't label assets** was verified
> after reopening. See `promotion/2026-10-25/AI-LABEL-STATUS.txt`. No app code,
> release, pricing or permissions changed; 1.0.1 phone tests remain deferred.
> The production-submission notes below are historical.

> Project transfer: this app now belongs to Utility apps, in `kreativ-fancy-text/`.
> See [store/RELEASE_CHECKLIST.md](store/RELEASE_CHECKLIST.md) for Google
> Play preparation. Signed 1.0.1 (4) adds a compact mobile editor, quick actions,
> Undo/Redo, saved-design deletion recovery, accessible controls/text scaling,
> and an export guard for messages that cannot fit. See
> [store/review/POLISH_1.0.1.md](store/review/POLISH_1.0.1.md) for current validation.
> The [privacy policy](https://madebykreativ.com/privacy/kreativ-fancy-text/)
> is published; **1.0.1 (4) is submitted for worldwide production review** with
> updated screenshots. Google Play shows Changes in review while automated checks
> run. Public availability awaits Google approval. Phone checks remain deferred
> by the owner. See [store/PLAY_SETUP.md](store/PLAY_SETUP.md).
> [PROJECT-HANDOFF.md](PROJECT-HANDOFF.md) preserves the transfer context.

A standalone, offline typography studio built with Capacitor 8 and a local web
interface. It reuses the Unicode mappings from Kreativ Font Translator and has
its own editor, design model, canvas renderer, and Android project.

## Free editor

- Matching copyable Unicode text and image artwork in six coordinated themes.
- Compact mobile introduction and persistent Preview / Copy text / Save PNG actions.
- Undo/Redo for the complete design; continuous typing and slider changes group
  together. Deleted saved designs can be restored while the app remains open.
  Edit history and deletion recovery are session-only, not backups.
- Tap individual word chips to change their Unicode style and image color.
  Unchanged words retain their overrides when surrounding text is edited.
- 17 text styles; multiline input, preserved accents and emoji, up to 1,200
  UTF-16 code units. Unsupported letters remain in their original form.
- Square (1080 × 1080), portrait (1080 × 1350), and story (1080 × 1920) PNGs.
  Standard or 2× resolution, optional transparent background, text size,
  alignment, and optional signature. Long text shrinks to fit the canvas; image
  export is blocked with an explanation if the message still cannot fit. Text
  remains copyable, and a taller format may make image export possible.
- Preview and PNG export use the same canvas renderer.
- Save up to 50 full designs locally, reopen them, and remove them. The current
  draft also restores after restarting the app. No cloud synchronization.
- Android clipboard, native text/image share sheets, and Save PNG through the
  system file picker. The user chooses the destination; no storage permission.
- Bundled fonts and assets. No server, WordPress, account, ads, analytics, keyboard,
  accessibility service, or internet permission in the Android application.

Text colors and visual artwork are part of PNGs. Copying text preserves Unicode
characters only. Their appearance in another app depends on that app's fonts.
The image preview shows the bundled font; the exported image preserves it.

## Earlier debug test version

The phone now has signed Pro test build 1.1.0 (7); its free-editor regression passes. The debug artifact below is
historical and cannot update that installation because the signing keys differ.
Preserve saved designs before any change that requires uninstalling the app.

`artifacts/kreativ-0.1.1-debug.apk` is a debug-signed, installable test build.
Transfer it to an Android device and open it to install. Android may require
allowing installation from the file manager used to open it.

Android 7.0+ (API 24) with an updated Android System WebView is the configured
minimum. The bundled web build targets Chrome/WebView 111 or newer. Native checks passed on a Samsung Galaxy A55 (SM-A556B) running Android 16:
text/image share previews and cancellation, standard and transparent high-resolution
PNG saving, save cancellation, and native draft/favorite restoration after restart.
Other physical devices and older WebView versions remain untested. Target SDK is 36.

For an emulator or a device you explicitly select:

```sh
adb -s YOUR_DEVICE_SERIAL install -r artifacts/kreativ-0.1.1-debug.apk
```

Do not use this debug build as a Play Store release. A production release needs
an owner-controlled signing key, release configuration, store artwork/listing,
and device testing. App ID: `com.kreativfont.studio`; version code: `2`.

## Develop and build

Requirements: Node.js 22.12+ (or a supported newer Node version), Android Studio,
Java 21, Android SDK Platform 36, and Android SDK build tools. Gradle downloads
its pinned distribution and build dependencies on the first build. Building
needs internet. Editing/export work offline; purchases and restore need Google Play connectivity.

The native build pins AGP 9.0.1 and Gradle 9.1.0. Capacitor 8's Kotlin plugin
temporarily requires `android.builtInKotlin=false` and `android.newDsl=false`;
remove these compatibility settings after migrating the plugins before AGP 10.
R8 full mode and integrated resource shrinking retain AGP 9's enabled defaults.

Run commands from this `kreativ-fancy-text` directory:

```sh
npm ci
npm run dev
npm run build
npm test
npm run apk
```

The web preview is at http://127.0.0.1:5174. `npm run apk` builds the web assets,
syncs Capacitor, builds `:app:assembleDebug`, and copies the APK and SHA-256 file
into `artifacts/`. It uses `JAVA_HOME` and `ANDROID_HOME` / `ANDROID_SDK_ROOT` if
set, or the standard macOS Android Studio runtime and SDK paths. Local paths
are written only to ignored `android/local.properties`.

Open the existing `android/` directory in Android Studio, or use:

```sh
npx cap open android
```

After editing web sources, run `npm run sync` before building in Android Studio.
The native `ExportFilePlugin.java` implements permission-free file saving through
`ACTION_CREATE_DOCUMENT`. PNG data is staged in the private cache first; only a
small file URI enters the pending activity call, avoiding Android saved-state
transaction limits for high-resolution exports. `file_paths.xml` exposes only the image-share cache.
Android cloud backup is disabled so saved drafts remain local to the install.

## Tests and evidence

For the signed release candidate:

```sh
npm run build
npm test
npm run store:assets
npm run bundle:release
npm run check:release
```

The existing upload key is read from the external private configuration described
in the handoff; do not regenerate it. Signed artifacts, the shrinking map, lint
reports, effective R8 configuration, checksums, and verification report are in `artifacts/play/`. Store copy,
five actual mobile-interface browser screenshots, an icon, a feature graphic,
and the policy HTML are in `store/`. The verifier checks signatures, packaged
assets/configuration, packaged AGP version, effective R8 optimisation settings,
DEX runtime plugin annotations, permissions, backup rules, and store formats. It does not
replace testing the shrunk release on Android. Previous 0.1.1 phone evidence
remains applicable to that earlier debug build only.

The exact signed 1.0.0 APK passed native checks on Samsung Galaxy A55 / Android 16
with WebView 155.0.8059.46. Clipboard, share previews/cancellation, opaque and
transparent high-resolution saves, save cancellation/cache cleanup, persistence,
saved-design restoration, privacy, and portrait/landscape checks passed. The
original draft/favorite were backed up and restored; the temporary same-signer
instrumentation was removed. See `store/review/NATIVE_RELEASE_1.0.0.md` and
`artifacts/phone-release-1.0.0/`. Other devices and accessibility font scaling
remain untested.

`npm test` runs nine model/history checks and 28 Playwright tests in desktop Chromium and a
Pixel 7 browser layout. They cover mixed word styles, edit preservation,
favorites save/reload/restore/delete, decoded PNG dimensions and transparency,
actual colored pixels, offline interactions, input safety, long-text fitting,
empty-state controls, privacy information, Undo/Redo, deletion recovery, extreme
line breaks, 48px control targets, 200% text layouts and axe accessibility scans.

- `artifacts/browser-results.json`: structured browser results.
- `artifacts/screenshots/`: editor screenshots and transparent export samples.
- `artifacts/build.log`: Android build output (when captured).
- `artifacts/VALIDATION.md`: completed native checks and practical limits.

The Android emulator verification uses a debug WebView connection. Native share
checks open and cancel the system chooser; they do not send to another person.
Physical-phone PNG saves were copied back and decoded at 1080 × 1080 and
2160 × 3840, including custom word colors and alpha transparency.

## Source map

- `src/model.js`: design state, word overrides, favorites validation.
- `src/unicode.js`: mappings carried over from the original generator.
- `src/render.js`: bundled font loading, wrapping, canvas preview and PNG export.
- `src/main.js`, `src/style.css`: mobile editor, collection, and responsive UI.
- `src/pro-model.js`, `src/pro-ui.js`, `src/pro.css`: Pro templates, backgrounds and presets.
- `src/billing.js`: Google Play bridge and development-only visual preview.
- `src/platform.js`: Capacitor preferences, clipboard, file saving, and sharing;
  browser fallbacks make local preview/test possible.
- `android/app/src/main/java/com/kreativfont/studio/`: Android entry point and file
  export plugin, signed purchase validation and durable acknowledgement.
- `public/PRIVACY.txt`, `public/licenses/`: bundled privacy and font/runtime notices.

The font files use their bundled SIL Open Font License notices. Capacitor uses
its bundled MIT notice. Fontsource packages provide the local font assets.

Dependency audit at creation found no production dependency advisories. The
Capacitor CLI's iOS-only xcode → uuid development dependency chain has a moderate
advisory; it is not included in the Android APK. Recheck with `npm audit` when
upgrading tooling.
