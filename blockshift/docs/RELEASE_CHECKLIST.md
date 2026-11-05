# Release checklist

## Identity and store content

- [ ] Clear BLOCKSHIFT as a game name in relevant stores/trademark registries; do not infer clearance from a web search.
- [ ] Confirm Made by Kreativ publisher rights and permanent identifier `com.madebykreativ.blockshift` before first Play upload.
- [ ] Final icon/adaptive/themed icon review, screenshots, description, support email and appropriate age/content rating.
- [ ] Verify the €2.99 target against actual Play-localized prices/taxes. Never hardcode the checkout price in a production billing flow.

## Android package

- [ ] Recheck [current Play target API policy](https://developer.android.com/google/play/requirements/target-sdk) on publication day. Checked 2026-10-16: new phone/tablet apps and updates require Android 16 / API 36.
- [ ] Export AAB with ARM64, target API 36 or newly required level; inspect the built manifest rather than trusting the preset.
- [ ] Verify every native dependency and compressed library for [16 KB page-size support](https://developer.android.com/guide/practices/page-sizes). Engine binaries alone do not certify future plugins.
- [x] 0.5.0 internal candidate signed with a dedicated private upload key outside the workspace; signature and certificate verified.
- [ ] Enroll in Play App Signing at first upload and back up the private upload-key recovery directory in an encrypted vault.
- [x] Current package permissions inspected: the offline game only requests VIBRATE for optional haptics. No network, location, storage, camera, contacts or account access is needed.
- [x] Current package excludes debug fixtures/tests/review data; release manifest is non-debuggable. Commerce adapters cannot grant test entitlements.
- [ ] Physically test phones/tablets, safe areas, cutouts, gesture navigation, pause/resume, calls, lock screen, storage and low memory.

## Commerce and privacy

- [ ] Keep disabled commerce adapters until real integrations have passed store sandbox tests. Do not publish a fake successful transaction.
- [ ] Deferred until commercial integration: Billing — verified purchases, pending/canceled/failed states, acknowledgment, restore, refunds/revocations, offline cached access and localized price.
- [ ] Deferred until commercial integration: Advertising — documented SDK dependencies, data collection, consent/age handling, regional choices, optional reward behavior and interstitial policy regression tests.
- [ ] Never initialize trackers before required consent; never display an ad during play or a tutorial; preserve all modes in the free version.
- [x] Full in-app policy and support contact use Made by Kreativ / Andrei Olaru and info@madebykreativ.com. Prepared hosted policy HTML at store/internal-testing/privacy-policy.html.
- [ ] Deploy the HTML at https://madebykreativ.com/privacy/blockshift/ and enter the live policy URL in Console. Set config/release.json privacy_policy_url only after deployment is verified.
- [ ] Complete Play Data safety, Ads declaration, target audience, content rating, app access and any developer verification using observed SDK behavior. The present build has no analytics/ad SDK; future SDK collection changes the answers.
- [ ] Review consent/privacy obligations for target regions and intended ages before enabling ads. No third-party tracking is included by default.

## Quality gates

- [x] Ran `tools/test.sh --render` and inspected the current screens, release manifest, native libraries and exported assets. Repeat before publication.
- [ ] Play internal-track billing/ad tests, pre-launch report and crash checks on physical devices.
- [ ] Run at least several weeks of player testing before claiming retention or commercial readiness.
- [ ] Ship correct Godot and font notices, replace or accept placeholder sound assets explicitly, and verify readable text/contrast.

## Internal testing 0.5.0

- [x] Remove unavailable PRO/restore purchase promotions; keep all four modes free and commerce adapters disabled.
- [x] Confirm non-debuggable ARM64/API 36 bundle, VIBRATE-only permission, backup disabled and no test/debug/store-handoff resources in exports.
- [x] Run 7,249 native gameplay/UI/reliability/graphics assertions with zero failures, plus audio asset checks.
- [ ] Supply Google-account tester addresses or a Google Group and configure the internal track.
- [ ] Upload the signed AAB, enroll in Play App Signing, roll out only to internal testing and test installation through the opt-in link.
- [ ] Complete fresh-player feedback, wider devices, actual 16 KB runtime, audible audio/haptics, gesture navigation and sustained performance before broader release.

Read review/INTERNAL_TESTING_0_5_0.md for current candidate evidence; 0.4.1 physical-device checks are historical.
