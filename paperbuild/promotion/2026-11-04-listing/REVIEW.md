# PaperBuild 1.2 final listing review

Submitted on 4 November 2026 after explicit owner approval. Publishing overview
confirms **Changes in review** for production 1.2.0, en-GB full description and
phone gallery. Google restarted automatic quick checks after submission;
completion is not yet confirmed. Approval and publication remain unconfirmed.
Managed publishing is off, so approval may publish the update automatically.
Evidence: `release-evidence/2026-11-04/1.2-submitted-for-review.png`.

## Reviewed listing

- Keep the existing title, **PaperBuild: Photo to PDF** (24/30), and short
  description (75/80). The full description is 3,527/4,000 characters.
- Describe new existing-PDF page editing, logo watermarks and reusable logo
  presets as part of the existing lifetime Pro upgrade. Free creation/cropping/
  Recent PDFs remain Free. No pricing or product-ID changes.
- Clarify separate non-destructive editor drafts and input limitations. Do not
  claim OCR, automatic edge detection or PDF text editing.
- Retain the live tutorial, public privacy-policy link and support address.
  Exact text and the previous description are retained in this folder.

## Reviewed gallery

Eight RGB PNGs at 1440 × 2560, 9:16 and under 8 MB, saved in this order:

1. Photos to PDF — unchanged Free workflow capture.
2. Crop pages — unchanged Free cropping capture.
3. PDF settings — unchanged Free settings capture.
4. Save/share — unchanged Free result capture.
5. Recent PDFs — unchanged Free library capture.
6. Edit PDF pages — fresh 1.2 phone capture with a three-page fictional brief.
7. Logo watermarks — actual exported sample, independently rendered at high
   resolution so the logo, text watermark and page number remain sharp.
8. Optional Pro — fresh 1.2 feature list, clearly labelled as a paid one-time
   upgrade, with no fabricated price or purchase state.

The first five captures are from 1.1; their Free UI is unchanged in 1.2. New
captures use the separately installed 1.2 QA app and a real signed review grant.
No production app data, account or real user document was used. Source hashes,
crop bounds and exported asset hashes are in `assets-manifest.json`.

The existing icon and feature graphic stay unchanged. Play's **Don't label
assets** option is selected: these are genuine app captures, native PDF output
and code-authored frames, without generative imagery or reconstructed UI.

## Validation

- Screenshot capture workflow: 1 test passed on Samsung SM-A556B / Android 16.
  This optional capture source set does not change the 24-test regression suite.
- The app-exported fictional sample reopens with pypdf; vector text and page
  number are present. Native app preview and Poppler render were inspected.
- All eight panels were reviewed together and the new panels at full size;
  source cropping, legibility, Free/Pro wording and gallery order were checked.
- Play saved the full description and 8/8 phone screenshots. Listing review
  shows **Changes ready to send for review**. Production v6 was revalidated
  with ReTrace mapping and zero devices lost, then staged for review alongside
  the listing. This was the prepared state before the owner approved submission.
- No runtime app code or signed release bundle changed during this review.
  Candidate AAB SHA-256 remains
  `45e102db44b1f757f74c278a62ada3e1cd0fca127c157853041e86a2087ae4d7`.
- Before submission, Publishing overview said **Your changes can now be sent for review**
  and offers **Submit 3 changes for review**. Proof is retained in
  `release-evidence/2026-11-04/1.2-ready-for-approval.png` and
  `1.2-three-changes-ready.png`. Public tutorial and the November 4 privacy
  policy were also verified in Chrome; their listing URLs remain unchanged.
- After owner approval, confirmed **Submit 3 changes for review** → **Send
  changes for review**. All three changes moved to **Changes in review**;
  automatic quick checks restarted. No approval or live publication is claimed.

Open `review.html` or `screenshots-preview.png` to review the saved artwork.
