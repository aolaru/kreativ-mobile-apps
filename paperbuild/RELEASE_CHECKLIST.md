# PaperBuild 1.0 Release Checklist

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
- [ ] Send the production release and app-information changes to Google for review.

## Boundaries for version 1.0

- [x] No account, analytics, ads, or internet permission.
- [x] No misleading auto-crop, OCR, signing, redaction, or merge claims.
- [x] Pro features are implemented and accurately described. Owner waived Play purchase testing for launch; billing remains unverified.
