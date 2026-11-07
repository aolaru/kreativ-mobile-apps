# PhotoFeather 1.1.0 (build 5) release preparation

Prepared 7 November 2026. No build was uploaded or submitted for review in this preparation.

## Play Console

The Latest releases and bundles page showed production version 1.0.0 (code 4) available on Google Play with a full rollout. The only listed app bundle was code 4; the internal build 4 release remained a draft. Publishing overview showed no unpublished changes and managed publishing was off. Build 5 has not been uploaded or validated by Play.

## Signed artifacts

The locally prepared production AAB, companion APK and matching R8 mapping are in the ignored `build/release-1.1.0/` directory. They were built by `scripts/build-play-release.sh` with the external upload key. The APK has application ID `com.kreativ.photofeather`, version name `1.1.0`, version code 5, and the same SHA-256 signing certificate fingerprint as the archived build 4 upload APK. The AAB was verified by jarsigner, and its embedded R8 mapping matched the archived mapping exactly.

- Signed AAB SHA-256: `ef3c97868d6c45ec7047b25fdce81b76d2b34c18882710156232d891bdce6561`
- Signed APK SHA-256: `1cb6673edbf7642a0a335c4d9fed2fafd3d7a6d3e22cc67bedea0afff7fb2a1b`
- Mapping SHA-256: `5621c26f19f040dc98de49123c80233c9d7277181de7850088b67c792f2c7f37`

Self-signed certificate and lack-of-timestamp warnings from jarsigner are expected for the upload key; the bundle signature verified. Keep the mapping with this exact bundle. QA builds can overwrite Gradle's normal output directory: use the archived files above.

## Device check

On a Samsung Galaxy A55, the production-signed, R8-optimised build 5 APK was installed with its actual production application ID. A generated 1600 × 1200 JPEG was selected; a preset was saved and applied; the batch prefix was set; preview completed; three output versions were generated; all four results were saved with correctly numbered names; and ZIP sharing reached Android's share sheet. All four saved JPEGs reopened, had no EXIF tags, and the generated source file's SHA-256 remained unchanged. This was a workflow smoke test; it did not independently inspect the ZIP contents on this exact build or cover all devices, providers, accessibility services or error scenarios. Earlier tests cover ZIP contents, cancellation and low storage separately, using builds described in `review/pro-features/REPORT.md`.

The [public privacy policy](https://madebykreativ.com/privacy/photofeather/) includes presets, ZIPs and custom names. Release notes are in `store-assets/play/en-US/release-notes.txt`. All new features are presently available without billing or a paywall.

The separate non-debuggable R8 QA release passed all three existing device smoke tests after updating their expected version. This test variant includes QA-only compatibility keep rules; it is not the production-signed APK used for the workflow smoke test above. Release lint passed with no errors. Earlier engine unit tests passed 26/26; this version change did not modify processing logic.
