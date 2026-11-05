# Local release validation — 24 October 2026

This report describes the earlier 1.0.0 build. Current source and artifacts are
1.0.1 (4); see `POLISH_1.0.1.md`. Console contains the unpublished 1.0.0 draft.

Kreativ Fancy Text 1.0.0 (3), `com.kreativfont.studio`. This is a release
candidate subsequently uploaded as an unpublished internal-testing draft.

## Results

- Web production build and Capacitor Android sync succeeded.
- Three model tests and ten browser tests passed (desktop and Android layout).
- Signed AAB/APK, R8 shrinking, release lint, and Gradle release unit task succeeded.
- The native unit task currently contains only the generated arithmetic example;
  it is not native export/runtime coverage.
- Release lint: 29 warnings, zero errors. Remaining warnings concern
  tooling/dependency updates and inherited icon/resource templates.
- Production npm audit: zero vulnerabilities.
- All 16 packaged-release/store checks passed.
- The exact signed release APK passed physical-phone checks on Galaxy A55 /
  Android 16, WebView 155.0.8059.46. See `NATIVE_RELEASE_1.0.0.md` for the scope
  and evidence. Original designs were restored and the helper was removed.
- Five screenshots, an RGBA 512 × 512 icon, and an RGB 1024 × 500 feature graphic
  were generated and inspected. Screenshots capture the bundled interface in a
  mobile Chromium viewport, not a physical Android device.

## Signed artifacts

| File | Bytes | SHA-256 |
| --- | ---: | --- |
| `kreativ-1.0.0.aab` | 1,920,259 | `1dc7c4617824171c1c8907b01392ee40f82d46678651965b4259e69cadfeebff` |
| `kreativ-1.0.0-upload-signed-release.apk` | 1,453,020 | `96d675961f833c18d5f58efd6fb0842e4ceb921d8b8a331d4689bfab840fac15` |

Both signatures verify against the preserved upload certificate:

`C7:E9:4D:08:11:22:6B:65:2E:B8:9E:8A:DD:A2:99:0D:AB:20:16:DA:D8:59:8E:5C:FE:16:27:B5:90:75:B4:B2`

Packaged APK permissions contain only the private AndroidX receiver permission.
Both artifacts contain the exact current web assets, local policy, and licenses.
Release logging/WebView debugging are disabled; no native `.so` libraries are
packaged. Explicit cloud-backup and device-transfer exclusions are retained in
the compiled APK. The custom export plugin/method/callback names survive R8.

## Remaining work

Single-device release testing is complete. Other physical devices, older
supported Android/WebView versions, tablets, large accessibility fonts, and
actual receiving-app import remain untested. Share-sheet checks opened previews
and cancelled without selecting a recipient. The Galaxy A55 now has signed
1.0.0 installed with its original draft and favorite restored.

The policy is published and publicly verified at
https://madebykreativ.com/privacy/kreativ-fancy-text/ . The owner
confirmed a personal developer account created on or before 13 November 2023;
the newer-account 12-testers/14-days requirement does not apply.
Initial Console setup is complete under Made by Kreativ: Free, en-US, target
ages matching PaperBuild (13+), IARC rating, listing/graphics, privacy, contact,
and app-content declarations are saved. The owner confirmed the signing backup
before upload. The signed bundle is uploaded to internal release draft 1. It is
not rolled out, has no testers, and no app-review submission has been made. Next
is choosing testers/authorizing rollout or preparing production countries and
release submission. See `../PLAY_SETUP.md` and `../RELEASE_CHECKLIST.md`.

Evidence: `../../artifacts/release-build.log`, `release-tests.log`,
`release-verification.log`, `production-audit.json`, and
`../../artifacts/play/verification.json`.
