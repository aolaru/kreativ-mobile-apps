# Pre-launch improvements — 1.0.1 (4)

Implemented on 24 October 2026 after the owner approved the recommended mobile
layout, Undo/Redo and accessibility/long-text work. App ID remains
`com.kreativfont.studio`; existing draft and favorites keys/formats are preserved.

## Changes

- Compact mobile introduction and a persistent Preview / Copy text / Save PNG
  bar. Its reserved page space follows its actual height, including enlarged
  text and Android safe areas. It hides in the saved-design workspace.
- Complete-design Undo/Redo keeps up to 60 edit steps in memory. Typing, color
  and slider changes group within 800ms; focus changes break the group. Clear,
  theme, word-style and export changes are reversible. Keyboard shortcuts and
  mobile history input events use the same history.
- Deleted saved designs have persistent, session-only Undo delete recovery.
  Multiple deletes restore in reverse order. Storage changes are committed
  before updating the library. Focus moves to the recovery control after delete.
- Controls have 48px minimum targets; labels use relative font units and
  stronger contrast. Layouts wrap at larger text sizes. Android's font-size
  preference is applied to WebView text on creation/resume. Navigation honors
  reduced motion; workspace current-page semantics are corrected.
- The renderer reports whether all lines fit. Unfittable messages disable
  image save/share with a live explanation; copy/share text remain available.
  Choosing a taller format can recover export. The PNG generator also checks
  fit independently. Empty line fragments no longer trigger font measurement.

History and deletion recovery last for the current app session. They are not
backup/restore features. Style previews, collection backup and more image
customization remain future work.

## Automated validation

- Final `npm test`: five model/history tests and 18 browser tests passed.
- Browser coverage includes desktop and Pixel layouts; entire design recovery,
  deletion recovery/order/persistence, offline operation, actual PNG dimensions,
  transparency/colors, extreme line breaks and taller-format recovery.
- axe scans pass for WCAG A/AA rules in the editor, privacy dialog and saved
  designs. Touch targets are checked at 320px width and 200% text on 360px,
  640px landscape and 800px layouts; no horizontal page overflow.
- Signed release build, R8/resource shrinking, native compilation/unit task and
  release lint succeeded. The native unit task remains a generated arithmetic
  example; it is not runtime coverage. All 16 packaged-release/store checks pass.
- Production npm audit: zero reported vulnerabilities. axe is a development-only
  test dependency and is not packaged in the app.
- Five store screenshots were refreshed locally. Full tests and visual
  evidence: `artifacts/release-polish-tests.log`, `artifacts/browser-results.json`,
  `artifacts/screenshots/` and `store/graphics/screenshots/`.

## Phone status and limits

Samsung Galaxy A55 / Android 16, explicitly selected device `RZCXA128W0Y`.
The original native preferences and installed 1.0.0 APK were freshly backed up
under owner-only `.local/phone-backups/20261024T150609Z/` before testing.
1.0.1 installed over 1.0.0 with the preserved signing key, without uninstalling
the app. Both native preference value hashes matched the backup after upgrade.

Interaction checks were interrupted by PhotoFeather taking the foreground.
Some picker/share observations were collected, but the physical-tap sequence
did not complete reliably. Do not treat them as a full native validation pass.
Native 200% font scaling, rotation, TalkBack, full Undo/Redo and deletion recovery
remain to be completed. The owner explicitly chose to defer phone testing until
later; no further phone interaction is authorized for this turn. Automated browser scans
do not prove TalkBack speech, focus behavior or Android font scaling.

Original draft and favorites were restored at a cold-start boundary and their
hashes matched the fresh backup. Temporary instrumentation was removed. No
font/rotation/accessibility setting was changed during the interrupted tests.
Signed 1.0.1 remains installed; no recipient was selected for sharing.
The earlier 1.0.0 runtime evidence is preserved separately and does not certify
this new APK. The release verifier correctly reports native testing pending.

## Google Play

The owner subsequently requested publication and explicitly selected all
available countries and regions. Signed 1.0.1 (4), its release notes and all five
updated screenshots were uploaded/saved in the existing app. Production targets
177 named country/region entries plus Rest of world (178 total). Google's release
review showed Ready to release with no reported release errors or warnings.
The missing Advertising ID declaration was saved as No, consistent with code,
SDKs and packaged permissions. The icon/feature AI labels remain as previously
saved; actual interface screenshots are not AI-labelled.

Eleven changes were sent for review. Publishing overview now shows **Changes in
review**, with automated checks still running before forwarding to review.
Managed publishing is off; public availability depends on Google approval and
has not yet been verified. The old 1.0.0 internal draft remains unchanged.
Native phone checks remain explicitly deferred by the owner; publication intent
is not evidence of a full native test pass. No phone interaction was performed
in this publication turn.

Confirmation: `store/review/play-production-submitted.jpg` and `.txt`;
country evidence: `store/review/play-production-countries.jpg` and `.txt`.
