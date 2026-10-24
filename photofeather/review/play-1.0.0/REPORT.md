# PhotoFeather 1.0.0 — signed release and internal draft

24 October 2026 · `com.kreativ.photofeather` · versionCode **4**.

## Completed

- Created a dedicated RSA 4096 upload key outside Git, stored its password in macOS Keychain, and verified an encrypted keystore backup with file permissions `0600`. Signing locations and recovery details are in [PLAY_RELEASE.md](../../docs/PLAY_RELEASE.md). No private key/password is included here.
- Built the production [signed AAB](PhotoFeather-1.0.0-build4-signed.aab) and [signed APK](PhotoFeather-1.0.0-build4-signed.apk). APK signature verification, AAB JAR signature verification and Google's bundletool 1.18.3 validation pass. [Final build](signed-build-final.log), [APK signature](apk-signature.txt), [AAB signature](aab-signature.txt), [certificate](upload-certificate.txt), [bundle validation](bundle-validation.txt), [checksums](SHA256SUMS.txt).
- Added a complete offline policy in **About → Privacy policy**, removed the provisional-name sentence from About, and prepared the same policy as an HTML artifact. [Running-app capture](privacy-policy-running.png). The public HTML page is **not deployed** and no proposed policy URL was entered as if it were live.
- Prepared English title, short/full descriptions, release notes, a 512 × 512 Play icon, a 1024 × 500 feature graphic and six genuine app captures. Play accepted the images at 1188 × 2112 / 9:16. Source screenshots use synthetic fixtures; system bars are cropped and neutral margins added, preserving all remaining app pixels. [Assets and provenance](../../store-assets/play/assets.json).
- Used the explicit **Create app → Check availability** control: `com.kreativ.photofeather` was available. The owner approved the Developer Programme Policies and US export declarations, then the app was created in the Made by Kreativ account. App ID: **4975471320992261227**.
- Saved the default **English (United States) listing as Draft**, including all text, icon, feature graphic and six screenshots. Saved **Photography**, `info@madebykreativ.com` and `https://madebykreativ.com/` as unsubmitted store settings. [Listing draft](console-listing-draft.png), [settings](console-store-settings.png).
- Uploaded the signed AAB; Play accepted **4 (1.0.0), API 26+, target SDK 36**. Play App Signing is in use; the Google-managed app signing key is separate from the local upload key. [Certificate source record](play-signing.json).
- Saved **“1.0.0 (4) — Internal draft”** with release notes. The track is visibly **Inactive** and the release **Draft**. No tester list was assigned, no invitations were sent and **Save and publish was not clicked**. [Final Console state](console-internal-draft.png).

Console: [internal testing](https://play.google.com/console/u/0/developers/7594712361231234431/app/4975471320992261227/tracks/internal-testing), [listing editor](https://play.google.com/console/u/0/developers/7594712361231234431/app/4975471320992261227/store-listings/default/edit).

## Verification

**95 final automated checks passed** on the host JVM and Samsung Galaxy A55 / Android 16:

| Suite | Passed | Evidence |
|---|---:|---|
| JVM policy, JPEG structure and resize | 26 | [XML results](test-results/) |
| Android image engine | 19 | [Log](engine-device-tests.txt) |
| App workflow and regression | 47 | [Final log](app-device-tests-final.txt) |
| Non-debuggable release smoke, including offline privacy policy | 3 | [Final log](release-smoke-tests-final.txt) |

App and engine debug/release lint report **zero errors**; advisory warnings remain. [Lint evidence](test-results/). [Verification build](verification-build.log), [QA release build](qa-release-build.log), [final privacy test build](privacy-test-build-final.log). `git diff --check`, signing helper syntax and store text/image limits also pass.

The first full workflow run had one timing-sensitive failure: a fixed 250 ms wait reached the failed-target screen too early. The unchanged test passed in isolation; its fixed delay was then replaced with a bounded wait for the actual visible result. The final full 47-case run passed. The new privacy test initially looked for a case-sensitive button label; Android's capitalised button presentation required a case-insensitive match. The final three-case release run passed. Initial logs are retained as [workflow attempt](app-device-tests.txt), [isolated result](target-failure-isolated.txt) and [privacy attempt](release-smoke-tests.txt); they are not counted as clean runs.

Tests ran in `.playverify` and `.releasecheck`, which were removed afterward. The existing `.debug` app/data were not cleared or replaced. These were local QA tests; the internal Play draft was not activated and no Play-installed end-to-end test is claimed.

Play's draft review shows **two warnings and no errors**: no testers are configured (intentional), and no deobfuscation file is attached (`minifyEnabled false`, so no app mapping file is generated). [Validation capture](console-internal-validation.png).

## Still required before a public release

- Publish and verify the prepared public privacy page when authorised, then set its real URL in Play. The in-app offline policy is already included.
- Complete the remaining applicable Play app-content forms, target-audience decision and content-rating questionnaire; none is claimed complete here.
- Run the human TalkBack speech/swipe pass and review broader device/provider coverage. This pass did not revalidate older Android; the emulator could not start because the Mac had insufficient free disk space. Earlier Android 9 evidence is historical.
- Select testers and explicitly activate an internal release only when the owner requests it. No production release/review/publication was performed.

The release preparation snapshot includes the source changes, signed artifacts, listing assets and validation evidence. The private signing key and password remain outside Git. No existing Made by Kreativ app, website deployment, paid service, billing, ads or analytics was changed.
