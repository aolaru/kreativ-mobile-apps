# PhotoFeather 1.0.0 — Google Play preparation

Package: `com.kreativ.photofeather` · version `1.0.0` · versionCode `4` · minSdk 26 · targetSdk 36.

Google Play's **Create app → Check availability** control reported **Package name available** on 24 October 2026. The signed initial bundle was subsequently accepted into the PhotoFeather internal release draft (Play app ID `4975471320992261227`). The production application ID is now fixed for this app. The earlier empty Android developer verification package draft was removed. Do not create another verification draft merely to check availability.

## Signed artifacts

Use `review/play-1.0.0/PhotoFeather-1.0.0-build4-signed.aab` for the first Play upload. The companion signed APK is for direct inspection; Google Play distributes its own APKs signed using its app signing key. The archived artifacts contain the offline privacy policy in About and no provisional-name notice.

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

## Privacy and declarations

The complete offline policy is in `app/src/main/res/raw/privacy_policy.txt` and available from About → Privacy policy. `store-assets/play/privacy-policy.html` is a prepared public-page artifact. **It has not been deployed.** Do not enter a proposed `/privacy/photofeather/` URL into Play as if it were live. Verify the public page first when website publication is authorised.

Proposed Play declarations, to verify against the uploaded artifact:

- Ads: no.
- App access: all functionality is available without special access.
- Data safety: PhotoFeather itself does not collect or share data off-device; user-chosen Android share-sheet transfers and cloud-picker retrieval are described separately in the policy. Reassess if dependencies or behaviour change. Internal-only apps are exempt from the public Data safety section, but prepare the declarations before broader release.
- Account creation/deletion: no app account exists.
- Sensitive photo/storage permissions: none requested. AndroidX's private receiver permission is not broad media access.
- Content rating, target audience and other applicable declarations: complete truthfully in Console; do not invent an IARC rating or claim questionnaire completion. No target-audience selection has been made.

## Internal testing boundary

The user authorised preparation of a signed build, listing and internal testing, and prohibited production submission/publication. The owner explicitly chose **draft only**; Play calls activation of an internal test publishing too. No open testing or production release should be created.

The prepared Create app form uses the title above, English (United States), App, Free and the validated package. The Developer Programme Policies and US export-law declarations require explicit approval before accepting them. The owner explicitly approved both declarations, and the app was created as PhotoFeather: Photo Compressor on 24 October 2026 (Play app ID 4975471320992261227). The owner specified an internal draft only; do not activate the test.

Completed in Console: created the app after explicit declaration approval, uploaded the archived signed AAB to **Internal testing**, verified version 4 / 1.0.0 / API 26+ / target 36, added release notes, saved the default listing as a draft, and saved Photography / support email / HTTPS website settings as unsubmitted changes. Play App Signing is in use. The uploaded bundle was accepted; an independent copy/download comparison of the Console upload fingerprint was not available, so the certificate record identifies its source rather than claiming that comparison.

Play validation showed **two warnings and no errors**: no testers are configured (intentional for a draft) and no deobfuscation file is attached (this build has `minifyEnabled false`). No tester list was changed, no invitation sent, and **Save and publish was not clicked**. Internal testing remains inactive.

Before any future production submission, deploy and verify the public privacy page, complete the remaining app-content declarations and human accessibility checks, and validate the intended testing audience. Do not activate the draft until the owner requests it.

Google guidance: [create an app](https://support.google.com/googleplay/android-developer/answer/9859152), [internal testing](https://support.google.com/googleplay/android-developer/answer/9845334), [signing](https://support.google.com/googleplay/android-developer/answer/9842756), [store assets](https://support.google.com/googleplay/android-developer/answer/9866151).
