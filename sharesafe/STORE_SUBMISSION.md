# ShareSafe store-submission checklist

## Google Play 1.1.0 / version code 2

ShareSafe is already live on Google Play. This update adds the optional permanent
`sharesafe_pro` unlock; free metadata cleaning remains available.

- [x] Recover the existing upload key and verify its certificate against Play Console.
- [ ] Re-run all 13 device tests on the unlocked Galaxy A55 after the final EXIF-library/backup change. The prior full run passed; the latest run passed 11 tests but two ActivityScenario checks could not resume while the phone was locked. All 6 purchase-verification unit tests pass.
- [x] Build a fresh signed Play AAB and run release lint with no errors.
- [x] Update the public privacy policy source for on-device editing and Play purchases.
- [x] Verify the updated policy is deployed at https://madebykreativ.com/privacy/sharesafe/ (October 11, 2026).
- [x] Activate `sharesafe_pro` / `lifetime` as a permanent one-time purchase. EUR checkout prices are 2.99, including tax; other currencies use Google's regional calculation (Romania RON 18.99, US USD 3.39).
- [x] Upload the signed AAB to internal testing, not directly to production. Release `1.1.0 – Pro internal test` is available to the existing `andrei.olaru@gmail.com` tester list.
- [ ] Install the Play-delivered release on a license tester's phone.
- [ ] Verify localized price, test purchase (no real charge), acknowledgement, cancel, pending payment, restore after reinstall, offline access, and revocation after a test refund.
- [ ] Review the Data safety and App content declarations against the Play build and privacy policy.
- [ ] Update listing copy to distinguish free cleaning from paid Advanced Edit.
- [x] Commit and push only ShareSafe changes; do not include other app work.
- [ ] Promote the verified bundle to production and submit for review.

Device tests use the separate `com.kreativ.sharesafe.debug` test app. They cover
EXIF orientation, clean filenames/metadata, skipped/resized inputs, transparency,
Cover and other effects, gallery saving, editing restoration and export locking,
main-screen recreation, and read-only URI sharing for JPEGs and ZIPs. These are not
a substitute for real Google Play Billing tests. Re-run after implementation changes.

Internal tester opt-in: https://play.google.com/apps/internaltest/4701651523312437391

The uploaded AAB SHA-256 is
`d9d66d309c65bd488b440c571b80c522f844da69d6fa5a1a992fc539d259dbc7`.
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
  purchase verification on a Play-delivered build remains a release prerequisite.
- `samsung`: free-only; Samsung IAP is not implemented.
- `amazon`: free-only; Amazon billing is not implemented.

Each store retains its own purchase entitlement. Do not promise that a purchase on
one store unlocks another store unless a customer account and receipt-verification
service are intentionally added.
