# Signed 1.0.2 physical-phone validation — 4 November 2026

Passed on Samsung Galaxy A55 (SM-A556B), Android 16 / API 36,
Android System WebView 156.0.8078.47. Package `com.kreativfont.studio`,
version 1.0.2, version code 5. The APK read back from the phone exactly matched
`artifacts/play/kreativ-1.0.2-upload-signed-release.apk`:

`8b6c8a5cfb6026828f21c1cd1c34a513b8736c9f5abfd2966142bdafc4adc543`.

The owner explicitly resumed phone testing and requested publication after a
successful test. Existing 1.0.1 was upgraded with the original upload key, without
uninstalling the app. A fresh owner-only backup of its installed APK, complete
Capacitor preferences, hashes and device settings is retained under
`.local/phone-backups/20261104T072357Z/`.

## Passed

- Both original draft/favourites preference value hashes survive upgrade exactly.
- The tested release remains non-debuggable with no target WebView DevTools socket.
- Mixed word styling, native clipboard write/read-back, complete-design Undo/Redo,
  saved-design deletion recovery and saved-design reopening work.
- Full privacy policy/support/version dialog works without horizontal overflow.
- Native text share preview contains the expected styled text; image share shows
  a preview with an actual 2160 × 3840 cached image. Both sheets are cancelled
  without selecting a receiving app or person.
- Android document picker saves an opaque 1080 × 1080 PNG and a transparent
  2160 × 3840 PNG. Both actual phone files were pulled and decoded on the host;
  custom purple word pixels are present (9,838 and 42,898 respectively), and
  corner alpha is 255/0 as expected.
- Cancelling a high-resolution save returns controls to enabled. Temporary source
  cache is empty after saves/cancellation, exercising the reflected activity callback.
- Normal cold launch without instrumentation retains the draft and favourite
  hashes. Reopening the test harness confirms the restored high-resolution draft.
- Android font scale 2.0 produces WebView text zoom 200. Portrait 384 × 832 and
  landscape 832 × 384 CSS viewports have no horizontal overflow. Visible action
  targets meet 48px dimensions; portrait quick actions fit inside the viewport.
  The quick-action bar is intentionally hidden at the wider landscape breakpoint;
  normal Copy/Save controls remain available. Copy works after rotation.
- No target-package crash-buffer entries or app JavaScript errors in the completed
  test sequence were observed. Preliminary test-controller assumptions/selectors
  were corrected and the affected sequences rerun; they were not app failures.

## Restoration and cleanup

Before cleanup, the current original preference snapshot was restored at a cold
startup boundary and both value hashes matched the fresh backup exactly. The
single original favourite is preserved. Test-only PNGs were removed from Downloads,
then their absence verified; the test share cache was cleaned through Filesystem.
The temporary same-signer instrumentation package was uninstalled and its absence
verified. Font scale, rotation and stay-awake settings match the original settings.
The normal signed 1.0.2 app is left installed and open with the original designs.

The harness is separate from the shipping APK/AAB; it does not alter them or enable
WebView debugging. Its generated APK contains the private snapshot and stays in
ignored, owner-only `.local/release-harness/`; never upload it to Google Play.

## Evidence and limits

`artifacts/phone-release-1.0.2/` contains `native-results.json`, core/share/save/
layout/PNG reports, filtered native chooser/picker controls, fixture screenshots,
decoded PNG exports, restored preference hashes and a target-only crash report.
The final `npm run check:release` passes all 18 checks and recognises matching
physical-device evidence for this exact signed APK hash.

Coverage is one phone and current WebView. Older Android/WebView versions, tablets,
TalkBack speech/focus and delivery into an external receiving app remain untested.
Play-delivered APKs use the Play app-signing key, which may differ from the upload
key used for this direct-install test. Publication state is recorded separately
in `store/play-console-state.json` and the handover.
