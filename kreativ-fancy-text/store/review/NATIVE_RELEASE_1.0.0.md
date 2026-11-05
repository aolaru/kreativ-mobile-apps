# Signed 1.0.0 physical-phone validation — 24 October 2026

Passed on Samsung Galaxy A55 (SM-A556B), Android 16 / API 36,
Android System WebView 155.0.8059.46. Package `com.kreativfont.studio`,
version 1.0.0, version code 3.

The installed APK was read back and its bytes matched
`artifacts/play/kreativ-1.0.0-upload-signed-release.apk` exactly:

`96d675961f833c18d5f58efd6fb0842e4ceb921d8b8a331d4689bfab840fac15`

The release remained non-debuggable with no WebView DevTools socket. No release
APK/AAB code or configuration was changed for testing. Temporary instrumentation,
signed with the existing upload key, exercised the real UI/native bridges and
inspected app-local preferences/cache. Normal cold launches without that
instrumentation were also checked. Nothing was sent to another person or
uploaded to Google Play.

## Passed

- Full in-app privacy policy, support contact, and version 1.0.0; no policy-dialog
  horizontal overflow.
- Mixed-style clipboard output written to Android and read back exactly.
- Text share sheet contained the expected styled test text; cancelled cleanly.
- Image share sheet contained a real high-resolution preview; cached image decoded
  as 2160 × 3840. Cancelled without selecting a receiving app or recipient.
- Opaque square PNG saved through Android's document picker to Downloads:
  1080 × 1080, 64,765 bytes, 6,090 pixels in the chosen purple word color.
- Transparent story PNG saved through the same native picker: 2160 × 3840,
  260,812 bytes, corner alpha 0, 131,246 painted pixels, 27,586 purple pixels.
- Both saved phone files were copied back and decoded on the host.
- Large PNG save-picker cancellation returned the editor controls to enabled.
- The private save cache was empty after successful saves and cancellation.
- Native draft/favorite hashes matched across a cold restart, preserving styled
  text, transparent 2× story settings, and favorite count. Reopening the saved
  design restored its opaque square configuration and purple word styling.
- Portrait 384 × 832 and landscape 832 × 384 CSS viewports had no horizontal
  overflow. Visible app, preview, policy, and landscape captures were inspected.
- No app JavaScript errors or target-process fatal exceptions were observed.

## Preservation and cleanup

The owner explicitly approved replacement of the differently signed debug app.
Before uninstalling debug 0.1.1, its installed APK and complete app-private data
were copied to an owner-only, ignored backup under `.local/phone-backups/`.
Backup file hashes were verified. Credentials and signing keys were unchanged.

The original draft and one favorite were restored before release testing and
again after removing the temporary test design. Preference value hashes matched
the backup exactly; after normal relaunch, the original draft area was also
pixel-identical to its initial screenshot. The signed release is left installed
and open with those original designs.

The temporary instrumentation package was uninstalled. Only the two PNGs created
by this release test were removed from phone Downloads after preserving decoded
host evidence. The test share cache was cleared, rotation returned to its
original portrait lock, and the original stay-awake setting was restored.
Earlier files and unrelated phone content were left alone.

## Evidence and limits

`artifacts/phone-release-1.0.0/` contains `native-results.json`,
`png-results.json`, restart/layout results, filtered chooser controls, screenshots,
decoded PNGs, and app-process crash logs. Contacts and unrelated chooser labels
were omitted from saved UI evidence. `artifacts/play/verification.json` now
references this phone evidence only when its APK hash still matches.

This covers one phone and one current WebView version. Older supported Android /
WebView versions, tablets, large accessibility fonts, and actual import into
another app remain untested. Share sheets were cancelled; delivery to an
external app or person was not tested. Play's delivered APK will use its
app-signing certificate, which may differ from the upload certificate tested here.
