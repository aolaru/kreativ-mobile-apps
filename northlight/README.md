# Northlight

An offline Android compass and emergency-light utility from Kreativ.

## What it does

- Shows a live magnetic compass using the device's orientation sensors.
- Includes a free, opt-in spoken compass using an installed offline voice, with single readings and optional automatic announcements (unreleased).
- Adds a free, opt-in GPS altimeter with metres/feet, explicit altitude reference and reported vertical uncertainty (unreleased).
- Adds opt-in spoken turn/alignment instructions and optional alignment haptics for saved Pro bearings (unreleased).
- Controls the camera LED as a lantern, requesting camera permission only when the user selects that feature.
- Provides a full-screen red-light mode for preserving night vision.
- Sends a repeating SOS flash signal.
- Offers brightness control when the device supports variable torch strength.
- Includes a Quick Settings lantern tile after camera access has been granted in the app.
- Handles portrait and landscape orientation, and explains when the compass needs calibration.
- Works without an account, analytics, ads, or tracking. Core compass and light tools work offline; the optional Pro purchase uses Google Play Billing.
- Clearly identifies when a phone lacks a usable compass sensor or camera flash.

## Field UI refresh (unreleased)

The home screen now uses a larger rotating compass scale with upright degree
labels, strong cardinal marks, a fixed heading index and native heading/direction
text inside the dial. This changes readability, not sensor precision. Tap the
compact accuracy status for calibration help. Lantern, night light and SOS are
directly below the compass; controls stack on narrow screens or with large text.

Pro owners see their saved bearings directly on home. Tapping a bearing opens
its direction guidance, while All Pro tools opens bearing management and the
remaining paid tools. The home night-light button offers red, amber and green
for owners; red remains free. Existing purchases and saved data are unchanged.

The compass-ring/amber-beam logo has adaptive and monochrome launcher variants.
The editable Play icon is `store-assets/play/northlight-icon.svg`; regenerate
the 512px PNG with `node scripts/render-icon.cjs` when Sharp is installed in the
Node module path. Existing store screenshots and feature graphic still need
fresh production assets before publishing this redesign.

JVM UI tests use Robolectric, without an emulator, automatic Play connection or
real flashing lights. Native-rendered layout previews are generated under
`app/build/field-previews/`; these are QA previews, not authentic Play screenshots.
Physical-phone QA is still required before release. The altimeter and spoken
compass and spoken saved-bearing guidance are documented separately below.

On 14 October 2026, the debug app and device-test APK compiled successfully,
all 28 JVM tests passed (including ten Field UI checks), and lint reported no
errors with only the existing Gradle-version advisory. Native-rendered free,
Pro, narrow-screen and large-text previews were inspected. No phone was connected;
the updated physical-device tests were compiled but not run. No emulator or
payment test was used, and this redesign has not been published.

## Free spoken compass (unreleased)

Spoken compass is independent of billing and available to everyone, in English
and Romanian. **Speak direction** reads the phone's current magnetic heading.
Automatic announcements are off by default and announce stable changes of at
least 15 degrees, with a selectable minimum interval of 3, 5 or 10 seconds.
Voice speed is adjustable. Opening speech settings, leaving the foreground,
locking the phone or losing audio focus pauses speech; it never resumes on its
own. Interval and speed preferences persist, but automatic mode does not.

Low/unreliable accuracy, missing sensors, missing readings and stale headings
produce explanatory warnings rather than numeric directions. Automatic warnings
do not repeat continuously. Startup always reads the latest state, never a
heading captured before a slow voice initialization. Initialization and playback
have bounded timeouts; late callbacks cannot resurrect cancelled speech.

The engine is initialized only after an explicit request. Northlight accepts
only installed, non-network voices matching the app language; missing voice data
has a settings/retry flow. Managing voice data opens the external Android voice
installer/settings only on user request and may require a download. Northlight
does not use the microphone, record audio, send headings to a speech server or
introduce new permissions. The manifest adds only a targeted TTS-service query.
With a spoken accessibility service active, app TTS and automatic announcements
are disabled; an explicit reading updates a polite live region for the screen
reader instead. The screen reader uses its own voice and speed settings.

This describes **phone orientation**, not walking direction, safe routes,
obstacles or safe road crossings. Keep surrounding sounds audible. Saved-bearing
audio guidance is a separate Pro feature below; voice commands and background
speech remain out of scope.

On 15 October 2026, all 70 JVM tests passed: the existing 28 checks plus 42 speech
policy, controller, offline-voice, engine-lifecycle and UI checks. Debug and
device-test APKs compiled; lint reported zero errors and only the existing
Gradle-version advisory. Native-rendered normal/narrow/large-text layouts were
inspected. The nine device UI tests were compiled, not run. No emulator, audible
playback, purchase, production upload or publication was performed.

Before release, test on a physical phone with installed English and Romanian
offline voices and airplane mode; check single/repeated requests, auto intervals,
pause, screen lock, leaving/re-entering, audio interruption, calibration warnings
and no-sensor/missing-voice fallback. Verify TalkBack focus order, repeated same
heading requests, polite announcements and no competing voices. Have blind or
low-vision users assess usability before describing it as an accessibility aid.
Refresh store screenshots/listing, review the public privacy wording for on-device
speech and increment the release version before uploading. This feature is local
and unpublished; no Pro purchase behavior has changed.

## Free GPS altimeter (unreleased)

A Field-styled altitude panel sits below the light tools and before spoken
compass. It is independent of Pro. It shows whole metres or feet, source and
altitude reference, reported vertical uncertainty and clear waiting/error states.
The selected unit persists; measurement sessions and altitude history do not.

GPS stays off on launch. **Start GPS altimeter** asks for optional, foreground
precise location only after an explanation. Fine/coarse permissions are requested
together so Android can offer approximate access; approximate-only permission
does not start GPS and is explained in the panel. After permission is granted,
the user explicitly taps Start again. Denied/permanently denied access offers a
retry or app-settings action; GPS disabled on the phone offers location settings.
The compass, lights and spoken compass never require location permission.
Location hardware remains optional in the manifest, including for Play filtering.

This uses Android's direct GPS/GNSS provider, not a network elevation service,
fused-location dependency or barometer. Location updates are requested at a
minimum interval of two seconds while the user enables the panel and remains
on the home screen. Stop, leaving the screen or locking the phone removes the
listener and clears the reading; returning never restarts measurement. A session
generation rejects late callbacks. No last-known/cached fix is requested.
Only scalar altitude, uncertainty and monotonic timestamps are kept transiently
in memory; no coordinates, Location objects, routes or altitude log are persisted
or uploaded. GPS may consume additional battery and satellite acquisition can
take time; after one minute without a fix the UI explains the delay.

When supplied by Android 14+, mean-sea-level altitude is preferred, with its own
MSL uncertainty. Otherwise the panel explicitly labels the GPS WGS84 ellipsoid
reference, **not sea level or height above ground**. Horizontal accuracy is never
used as vertical accuracy. Unknown uncertainty is labeled, uncertainty is rounded
up for display, and reported vertical uncertainty above 30 metres is called low
accuracy. Android's uncertainty describes 68% confidence, not a guaranteed bound.
Missing/non-finite altitude, simulated locations, out-of-order/old-session fixes
and readings older than approximately 15 seconds are never displayed as current
numeric altitude. Units do not change the underlying reading. The altimeter is
not intended for safety-critical use. See the [Android Location reference](https://developer.android.com/reference/android/location/Location)
and [runtime location-permission guidance](https://developer.android.com/develop/sensors-and-location/location/permissions/runtime).

On 15 October 2026, this implementation was compile-checked with
`./gradlew :app:assembleDebug`. **No tests, emulator, phone install or live GPS
session were run, at the user's request.** The 70-test result above predates the
altimeter and does not verify this addition. It is local and unpublished; the
release version has not been incremented. Before publishing, update the live
privacy policy and review Play's Data safety declarations for optional on-device
location processing; the companion HTML policy is an unpublished draft for this
release. Store copy/screenshots still need to reflect the new features. Barometric
altitude, calibration and altitude speech are not part of this first implementation.

## Spoken saved-bearing guidance (unreleased, Pro)

Pro owners can select a saved bearing (including the home-screen shortcut), then
choose **Speak guidance** for one instruction or **Start automatic guidance**.
Prompts identify the target by magnetic direction/degrees, followed by how far
to turn the **phone** left/right, or “On bearing.” Saved names are not spoken.
This uses the existing Pro entitlement/product; no billing behavior has changed.
Basic spoken compass, altimeter and existing core free features remain free.

Guidance reuses the offline speech engine, lifecycle handling, accuracy/freshness
checks, missing-voice flow and TalkBack handoff from free spoken compass. Its
target-relative announcement policy is separate: after a heading settles for
at least 500ms, alignment/left-right changes or a change of at least 10 degrees
in the remaining turn can be announced, respecting the shared 3/5/10-second
minimum interval. Alignment enters within 3 degrees and exits beyond 6 degrees.
Outdated left/right/aligned speech is stopped when the stable instruction changes.
Visual guidance also hides directional instructions for low/unknown accuracy or
missing sensors. Unreliable/stale/missing readings produce spoken warnings, not
turn instructions; nothing announces continuously while the state is unchanged.

**Vibrate on alignment** defaults off. A stable, fresh aligned reading can issue
one native confirmation cue, with a five-second cooldown and rearming after a
turn more than eight degrees away. Native feedback honors Android's settings;
no VIBRATE permission, background service or repeating effect is added. Phones
without a vibrator show an unavailable control. With TalkBack active, automatic
app speech is disabled; a single requested instruction goes through a polite
live region, with an optional cue only for a stable, reliable aligned reading.
See [Android haptic feedback guidance](https://developer.android.com/develop/ui/views/haptics/haptics-apis).

Pause, selecting/changing/clearing a target, editing a bearing, opening speech
settings, losing Pro entitlement, leaving Pro or locking/rotating the phone stops
automatic output. No lifecycle transition starts it again. Voice failure or audio
interruption also pauses it. Target selection can survive recreation, but active
speech never does. Voice speed/interval preferences are shared with free speech;
the optional haptic preference is device-local. No GPS, location permission,
microphone, audio recordings, server speech or saved-name transmission is involved.
Alignment is not an arrival or a statement that a path is clear: this describes
phone orientation, not navigation, obstacles, safe walking or road crossings.

On 15 October 2026, the updated debug APK compiled with
`./gradlew :app:assembleDebug`. No tests, emulator, phone install, live speech,
vibration or purchase were run, as requested. The earlier 70-test result predates
this change to the shared speech controller and does not verify this feature.
Existing UI fixtures that inject headings without accuracy events will need to
model a reliable sensor before asserting directional instructions. Runtime QA,
store assets, release versioning and publishing remain deferred; the local privacy
draft now covers Pro guidance and its optional haptic preference.

## Pro benefits overview (unreleased)

The non-owner Pro screen now explains the upgrade instead of presenting a long
list of disabled tools. A Field-style **Pro adds** card lists the eight named
bearings, spoken phone-turn guidance with optional alignment vibration, amber
and green screen lights, and adjustable strobe. A separate **Free, with or
without Pro** card lists compass/calibration, lantern/red light/SOS/Quick Settings
and supported flash brightness, basic spoken headings (single or automatic),
and GPS altitude in metres/feet. Red light and standard SOS are explicitly called
out as free in the paid-feature descriptions too.

The page states that Pro is a one-time purchase with no subscription; pricing
still comes from Google Play through the existing purchase button, without a
hardcoded fallback price. Restore is available, and **Keep using free tools** or
the header Close button dismisses the overview without initiating a purchase.
No account, ads or tracking applies to both versions, not just Pro. Capability
notes explain installed offline voices, precise location for the optional
altimeter, supported hardware, and phone-orientation—not route—guidance.

Owners skip both comparison cards and purchase messaging and see their field
tools directly. Ownership transitions switch the view and scroll to its top;
ordinary billing refreshes do not reset the scroll position. Tool entitlements,
purchase/restore actions, saved-bearing data and speech lifecycle are unchanged.
Native text wraps in vertically stacked feature sections, with accessibility
headings; the owner night-light palette also stacks on narrow/large-text screens.
English/Romanian home-screen Pro summaries now include spoken saved-bearing
guidance and explicitly keep basic spoken compass and altitude free.

On 15 October 2026, the debug APK compiled using `./gradlew :app:assembleDebug`.
No tests, emulator, phone installation, UI rendering or payment flow were run,
as requested. Earlier preview/test results do not verify this layout. The benefits
overview, like the other new features above, remains local and unpublished.

## Outdated Fragment SDK warning (unreleased fix)

The 13 October 2026 Play Console notification flags
`androidx.fragment:fragment:1.1.0` in the published version code 4 (1.1.1).
It is a transitive library from Play Billing 8.0.0 through Play services
base/basement, not the app's Android target SDK.

The app now declares an implementation dependency constraint for Fragment
1.9.1, the stable version listed in the [AndroidX Fragment release notes](https://developer.android.com/jetpack/androidx/releases/fragment).
This upgrades the existing transitive dependency in all implementation variants
without forcing an older version if a future dependency requires something newer.
Play Billing remains at 8.0.0, compile/target SDK at 36, and minimum SDK at 26.

On 15 October 2026, `:app:dependencyInsight` for `debugRuntimeClasspath` confirmed
that the old 1.0.0/1.1.0 requests resolve to 1.9.1, and `:app:assembleDebug`
succeeded, including AAR compatibility checks and duplicate-class checks.
No tests, emulator, phone install or payment flow were run. No release was
signed, uploaded or published; the existing Play notification still refers to
the unchanged published release. Versioning and a future production update are
needed to deliver this fix to users. Runtime compatibility remains unverified.

## Northlight Pro (1.1.1 / version code 4)

The optional one-time unlock adds up to eight named device-local bearings,
with rename/delete controls and turn guidance, amber and green screen lights,
and an adjustable LED strobe. Compass, lantern,
red light and SOS remain free.

Google Play product ID: `northlight_pro`. Purchase option: `lifetime` (Buy).
EUR checkout price: 2.99, with Play-converted non-EUR regional prices. Purchases are
non-consumable and restored through Google Play. A successful ownership query
caches access locally for offline use; a later successful query reconciles
ownership, including refunds. Pending payments do not unlock Pro.

On 10 October 2026, version code 3 (1.1.0) was released to Internal testing
and the `lifetime` purchase option was activated in 174 countries/regions.
Version code 4 (1.1.1) was subsequently published to production on 10 October;
the 11 October Play Console check confirmed 100% rollout in 178 countries/regions
with no unpublished changes. The public privacy policy was updated for billing.
Purchase/restore testing was skipped at the user's request and remains unverified.

The published reliability update includes the following changes.
Both compass screens now share rotation compensation, sensor fallback and
accuracy feedback. Naming a bearing freezes the heading at the time of the tap;
the ninth bearing is refused rather than deleting existing data. Night lights
stay awake and remember adjustable brightness. SOS stops on torch failure,
brightness is remembered, and the Quick Settings tile observes hardware state.
All screens handle system bars/cutouts and scroll when needed. UI strings are
available in English and Romanian, with English fallback for other languages.

Billing refreshes ownership at app foreground entry and offers manual retry.
Completed purchases enqueue unique, network-constrained WorkManager jobs with
exponential backoff to acknowledge purchases even after leaving Pro or process
death. Pending payments never grant new access; failed queries preserve cached
access. Background execution still depends on Android scheduling, connectivity,
and users not force-stopping/uninstalling the app; no client can guarantee a
confirmation deadline while the device stays offline.

## Verification

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
./gradlew :app:assembleDebugAndroidTest
# Only with an authorized phone connected (no emulator required):
ANDROID_SERIAL=YOUR_PHONE_SERIAL ./gradlew :app:connectedDebugAndroidTest
```

Debug builds use `com.kreativ.northlight.debug`, leaving production data untouched.
The device regression suite uses a test-only Application and cached entitlement;
it does not contact Play, make purchases or activate flashing lights. It covers
screen launch, landscape/large-text scrolling, night-light wakefulness, frozen bearing
capture, and follow/rename/delete. It is not a substitute for real Billing QA.

On 10 October 2026, all six device regression tests passed on a Samsung Galaxy
A55 (SM-A556B), Android 16/API 36, together with all 18 JVM unit tests. The phone
test exposed a clipped Pro control: safe-area insets now belong to a non-scrolling
outer container, so Android scroll-to-rectangle calculations use the correct
viewport. No emulator, Play payment or flashing-light test was used in this pass.
Lint reports only the advisory that a newer Gradle version is available.

Before release, manually verify:

- Compass consistency in all four rotations, calibration and sensorless devices.
- Layouts with large text, a display cutout, gesture and three-button navigation.
- Flash/SOS failure with the camera busy; brightness after off/on; tile state
  after external torch changes and service recreation.
- Night lights beyond the normal screen timeout and brightness restoration on exit.
- Play license-test purchase, acknowledgement, pending approval/cancellation,
  purchase cancellation, restore after reinstall, offline access and refund
  reconciliation. Use the signed internal-testing build, not the debug package.
- Public privacy-policy update before the paid production rollout.

## Build

Open this folder in Android Studio and run the `app` configuration, or run:

```sh
./gradlew assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Release bundle

Release signing is intentionally kept outside this repository. Set these environment variables before building a Play upload bundle:

```sh
export KREATIV_NORTHLIGHT_KEYSTORE="/path/to/northlight-upload.jks"
export KREATIV_NORTHLIGHT_KEYSTORE_PASSWORD="your-keystore-password"
./gradlew bundleRelease
```

The signed bundle is written to `app/build/outputs/bundle/release/app-release.aab`. Never commit the keystore or its password.

### Northlight’s current upload key

The registered upload key is kept outside the repository at
`~/.android/northlight-upload.jks`, with an encrypted backup in
`~/Documents/Android Signing Backups/Northlight/`. Its password is stored in
macOS Keychain under service `com.kreativ.northlight.upload-key` and account
`northlight-upload`.

On this Mac, build a signed release without exposing the password by running:

```sh
KREATIV_NORTHLIGHT_KEYSTORE="$HOME/.android/northlight-upload.jks" \
KREATIV_NORTHLIGHT_KEYSTORE_PASSWORD="$(security find-generic-password -s 'com.kreativ.northlight.upload-key' -a 'northlight-upload' -w)" \
./gradlew bundleRelease
```

Keep at least one encrypted copy of the `.jks` file off this Mac as disaster
recovery. The Keychain entry and keystore file are both required to sign a
future upload.

The replacement `northlight-upload-2026.jks` was prepared during recovery;
its Play reset request was cancelled after the original password was found.
Use the registered original key above for releases.

## Notes

The compass reports magnetic north. Nearby magnets, metal objects, and some phone cases can affect accuracy. The LED lantern and SOS signal require camera permission because Android treats control of the camera flash as a camera capability.
