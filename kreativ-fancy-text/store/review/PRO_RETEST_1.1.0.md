# Fancy Text Pro phone retry — 4 November 2026

At the owner's request, retried Google Play ownership restoration and product
lookup on the connected Galaxy A55 / Android 16. Installed the exact signed,
shrunk **1.1.0 (8)** APK in place over 7; app data survived the upgrade. APK SHA-256:
`440b156a0582d42ad6e6238a94a564f71b372d67809cdbe78922e8c2dc44b59a`.
The app is non-debuggable; no WebView debugging socket was enabled.

## Result

**Billing is still blocked.** Native restore reports **“No completed Pro purchase
found for the current Google Play account.”** Status is `owned=false`,
`available=false`, `pending=false`. Product lookup still makes the purchase button
unavailable. An existing recognised Fancy Text Pro purchase therefore does not
explain this attempt's failure. No checkout or payment was performed.

Read-only Console checks reconfirmed Fancy Text Pro, SKU `kreativ_pro_lifetime`,
permanent `buy` option **Active** in **174 countries/regions**. The selected license
tester list contains the owner; the phone's Play listing shows **Internal Beta**
and **“You're an internal tester.”** No Console setting was changed.

A cache-only Play Store refresh command did not return and its host process was
stopped; cache-refresh success is **not** claimed. A subsequent cold app session
and Restore purchase returned the same result. No Play Store data reset, account
removal or main-app uninstall was performed.

The earlier code-7 unminified SDK diagnostic found successful query response 0
but `PRODUCT_NOT_FOUND` (3); this retest did not collect fresh numeric SDK results.
Propagation remains a possible cause, not a confirmed diagnosis. Google supports
sideloaded builds for registered license testers, so local build 8 alone does not
establish the cause. [Official billing testing guidance](https://developer.android.com/google/play/billing/test).

## Focused phone checks passed

- Header Pro button absent; dialog and unlock controls say **Fancy Text Pro**.
- All 12 premium template previews open; solid/gradient background previews work.
- Preview exploration leaves draft/favourites unchanged; applying a premium look
  and saving a preset remain locked without a verified purchase.
- Native clipboard and Undo/Redo work; no captured JavaScript errors.
- Original draft/favourites restored and hash-matched against a fresh pre-retest
  backup. Billing cache remained empty. Device settings restored, helper removed,
  exact signed APK byte-matched, and ordinary shipping app launched normally.

Evidence: `artifacts/phone-pro-retest-1.1.0-code8/`, including
`initial-restore.json`, `retest-results.json`, `cleanup-results.json`,
`header-without-pro.png`, `fancy-text-pro-unavailable.png`.
Private fresh APK/preferences/billing backup stays under ignored
`.local/phone-backups/pro-retest-20261104T093459Z/`; do not publish receipts or
replace this with the older pre-Pro snapshot.

These are focused smoke checks, **not** the full code-8 native regression.
Checkout cancellation/success, receipt acknowledgement, owned templates/custom
backgrounds/presets and exports, fresh restore, offline access and revocation
remain unvalidated. Do not reuse code-7 coverage as code-8 coverage or introduce
an unlock bypass. Retry when Play returns the product; if it persists, investigate
the Play catalogue/account issue before production. Use only an explicitly
no-charge Google Play test instrument.

Phone now: **1.1.0 (8)**. Internal track: **1.1.0 (7)**. Production: **1.0.2 (5)**.
No new upload, production submission or publication occurred in this retry.
