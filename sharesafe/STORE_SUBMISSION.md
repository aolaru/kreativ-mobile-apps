# ShareSafe store-submission checklist

## Recommended order

1. Samsung Galaxy Store
2. Amazon Appstore — Fire tablets only
3. Google Play, once its developer account is restored

## Common release prerequisites

- [ ] Create one permanent release keystore outside this repository.
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

## Premium unlock — later implementation

When the Lifetime Premium feature is ready, add a store-specific billing SDK only to
the matching flavor:

- `samsung`: Samsung IAP non-consumable
- `amazon`: Amazon Appstore SDK entitlement
- `play`: Google Play Billing non-consumable

Each store retains its own purchase entitlement. Do not promise that a purchase on
one store unlocks another store unless a customer account and receipt-verification
service are intentionally added.
