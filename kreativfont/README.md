# Kreativ Font for Android

The focused Android companion to kreativfont.com, using the existing Google Play package `com.kreativ.fontapp`.

## First-version scope

- Discover the ten latest commercial fonts and ten latest free fonts from the live WordPress API. Each horizontal section ends with a Load 10 more card; the two sections paginate independently and preserve the current scroll position. End-of-catalogue and retry states are explicit, and repeated article IDs are merged when page boundaries shift.
- Website-matched branding: the original Kreativ Font logo, indigo accents, dark type and two swipeable preview sections. Tap a preview or title for the full description; tap View font for the original article.
- See genuine website artwork and article excerpts, with links to each original article.
- Search the full public commercial/free font catalogue on kreativfont.com, ranked by the website's relevance search, with ten results per page and Load 10 more. Typing is debounced, stale responses are ignored, and search results are separate from the loaded home shelves. Search requires internet and sends the phrase to the website. Favorites search remains local to saved items.
- Save and remove favorites without an account. Each favorite retains its own article ID, description, artwork URL and article link after it leaves the rolling feed.
- Update on launch or tap Refresh. Previously fetched entries remain available offline; Glide caches downloaded artwork, subject to normal cache eviction. Article browsing requires internet access.
- Existing project selections migrate into Favorites. Older names without an article link open a clearly described website search until the article is matched in the live feed.
- Pairing and image recognition are deferred; there are no simulated specimens or image-picker placeholders.

The only runtime capability permission is `INTERNET`. The app fetches public JSON and preview images from kreativfont.com and its WordPress image CDN. It contains no analytics or advertising SDK and requests no camera, photo-library, location or account access. Favorites stay in private local storage; cloud backup and device transfer are disabled. Website catalogue search sends the phrase as an HTTPS API query; the app does not persist search phrases or log query-bearing errors. Hosting logs can include request URLs. Website articles and the privacy policy open in the user's browser. Before publishing 2.3.0, update the live Android privacy appendix and Play Data safety for remote search; the existing 2.2.1 local-search statement is no longer accurate for this build. See `review/release-2.3.0.txt`.

## Build

Open this folder in Android Studio and run the `app` configuration, or run:

```sh
JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew assembleDebug
```

Use JDK 17. Android Gradle Plugin 8.10.1 with Gradle 8.11.1 supports the app's compile/target API 36 without suppressing compatibility warnings. The minimum Android version is API 26 (Android 8).

The final legacy production release is `2.0.0` / version code `200`; the rebuilt production app is `2.2.1` / code `204`. This next build is `2.3.0` / code `205`, not yet submitted to Google Play. The legacy app is not enrolled in Play App Signing, so its original private signing key is used for compatible releases. The recovered JKS has been verified against the legacy APK's certificate and uses the alias `andrei.olaru`.

To create a signed release without exposing its password to source control:

1. Copy `keystore.properties.example` to `keystore.properties`.
2. Enter the keystore password locally for `storePassword` and `keyPassword` (they are often the same, but use the values that work for the key).
3. Build the release bundle with `./gradlew bundleRelease` using JDK 17.

`keystore.properties` and private keys are ignored by Git. Verify the available release options in Play Console before uploading: this legacy app may still accept its self-signed APK. An Android App Bundle requires Play App Signing; enrollment and transfer of the original signing key are separate security-sensitive steps and must be approved before proceeding. Keep key material securely backed up, and never use a debug APK for store distribution.

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`. Signed release outputs are `app/build/outputs/apk/release/app-release.apk` and `app/build/outputs/bundle/release/app-release.aab`.

## Google Play release preparation — 11 October 2026

- Google Play accepted the original-key-signed APK `204 (2.2.1)`, targeting API 36, in a production release draft. No Play App Signing enrollment or private-key transfer was required for this upload.
- Release notes were saved in en-GB, en-US and Romanian. Version 2.2.1/code 204 and 59 associated changes were submitted on 11 October 2026. The release is now live: the public listing independently confirmed version 2.2.1, updated 11 October 2026, and Console reports a 100% production rollout.
- The updated IARC questionnaire is completed (Everyone / PEGI 3), using `contact@kreativfont.com`. Advertising ID, financial features, health features, government status and app access declarations were also saved.
- The user-approved Android privacy appendix was published on 11 October 2026 at https://kreativfont.com/blog/terms-of-use-privacy-policy#android-app-privacy, preserving the original website terms. Data safety was completed and included in the submitted changes: Diagnostics and Device or other IDs, collected/required/non-ephemeral, for functionality, troubleshooting (Google's Analytics definition) and security; service-provider processing is exempt from sharing. HTTPS transport and email-based deletion requests are declared. The native app does not run website Analytics scripts. Hosting/CDN and Redirection logs are covered in the appendix. See `review/android-privacy-draft.txt` for evidence and `review/android-privacy-appendix-for-approval.txt` for the published text.
- Store descriptions were refreshed in all 13 existing languages. The original logo, matching feature graphic and three genuine Samsung SM-A556B screenshots replace the old assets. The old video link and unused TV banner were removed from the listing, not permanently deleted. Only the new AI-assisted feature graphic is labelled accordingly. Screenshot sources and submission proof are in `review/play-listing-2.2.1/`. The phone was updated to signed 2.2.1 without losing its existing favorites; no emulator images were used.
- A follow-up listing-only clarification was saved and submitted on 11 October 2026: all 13 languages use the name "Kreativ Font: Font Discovery", put the no-phone-font-installation warning in the short description, and begin the full description by distinguishing font discovery from installation or text editing. Commercial/free articles, browser links, license checks and English app content remain accurately described. The 39 metadata changes were initially in review; the public English listing subsequently confirmed the new name, short/full descriptions and revised developer reply live. Evidence: `review/play-listing-2.2.1/discovery-copy-saved.png` and `discovery-submitted.png`.
- The defensive developer reply to the 28 September 2017 review about paid fonts/text editing was replaced on 11 October 2026 with a 315-character apology and explanation of the rebuilt app, discovery purpose, browser links and publisher-controlled prices/licenses. Console verified the published reply with today's timestamp. Evidence: `review/play-listing-2.2.1/discovery-review-reply.png`. Other reviews were left unchanged.
- Release validation reported three warnings: 4,058 previously supported device models no longer supported, APK optimization advice and missing deobfuscation data. The app now requires Android 8 (API 26), whereas the legacy APK supported API 14. Do not include the obsolete API-26-targeted legacy APK simply to suppress the compatibility warning.

## Verification

```sh
JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew testDebugUnitTest lintRelease assembleRelease bundleRelease
KREATIVFONT_LIVE_TESTS=true JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew testDebugUnitTest --rerun-tasks
```

Unit tests cover feed parsing, HTML decoding, absent artwork, URL validation, storage round trips, favorite retention/migration, request encoding, category classification, independent paging, duplicate merging, retries, end states and stale responses. Optional live tests check 20 preview URLs, both sections' next pages, older search matches and search pagination. `CatalogueWorkflowTest` exercises the signed release on a real Android device, including ten-per-section, both horizontal Load more controls, full-catalogue search, paged results, rotation and favorite preservation. It does not clear app data or change network settings. Building a signed bundle does not submit it to Google Play.
