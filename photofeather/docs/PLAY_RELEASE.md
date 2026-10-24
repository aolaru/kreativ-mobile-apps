# PhotoFeather 1.0.0 — Google Play preparation

Package: `com.kreativ.photofeather` · version `1.0.0` · versionCode `4` · minSdk 26 · targetSdk 36.

Google Play's **Create app → Check availability** control reported **Package name available** on 24 October 2026. The signed initial bundle was subsequently accepted into the PhotoFeather internal release draft (Play app ID `4975471320992261227`). The production application ID is now fixed for this app. The earlier empty Android developer verification package draft was removed. Do not create another verification draft merely to check availability.

## Signed artifacts

Use `review/play-1.0.0/PhotoFeather-1.0.0-build4-signed.aab`, already accepted by Play and selected from the artifact library for the production candidate. The companion signed APK is for direct inspection; Google Play distributes its own APKs signed using its app signing key. The archived artifacts contain the offline privacy policy in About and no provisional-name notice.

Rebuild on this Mac:

```sh
bash scripts/build-play-release.sh
```

The script retrieves the password from macOS Keychain and verifies the resulting AAB and APK signatures. It does not upload anything. Outputs are in `app/build/outputs/bundle/release/` and `app/build/outputs/apk/release/`. Copy a final build to the review folder and refresh checksums before uploading; QA builds can replace Gradle's transient release outputs.

### Dedicated upload key

- Keystore: `~/Library/Application Support/Kreativ/PhotoFeather/photofeather-upload.jks`
- Key alias / Keychain account: `photofeather-upload`
- Keychain service: `com.kreativ.photofeather.upload-key`
- Encrypted keystore backup: `~/Documents/Android Signing Backups/PhotoFeather/photofeather-upload.jks`
- RSA 4096; public certificate/fingerprint evidence is in `review/play-1.0.0/upload-certificate.txt`.

The key and password are not in this repository. The backup is on this Mac; preserve an encrypted copy separately for disaster recovery. The Keychain password is also needed to use the backup. This is an upload key; it is not a Google-generated Play app signing key and was used for the accepted first Play upload. The Google-managed app signing certificate is separate.

External signing overrides remain available through `PHOTOFEATHER_KEYSTORE`, `PHOTOFEATHER_STORE_PASSWORD`, `PHOTOFEATHER_KEY_ALIAS` and `PHOTOFEATHER_KEY_PASSWORD`. Do not pass secrets as command-line options or commit signing material. A plain Gradle release command without these values is still unsigned.

## Listing materials

`store-assets/play/en-US/` contains the 30-character title, 70-character short description, full description and release notes. Saved category: **Photography**. The app is free, contains no ads or purchases, and has no login or restricted features. Support and feedback email: `info@madebykreativ.com`, matching the existing Made by Kreativ website. Website: `https://madebykreativ.com/`.

`store-assets/play/` contains a 512 × 512 RGBA Play icon, a 1024 × 500 RGB feature graphic and six RGB phone screenshots. `assets.json` records alt text and the source of every screenshot. Captures are from the running app with synthetic fixtures; system status/navigation bars are cropped and neutral margins preserve the full remaining UI at the Console’s required 9:16 ratio. No app UI or result figures are reconstructed. Screenshots are 1188 × 2112 with no scaling or distortion.

To regenerate graphics, provide Node.js and `sharp` in Node's module search path, then run:

```sh
node scripts/prepare-play-assets.cjs
```

Editable graphics are SVGs. This script does not call image-generation services, upload photos or modify the app's launcher resources.

The required listing review is complete and saved in Publishing overview. In Play's AI-asset declaration, the icon and feature graphic are labelled as created/edited using AI because their vector artwork was AI-assisted. The six genuine running-app screenshots are not labelled as AI-generated.

## Privacy and declarations

The complete offline policy is in `app/src/main/res/raw/privacy_policy.txt` and available from About → Privacy policy. The owner authorised website publication: **https://madebykreativ.com/privacy/photofeather/** is live over HTTPS, effective 24 October 2026, and its verified URL is saved in Play. The existing in-app policy text satisfies Google's allowance for a privacy-policy link or text inside the app; no app rebuild was needed. `store-assets/play/privacy-policy.html` remains the prepared standalone artifact, while the live Astro page is maintained in the separate website repository.

Saved Play declarations, checked against build 4:

- Ads and Advertising ID: no.
- App access: all functionality is available without special access.
- Data safety: no reportable data collection or sharing by PhotoFeather. On-device processing, user-chosen secondary-app sharing and cloud-picker retrieval are described separately in the policy. Reassess if dependencies or behaviour change.
- Account creation/deletion: no app account exists.
- Sensitive photo/storage permissions: none requested. AndroidX's private receiver permission is not broad media access.
- Target audience: ages 13–15, 16–17 and 18+, explicitly chosen by the owner.
- Government, financial and health features: no.
- IARC: owner explicitly accepted its terms; questionnaire completed on 24 October 2026. Ratings include ESRB Everyone, PEGI 3, Brazil All ages, USK All ages and generic IARC 3+. Ratings are distinct from the chosen target audience.

The App content overview reports no declarations needing attention. See [production preparation evidence](../review/production-preparation/REPORT.md).

## Internal testing and production boundary

The owner initially chose **internal draft only**; Play calls activation of an internal test publishing too. The later request authorised completing Play setup and preparing a production candidate, while explicitly prohibiting submission or app publication. No internal test has been activated, no tester list changed and no invitations sent. Do not activate testing or submit the production changes without a new request.

The prepared Create app form uses the title above, English (United States), App, Free and the validated package. The Developer Programme Policies and US export-law declarations require explicit approval before accepting them. The owner explicitly approved both declarations, and the app was created as PhotoFeather: Photo Compressor on 24 October 2026 (Play app ID 4975471320992261227). The owner specified an internal draft only; do not activate the test.

Completed in Console: created the app after explicit declaration approval, uploaded the archived signed AAB to **Internal testing**, verified version 4 / 1.0.0 / API 26+ / target 36, and added release notes. The default listing, Photography category, support email, HTTPS website, public privacy URL and all applicable declarations are now saved. Play App Signing is in use. The uploaded bundle was accepted; an independent copy/download comparison of the Console upload fingerprint was not available, so the certificate record identifies its source rather than claiming that comparison.

Play validation showed **two warnings and no errors**: no testers are configured (intentional for a draft) and no deobfuscation file is attached (this build has `minifyEnabled false`). No tester list was changed, no invitation sent, and **Save and publish was not clicked**. Internal testing remains inactive.

The **1.0.0 (4) — Production candidate** release reuses the accepted bundle. It targets all available locations: 177 named countries/regions plus Rest of world (178 selection entries). Production validation reports no errors and one expected missing-deobfuscation-file warning; the build is not obfuscated. The release was saved to Publishing overview, which shows **11 changes not yet submitted for review**. The dashboard confirms **Not yet sent for review**. No Submit/Send for review, Publish or Save and publish action was taken. Managed publishing is currently off; review submission is a separate consequential step and must be explicitly authorised.

Google's automatic quick checks begin after saving the candidate; their final status is recorded in the production-preparation report. They are not production approval or evidence of Play-installed testing. Human TalkBack speech/swipe verification and broader physical-device/provider coverage remain outside the completed automated QA. Before a future submission, review these remaining validation limits and confirm the desired launch behaviour with the owner.

Google guidance: [create an app](https://support.google.com/googleplay/android-developer/answer/9859152), [internal testing](https://support.google.com/googleplay/android-developer/answer/9845334), [signing](https://support.google.com/googleplay/android-developer/answer/9842756), [store assets](https://support.google.com/googleplay/android-developer/answer/9866151).
