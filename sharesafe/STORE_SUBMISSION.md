# ShareSafe store-submission checklist

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
