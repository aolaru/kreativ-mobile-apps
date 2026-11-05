# Kreativ Pro 1.1.0 (7) — implementation and launch status

> Latest state: phone code 9 now loads the Pro price and opens checkout. No
> purchase completed; further tests stopped at owner request. See
> [billing investigation](BILLING_FIX_1.1.0.md). Older status below is history.

> Latest phone state: signed code 8 is installed. Focused previews/copy/history
> pass; restore finds no completed purchase and Play product availability still
> blocks checkout. See [4 November retest](PRO_RETEST_1.1.0.md).

> **Superseded locally by code 8:** Header entry removed; brand renamed to
> Fancy Text Pro in the app and saved Google Play product. See
> [latest validation](NAME_CHANGE_1.1.0.md). Internal/phone tests below apply to
> code 7; its artifacts are archived at
> `artifacts/archive/pro-1.1.0-code7-before-name-change/play/`.

Prepared 4 November 2026 for `com.kreativfont.studio`. The owner chose **€4.99
one time** for premium templates, custom backgrounds and saved style presets.
The app remains free to download. Pro has **not been submitted to production**.

## Delivered features

- 12 bundled, code-rendered templates across Quotes, Names, Greetings and Social.
  Preview without changing the draft; keep current words or use the sample.
- Solid or two-colour gradient backgrounds, four gradient directions, global ink,
  and fine-line, double-line, corners or arch frames. Existing word colours survive
  background changes. Transparent export hides the background.
- Up to 30 named local style presets, duplicate-name protection and deletion Undo.
  Presets include signature/export settings and preserve new words when applied.
  Position-specific word overrides reset when applying a preset/template.
- Complete-design Undo/Redo and draft/favourite persistence include Pro styling.
  Pro designs are preserved if access is temporarily lost; text remains copyable,
  image export is locked, and Use free look provides an undoable conversion.
- Existing six themes, 17 styles, word styling, 50 saved designs, transparent PNG
  and 2× export remain free.

## Google Play configuration

| Setting | Verified state |
| --- | --- |
| App | `4976006210494663211` / `com.kreativfont.studio` |
| Product | `kreativ_pro_lifetime` — Kreativ Pro |
| Purchase option | `buy`, permanent Buy / non-consumable, Active |
| Euro pricing | All 33 EUR rows verified **EUR 4.99** |
| Billing availability | 174 countries/regions; Google-converted localized prices |
| Examples | Romania RON 26.99; US USD 4.69; UK GBP 4.19 |
| Subscription / rental | None |
| Internal test | Track `4701453847955130384`, release 1, build 7 available |
| Owner tester | Existing single-account license list, internal access saved and invitation accepted with explicit owner approval |
| Alpha | Track `4700669451162278416`, release 1, saved build 7 draft; no rollout |
| Production | **1.0.2 (5)**, published at 100% rollout in Console |

Play's bulk conversion initially added VAT above the requested euro amount.
Regional prices were corrected before activating the product, then read again
from the saved purchase option. EUR 4.99 is the actual saved checkout price.
Other currencies use Google's conversion/charming from a EUR 4.15 net reference.
No real-money transaction has been attempted.

Evidence: `pro-product-active.png`, `pro-euro-price.png`,
`pro-regional-prices.txt`, `pro-1.1.0-internal.{png,txt}`,
`pro-code 7-alpha-draft.{png,txt}`, `pro-owner-enrolled.{png,txt}`.
Production evidence is `play-1.0.2-published-2026-11-04.{png,txt}`.
Root production metadata and development metadata remain separate in
`../play-console-state.json`.

## Purchase implementation

Google Play Billing 9.1.0 supplies product details, eligible base `buy` offer token
and localized price. A price is never guessed or hardcoded into the purchase UI.
Native RSA receipt verification checks the package, product and PURCHASED state.
Pending payments never grant access. Signed JSON/signature are cached in private
preferences for offline use. Failed ownership queries preserve verified access;
successful queries without an owned purchase revoke it. Stale queries cannot
replace a later purchase event. WorkManager 2.10.1 durably retries acknowledgement.

There is no purchase backend. Local signed-receipt validation is appropriate for
this offline implementation but has weaker tamper resistance than server-side
verification. Google recommends server-side verification for stronger protection:
[Billing security](https://developer.android.com/google/play/billing/security).
The release contains no development entitlement switch. The owned browser
preview requires both Vite development mode and `VITE_PRO_PREVIEW=1` and cannot
perform purchases.

The Play public licensing key was read from this app's Monetisation setup and
is kept outside the repository at `~/.config/kreativ-android/billing-public-key.txt`.
The original upload key and credentials were preserved. Never regenerate them.

## Build failure caught and corrected

Initial uploaded build 6 failed on native startup: AGP 9 full-mode R8 removed the
no-argument constructor of Room's reflected `WorkDatabase_Impl`, pulled in by
WorkManager. Build 7 adds only the targeted Room database constructor keep rule.
Optimisation, shrinking and full mode remain enabled. Build 7 starts normally;
all native regression checks below apply to 7. Build 6 is archived in
`artifacts/archive/pro-code 6-startup-failure/`, removed from Alpha and must never
be rolled out. The corrected 7 replaced it in Alpha and internal testing.

## Validation

- **9 model/history tests and 28 desktop/mobile browser tests pass.** Coverage
  includes owned development preview, real decoded gradient/transparent PNGs,
  preserved words/word colours, presets/reload/deletion Undo, locked restored
  designs, invalid browser unlock flags,320 px/200% layouts and axe WCAG scans.
- **3 billing JVM test methods pass:** actual RSA signature/tamper/wrong-key and
  malformed inputs; ownership reconciliation on interrupted/empty/verified
  queries. Native XML results are preserved under `artifacts/native/pro-unit-tests/`.
- **20 release checks pass:** signed APK/AAB and checksums, AGP 9.0.1/Gradle 9.1.0,
  optimising R8 rules, retained Room constructor/Capacitor annotations, exact
  bundled web build/policy, backup exclusions, public key and no dev unlock.
- **Galaxy A55, Android 16/API36, WebView 156.0.8078.47:** exact upload-signed APK
  byte-match and upgrade data retention; native clipboard, styled text/image
  shares cancelled without recipient, 1080×1080 opaque and 2160×3840 transparent
  PNG saves, cancellation/cache cleanup, actual custom-colour pixels, Undo/Redo,
  saved deletion recovery/reopening, policy/version, normal cold launch and
  persistence, 200% Android text in portrait/landscape. Locked Pro previews do not
  change draft/favourites and cannot apply without a verified purchase.
- Original preferences hash-matched the fresh pre-Pro backup after restoration;
  generated Downloads/share-cache files were removed, temporary helper uninstalled,
  and original rotation/font/stay-awake settings restored. Keep helper artifacts
  and backups private under ignored `.local/`.

Logs: `artifacts/pro-1.1.0-final-tests.log`,
`artifacts/pro-1.1.0-code 7-build.log`,
`artifacts/pro-1.1.0-code 7-verification.log`.
Phone evidence: `artifacts/phone-release-1.1.0/` and `native-results.json`.
Visual development preview: `artifacts/pro-screenshots/lilac-template-preview.png`.

**Billing runtime is still pending.** On the confirmed owner license-test account,
Play returns product unavailable after product activation. Purchase is disabled;
no test purchase or real payment occurred. Enrollment is now confirmed; retry
lookup after propagation and investigate the native SDK result if it persists.
Do not mark successful purchase, cancellation, acknowledgement, restore,
offline paid access, refund/revocation or owned Pro features as phone-tested yet.
Native free-editor regression is recorded separately from billing validation.


A temporary same-signer **unminified diagnostic** returned BillingResponseCode 0
(success) with zero fetched products and `UnfetchedProduct.statusCode=3`
(**PRODUCT_NOT_FOUND**) for the exact `kreativ_pro_lifetime` / `inapp` request.
This reproduces independently of release shrinking. Google's reference includes
incomplete propagation of a newly created product among possible causes; that
cause is an inference here, not a confirmed response from Google support.
[Official status-code reference](https://developer.android.google.cn/reference/com/android/billingclient/api/UnfetchedProduct.StatusCode).
See `artifacts/phone-release-1.1.0/billing-debug-diagnostic.log`. The diagnostic
was removed; the phone was returned to the exact signed build 7 APK (byte-matched),
original designs hash-matched and helper uninstalled again.

## Earlier code-7 artifacts (now archived)

- AAB: `artifacts/play/kreativ-1.1.0.aab`
  SHA-256 `7584e06f08f9f4b9058c7fe1ddc8f109020e06d9cd2a81e52f670c01d9dc9c56`.
- APK: `artifacts/play/kreativ-1.1.0-upload-signed-release.apk`
  SHA-256 `8a5902b10dcfb0698e8af61b6e315ad59a3e2d07b69c3e329a5203752ac96073`.
- Final report: `artifacts/play/verification.json` — 20 checks pass, matching internal
  upload and free-editor native evidence, billing and hosted-policy validation pending.

## Before a production Pro update

- [ ] Complete explicitly no-charge Google Play license tests: cancel, successful
  purchase/signature/unlock, acknowledgement, fresh restore, offline cached Pro,
  revoked/refunded access; verify native template/background/preset export and
  cold-start persistence. Pending approval/decline paths should also be exercised.
- [ ] Publish and publicly reverify updated `public/PRIVACY.txt` / `store/privacy/`.
  The October policy is live; November Pro policy is currently local. Existing
  `publication.json` does not match the new policy hash; verifier reports pending.
- [ ] Review Data safety and paid App access instructions with the final SDK
  integration. Payment details collected directly by Google differ from purchase
  history received/stored locally; do not blindly reuse 1.0.x “no billing SDK” notes.
- [ ] Review/upload the local Pro listing copy and suitable screenshots. Existing
  live listing assets are not automatically replaced by regenerated local assets.
- [ ] Re-run release checks against the exact final artifacts. Any new uploaded
  binary needs versionCode 8 or higher; 6 and 7 are already consumed in Play.
- [ ] Prepare production only after the owner requests the update. No Pro
  production release or review request is authorized by the current implementation task.

Older devices/WebViews, tablets and TalkBack speech/focus remain untested. The
upload-signed APK tested locally differs in signing identity from Play-delivered
APKs if Play uses its separate app-signing certificate.

References checked 4 November 2026:
[Billing integration](https://developer.android.com/google/play/billing/integrate),
[license testing](https://developer.android.com/google/play/billing/test),
[one-time products](https://developer.android.com/google/play/billing/one-time-products),
[Data safety payment-service guidance](https://support.google.com/googleplay/android-developer/answer/10787469?hl=en).
