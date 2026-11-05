# Kreativ Fancy Text release checklist

> **Current — 4 November 2026:** Production 1.0.2 (5) is published at 100% rollout.
> Pro 1.1.0 (7) is an internal test release with a separate saved Alpha draft.
> The optional lifetime product is Active at €4.99 in EUR regions; real no-charge
> purchase validation remains pending. Updated policy sources are local and have
> not replaced the hosted October policy. Before any Pro production update,
> complete billing tests, review Data safety and App access for paid features,
> publish the matching policy and update the listing. See
> [Pro status and launch checklist](review/PRO_1.1.0.md).
> Earlier records below describe prior versions; “no billing SDK” applies to 1.0.x.


## Current submission 1.0.2 (5) — 4 November 2026

- [x] Exact signed/shrunk release tested on Galaxy A55 / Android 16, including
  native export callbacks, persistence, Undo/Redo and 200% Android text/rotation.
- [x] Original designs/settings restored; temporary helper and test exports removed.
- [x] All 18 release checks pass with matching native evidence.
- [x] Upload the preserved AAB and size-optimisation notes to production release 2.
- [x] Save 100% rollout in all 178 targeted countries/regions; Ready to release.
- [x] Send one production change for review; Changes in review is confirmed.
- [ ] Await Google quick checks/review and verify public 1.0.2 availability.
- [ ] Recheck Google's R8 recommendation after the updated release is processed.

Managed publishing is off. See `review/NATIVE_RELEASE_1.0.2.md` and
`review/play-1.0.2-submitted-2026-11-04.png`. Historical checklists follow.

## Local candidate 1.0.2 (5) — 1 November 2026

Addresses Google's R8 recommendation using AGP 9.0.1, Gradle 9.1.0, the optimising
default ProGuard file, integrated resource shrinking and explicit Capacitor
annotation keep rules. Use `review/R8_1.0.2.md` and the current verification JSON
for build evidence. Native testing is still deferred by the owner; this bundle
has not been uploaded/submitted. Last verified live release: 1.0.1, 25 October.

- [x] Signed APK/AAB built with the original upload key; release lint has no errors.
- [x] Five model/history tests and 18 browser tests pass.
- [x] All 18 release checks pass, including packaged AGP/R8 and DEX annotation checks.
- [x] Preserve 1.0.1 artifacts and deliver the new mapping/configuration/resource reports.
- [ ] Test the exact optimised APK on Android when the owner resumes phone testing.
- [ ] Upload/submit the validated update and verify Google's recommendation after processing.

## Historical 1.0.1 submission checklist

Current submission: **1.0.1 (4)**; package `com.kreativfont.studio`.
Production submission is accepted for all 178 available destinations; Google
shows Changes in review while automated checks run. Public availability has not
yet been verified. The old 1.0.0 internal draft remains.

## Current 1.0.1 validation and next steps

- [x] Compact mobile editor and persistent Preview, Copy and Save actions.
- [x] Complete-design Undo/Redo, grouped typing/sliders, keyboard shortcuts.
- [x] Undo deletion of saved designs during the current app session.
- [x] 48px controls, scalable text, stronger contrast, reduced-motion navigation.
- [x] Apply Android system font preference to the WebView on launch/resume.
- [x] Block PNG save/share if extreme line breaks cannot fit; retain text copying.
- [x] Five model/history and 18 desktop/mobile browser checks pass on the final build.
- [x] axe WCAG A/AA scans of editor, privacy dialog and saved designs pass;
  check 320 px screens and 200% text at 360px, 640px and 800px widths.
- [x] Rebuild signed APK/AAB with the preserved key; all 16 release checks pass.
- [x] Refresh five local store screenshots; preserve earlier artifacts/reports.
- [x] Fresh phone preference/APK backup and upgrade from 1.0.0 preserve designs.
- [x] Restore and hash-verify original designs after partial phone checks;
  remove temporary instrumentation.
- [ ] Finish native Undo/Redo, deletion recovery, 200% Android font scaling,
  rotation, clipboard/export and TalkBack checks when the phone is available.
  Phone interaction was interrupted by PhotoFeather taking the foreground;
  partial device observations are not a full native validation pass.
- [x] Upload signed 1.0.1 and refreshed screenshots to the existing app.
- [x] Owner selected all available countries; save 177 named entries + Rest of world.
- [x] Complete Advertising ID declaration as No; release review shows Ready to release.
- [x] Send all 11 production/listing/content changes for review at owner request.
- [ ] Await Google checks/review and verify public availability after approval.
  Native phone checks remain deferred; submitting does not mark them complete.

Current artifacts: `artifacts/play/kreativ-1.0.1.aab`,
`kreativ-1.0.1-upload-signed-release.apk`, `kreativ-1.0.1-mapping.txt` and
`verification.json` in that folder. Detailed evidence: `review/POLISH_1.0.1.md`.

## Earlier 1.0.0 checklist (historical)

The completed native tasks below apply only to 1.0.0, not to the rebuilt 1.0.1.

## Completed locally — 24 October 2026

- [x] Preserve the existing upload key and external owner-only credential files.
- [x] Build upload-signed AAB and companion APK with shrinking/minification.
- [x] Disable release WebView debugging and Capacitor logging.
- [x] Target API 36; minimum API 24 with WebView/Chrome 111+ build target.
- [x] Show version 1.0.0 from package metadata; include full policy and support contact.
- [x] Disable backup and configure Android 12+ device-transfer exclusions.
- [x] Run the three model checks and ten browser checks, including full in-app policy.
- [x] Run release lint; remaining warnings concern update suggestions and template resources/icons.
- [x] Prepare listing copy, icon, feature graphic, and five mobile-interface screenshots.
- [x] Prepare a standalone HTML privacy policy for hosting and draft data-safety answers.

Run `npm run check:release` and inspect `artifacts/play/verification.json` for
actual signed-artifact checks. The checklist alone is not build evidence.

## Required before publication

- [x] Test the exact signed, shrunk release on the Galaxy A55 / Android 16.
  With owner approval, back up debug 0.1.1 and replace it with signed 1.0.0;
  restore the original draft/favorite exactly and remove temporary instrumentation.
- [x] Verify native clipboard, text/image sharing and cancellation, square PNG
  save, transparent 2160 × 3840 story save, picker cancellation, and local
  draft/favorite restoration after restart. Release evidence is recorded in
  `artifacts/phone-release-1.0.0/`; earlier debug evidence is retained separately.
- [x] Check portrait and landscape layout on the Galaxy A55 without horizontal overflow.
- [ ] Check older supported Android/WebView versions, tablets, accessibility
  text scaling, and a receiving app for text/images.
- [x] Review and save listing/graphics, privacy policy, publisher identity,
  target ages (matching PaperBuild, 13+), and Free pricing.
- [ ] Choose production distribution countries when preparing a public release.
- [x] Publish and verify the policy without login at
  https://madebykreativ.com/privacy/kreativ-fancy-text/ (24 October 2026).
- [x] Owner confirmed a secure backup of the existing keystore/credentials
  before upload. No private signing material was uploaded or replaced.
- [x] Confirm developer account type: personal, confirmed by the owner.
- [x] Owner confirmed account creation on or before 13 November 2023; the newer
  personal-account closed-test requirement does not apply.
- [x] Access the Made by Kreativ personal account in Console; existing production
  apps and the new-app form are available.
- [x] Complete the app-creation policy/export declarations after owner confirmation.
- [x] Create the new Play app and upload the AAB to an internal-testing draft.
  Play defaults retained; mapping is attached and retained for this exact version.
- [x] Complete Data safety, app access, ads, target audience, IARC rating, contact,
  category, and government/financial/health declarations. Initial setup is unlocked.
- [ ] Select testers/authorize rollout or prepare production release and countries.
- [ ] Check available Play pre-launch reports after distribution testing.
- [x] Not applicable: the 12-testers/14-days closed-test requirement for personal
  accounts created after 13 November 2023. Native release validation and any
  requirements actually shown in Play Console still remain.
- [ ] Submit for review only after these steps and the owner's publishing decision.

## Artifacts

- `artifacts/play/kreativ-1.0.0.aab`: Play upload candidate.
- `artifacts/play/kreativ-1.0.0-upload-signed-release.apk`: companion for native testing;
  Play's delivered APK uses its app-signing certificate, which can differ.
- `artifacts/play/kreativ-1.0.0-mapping.txt`: exact release shrinking map.
- `artifacts/play/lint-results-release.{html,xml}`: lint evidence.
- `artifacts/play/verification.json`: signature, packaging, privacy/permission,
  release-configuration, checksum, and store-asset checks.
- `artifacts/release-build.log`, `artifacts/release-tests.log`,
  `artifacts/browser-results.json`, `artifacts/production-audit.json`.

## Requirements checked against Google documentation

- [API 36 required for new phone apps from 31 August 2026](https://support.google.com/googleplay/android-developer/answer/11926878?hl=en)
- [Play preview assets](https://support.google.com/googleplay/android-developer/answer/9866151?hl=en)
- [Testing for newer personal accounts](https://support.google.com/googleplay/android-developer/answer/14151465?hl=en)
