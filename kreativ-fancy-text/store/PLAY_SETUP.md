# Google Play setup — 24 October 2026

> **Current — 4 November 2026:** Production 1.0.2 (5) is published at 100% rollout.
> Pro 1.1.0 (7) is an internal test release with a separate saved Alpha draft.
> The optional lifetime product is Active at €4.99 in EUR regions; real no-charge
> purchase validation remains pending. Updated policy sources are local and have
> not replaced the hosted October policy. Before any Pro production update,
> complete billing tests, review Data safety and App access for paid features,
> publish the matching policy and update the listing. See
> [Pro status and launch checklist](review/PRO_1.1.0.md).
> Earlier records below describe prior versions; “no billing SDK” applies to 1.0.x.


## Current status — 4 November 2026

**1.0.2 (5) is uploaded and submitted for production review**, following successful
native tests of the exact signed/shrunk APK on the Galaxy A55. Publishing overview
confirms **Changes in review** for one production change, Start full rollout.
Automated quick checks are running before forwarding to review; managed
publishing is off. The update will publish automatically after approval, unless
Google requests action. Public availability of 1.0.2 has not yet been verified.

Production track `4697703135566350538`, release **2**, name `1.0.2 (5)`; rollout
100%, available in all targeted countries (178 destinations retained). Accepted
version 5, minimum API 24, target 36 and attached ReTrace mapping. The review page
showed Ready to release, no listed release errors/warnings and zero previously
supported devices lost. Release note: “App size optimisations for a lighter
installation.” Google estimates 1.08 MB for new installs (-159 KB) and 454 KB
for updates. Existing listing/media/declarations were not edited in this update.

Evidence: `review/play-1.0.2-submitted-2026-11-04.{png,txt}`,
`review/NATIVE_RELEASE_1.0.2.md` and current `play-console-state.json`.
The earlier local/preparation/submission notes below are historical.

## Local candidate — 1 November 2026 (historical)

**1.0.2 (5)** addresses the R8 recommendation with AGP 9.0.1 / Gradle 9.1.0,
code optimisation and integrated resource shrinking. See `review/R8_1.0.2.md`
and `../artifacts/play/verification.json`. It has not been uploaded or submitted;
Google's recommendation cannot be considered cleared until Play processes the
new bundle. Physical testing of the optimised release remains pending and
deferred by the owner. Existing Console state and listing-refresh drafts are
preserved; no new Play action was performed for this local update.

## Latest status — 25 October 2026

Public listing/About dialog confirms **1.0.1 is live**. Publishing overview
showed Last published on 25 October 2026 with no pending/in-review table.
At the owner's request, corrected the existing vector/app-rendered icon and
feature graphic declarations; **Don't label assets** remained selected after
reopening. Existing copy and image files/order were unchanged. See
`../promotion/2026-10-25/AI-LABEL-STATUS.txt` and `review/ai-labels-off-2026-10-25.png`.
New marketing copy, layouts and tutorial are local drafts only. No release,
pricing, permissions, website or social changes. Earlier status below is historical.

**1.0.1 (4) is submitted for worldwide production review.** Publishing overview
confirms **Changes in review** for 11 changes. Google's automated checks are
still running; the changes will proceed to review once those checks succeed.
Managed publishing is off. Public availability has not yet been verified and
still depends on Google approval. No further release action is currently required
from the owner unless Google reports an issue.

The owner requested publication and explicitly chose all available countries and
regions. Phone testing remains explicitly deferred, as requested earlier; the
1.0.1 build must not be described as fully phone-tested. No phone interaction
was performed during this publication turn.

## Identity and links

- Developer: Made by Kreativ, personal account created on or before 13 November 2023.
- Play app ID: `4976006210494663211`; package: `com.kreativfont.studio`.
- Title: Kreativ Fancy Text; default language en-US; price Free (owner confirmed).
- [Dashboard](https://play.google.com/console/u/0/developers/7594712361231234431/app/4976006210494663211/app-dashboard)
- [Publishing overview](https://play.google.com/console/u/0/developers/7594712361231234431/app/4976006210494663211/publishing)
- [Production release](https://play.google.com/console/u/0/developers/7594712361231234431/app/4976006210494663211/tracks/4697703135566350538/releases/1/review)

## Submitted production release

- Track ID: `4697703135566350538`; release ID 1; name `1.0.1 (4)`.
- Bundle: `artifacts/play/kreativ-1.0.1.aab`, accepted as 4 (1.0.1), minimum API 24,
  target API 36. ReTrace mapping is recognized. Existing signing and enhancement
  defaults retained; original upload key was not replaced.
- Bundle SHA-256: `d3ccd68466c4839862df0c5fcd9d1a68315fa09b077d504c15bcf24808885e46`.
- Release notes from `en-US/release-notes.txt` are saved and submitted.
- Google's release review showed Ready to release with no listed release errors
  or warnings before saving the production change.
- Countries: **177 named country/region entries plus Rest of world**, 178 total,
  selected with Select all rows and verified as Targeted. Submitted as two
  availability changes: add 177 countries/regions, and add Rest of world.
- Saved production change is Start full rollout. This is submitted for Google
  approval; it is not evidence of public availability yet.
- Eleven changes were confirmed with Send changes for review. The resulting
  overview shows Changes in review and running automated checks.

## Listing and declarations

All 11 initial app-setup tasks were completed before production preparation.

- Published privacy policy: https://madebykreativ.com/privacy/kreativ-fancy-text/
- Listing title, 68-character short description and 1,848-character full description
  retained. Five 1.0.1 screenshots were freshly uploaded, reordered 01–05 and saved:
  create, word styles, matching image, PNG options, saved designs.
- Existing icon/feature graphic and their AI labels retained. Actual interface
  screenshots are not AI-labelled. No video or new tablet/desktop/XR graphics.
- Category: Art & design. Support: info@madebykreativ.com;
  website: https://madebykreativ.com . No phone number added.
- App access: unrestricted; no account, login or paid content. No ads.
- **Advertising ID: No**, newly completed during production preparation after
  Google's quick checks identified the missing declaration. No advertising SDK
  or AD_ID permission exists in the verified build.
- Government app: No; financial features: None; health features: None.
- Target audience: 13–15, 16–17, 18+, matching PaperBuild's live selected groups
  as requested by the owner.
- Data safety: no required data collected/shared. User-initiated Android sharing
  is an exempt transfer and is disclosed in the privacy policy.
- IARC: Completed after owner approval of its Terms of Use; contact
  info@madebykreativ.com. ESRB Everyone, PEGI 3, ClassInd/USK all ages,
  generic/Russia/South Korea 3+. Certificate ID was '-' when initially verified;
  do not invent one. Content rating and intended target audience are distinct.

The owner approved the app-creation policy/export declarations and IARC terms,
and confirmed a secure upload-key/credentials backup before the first upload.
No new legal agreement was presented for this production submission. The final
confirmation only stated that changes would be sent to Google for review.

## Earlier internal draft

1.0.0 (3) remains an unpublished internal-testing draft in track
`4701453847955130384`, release 1. No testers were selected or internal rollout
started. Do not promote this older version instead of the submitted 1.0.1.

## Evidence and next status

- `play-console-state.json`: observed current state, including deferred phone tests.
- `review/play-production-submitted.jpg` and `.txt`: Changes in review confirmation.
- `review/play-production-countries.jpg` and `.txt`: worldwide targeting evidence.
- `review/POLISH_1.0.1.md`: code changes, automated validation and phone limitations.
- `artifacts/play/verification.json`: package/signature/store checks; native runtime
  testing remains false, and submission state is tracked separately.

Policy deployment: website commit `ebc7c57106f6341e742e6b8e6729978075908fd2`,
[successful deployment](https://github.com/aolaru/madebykreativ.com/actions/runs/40087390720).
Public HTTPS returned 200 without login and all 12 policy metadata/section
comparisons passed against the rendered page.

Next: allow Google's automated checks and review to finish, address any reported
issue, and verify public availability after approval. Do not claim Published
until Google Play or the public listing confirms it. No recurring monitor was
requested or created.
