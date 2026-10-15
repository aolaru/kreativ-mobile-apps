# ShareSafe store-submission checklist

## Play update 1.2.0 / code 4 — October 15, 2026

Production release `1.2.0 – Privacy Check & Face Assist` is **submitted** in Play
Console at 100% of the existing 178 targeted countries/regions. Console confirms
**Changes in review**, with automated quick checks running (up to 14 minutes
remaining at confirmation). The production bundle, full description, refined
store icon and Data safety declaration were sent together. The existing private
reviewer instructions were also updated with Face Assist guidance, preserving
their reusable code and complimentary access. Existing screenshots and feature
graphic are unchanged. Managed publishing is **off**: publication is automatic
after approval. Version 1.1.0 / code 3 remains live until that happens. No price,
product, signing-key, package-name or country-scope change is included.

- Fresh signed AAB SHA-256:
  `dba354ddbb601078d9b2f0d8fb1da6c9a5b12447ff329e27870e9e5eb82398f2`.
- Verified version 1.2.0 / code 4, target SDK 36 and the existing upload certificate.
- All 8 release unit tests pass, with no skipped tests; release lint has 0 errors
  and 16 warnings. All 32 debug instrumentation tests pass on the Galaxy A55
  after the version bump (16.642 seconds). The live app package was not replaced;
  these tests use `com.kreativ.sharesafe.debug`.
- Native LOAD/ZIP alignment checks and APK `zipalign -c -P 16 4` pass. The two
  SDK RELRO boundary warnings remain; **16 KB runtime testing is incomplete**.
- Play's release validation reports no blocking errors. Its two warnings are
  missing deobfuscation mapping (this build is not obfuscated) and missing native
  debug symbols for the prebuilt ML Kit library.

Release decision: after being told the 16 KB runtime test could not be completed
on this Mac, the user requested publication. We reiterated that compatibility
risk before preparing this release. This is authorization to proceed with the
known limitation, not a passing runtime test. The earlier end-to-end payment
test waiver also remains in effect. Do not mark either unverified check complete.

## Unreleased: Privacy Check + Face Assist

Free Privacy Check and the existing Play Pro unlock's Face Assist/area editing
are implemented in source. Do not treat the older test results below as validation
of these changes. No new SKU or price change is required.

Development checks (October 15, 2026): Play debug app and instrumentation APKs
build; all test sources compile. All 8 unit tests pass (0 skipped); Play debug
lint has 0 errors / 19 warnings. Amazon shared app/test sources and Samsung app
sources also compile without the face SDK. All **30 device tests pass** on the
unlocked Galaxy A55 (SM-A556B, Android 16 / API 36.1, October 15, 2026), with the
private reviewer argument supplied and no skipped tests. This includes the real
bundled detector on Google's public group-photo fixture, Privacy Check before/after
and failure states, drag/resize/numeric editing, undo/redo, state recovery and
existing export/share regressions. The first run's numeric-edit test clicked Save
before Android delivered its queued Dialog OnShow callback; adding a main-loop
idle boundary fixed the test. The isolated interaction test and the entire suite
then passed; app behavior was not bypassed or relaxed.

The updated debug app and test APK are installed separately from the live app.
The A55 reports a 4096-byte memory page size, so this pass is **not** 16 KB runtime
validation. End-to-end payment testing remains waived/unverified.

Native check: the bundled detector's arm64/x86-64 ELF LOAD alignment and APK ZIP
offsets pass 16 KB checks. The stricter RELRO-end-boundary diagnostic warns for
the SDK. Google lists `face-detection:16.1.7` as 16 KB-compatible, but that is not
a runtime test of this app. Resolve/validate the warning on a 16 KB runtime before
declaring the signed release compatible. Do not patch the proprietary SDK binary
or suppress this warning as a substitute for testing. Run:
`python3 scripts/check-native-alignment.py path/to/app.apk --strict-relro`.
See [Android's checks](https://developer.android.com/guide/practices/page-sizes)
and [Google's SDK release notes](https://developers.google.com/ml-kit/release-notes).

### 16 KB release-artifact audit (October 15, 2026)

**Static packaging checks pass; 16 KB runtime testing remains incomplete.**
Do not mark the runtime checklist item complete or publish on this result alone.

- Built a fresh signed Play AAB and release APK from the current source, plus
  release-variant instrumentation (temporary Gradle init sets `testBuildType` to
  `release`; the normal project configuration still defaults to debug tests).
- `bundletool 1.18.3 validate` passes. `dump config` reports uncompressed native
  libraries enabled with **PAGE_ALIGNMENT_16K**. The tool was fetched from
  Google's official GitHub release and its published SHA-256 digest verified.
- Both 64-bit ML Kit JNI libraries (arm64-v8a / x86_64) pass ELF LOAD alignment in
  the signed AAB and APK. `zipalign -c -P 16 4` passes for the signed release APK
  and the universal APK generated from the AAB by bundletool.
- The same two RELRO-end-boundary diagnostics remain. No SDK binary was patched,
  no warning was suppressed, and no runtime compatibility pass is claimed.
- The release instrumentation APK and release app have matching upload signing
  certificate SHA-256 `9D:64:E7:84:F2:8E:3F:24:86:86:1B:B0:44:13:B1:BB:14:79:80:51:20:4A:05:09:47:C8:6B:78:03:A7:F2:B5`.

Artifact SHA-256:

- AAB: `901e12ebe6706286b7788287b1d63eb875cdf32e3761dcfbf0fbf8a08027f7d2`
- Release APK: `098f1c9cd504726a77628f4ba77ef4cadf8ab812da7cc2f353451fae44e70483`

These are **local test candidates still carrying code 3 / version 1.1.0**, not a
new Play release; increment version/code and regenerate before any upload.

Runtime environment limitation: only a 4 KB API 36.1 system image is installed.
The official API 35 16 KB ARM64 image expands to about 4.04 GB before emulator
data. The Mac had about 3.5 GB available, and the user cannot free more space.
A small-download fallback reused the installed system image with Google's API
36.1 16 KB kernel/ramdisk/vendor files in a separate temporary AVD. ZIP entries
were length/CRC-verified. That mixed-image environment did not become usable
(kernel-module version mismatches); its initial userdata creation also requested
7.37 GB. During the last isolated boot attempt the disk-space guard stopped the
emulator as host free space dropped below 1 GiB. It never provided a usable
`getconf PAGE_SIZE = 16384` Android runtime, and no ShareSafe tests ran on it.
This is an environment failure, not evidence of a ShareSafe runtime defect.

The user's existing AVD and USB phone were not changed. The Galaxy A55 still
reports 4096-byte pages; its installed non-debug app remains code 2 with the same
October 11 update time. No release was uploaded, submitted or published.
The temporary AVD, downloaded boot/tool files and duplicate generated APKs were
removed after the attempt; signed build outputs remain in `app/build/outputs`.
The next runtime test requires another machine, external storage, or a 16 KB
remote device such as [Samsung Remote Test Lab](https://developer.samsung.com/remote-test-lab/blog/en/2025/07/07/optimize-your-applications-for-16-kb-page-size-compatibility-using-samsungs-remote-test-lab).

- [x] Build and run `PrivacyCheckTest`, `ProControlsTest`, the real bundled-model
  integration test, and the existing regression suite on a connected device.
- [ ] Verify manual drawing, area selection/movement/resize, accessible area choice
  and numeric editing, undo/redo, rotation/process restoration, face suggestion
  review/dismissal, empty/error results, flattened export, and unchanged originals.
- [ ] Check face detection using group photos, small/sideways/occluded faces;
  detection must never be described as guaranteed anonymisation.
- [ ] Verify release native libraries support 16 KB memory pages (ML Kit adds JNI).
- [x] Publish revised privacy policy before submitting (October 15, 2026; deployment verified below). Included disclosure:
  "Face Assist uses a bundled Google ML Kit model. Images and detection results
  are processed on-device and are not sent to Google. The SDK collects device/app
  information, per-installation identifiers, performance/error metrics and API
  usage/configuration information for diagnostics and usage analytics. It may
  contact Google for maintenance information. Face detection does not identify
  people and can miss faces. Users must review suggestions and apply covers."
- [x] Prepare and save Play Data safety against the merged manifest and ML Kit's
  actual SDK disclosure; do not retain an unqualified 'no data collected' claim.
  See [Google's SDK disclosure](https://developers.google.com/ml-kit/android-data-disclosure)
  and [ML Kit privacy](https://developers.google.com/ml-kit/terms). Saved in Publishing
  overview, not yet submitted for review; include it with the updated bundle.
- [ ] Keep image processing local; no location permission, broad gallery access,
  account, new subscription or photo-upload endpoint is introduced.
- [ ] Update listing/screenshots and the Pro reviewer instructions to cover the
  new tools, increment versionCode, and generate a new signed release.

The initial feature implementation did not change the website or Play Console;
the privacy-disclosure follow-up below records those separate changes.
The older payment-test waiver remains a limitation, not a passing payment test.

### Privacy disclosures for the upcoming update (October 15, 2026)

Website source: `aolaru/madebykreativ.com`, commit `6716b59`, updates the existing
`https://madebykreativ.com/privacy/sharesafe/` page for free Privacy Check, Pro
Face Assist and manual editing, SDK diagnostics, local retention/deletion limits,
and user-selected external links. It explicitly distinguishes the upcoming Play
edition from old releases and Amazon/Samsung builds without ML Kit. The obsolete
no-analytics/no-face-detection claims are removed. Website build, HTML, local-link
and artifact checks pass. GitHub's full build/accessibility/responsive quality gate
and Pages deployment both succeeded in [run 38794920555](https://github.com/aolaru/madebykreativ.com/actions/runs/38794920555).
The live policy was opened in Chrome and verified to show October 15, 2026,
Privacy Check, Face Assist, the ML Kit diagnostics section, retention limits and
external-link handling. The canonical policy URL remains unchanged.

Play Data safety is **saved, not submitted for review or published**. Publishing
overview shows exactly one pending change: Data safety. Leave it pending until
the updated bundle and final review are ready. Existing production code 3 is
unchanged; no new release was uploaded in this step.

The saved declarations cover:

| Item | Saved answer / reason |
| --- | --- |
| Collected data | App interactions (SDK feature events), Diagnostics (technical/performance/configuration metrics), Device or other IDs (per-installation identifiers). |
| Purposes, all three types | Analytics; Fraud prevention, security and compliance. The latter reflects ML Kit's stated misuse/abuse-detection use. No advertising, personalisation or developer communications. |
| Required / ephemeral | Required, not ephemeral. There is no in-app SDK-metrics opt-out and no evidence of memory-only server processing. Optional Pro access is not treated as an SDK collection opt-in. |
| Sharing | Collected only, following Google's ML Kit disclosure of no onward third-party transfer. User-selected Android sharing is a user-initiated transfer, not background photo collection. |
| Encryption | Yes; ML Kit documents HTTPS for the disclosed SDK data. |
| Accounts | No in-app account creation or external-account sign-in. Reviewer access is a local code, not an account; Google Play purchases use Google's services. |
| Data deletion request | No. Local Android clear-data/uninstall controls do not establish a way to delete Google's server-side SDK metrics; no automatic 90-day deletion claim. |
| Photos / location / financial data | No photo content, face outputs, EXIF values or location sent to a developer server. Payments are handled by Google Play, receipts are local and purchase confirmation uses Google Play services; no card data is received by ShareSafe. |

Source guidance checked: [ML Kit Android data disclosure](https://developers.google.com/ml-kit/android-data-disclosure),
[ML Kit Terms & Privacy](https://developers.google.com/ml-kit/terms), and
[Play Data safety definitions](https://support.google.com/googleplay/android-developer/answer/10787469?hl=en).
The public privacy-policy URL remains unchanged in Play Console. The saved preview
shows all three collected-data types, encryption, no sharing, and no deletion
request mechanism; no unsupported security badge is claimed.

### Outdated Fragment notification (October 15, 2026)

Play Console's October 13 notice identifies `androidx.fragment:fragment:1.1.0`
in live version 1.1.0 / code 3. It is an outdated-library advisory, not a target
API warning; no enforcement deadline is shown in that notification.

The Play-only dependency constraint now selects Fragment **1.9.1**, the current
[stable release](https://developer.android.com/jetpack/androidx/releases/fragment).
Google SDKs still request older versions, but Gradle resolves those requests to
1.9.1. Both release dependency inspection and the built debug APK's
`META-INF/androidx.fragment_fragment.version` confirm the replacement. Amazon's
release runtime still has no Fragment dependency; other stores are unchanged.

Post-upgrade checks: Play app and instrumentation APKs build, release Java
compilation and AAR compatibility checks pass, and all 8 unit tests pass with
zero skipped. Play debug lint has 0 errors / 20 warnings: the prior 19 plus a
Lifecycle 2.10.0 custom-lint registry requiring newer lint than AGP 8.6.1 supplies.
That registry's checks are not validated by this lint run; no warning was
suppressed. The native LOAD/ZIP checks still pass with the same two ML Kit RELRO
diagnostics described above; they do not establish 16 KB runtime compatibility.

After reconnecting the unlocked Galaxy A55 (SM-A556B), the rebuilt app and test
APKs were installed as `com.kreativ.sharesafe.debug` and its test package.
All **30 device tests pass** again with Fragment 1.9.1 (16.648 seconds,
October 15, 2026), including the bundled detector, Privacy Check, editor controls,
export/share and reviewer-access regressions; the private reviewer argument was
supplied and no tests were skipped. The installed non-debug app was not updated.
This is still a 4 KB-page device, not a 16 KB runtime or a payment-flow test.
No version bump, signed release, upload or publication was performed. The
notification refers to live build 3, which has not been replaced.

### Home-screen version credit

The shared home screen ends with plain `Version <BuildConfig.VERSION_NAME>`
above a centered, accent-coloured `Made by Kreativ` website link. Both use 12sp
text and wrap instead of truncating. The version comes from the build, not a
hardcoded release number. Only the credit is interactive, with a 48dp minimum
tap-target height and a screen-reader description identifying the external link.
It opens `https://madebykreativ.com/apps/` via a browsable ACTION_VIEW intent,
without extras, photo URIs or URI permissions, and handles a missing browser.
Play app/test APKs build, Amazon shared test sources and Samsung app sources
compile, all 8 unit tests pass (0 skipped), and lint completes without errors.
A new device regression covers the footer's text, placement, scroll reachability,
recreation and large-text wrapping. After reconnecting the unlocked Galaxy A55,
the updated debug app and test APKs were installed separately from the non-debug
app. All **31 device tests pass** (12.279 seconds, October 15, 2026; private
reviewer argument supplied, zero skipped). The first run's footer check scrolled
before the recreated view finished layout; an explicit layout wait fixed the test,
without modifying app behavior. The isolated footer test also passes. The
installed non-debug app was not updated. This is not payment-flow or 16 KB
runtime validation, and nothing was uploaded or published.

After making the credit a link, the rebuilt Play test app and instrumentation
APK were installed on the A55 again. All **32 device tests pass** (17.68 seconds,
October 15, 2026; zero skipped), including the external-link destination/intent,
plain non-clickable version, 48dp target, scroll reachability, recreation and
large-text wrapping of both labels. The 8 unit tests pass again; lint has no
errors (the same 20 warnings). The non-debug app remains unchanged.

## Google Play 1.1.0 / version code 3

ShareSafe is already live on Google Play. This update adds the optional permanent
`sharesafe_pro` unlock; free metadata cleaning remains available.

- [x] Recover the existing upload key and verify its certificate against Play Console.
- [x] Re-run all 13 device tests on the unlocked Galaxy A55 after the final EXIF-library/backup change: zero failures, errors or skipped tests (October 11, 2026). All 6 purchase-verification unit tests also pass.
- [x] Build a fresh signed Play AAB (code 3) and run release lint with no errors (11 warnings). All 8 unit tests pass, including private reviewer-code acceptance and invalid-code rejection.
- [x] Update the public privacy policy source for on-device editing and Play purchases.
- [x] Verify the updated policy is deployed at https://madebykreativ.com/privacy/sharesafe/ (October 11, 2026).
- [x] Activate `sharesafe_pro` / `lifetime` as a permanent one-time purchase. EUR checkout prices are 2.99, including tax; other currencies use Google's regional calculation (Romania RON 18.99, US USD 3.39).
- [x] Upload the initial code 2 AAB to internal testing. Release `1.1.0 – Pro internal test` is available to the existing `andrei.olaru@gmail.com` tester list. Code 3 is the submitted production replacement; payment tests were explicitly waived.
- [x] Install the Play-delivered release on the license tester's Galaxy A55. ADB confirms `com.kreativ.sharesafe`, version 1.1.0 / code 2, installed by `com.android.vending` (October 11, 2026).
- [x] Verify `andrei.olaru@gmail.com` is in both the selected internal tester list and the selected licence-testing list. Do not use a real payment method for billing tests.
- [ ] Verify localized price, test purchase (no real charge), acknowledgement, cancel, pending payment, restore after reinstall, offline access, and revocation after a test refund.
- [x] Review the relevant Data safety, content-rating and Sign-in details declarations against the Play build and privacy policy. On-device processing remains unchanged; paid Pro and free reviewer access are declared.
- [x] Update and save listing copy to distinguish free cleaning from paid Advanced Edit.
- [x] Commit and push only ShareSafe implementation changes; do not include other app work.
- [x] Save the initial code 2 production draft; discard only that unsubmitted draft to replace it with code 3. Code 2 remains in the artifact library and internal testing; the live code 1 release is unaffected.
- [x] Obtain approval to accept IARC Terms of Use and save the updated content-rating questionnaire, including digital purchases and no random items. Generated ratings: Everyone / PEGI 3 / USK all ages / IARC 3+, Brazil 14+; in-app purchases declared.
- [x] Implement free, reusable reviewer access to all Pro editing features, independent of paid purchase receipts.
- [x] Save accurate Sign-in details with the private reviewer code and instructions; Console confirms the change is saved and identifies restricted functionality.
- [x] Save the replacement production release `1.1.0 – ShareSafe Pro (build 3)`, code 3, at 100% in the existing targeted countries. No blocking Console errors; only the non-obfuscated-build mapping warning.
- [x] Submit the production update, short/full listing descriptions and content-rating questionnaire for review, with reviewer access supplied. Console shows **Changes in review** and automated quick checks running (October 11, 2026).

Billing release decision (October 11, 2026): the user explicitly requested
production submission without completing end-to-end Google Play payment tests.
The Play-installed app displayed the Romanian price, 18.99 RON, and opened
Google Play's purchase flow; a completed payment, Pro unlock, acknowledgement,
restore, pending/cancel handling, offline entitlement and refund revocation have
**not** been verified. This is an accepted release risk, not a passing test.

Submission status (October 11, 2026): **submitted**. Play Console shows **Changes
in review**, with quick checks running before the changes enter Google's review.
The submitted release is `1.1.0 – ShareSafe Pro (build 3)`, including the revised
listing and IARC questionnaire. Private Sign-in details give reusable free access
to Pro. Managed publishing remains off for automatic publication after approval.
The live production release remains version 1.0.0 until approval/publication.
The Galaxy A55 is disconnected; the new reviewer workflow instrumentation test
has compiled but has not run. The emulator could not start due to low disk space.
The earlier 13 device tests passed on code 2; no new device-test pass is claimed.
Only regeneratable ShareSafe build intermediates were cleared to recover disk
space after the signed bundle was uploaded. Final outputs and test/lint reports
are preserved.

Device tests use the separate `com.kreativ.sharesafe.debug` test app. They cover
EXIF orientation, clean filenames/metadata, skipped/resized inputs, transparency,
Cover and other effects, gallery saving, editing restoration and export locking,
main-screen recreation, and read-only URI sharing for JPEGs and ZIPs. These are not
a substitute for real Google Play Billing tests. Re-run after implementation changes.

Internal tester opt-in: https://play.google.com/apps/internaltest/4701651523312437391

The uploaded AAB SHA-256 is
`090bd090cd6974402982b3bd85566f4438adbdb69c8fd193a1efbf65ba4fa961` (code 3).
The only Console validation warning was the absence of a deobfuscation mapping;
this build is not obfuscated. Production remains version 1.0.0.

Suggested release notes:

> New optional ShareSafe Pro: Advanced Edit with Cover, blur and pixelation, plus
> zoom, pan and undo. Metadata cleaning stays free. Improved editing recovery,
> export reliability and privacy information. Originals remain unchanged.

## Common release prerequisites

- [x] Keep the existing permanent release keystore outside this repository.
- [ ] Keep `com.kreativ.sharesafe` and the same signing key for every store.
- [ ] Build and test the appropriate signed release artifact; never submit a debug APK.
- [ ] Increment `versionCode` for every store update.
- [ ] Verify the privacy-policy URL and support email work.
- [ ] Prepare a 512 × 512 app icon and phone/tablet screenshots that accurately show the app.
- [ ] Test image selection, metadata removal, ZIP sharing, and gallery saving on a real device.

## Samsung Galaxy Store

Samsung is the first target because ShareSafe is a standard touch-screen Android app
and needs no device-specific changes.

- [ ] Create a Samsung account, register it in Seller Portal, and apply for commercial seller status.
- [ ] Confirm the Samsung account country matches the country of the payout financial institution.
- [ ] Prepare the App Information fields: `ShareSafe`, privacy-policy URL, support email,
  age rating, category, English listing, screenshots, and description.
- [ ] Build `assembleSamsungRelease` and upload the signed APK. An AAB is also supported,
  but an APK keeps signing under the developer's control.
- [ ] Start with a closed beta if Samsung IAP is added; otherwise use a normal review submission.
- [ ] Select only the countries intended for launch and submit the app for review.

## Amazon Appstore

ShareSafe should target Fire tablets, not Fire TV. The `amazon` manifest requires a
touch screen so Amazon's device filtering excludes television devices.

- [ ] Create an Amazon Developer account and complete its tax and payment profile before monetizing.
- [ ] Build `assembleAmazonRelease` and upload the signed APK.
- [ ] Select Fire tablets as the only supported device family in the Developer Console.
- [ ] Complete Amazon's mandatory privacy questionnaire. With the current app, photos
  remain on-device and are not collected or transferred.
- [ ] Test on a Fire HD 10 or Fire Max 11: photo selection, individual sharing, ZIP sharing,
  gallery saving, rotation, and relaunch.
- [ ] Restrict initial countries deliberately; do not accept the default all-country scope without review.

## Premium unlock and store isolation

- `play`: Google Play Billing permanent non-consumable, implemented; end-to-end
  purchase verification on a Play-delivered build remains unverified and was
  explicitly waived by the user for this update. Reviewer access and accurate
  Play declarations are still required before submission.
- `samsung`: free-only; Samsung IAP is not implemented.
- `amazon`: free-only; Amazon billing is not implemented.

Each store retains its own purchase entitlement. Do not promise that a purchase on
one store unlocks another store unless a customer account and receipt-verification
service are intentionally added.
