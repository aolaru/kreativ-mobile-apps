# Tile and ambience feedback — 2026-10-17

Player reports: placed tiles appear not to match the grid boxes; ambience is hard to hear.

## Changes

Placed tiles now fill exactly the same rectangle and rounded silhouette as empty grid cells. The raised face that was three logical pixels shorter has been removed. Board cells and placement previews use shared rectangle geometry. A rendered pixel test compares occupied and empty bounds along both axes after placement settles.

The original bass-only pad measured -35.1 dBFS RMS and faded down every eight seconds. Its replacement is a seamless 12-second sustained pad at -20.3 dBFS RMS with 262–784 Hz harmonics, no clipping or silent dips. Player volume settings remain unchanged. Settings now shows percentages, a media-volume hint and Preview ambience. Playback and preview respect mute and application focus; raising the slider restores playback.

## Validation

- 7,049 gameplay assertions and 37 rendered UI assertions passed; 80 stress games / 1,285 placements.
- Source audio checks passed: level, clipping, half-second sustained level windows and loop seam.
- Rendered UI tests cover exact tile/slot bounds, continuous looping, mute/unmute, focus, preview and previous touch/tutorial/layout behavior. Synthetic button tests now wait for container layout and update pointer position before pressing.
- Native GUI hit testing is not supported reliably by the headless display backend. The default test command explicitly runs core and audio checks and reports UI skipped; --render runs the full native UI suite. Earlier diagnostic runs exposed focus/layout issues in the harness; the final rendered suite passes without bypassing its assertions.
- Debug APK exported successfully; package signature, 16 KB alignment, permissions and development-resource exclusion checks passed.
- APK installed over the existing build on Samsung Galaxy A55 / SM-A556B and launched. Existing board, pieces, score, RNG, move count and all saved settings match the preinstall checkpoint. Real phone touches opened Settings and Preview ambience; the preview dialog shows playback active. No runtime script errors were captured. Device media volume was read at 3/15 and was not changed. Actual listening confirmation remains with the player.

Current debug APK and source ZIP contain these changes. Both AABs remain the preceding baseline builds; the full emulator harness was not rerun for this UI/audio update. See logs/tile-audio-feedback-tests.log, logs/tile-audio-default-tests.log, logs/tile-audio-phone.json and screenshot 19-tile-grid-alignment.png.
