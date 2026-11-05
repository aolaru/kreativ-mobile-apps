# BLOCKSHIFT

An offline, portrait block puzzle for Android, published under **Made by Kreativ**. Native Godot 4.6.3 project, typed GDScript, original geometric artwork, original replaceable placeholder tones, Manrope typography. Working title; naming is not cleared for publication.

Version **0.5.1** adds portable progress export, verified restore previews and Undo restore. Native Android file selection needs Android 10 or later and adds no storage permission. All mode boards, statistics, daily history and preferences travel in one private file. The free offline internal-test edition uses a dedicated upload signature, confirmed support contact and hosted privacy policy. Independent mode saves, charged rescues, reversible previews and version 3 save migration remain available. The SHIFT lesson starts empty and earns a charge through four separate line clears.

## Play or edit

1. Install [Godot 4.6.3 stable](https://godotengine.org/download/archive/4.6.3-stable/) (standard, not .NET).
2. Import `project.godot` and press F6 on `scenes/main.tscn`, or F5.
3. Desktop: drag with the left mouse button. Android: drag with one finger. The held piece lifts above your finger; outlined cells preview the drop. An X marks an invalid cell. Releasing elsewhere cancels.
4. Fill any row or column. All completed lines clear simultaneously. Three used pieces deal a new hand.

```sh
cd blockshift
# Replace godot with your engine binary if it is not on PATH.
godot --editor --path .
godot --path .
GODOT_BIN=/path/to/godot ./tools/test.sh --render
```

## Modes

- **Classic:** no timer, no SHIFT; run ends when none of the remaining pieces fits.
- **SHIFT:** earn one energy per full line, four energy per charge. Tap Use SHIFT, switch between Rows/Columns, touch a line, then swipe or tap a direction to preview. White plus marks identify cells that will clear, and tray captions show which pieces will fit afterward. Tap Apply SHIFT to commit; Cancel keeps your charge. One-cell cyclic movement includes empty cells. Invalid and unchanged moves preserve charge. A successful movement consumes charge before its clears earn new energy. SHIFT is optional. When no piece fits, a charged move that opens space or completes the Daily objective keeps the run alive. A useful rescue is suggested automatically. Unhelpful moves on a blocked board preserve the charge. The run ends when neither placement nor a charged rescue is possible. An ordinary preview warns if applying a move would end the run. Finished historical runs stay finished.
- **Zen:** no timer or failure. When none of the remaining pieces fits, a fresh board and hand appear automatically; your score continues. Finish a session from Pause. Calmer colors and fewer effects.
- **Daily orbit:** clear eight lines with SHIFT available. Local calendar date plus rules version creates the seed. Retries start identically; subsequent deals depend on board choices and seeded randomness. The date stays locked through midnight and resume. No global clock, leaderboard, account, or clock-tampering protection.

The interactive tutorial starts with an empty SHIFT meter. Clear four outlined practice rows over separate turns to see charge accumulate and persist, then preview and apply a wrapping move that clears a column. Preview and Cancel keep the charge; Apply spends it. The last clear remains visible before the lesson changes boards. Its Play SHIFT action resumes an existing SHIFT board or starts that mode while keeping other boards. It preserves your interrupted run and does not affect statistics.

## Graphics and game feel

`config/visuals.json` tunes finite pickup, landing, SHIFT, clear, reward and deal timings. `BoardEffects` owns only visual snapshots and a separate particle RNG. Board/scoring transactions commit immediately; new input discards stale overlays. Landing, held pieces and edge-wrap fragments share the same original two-layer vector tile material and exact cell footprint. `Atmosphere` uses three small static gradient textures and no continuous animation or post-processing shader. Canvas drawing stops once transient effects settle. Particle budgets are 40 normally and 16 with battery saver; Zen emits none. Battery-life gains and sustained device frame pacing are not measured.

The rendered test command opens temporary test windows above other windows for reliable rendering and records a five-second demonstration from controlled QA fixtures into `build/preview_frames/`. This is real Godot output, not a frame-rate benchmark. An optional preview can be encoded with `ffmpeg -framerate 30 -i build/preview_frames/%04d.png -c:v libx264 -crf 20 -pix_fmt yuv420p -an build/BLOCKSHIFT-graphics-preview.mp4`. Build directories and recordings are excluded from imports and Android exports.

## Structure

| Directory | Responsibility |
| --- | --- |
| `scripts/core/` | Rendering-free board, pieces, scoring, SHIFT, modes, daily rules, transactional session |
| `scripts/services/` | Recoverable saves, settings, audio, themes, commerce policy |
| `scripts/platform/` | Disabled purchase/advertising adapter contracts |
| `scripts/ui/` | Navigation, native controls, board/tray rendering and pointer ownership |
| `scripts/debug/` | Editor-only preset boards, excluded from Android packages |
| `config/` | Tuning values, mode metadata, weighted piece catalog |
| `assets/` | Vector identity, fonts, licenses, audio manifest and WAV/OGG replacement paths |
| `tests/` | Deterministic logic and rendered input harnesses |
| `docs/` | Architecture, Android build, release checklist, limitations and roadmap |
| `review/` | Reproducible UI screenshots and validation evidence |

`GameSession` is the game manager and transaction boundary. Board state commits immediately; animation never delays a rule decision. Line clearing is a pure board operation, and `GameCanvas` owns pointer-to-grid placement. Scenes remain small because reusable native UI is assembled in code.

## Tests and developer tools

```sh
godot --headless --path . --editor --import --quit
godot --headless --path . --script tests/run_tests.gd
godot --path . --script tests/ui_smoke.gd
godot --path . --script tests/reliability_smoke.gd
```

The logic suite covers boundaries, overlap, simultaneous lines, scoring and streaks, row/column wrap, charge consumption, SHIFT-generated clears, game over, generator fairness, daily determinism, all modes, save round trips and recovery, migrations and mode-specific scores. Eighty seeded stress games exercise repeated placement and resumes. The reliability harness covers failed-save warnings, native Retry hit testing, automatic retries, read-only and failed reset transactions, independent dialog Back destinations, completion timing, reduced motion, manual ending reasons and simultaneous combo/charge feedback. The UI harness injects viewport-local screen-touch events for board gestures and mouse events for desktop native controls, tests second-finger isolation and rapid cancellation, completes the tutorial, checks pause and several portrait sizes, and captures screens with Godot’s renderer. This is rendered engine-level coverage, not physical Android certification. It requires a graphical display: `tools/test.sh` runs logic and audio asset checks; add `--render` for native input and pixel/layout checks. Headless native button/pointer hit testing is not treated as a valid substitute. QA uses isolated saves. Desktop host focus is controlled during automated rendering; foreground/background audio behavior and real focus-loss pause policy are exercised explicitly. `tools/android_smoke.py` supplies a separate emulator-only touch/restart harness; its explicit reset flag clears only the game’s emulator test data.

In the editor, use Pause → Developer boards or F8 for cross-clear, wrap, charged rescue, crowded and new-hand presets. Presets never update player statistics. The entire debug resource and all tests/tools/review artifacts are excluded from Android exports; release guards also block the menu.

## Tuning and assets

Scoring, energy, daily target and ad spacing live in `config/rules.json`. The catalog in `config/pieces.json` contains twenty fixed, unrotated shapes. Weighted generation guarantees at least one currently placeable piece on a new deal when any catalog shape fits; it does not guarantee the entire hand can be played or keep rescuing pieces already dealt. Daily generation uses the same rules. A future catalog or balance change must bump daily/save rule versions.

Replace paths in `assets/audio/manifest.json` with Kreativ Sound WAV or OGG files. `AudioManager` preloads and uses a six-voice SFX pool and an independent ambience bus; there are no waits on gameplay input. `tools/generate_audio.py` recreates the original placeholder tones, including a seamless 12-second pad with midrange harmonics for phone speakers. Settings offers an ambience preview and percentage controls; playback respects mute and application focus. `tools/test_audio.py` checks source level, clipping and loop gaps. WAV loop settings are in the corresponding Godot import. SFX, ambience, particles, motion, haptics and 120% interface text are configurable. Unfinished premium-theme and PRO promotions are hidden in the internal-test edition. Commercial adapters remain disabled and cannot grant PRO.

Progress is stored under Godot's `user://` folder, never the project. Version 3 save envelopes use a sequence, SHA-256 checksum, flushed temporary file and recovery backup. Version 1/2 data migrates to a canonical `runs` dictionary plus a `last_mode` pointer. Each mode keeps one board; resuming adds no new game count. Modes → New game replaces only the selected mode after confirmation. An unfinished Daily keeps its original date; starting today’s challenge asks before replacing an older Daily. Startup selects the newest valid main/temp/backup. Unknown future versions are read-only. A checkpoint occurs after successful moves, every 15 active seconds, on pause/background/navigation, and on exit. Best scores update during a run. Statistics count starts, cleared lines, clearing-turn streaks and active gameplay time, separately per mode. SHA-256 detects corruption, not tampering.

Use Settings → About → Back up & restore progress before changing installation signatures or phones. Export writes a `.blockshift` file to your selected location and reads it back for verification. Restore validates its checksum and all saved runs, displays a preview, and requires confirmation before replacing progress. The previous progress remains available through Undo last restore. Backups are unencrypted: keep them private and outside the app before uninstalling. Interrupted import staging cannot replace a valid save. Automated transfer and rendered UI tests use disposable storage; native phone and Play-delivered checks are reported separately.

## Android and commercial status

See [Android build instructions](docs/ANDROID.md), [release checklist](docs/RELEASE_CHECKLIST.md), [known limitations](docs/KNOWN_LIMITATIONS.md), [validation](review/VALIDATION.md) and [roadmap](docs/ROADMAP.md). APK/AAB output is under `build/` and is ignored by Git. The dedicated upload key and recovery credentials live outside the workspace; they must never be committed or included in the tester handoff. There are no advertising/billing SDK credentials. No tracking or network SDK is installed. Commercial adapters are deliberately unavailable rather than fabricated successful transactions.

Internal-testing handoff: `store/internal-testing/play-console-checklist.txt`. Generate the signed bundle with `./tools/android_build.sh aab-internal`; the private upload key must already be configured locally. Current 0.5.1 evidence is recorded in `review/INTERNAL_TESTING_0_5_1.md`; 0.5.0 evidence remains historical. The policy is live at `https://madebykreativ.com/privacy/blockshift/`. Play Console release status and physical-device coverage are separate from successful local builds.
