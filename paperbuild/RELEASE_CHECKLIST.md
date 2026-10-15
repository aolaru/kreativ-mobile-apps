# PaperBuild Release Checklist

## Version 1.1.0 preparation — 15 October 2026

- [x] Version code 4 / version name 1.1.0, developed locally; the published 1.0 release is unchanged.
- [x] Free: Recent PDFs with private durable storage, previews for unencrypted PDFs, rename/open/share/Downloads save, reversible removal and confirmed permanent deletion.
- [x] Free: non-destructive manual cropping, full-page restore, precise percentage controls, rotation-aware bounds and draft recovery.
- [x] Pro: AES-256 password-protected exports, Unicode text watermarks, rotation-aware page numbers, and saved export presets. Existing lifetime ownership is used unchanged.
- [x] Pro options apply to image, merge and extraction outputs. Passwords are memory-only, excluded from drafts/presets, cleared for each export, and required again when loading a protected preset. Protected library copies retain the encrypted bytes and do not generate previews.
- [x] Include dynamic version/Made by Kreativ footer and Fragment 1.9.1 dependency constraint.
- [x] QA and release APK builds pass; debug/release lint passes with warnings and no errors. Twelve JVM tests pass in each build type.
- [x] Sixteen device tests pass on Samsung SM-A556B / Android 16, zero failures/errors/skips: original document regressions, footer, crop recovery/rotation, encrypted export and wrong-password rejection, library lifecycle, presets, Free gating and Pro settings UI.
- [x] Inspect rendered PDFs with Poppler at 0/90/180/270-degree rotations: watermark and page numbers are upright, unclipped, and leave original page content intact. Android PDFBox's test renderer renders the image watermark darker than Poppler; independent rendering confirms the PDF opacity is correct.
- [x] Update local README, privacy policy, About page and draft listing for 1.1. No new internet, advertising-ID, broad photo or camera permission.
- [ ] Complete final owner review on the separately installed **PaperBuild QA** app (`com.kreativ.paperbuild.qa`). The QA app does not replace Google Play's production package.
- [x] Commit and push only PaperBuild changes: source commit `9b7c4ad5adb504608007d557b36f32654f31182e`, on `codex/paperbuild-1-1` and remote `main`. The updated public GitHub privacy policy is included. Shared checkout/index and unrelated app work are preserved.
- [x] Build the signed version code 4 / 1.1.0 App Bundle with the existing upload key. `bundleRelease testReleaseUnitTest lintRelease` succeeds; release lint reports 0 errors / 28 warnings. `jarsigner` verifies the signature; the self-signed certificate/no timestamp warnings are expected for this Android upload key.
  - Upload: `app/build/outputs/bundle/release/app-release.aab`; retained local copy: `app/build/outputs/releases/1.1.0/paperbuild-1.1.0-v4.aab` (build outputs are not committed).
  - Size: 11,551,179 bytes. SHA-256: `1f013f8820a82588076d5cfc788700e8d72ffbfa7681192f2e8ddefe6c549781`.
  - Upload certificate SHA-256 matches the existing keystore: `9B:D5:B7:6E:E4:94:B9:BB:80:A1:F7:51:3C:B7:00:76:28:11:82:72:FC:F5:2A:E7:57:CF:8A:6B:E9:82:B2:66`.
  - Release manifest: `com.kreativ.paperbuild`, min SDK 26, target SDK 36, not debuggable; no new internet/advertising/photo/camera permissions. Bundle dependency metadata includes Fragment 1.9.1. Built source matches the pushed source commit.
- [x] Upload v4 to Google Play production and save **1.1.0 — Recent PDFs and Pro export tools** as a draft, with en-GB release notes from `release-notes/1.1.0-en-GB.txt`. Play recognises **4 (1.1.0)**. No review submission or rollout is authorised by this preparation step.
- [x] Run Play's Preview and confirm validation: no blocking errors, one expected missing-deobfuscation-file warning, and zero devices lost relative to v3. Return to the saved draft without pressing the final **Save** that queues a rollout in Publishing overview. Validation evidence: `release-evidence/2026-10-15/release-1.1-validation.jpg`.
- [x] Save the updated en-GB listing text as a draft. Existing icon and banner retained; phone screenshots subsequently refreshed below. No price/product ID changes.
- [x] Replace the draft phone gallery with six unmodified 1.1 real-device captures: Home, Free cropping, export settings, Ready to share, Recent PDFs and expanded Pro features. Files: `store-assets/play/screenshots/1.1/01-home.png` through `06-pro.png`, each 1080 × 2340. Order verified in Play (6/8); **Your changes have been saved** shown. Icon/banner unchanged. Evidence: `release-evidence/2026-10-15/gallery-1.1-saved.jpg`.
- [x] Complete Recent PDFs and expanded Pro captures directly from the connected phone, at the owner's request after Android Studio stopped responding. Pro screenshot shows real feature cards without fabricating a QA price or displaying unavailable billing.
- [ ] After final owner review, confirm the release and listing changes and send the update to Google for review. Managed publishing was previously off; approval may publish automatically, so do not submit until launch is authorised.
- Follow-up: the current live v3 dashboard flags DEX code optimisation (obfuscation 1%) with a February 2027 deadline. Version 1.1 retains `minifyEnabled false`; R8 optimisation and PDFBox/billing regression checks remain separate work, not a claimed fix in this build.
- Purchase/restore testing was not rerun. The owner's original payment-test waiver remains recorded below; it must not be described as passed.

### Final review progress — 15 October 2026

- [x] Revalidate `testDebugUnitTest testReleaseUnitTest lintDebug lintRelease`: successful, existing 12-test JVM reports per build type current/up-to-date, 0 lint errors / 28 warnings per build type.
- [x] Rerun all 16 selected QA device regressions on Samsung SM-A556B / Android 16: 16 tests, 0 failures, errors or skips. Includes Pro settings/presets UI, encrypted exports, crop/draft recovery, library lifecycle, original document regressions and footer. Play purchase and signed reviewer-code device testing are not included in this run.
- [x] Hands-on Free flow: import two fictional sample PNGs through Android's system file picker, drag and apply a crop, name and export an A4 colour PDF, save to Downloads/PaperBuild, and open in Samsung's independent PDF reader. Recent PDFs shows the correct title, two-page count, file size and preview.
- [x] Independently inspect both pages of the exported PDF with Poppler: valid 2-page A4 PDF, 127,706 bytes, clean rendering with readable content and no accidental clipping. Intermediate review files remain under `tmp/pdfs/`, outside Git.
- [x] Inspect main-screen Pro/Free labels and bottom layout: Privacy & About is below Pro, version credit reads **Version 1.1.0 · Made by Kreativ**, and both have space above Samsung's navigation bar.
- [x] Recheck the retained signed v4 AAB hash: unchanged at `1f013f8820a82588076d5cfc788700e8d72ffbfa7681192f2e8ddefe6c549781`. Physical Play package remains v3 / 1.0.0 installed by Google Play; production app data was not replaced. QA was reinstalled after the automated test runner removed its isolated test package.
- [x] Direct-phone checks: exported a fresh two-page fictional document, verified Android's share sheet receives `Weekend plans.pdf` and cancelled without sending, moved the QA library copy to Removed PDFs and restored it successfully. No permanent deletion or production data changes.
- [x] Read About/privacy page and visually inspect its bottom layout: Privacy Policy, Contact support, Back and version/Made by Kreativ credit are readable and clear of navigation. Inspect all six Pro feature cards, including password protection, watermarks/page numbers and presets. Automated Pro settings/preset coverage passed; no purchase was attempted.
- [ ] Remaining manual coverage: real camera capture/return, sending to an actual share target, Android 8/9 storage-permission denial, and full hands-on Pro export workflows. These are not claimed as passed; automated Pro/document/library coverage is listed above. Purchase/restore remains explicitly waived and unverified.
- [ ] Owner's final launch approval and Google review submission. No release or listing changes have been submitted or published by this review.

## Production submission — 12 October 2026

- [x] Owner approved publishing the production launch. Confirmed **Send 11 changes for review** in Play Console; Publishing overview now shows **Changes in review**, including **1.0.0 — PaperBuild launch / Start full rollout**, listing, availability, and app-information changes.
- Google is still running its automated quick checks; the submission will proceed to review when those checks complete successfully. **Not yet approved or publicly live.** Managed publishing remains off. Evidence: `release-evidence/2026-10-12/production-submitted.png`.
- The preparation entries below retain their original dates and saved-for-review status. They were included in this submission. Payment testing remains explicitly waived and unverified; unfinished hands-on checks remain unfinished.

## Device smoke test

- [x] Run the six automated document regressions on Samsung SM-A556B / Android 16 (11 October 2026): merge/extract text and original content, page geometry/rotation/links, compact exports, mirrored EXIF/grayscale, private draft recovery, malformed-input cleanup, and permissions.
- [x] Run all eight JVM tests and all eight Samsung device tests, including real signed reviewer access (11 October 2026): zero failures, errors, or skips. Lint passed. Review grants survive billing refresh and reopening; leaving preserves real purchase ownership. On-screen reviewer-dialog testing remains a hands-on check.
- [x] Install `app/build/outputs/apk/qa/app-qa.apk` on Samsung SM-A556B / Android 16 for the on-screen checks (11 October 2026). Home and Pro layout checked; the remaining hands-on flows below still need testing. This separate QA copy does not replace the release app.
- [ ] Capture one page, then capture a five-page document.
- [ ] Add two image files through the system picker, including one sideways photo.
- [ ] Confirm every thumbnail matches its page and rotation.
- [ ] Build A4 colour, A4 high-contrast, and US Letter grayscale PDFs.
- [ ] Open each PDF in an independent PDF viewer and inspect each page.
- [ ] Share a PDF to a messaging or email app.
- [ ] Save a PDF and find it in `Downloads/PaperBuild`.
- [ ] Deny storage permission on Android 8/9 and confirm the app explains why saving cannot continue.

## Store preparation

- [x] Final application ID: `com.kreativ.paperbuild`
- [x] Public privacy-policy URL prepared
- [x] Store listing copy prepared
- [x] Create a real release keystore and store its password in the local macOS Keychain. Back up both before publishing.
- [x] Build and verify a fresh signed App Bundle for version code 2 / 1.0.0 (11 October 2026). `jarsigner` verified the release signature; this exact bundle was uploaded and released to Internal Testing.
- [x] Build and verify the signed version code 3 / 1.0.0 bundle with reviewer access (11 October 2026). Size: 11,157,579 bytes. SHA-256: `14e2be264160aa45b422c6187b67e887a5e1c126208a4cfbc19abf4addf68223`.
- [ ] Capture the five real-device screenshots described in `STORE_LISTING.md`.
  - [x] Captured the unmodified home screen from the installed Play v3 build on Samsung SM-A556B (12 October 2026), 1080 × 2340, at `store-assets/play/screenshots/01-home.png`. Other screenshots and hands-on document checks remain incomplete.
  - [x] Captured Pro's three features and the real Romanian price, 1080 × 2340, at `store-assets/play/screenshots/05-pro.png`. Uploaded Home first, then Pro. Google's two-screenshot minimum is met; the expanded five-screen marketing gallery remains a follow-up.
- [x] Complete Google Play content, target audience, data safety, and app access forms (saved for review).
  - [x] Save privacy policy URL, no-ads, non-government, no-financial-features, and no-health-features declarations in Play Console (11 October 2026). These declarations are saved for review, not yet reviewed by Google.
  - [x] Set the app category to Productivity and save the public support email `info@madebykreativ.com`.
  - [x] Finalize Data Safety. Saved the no-collection/no-sharing declaration after checking code and Google's [Data Safety guidance](https://support.google.com/googleplay/android-developer/answer/10787469?hl=en), and completing target audience (11 October 2026). Saved for review, not submitted to Google.
  - [x] Owner approved teenagers and adults (13+) as the intended audience. Saved 13–15, 16–17, and 18+ in Play Console (11 October 2026); not yet submitted for Google review.
  - [x] Implement reusable, free reviewer access to Pro in version code 3, approved by the owner. Signed grants are verified offline; the private key and issued code are outside Git. Normal Pro purchases remain unchanged.
  - [x] Save the private reviewer code and English instructions in Play Console's restricted-access declaration after v3 is available (11 October 2026). Saved for review, not submitted. Optional credential use on trusted partner devices is off. No inaccurate unrestricted-access declaration was saved.
  - [x] Finish store listing (12 October 2026). The dashboard's initial setup gate is cleared and production tasks are unlocked. App access, content rating, target audience, Data Safety, listing text, icon, banner, and the two screenshots are saved for review. Icon and banner are declared AI-assisted; unmodified physical-device screenshots are not AI-labelled.
    - Listing text, 512 × 512 app icon, and 1024 × 500 feature graphic saved as a draft on 12 October 2026. Google still requires at least two real phone/tablet screenshots before completion; the five-screen marketing brief remains recommended.
    - Accepted the IARC Terms after explicit owner confirmation and saved the questionnaire on 12 October 2026. Ratings include Everyone (North America), PEGI 3 (Europe), and 14+ (Brazil), with in-app purchases disclosed. Saved for review only; production has not been submitted.
  - [x] Complete the Advertising ID declaration as **No** (12 October 2026). Verified no `AD_ID` permission, Advertising ID API use, or ads identifier dependency in source and the merged release manifests. Play Console confirms the declaration was saved and includes it among the review information.
- [x] Create the non-consumable one-time product `paperbuild_pro_lifetime` with €2.99 EUR base and automatic country pricing (active).
- [x] Enable the existing `Licence testers (auto-generated)` list (one account) for the internal track. Other tester lists remain unselected.
- [ ] Complete hands-on merge, extraction, compact export, and unlimited-page checks. **Payment testing was explicitly waived by the owner on 12 October 2026; purchase, restore, pending-payment, and acknowledgement-network tests remain unverified, not passed.** No paid purchase is authorised or required for this release workflow.
  - Play v3 opens successfully and loads the live Romanian price **18,99 RON**. Opening Unlock Pro reached Google's **Review and agree** notice about immediate contract performance and loss of withdrawal rights. Owner reported tapping Agree, then requested continuing without payment testing. No purchase sheet/payment method or no-charge purchase has been verified; this workflow made no purchase. Evidence: `release-evidence/2026-10-12/play-billing-consent-required.png`.
- [x] Publish version code 2 / 1.0.0 to Internal Testing (11 October 2026, 22:49 local). Play Console shows **Active** and **Available to internal testers**. This is not a public production release.
- [x] Publish version code 3 / 1.0.0 to Internal Testing (11 October 2026, 23:55 local). Play Console shows **Active**, latest release **1.0.0 — Internal test v3**, and **Available to internal testers**. Production was not changed.
- [x] Accept the internal test invitation with the existing license-test account (11 October 2026). Google confirms **You're a tester** for `com.kreativ.paperbuild (unreviewed)`. Test link: https://play.google.com/apps/internaltest/4701743097314612272.
- [x] Complete the phone installation through Google Play. After owner account verification and removal of the backed-up development copy, the read-only package check on 12 October 2026 confirms **version code 3 / 1.0.0**, no `DEBUGGABLE` flag, and both installer and initiator `com.android.vending` on Samsung `RZCXA128W0Y`. Billing verification remains incomplete; no purchase has been made.
  - The earlier read-only device check on 12 October 2026 showed version code 1, debuggable, installed by `com.android.shell`. This old copy was subsequently backed up and removed with owner permission, then replaced by Play v3 as recorded above.
  - Signing check on 12 October 2026 confirmed the old APK used `CN=Android Debug`, SHA-256 `17:6B:21:23:03:3F:56:BF:DE:41:DD:0F:29:F2:AB:67:F2:51:A9:79:A9:4B:36:D1:62:FA:76:29:47:7E:44:DC`. Play Console's app-signing certificate in Digital Asset Links is `89:23:70:EB:47:5D:5F:31:6E:AE:AC:E0:92:08:77:77:97:92:A6:44:0A:40:82:D7:CC:D6:CE:0E:51:41:C4:6F`. These differ, so the old development copy could not be updated directly to the Play release.
  - [x] With explicit owner permission on 12 October 2026, backed up the old APK and its complete private app directory outside Git under `~/Library/Application Support/Kreativ/PaperBuild/device-backups/2026-10-12-old-debug.olMNRd/`. Backup folder is mode 700, files are mode 600; tar listing and SHA-256 checks succeeded. Uninstalled **only** `com.kreativ.paperbuild` from Samsung `RZCXA128W0Y`; package-manager success and an empty package lookup confirmed removal. Exported PDFs were not touched. Backup recovery would require the old development APK; it has not been restored into the Play version.
  - Owner completed Google's web account verification after the renewed installation request for **Andrei A55 5G only**; Play v3 installation is confirmed. Payment tests remain waived and unverified.
- [ ] Resolve tester feedback before creating a production release.
- [x] Save production availability for all 178 countries/regions offered by Play Console, including Rest of world (12 October 2026). This is pending review, not yet public availability.
- [x] Create and confirm production release **1.0.0 — PaperBuild launch** using the existing version code 3 bundle from Play's library (12 October 2026). Bundle SHA-256 rechecked and unchanged. No blocking validation errors; the sole deobfuscation warning is expected because `minifyEnabled false`. Saved for review, not yet submitted.
- [x] Send the production release and app-information changes to Google for review (12 October 2026). All 11 changes were accepted into **Changes in review**, pending automated quick checks and Google's review; no public launch is claimed yet.

## Boundaries for version 1.0

- [x] No account, analytics, ads, or internet permission.
- [x] No misleading auto-crop, OCR, signing, redaction, or merge claims.
- [x] Pro features are implemented and accurately described. Owner waived Play purchase testing for launch; billing remains unverified.
