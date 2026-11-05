# Fancy Text Pro billing availability — 4 November 2026

**Availability is working on signed 1.1.0 (9).** Google returns one product,
`kreativ_pro_lifetime` / `inapp`, permanent `buy` option at **26.99 RON**.
Initial setup, configuration, product and ownership requests all return response 0;
country is RO. Checkout opened with **Test card, always approves** and Google's
explicit **“This is a test order, you will not be charged.”** notice.
Checkout was cancelled; final state is `owned=false`, `pending=false`,
`purchasing=false`, with “Purchase cancelled. No payment was taken.”

The owner said “wait ... the tests have passed”; additional tests were stopped.
**No automated Buy tap or completed purchase occurred.** Successful purchase,
acknowledgement, fresh ownership restoration, offline Pro and owned native tools
are not validated. Do not interpret available/opened checkout as a paid unlock.

## Findings and code fixes

- Before recovery, the exact native SDK response was successful request 0 but
  no fetched products, with SKU status **3 / PRODUCT_NOT_FOUND**. Ownership query
  returned 0 matching purchases. This was not an already-owned error.
- Console IDs, Active `buy` option, RO availability and RON price matched code.
  Owner license-test selection/internal enrollment were verified. Billing Lab
  was signed in with the owner; response simulator was off with zero active codes.
- Cleared only Play Store's cache through its native Settings screen; disabled
  Clear cache control confirmed completion. No Play Store data reset/account removal.
- Found an independent initialization defect: with auto reconnection enabled,
  `isReady()` was true before the app had received its initial setup callback,
  so the original guard skipped explicit setup. Code 9 now tracks completion,
  calls `startConnection()` initially and gates queries/purchases until success.
  Verified callback 0 and `setupComplete=true` on the physical release.
- Native support diagnostics expose response codes/public catalogue IDs, not
  receipts, signatures, account IDs, purchase/offer tokens or credentials.
  Product-not-found UI now states the actual lookup failure. Explicit purchase
  and restore result messages survive subsequent catalogue/config callbacks.
  Final cancellation message survived a fresh status query.

During the investigation the main package became absent (the mechanism was not
established). Installed the final signed test build and restored the fresh design
backup. Product lookup then returned the item. A controlled comparison installed
the older code-9 diagnostic **without** the explicit-setup fix: it also returned
the product and price. Restored the final APK afterward. Therefore the setup fix
alone did **not** resolve the original SKU-not-found failure. Delayed/stale Play
catalogue state or installation/account state is the likely explanation; the
specific server/cache cause is unconfirmed. No Console activation, price,
security setting, product ID or release was changed.

Google documents newly created products not yet propagated as one possible
PRODUCT_NOT_FOUND cause: [official status reference](https://developer.android.com/reference/com/android/billingclient/api/UnfetchedProduct.StatusCode).
The initial-handshake fix follows [Google's integration guidance](https://developer.android.com/google/play/billing/integrate#connect_to_google_play).

## Validation and cleanup

- Signed APK/AAB build, lint, three billing JVM test methods and **20 release checks** pass.
- Exact final APK was pulled from the phone and byte-matched. Non-debuggable;
  no WebView debugging socket enabled. Original draft/favourites hash-match the
  fresh backup, original font/rotation/stay-awake/screen timeout settings restored,
  and temporary same-signer helper uninstalled. Normal app 9 left installed.
- Google's Billing Lab was installed from Google Play. Owner approved its displayed
  terms; sign-in was already completed when inspected. An unsaved country field
  was inspected, but **no configuration was applied**, no simulator enabled and
  no subscription or real-payment setting changed. Lab was closed at cleanup.
- This is focused native billing availability/checkout evidence, not the full
  free-editor regression or completed purchase validation. The verifier correctly
  leaves full native and paid billing validation pending.

APK SHA-256: `65c557f7078dc10c1a298c8270571149c8d678212e82a54c8cdaa0be31bc5621`.

AAB SHA-256: `81ff85624e71b5052030e9654c1dcf50b052f61ffd08b7d5ae732d8bd2487876`.

Evidence: `artifacts/billing-investigation-1.1.0-code9/`:
`after-play-cache-refresh.json`, `after-explicit-setup.json`,
`after-fresh-install.json`, `old-client-after-catalog-recovery.json`,
`final-billing-status.json`, `cleanup-results.json`, `fancy-text-pro-available.png`.
The attempted intermediate APK pull after the package became absent failed;
no exact intermediate-runtime hash is claimed. Before-setup diagnostic APK is
archived with its mapping for the comparison. Code 8 is archived under
`artifacts/archive/pro-1.1.0-code8-before-billing-investigation/`.

Build/check logs: `artifacts/pro-billing-code9-final-build.log`,
`artifacts/pro-billing-code9-release-checks.log`. Fresh private restore snapshot
is selected by `.local/current-billing-fix-backup-path.txt`; never publish its
preference/receipt content or replace it with an older snapshot.

Phone: **9**. Internal Play: **7**. Production: **1.0.2 (5)**.
Publication preparation now supersedes the earlier local-only status: exact code 9
was accepted by Play, and the production release is saved at 100% of all 178
targeted countries. Pro listing, app access and hosted policy are updated. All three changes
were sent for review after owner-approved IARC acceptance; see
`PRODUCTION_1.1.0.md`. Google approval/public availability are pending. Resume
no-charge purchase/acknowledgement/restore/owned-feature tests only when requested.
