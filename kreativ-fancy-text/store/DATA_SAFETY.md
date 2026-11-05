# Data safety and app-content draft

> **Current — 4 November 2026:** Production 1.0.2 (5) is published at 100% rollout.
> Pro 1.1.0 (7) is an internal test release with a separate saved Alpha draft.
> The optional lifetime product is Active at €4.99 in EUR regions; real no-charge
> purchase validation remains pending. Updated policy sources are local and have
> not replaced the hosted October policy. Before any Pro production update,
> complete billing tests, review Data safety and App access for paid features,
> publish the matching policy and update the listing. See
> [Pro status and launch checklist](review/PRO_1.1.0.md).
> Earlier records below describe prior versions; “no billing SDK” applies to 1.0.x.


For `com.kreativfont.studio` 1.0.0 (3). Saved in Play Console on 24 October 2026.
App review has not been requested; see PLAY_SETUP.md for the current state.

## Saved data answers

- Data collected off-device by this app or its SDKs: **No**.
- Data shared for developer/SDK purposes: **No**. Clipboard, chosen save
  destinations, and Android share sheets act only on specific user requests.
  Google lists expected user-initiated transfers as an exception to the Data
  safety definition of sharing; disclose the behavior in the privacy policy.
- Accounts: none. No registration, sign-in, or account-deletion flow applies.
- Security-review and Families badges: do not claim either certification.
- Encryption in transit: no app-server transmission. Follow the form's actual
  branching after selecting no collection; do not invent a server-encryption claim.
- Data deletion: drafts/favorites can be removed locally, and Android Clear
  storage or uninstall removes all app-local state. Exported/shared copies must
  be deleted at their destinations. Support correspondence is separate from
  automatic app data and is described in the policy.
- Privacy policy: https://madebykreativ.com/privacy/kreativ-fancy-text/ . Published
  and verified over public HTTPS without authentication on 24 October 2026; all
  policy sections and metadata match the bundled text. Enter this URL in Console.

## Evidence to review

- `src/platform.js`: Capacitor Preferences, Clipboard, Filesystem cache, native
  `Share.share`, and `ExportFile.savePng`; no remote endpoint or telemetry.
- `src/main.js`, `src/render.js`: local editing/rendering and bundled fonts.
- `public/PRIVACY.txt`: exact policy embedded in the privacy dialog and packaged
  in the APK/AAB; `info@madebykreativ.com` is the approved support address.
- Release merged manifest: no INTERNET, storage, camera, location, advertising
  ID, or accessibility permissions. Only the private AndroidX dynamic receiver
  permission remains; the release verifier checks this allowlist.
- Android backup disabled, with explicit Android 12+ cloud/device-transfer
  exclusions. No cloud favorites synchronization or remote account storage.
- No advertising, analytics, crash collection, billing, or remote font SDK.
- Production dependency audit and signed-artifact checks are in `artifacts/`.

## Other app-content entries

- Ads: No; this build has no ads or advertising SDK.
- App access: all functionality is available without login or credentials.
- Category: Art & design (typography/image creation), saved.
- Content-rating questionnaire: answer from the local creation tools and supplied
  themes. There is no hosted content feed, in-app chat, or online user-content
  service. Text/image sharing launches another app selected by the user. Do not
  assign a rating until the questionnaire produces it.
- Developer account type: personal, confirmed by the owner on 24 October 2026.
  Owner confirmed creation on or before 13 November 2023, so the closed-test
  requirement for newer personal accounts does not apply. Console access and the
  available new-app form were verified for Made by Kreativ.
- Price: Free, explicitly confirmed by the owner.
- Target ages: 13–15, 16–17, 18+, saved to match PaperBuild after the owner asked
  to use the same settings as previous apps. IARC rating completed under explicit
  owner approval. Distribution countries remain a production-release decision.
  Do not assume that this is a child-directed app or enroll it in Families.

## Official references checked 24 October 2026

- [Google Play Data safety definitions and exceptions](https://support.google.com/googleplay/android-developer/answer/10787469?hl=en)
- [Google Play User Data policy and privacy-policy requirements](https://support.google.com/googleplay/android-developer/answer/10144311?hl=en)
- [Android backup and transfer controls](https://developer.android.com/identity/data/autobackup)

These answers apply to this Android package, not to the separately hosted
WordPress plugin, website, or Chrome extension.
