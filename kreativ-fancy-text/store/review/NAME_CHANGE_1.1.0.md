# Fancy Text Pro name and header change — 4 November 2026

The owner requested removal of the header Pro button and renaming the optional
upgrade from Kreativ Pro to **Fancy Text Pro**.

- Removed the header entry and its unused styles. Upgrade information remains
  inside **Make it yours**, including Restore purchase; locked template/background
  previews and saved-preset actions still open the purchase dialog when needed.
- Updated dialog title, purchase/apply buttons, billing messages, bundled policy,
  local hosted-policy sources and draft listing/release notes to Fancy Text Pro.
- Saved **Fancy Text Pro** as the Google Play one-time product's display name.
  Product ID `kreativ_pro_lifetime` and permanent `buy` option stay intact;
  price remains €4.99 in EUR regions. Evidence:
  `fancy-text-pro-renamed.{png,txt}`.
- Prepared local signed **1.1.0 (8)** because uploaded version codes 6 and 7 are
  immutable. At preparation, internal testing and the phone remained on 7. The
  subsequent retest installed 8 on the phone; internal testing remains 7 and
  production remains 1.0.2 (5). Build 8 has not been uploaded or published.

Validation: all **9 model/history and 28 desktop/mobile browser tests pass**,
including absence of the header entry, the new dialog name, locked/free/owned
flows, 320 px / 200% layouts and axe accessibility checks. Existing test cases were
updated for the new access point. Signed APK/AAB build and all **20 release checks pass**. The aggregate
Android test task also passes the three billing unit-test methods. Code 7 native evidence is not reused for code 8.

Logs: `artifacts/pro-name-change-tests.log`,
`artifacts/pro-name-change-release-build.log`,
`artifacts/pro-name-change-verification.log`.
Previous code 7 artifacts/reports are preserved in
`artifacts/archive/pro-1.1.0-code7-before-name-change/`.

The previously recorded Play PRODUCT_NOT_FOUND issue and paid purchase/restore
validation remain pending. The updated hosted privacy policy is still local and
must be published before a Pro production release.

## Signed local build 8

- APK SHA-256: `440b156a0582d42ad6e6238a94a564f71b372d67809cdbe78922e8c2dc44b59a`.
- AAB SHA-256: `78bc1bcd83ad57f127dee205c8819daf7e18be0a2da5f85e5510aed1aaf1116c`.
- Exact release flags, signatures, packaged name/policy, R8 configuration and
  retained reflection callbacks pass the release verifier. Full native
  regression for this APK remains pending; focused code-8 phone checks and
  the still-blocked billing retry are recorded in `PRO_RETEST_1.1.0.md`.
  Code 7 evidence is preserved.
