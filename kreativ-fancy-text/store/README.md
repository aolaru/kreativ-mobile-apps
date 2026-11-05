# Kreativ Fancy Text — Play preparation

> **Current — 4 November 2026:** Production 1.0.2 (5) is published at 100% rollout.
> Pro 1.1.0 (7) is an internal test release with a separate saved Alpha draft.
> The optional lifetime product is Active at €4.99 in EUR regions; real no-charge
> purchase validation remains pending. Updated policy sources are local and have
> not replaced the hosted October policy. Before any Pro production update,
> complete billing tests, review Data safety and App access for paid features,
> publish the matching policy and update the listing. See
> [Pro status and launch checklist](review/PRO_1.1.0.md).
> Earlier records below describe prior versions; “no billing SDK” applies to 1.0.x.


Latest update, 25 October 2026: version 1.0.1 publicly verified live. Existing
code-rendered icon/feature AI labels corrected; persisted Don't label assets
verified. No other live listing changes. New promotion pack is a local draft at
`../promotion/2026-10-25/index.html`; see its AI-LABEL-STATUS.txt for evidence.
Earlier submission notes below are historical; phone testing remains deferred.

1.0.1 (4) was submitted for worldwide production review on 24 October 2026,
with refreshed screenshots. Google Play shows Changes in review while automated
checks run; public availability awaits Google approval. The older 1.0.0 internal
draft is preserved. Phone checks remain deferred. See `PLAY_SETUP.md` and
`review/POLISH_1.0.1.md`.

- `en-US/`: title, short/full descriptions, and release notes for review.
- `graphics/icon-512.png`: 512 × 512 RGBA Play icon, derived from the existing vector.
- `graphics/feature-1024x500.png`: 1024 × 500 RGB feature graphic.
- `graphics/screenshots/`: five 1080 × 1920 RGB screenshots of the real bundled
  interface in Chromium at a 432 × 768 mobile viewport, 2.5× scale. These show
  app content without Android system chrome; they are not physical-phone captures.
- `graphics/manifest.json`: provenance, dimensions, and hashes of those assets.
- `privacy/`: full policy in HTML and text, matching the in-app policy. Published
  at https://madebykreativ.com/privacy/kreativ-fancy-text/; `publication.json`
  records deployment and public-content verification. Publisher branding matches
  Kreativ / the Made by Kreativ developer account.
- `DATA_SAFETY.md`: saved Console answers and supporting implementation evidence.
- `RELEASE_CHECKLIST.md`: verification results and remaining publication steps.
- `upload-certificate.pem`: public upload certificate only. The private key and
  passwords remain outside this repository in their original location.

Rebuild in order from the app directory:

```sh
npm run build
npm test
npm run store:assets
npm run bundle:release
npm run check:release
```

`store:assets` uses bundled Playwright Chromium and the existing brand artwork;
no image-generation or network service is required. `check:release` inspects
both signed artifacts, checks their matching certificate and packaged web build,
checks APK permissions/release flags, and validates the store asset formats.
It reports native runtime validation only when the recorded phone evidence
matches the exact signed APK hash. It does not claim Play Console approval.
The tested 1.0.0 phone results are summarized in `review/NATIVE_RELEASE_1.0.0.md`.

The signed AAB, companion APK, shrinking map, lint reports, checksums, and
verification report are under `../artifacts/play/`. The build log is
`../artifacts/release-build.log`; browser/model results are in
`../artifacts/release-tests.log` and `../artifacts/browser-results.json`.

Initial Play setup is complete; see `PLAY_SETUP.md` for the app ID, declarations
and submitted 1.0.1 production release. `play-console-state.json` records the
observed Console state; public rollout is not yet verified.
