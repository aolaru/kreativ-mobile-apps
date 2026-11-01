# Kreativ Font for Android

The focused Android companion to kreativfont.com, using the existing Google Play package `com.kreativ.fontapp`.

## R8 update — 1 November 2026

Local **2.3.1 / code 206** enables release code optimization, obfuscation and resource shrinking with the standard Android optimization rules. The signed APK is **360,750 bytes**, down from **2,814,223 bytes** for the published 2.3.0 APK (about **87.2% smaller**). This measures file size, not startup or memory performance. Package, signing key, Android compatibility, permissions and privacy practices are unchanged. This update has not been uploaded or submitted to Google Play; 2.3.0 remains the confirmed live app.

Verified on the real Samsung: both optimized-release workflows pass (28.753 seconds), covering home/search paging, All/Commercial/Free filters, favorites preservation/recreation, full-screen artwork and zoom restoration, version footer and maker-link intent. All 32 unit tests pass; lint has zero errors and six dependency-version warnings. An intermediate website HTTP 429 was recorded before the final paced run passed without relaxing count assertions. The signed update is installed in place; the temporary test helper was removed and the main app reopened. No emulator or app-data clearing was used.

Release artifacts and the exact matching R8 `mapping.txt` are retained outside Gradle's cleanable build directory in Git-ignored `.release-archives/2.3.1-code206-r8/`. Keep these together and upload the mapping with this legacy APK when publishing. Do not regenerate a mapping later and associate it with an older APK.

For optimized-device checks, build with `JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew -PkreativfontOptimizedChecks assembleReleaseAndroidTest` and run `com.kreativ.fontapp.test/com.kreativ.fontboard.OptimizedReleaseRunner` on the unlocked physical Samsung. The test-only platform runner avoids AndroidX test-runner dependencies removed from the app by R8; checks use UI controls and framework lifecycle APIs, not app-private fields or optimization-restricting keep rules. The older white-box device tests are historical and should not be used to validate this optimized APK. See `review/release-2.3.1.txt` for verification status, hashes and commands.

## Marketing preparation on 18 October 2026

**Latest Play update:** Console confirms the original listing refresh published
on 18 October. A separate en-GB full-description-only change adding the live
tutorial link was saved and submitted with user approval. Console shows
**Changes in review**, with quick checks running; link approval/publication
pending. Other fields/assets and the AI declaration were unchanged. See
`promotion/2026-10-18/listing/TUTORIAL-LINK-SUBMISSION.txt` and its screenshot.
Earlier submission/preparation notes below are historical.

**Website publication update:** The five-screenshot tutorial is live at
https://madebykreativ.com/news/discover-fonts-android-kreativ-font/ and linked
from the existing app page. Full website checks/deployment passed; the public
page, app-page cross-link, five original/responsive screenshots, Updates, RSS
feeds and sitemap were verified. See
`promotion/2026-10-18/tutorial/PUBLISHING-NOTES.txt`. Social/outreach remain drafts.
No Play changes or new approval-status lookup occurred in this publication step.
The earlier preparation/review notes below are historical.

**Listing submission update:** The user approved retaining **Kreativ Font: Font
Discovery** and submitting the refreshed copy, five screenshots and feature
graphic without AI marks. Console confirms **Changes in review** for four
default-listing changes; quick checks still running. Approval/publication pending.
The other 12 languages' text was verified unchanged, with new default visuals
shown in each locale asset view. The old AI-assisted feature was replaced, not
reclassified; **Don't label assets** was verified after saving. No production
release, pricing, permissions, privacy, account or website changes were made.
See `promotion/2026-10-18/listing/SUBMISSION-STATUS.txt`. The preparation notes
below are historical; website/social/outreach remain drafts.

The public Google Play About this app dialog confirms **2.3.0 is live**, updated
15 October. The older review-status notes below are historical.

A draft marketing/listing pack is in `promotion/2026-10-18/`; open `index.html`
for the review board. It recommends keeping **Kreativ Font: Font Discovery**,
with revised en-GB copy, five real-screen layouts, a code-built feature graphic,
three social graphics, English/Romanian captions, a 20-second silent screenshot
tour, a five-image tutorial, website proposal and three unsent editorial pitches.
The app is free; linked fonts have their own prices and licences. No installer,
editor, identifier, custom-text testing or fully offline catalogue is implied.

Nothing was saved to Play Console, submitted, published or sent. The 13 existing
listing languages, app code, account, pricing, permissions and privacy practices
were unchanged. No AI marks are baked into the new assets; current Console
declarations were not inspected or changed. The old AI-assisted feature graphic
must be replaced before any non-AI declaration. See the pack's `START-HERE.txt`,
`claims-and-sources.txt` and `verification.json` for provenance and review limits.

## Current release status — 15 October 2026

Signed APK **2.3.0 / code 205** and all 41 prepared metadata changes were submitted together to Google Play. Publishing overview confirmed **Changes in review**, with automated quick checks still running before review. The production release targets a full rollout in all existing countries after approval. Managed publishing remains off, so approval can publish automatically; submission is not confirmation that 2.3.0 is live. Version 2.2.1/code 204 was the last confirmed live release. Original-key APK updates remain in use, without Play App Signing enrollment or key transfer. Evidence and artifact hash: `review/play-listing-2.3.0/README.txt`.

## First-version scope

- Discover the ten latest commercial fonts and ten latest free fonts from the live WordPress API. Each horizontal section ends with a Load 10 more card; the two sections paginate independently and preserve the current scroll position. End-of-catalogue and retry states are explicit, and repeated article IDs are merged when page boundaries shift.
- Website-matched branding: the original Kreativ Font logo, indigo accents, dark type and two swipeable preview sections. Tap a preview or title for the full description; tap View font for the original article.
- Load more is available as a full-width, section-labelled button below each carousel, as well as inside a tinted, indigo-outlined end card matched to the carousel height. Both actions share the same paging/loading/retry state and use a minimum 56dp tap target.
- A centered footer below the website/privacy links reads "Version 2.3.0 · Made by Kreativ", matching Northlight. The version is populated from the build configuration rather than hard-coded and remains plain, muted text. Only "Made by Kreativ" is an indigo link with a 48dp touch target, opening `https://madebykreativ.com/apps` in the user's browser. The footer appears on Fonts, search and Favorites screens.
- See genuine website artwork and article excerpts, with links to each original article.
- Tap website artwork to open a dedicated full-screen viewer with pinch zoom, drag-to-pan, double-tap zoom/reset and accessible Zoom in / Zoom out / Fit buttons. The viewer uses original artwork when available, bounds decoded image dimensions, offers retry on failure and restores zoom after recreation. Existing saved previews remain compatible.
- Search the full public commercial/free font catalogue on kreativfont.com, ranked by the website's relevance search, with ten unique results per batch and Load 10 more. If cached server pages overlap, search fetches additional pages to fill the batch and buffers unused matches for the next tap; only the catalogue's end may produce a smaller batch. Each tap is bounded to five server requests, with a retry state retaining successful partial work. All / Commercial / Free filters query the server, apply to every result page, reset pagination on change and survive activity recreation. Typing is debounced, stale responses are ignored, and search results are separate from the loaded home shelves. Search requires internet and sends the phrase to the website. Favorites search remains local to saved items.
- Save and remove favorites without an account. Each favorite retains its own article ID, description, artwork URL and article link after it leaves the rolling feed.
- Update on launch or tap Refresh. Previously fetched entries remain available offline; Glide caches downloaded artwork, subject to normal cache eviction. Article browsing requires internet access.
- Existing project selections migrate into Favorites. Older names without an article link open a clearly described website search until the article is matched in the live feed.
- Pairing and image recognition are deferred; there are no simulated specimens or image-picker placeholders.

The only runtime capability permission is `INTERNET`. The app fetches public JSON and preview images from kreativfont.com and its WordPress image CDN. It contains no analytics or advertising SDK and requests no camera, photo-library, location or account access. Favorites stay in private local storage; cloud backup and device transfer are disabled. Website catalogue search sends the phrase as an HTTPS API query. The app keeps no search-history list or query-bearing error logs; Android may retain the current query to restore the search screen. Hosting logs can include request URLs. Website articles and the privacy policy open in the user's browser. The Android privacy appendix was updated publicly on 15 October for 2.3.0. Data safety and all 13 listing languages/screenshots were submitted with the signed APK as 42 changes in total. Console confirmed Changes in review, pending successful automated quick checks. See `review/play-listing-2.3.0/README.txt` and `review/release-2.3.0.txt`.

## Build

Open this folder in Android Studio and run the `app` configuration, or run:

```sh
JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew assembleDebug
```

Use JDK 17. Android Gradle Plugin 8.10.1 with Gradle 8.11.1 supports the app's compile/target API 36 without suppressing compatibility warnings. The minimum Android version is API 26 (Android 8).

AndroidX Fragment is constrained to `1.9.1`, replacing Glide 4.16.0's transitive `1.3.6` request, using the same approach as Northlight. The release dependency graph and packaged `META-INF/androidx.fragment_fragment.version` both confirm `1.9.1`.

The final legacy production release is `2.0.0` / version code `200`; the last confirmed live rebuilt app is `2.2.1` / code `204`. Build `2.3.0` / code `205` was submitted for review on 15 October 2026 with all prepared metadata. The legacy app is not enrolled in Play App Signing, so its original private signing key is used for compatible releases. The recovered JKS has been verified against the legacy APK's certificate and uses the alias `andrei.olaru`.

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

Unit tests cover feed parsing, HTML decoding, absent artwork, URL validation, original-image metadata compatibility, storage round trips, favorite retention/migration, request encoding, server-side filters, category classification, independent paging, duplicate merging, retries, end states, stale responses and zoom/pan bounds. Optional live tests check 20 preview URLs, both sections' next pages, older search matches, search pagination and both category filters. `CatalogueWorkflowTest` exercises the signed release on a real Android device, including ten-per-section, Load more layout and both entry points, full-catalogue/filtered search, paged results, rotation and favorite preservation. A temporary favorite is saved, persisted across recreation, then removed without changing the original saved list. `PreviewWorkflowTest` checks the full-screen image, zoom controls, fit, recreation, and injected double-tap/two-finger pinch gestures. These do not clear app data or change network settings. Building a signed bundle does not submit it to Google Play.

Latest fix and verification, 15 October 2026: the 19-results issue was traced to overlapping cached website pages (different server cache revisions and repeated article IDs), not a missing font card. `FontSearch` now fills ten-unique-font batches and buffers extras without weakening deduplication. Six new deterministic tests cover overlapping/repeated pages, surplus results, retry after a partial fetch, catalogue ends, bounded work and separate query sessions. All 32 unit tests pass; the expanded three live API checks also passed, including twenty unique results in All/Commercial/Free. Lint has no errors (five existing dependency-version warnings), and signed APK/AAB/device-test APK builds pass. Search error diagnostics record only HTTP status or exception class, never phrases, URLs or exception messages.

The latest signed 2.3.0/code 205 was installed in place on Samsung SM-A556B. All four device tests passed in 14.087 seconds, including ten-to-twenty results for each search filter, both home paging actions, favorites preservation, preview gestures/recreation, plain dynamic version text and the separate 48dp maker credit/browser intent. An earlier extended run encountered a Commercial batch-2 request failure; the final full run passed unchanged search assertions. The Apps page returns HTTP 200 with title "Kreativ Apps | Made by Kreativ". The temporary test helper was removed and the main app reopened. No emulator was used and app data was not cleared. The verified artifact and all privacy/Data safety/listing changes have since been submitted together for Google Play review as described above.

Earlier completed verification on 15 October, before making the credit clickable, passed all 29 unit/live API tests and all three real-phone workflows, including the version footer and Fragment 1.9.1 upgrade. Catalogue paging/search/favorites, preview gestures/recreation and the dynamic footer on Fonts/search/Favorites were checked, with original favorites unchanged. Earlier artwork/Load more screenshots are in `review/phone-2.3.0/review/`. These historical checks preceded the completed privacy/Data safety/listing preparation described above.
