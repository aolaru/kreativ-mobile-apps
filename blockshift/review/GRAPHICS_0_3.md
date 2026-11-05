# Graphics and animation pass — 0.3.1

Original SVG tile materials add an upper highlight, bevel and restrained rim. Grid slots sit inside a recessed board with a subtle shadow. Held, previewed and placed blocks share the same cell footprint, including cropped UVs for fragments crossing a SHIFT edge. The background uses small static gradient textures; primary buttons have a scalable gradient surface. The home illustration and Daily success badge are original vector assets.

Pickup, placement settling, line dissolves, directional SHIFT wrap, score/combo rewards, personal bests and new-hand reveals use finite timelines. SHIFT movement finishes before the outgoing column dissolves. The interactive lesson leaves time to see that sequence before showing completion. Screen and sheet fades use node-owned tweens that disappear safely on navigation.

`scripts/ui/board_effects.gd` owns visual snapshots and its own random source. Gameplay commits immediately; animation cannot change the authoritative board, score, hand or generator state. New input discards stale overlays. Pause and navigation stop board effects. Once effects settle, board redraw processing stops. Particle limits are 40 normally and 16 in battery saver; Zen suppresses particles. Reduced motion skips the timelines. Durations are configurable in `config/visuals.json`.

## Verification

- 7,075 gameplay assertions, 55 rendered UI assertions and 16 graphics assertions passed, including immediate model commits, snapshot isolation, wrapping, finite effect lifetimes, pause, reduced motion, new-hand interaction and delayed tutorial completion.
- 80 seeded stress games / 1,285 placements and existing audio checks passed.
- Screenshots 25–35 and the 150-frame, five-second preview are actual Godot renders of controlled QA fixtures. The preview is silent and is not a device frame-rate benchmark.
- The 0.3.0 pass was first inspected on the physical Galaxy A55 in the restored Classic board, pause, settings and Zen screens. After the user confirmed phone availability, a full native tutorial replay passed. Its prior process log also revealed the navigation bug described below. The final 0.3.1 APK was then installed and replayed: charging, arming, wrap, column clear, delayed completion and injected Android Back all passed. An exact full save-payload comparison preserved the last saved run, preferences, statistics and daily history across that patch update and replay. The fresh 0.3.1 process log contains zero script errors. See `logs/graphics-phone.json` for exact scope.
- APK, development AAB and unsigned release AAB pass package inspections. The release bundle passes bundletool validation. Preview frames, review files, tests and development build resources are excluded from exports.

## Navigation repair in 0.3.1

The 0.3.0 device log revealed an empty-session refresh after choosing Play SHIFT from a lesson while an interrupted run needed replacement confirmation. The lesson now exits to the home page before opening that confirmation. Keeping the existing run leaves a usable home page. Refresh also safely ignores a missing session. Two rendered regression assertions cover expiry of the lesson status message and cancellation without replacing saved progress. A fresh 0.3.1 process completed the native replay with zero script errors.

## Limits

No sustained phone frame-time, thermal, battery or broad GPU benchmark was performed. The full emulator restart/tutorial harness remains historical evidence from 0.1.0. Billing, ads and production signing remain disabled or external setup steps. See `VALIDATION.md` and `../docs/KNOWN_LIMITATIONS.md`.
