# Roadmap

## Implemented in 0.5.3

Forest has a much quieter continuous wind/noise bed and clearer bird phrases after real phone-listening feedback. Rain and Ocean are byte-identical to 0.5.2. Added decoded audio acceptance thresholds for quiet background level and bird-to-bed contrast. No gameplay or save-format changes.

## Implemented in 0.5.2

Rain, Forest and Ocean soundscapes plus Off, selected-sound previews, saved preferences and bounded crossfades. Three original 48-second stereo Vorbis loops ship offline. Older saves retain all game data and volume, adding Rain as the default. Runtime and UI tests cover switching, previews, mute, rapid transitions, focus loss and save/transfer persistence. Physical 0.5.2 listening and Play delivery remain separate acceptance checks.

## Implemented in 0.4.1

Persistent save-failure warnings and retries, staged progress reset with failure preservation, explicit dialog Back destinations, final-clear presentation before results, saved manual-ending reasons, a zero-to-full SHIFT charging lesson and retained combo/points feedback. Added storage, navigation, completion and tutorial regression coverage. The Galaxy A55 upgrade, full charging tutorial, preview/Apply and independent Classic/Zen save/restart checks now pass; existing high scores and the prior SHIFT save are preserved.

## Implemented in 0.4.0

Charged rescue-aware game over, pure SHIFT previews with explicit Apply/Cancel, predicted clear markers, future-fitting piece captions, risk warnings, a revised interactive lesson, independent per-mode saves, migration of legacy saves and mode-aware resume/new-game actions. Added deterministic rescue/save tests and rendered input coverage. Physical-phone installation was pending for that build; see 0.4.1 above for current device checks.

## Implemented in 0.3.1

Fixed the lesson-to-game replacement confirmation retaining an empty game session. The confirmation now appears over the home page; keeping the existing run leaves a usable menu. A regression test covers status-message expiry during that confirmation.

## Implemented in 0.3.0

Original layered vector tile material and specular detail, atmospheric background, recessed board frame, exact wrap-fragment texture cropping, finite pickup/landing/clear/deal/reward effects, combo and best-score feedback, daily badge artwork and softer page/sheet transitions. Visual snapshots remain separate from game rules and are discarded on new input or pause. Reduced motion, particle preferences and smaller battery-saving budgets are covered by tests. Future work includes device frame-time measurements and longer human playtests.

## Implemented in 0.2.0

Compact home and saved-board card; grouped Play/Sound/About settings; adaptive board/hand spacing and tall-piece fitting; configurable drag height with consistent preview geometry; a 30 fps battery-saving rendering option; clearer SHIFT directions and selected-line outlines; in-game rules; scrollable modal sheets; saved-game-safe settings restoration; parent-aware Back navigation. Existing players' boards and preferences remain compatible. Battery-life gains still require measurement.

## Before public testing

1. Playtest difficulty and fairness with new and experienced players. Tune only the resource configuration; version daily rules when changing generation or objectives.
2. Test 30-minute sessions on low/mid/high-range physical Android hardware, compact phones, tablets, cutouts, gesture navigation, screen lock, incoming calls, force-stop and storage pressure.
3. Test Android accessibility services and improve the board's non-drag interaction model. Add system font-scale integration and localization.
4. Replace placeholder tones with mastered Kreativ Sound assets; verify interruptions, Bluetooth routes and muted startup.
5. Clear the game name and identity. Prepare original Play assets and hosted privacy/support pages.

## Commercial integration

1. Select maintained Godot Android Billing and advertising plugins compatible with API 36, ARM64 and 16 KB native pages.
2. Implement verified non-consumable PRO entitlement, pending purchases, acknowledgment, localized price and restore/refund handling. Test through a Play internal track with license testers.
3. Implement consent and regional privacy choices before initializing any advertising SDK; update permissions, privacy and Play disclosures from actual SDK behavior.
4. Add optional rewarded uses only after a specific player-friendly mechanic is defined. Do not gate regular placement or SHIFT behind ads.
5. Exercise interstitial limits outside active play and protect the first session, tutorials, PRO and short repeated games.
6. Produce a real signed release with a private upload key and complete Play's pre-launch reports.

## Later releases

Curated visual themes, additional customization, optional puzzle packs, more daily objectives, iOS billing/export adapters, accessible tap placement, wider localization and optional user-controlled cross-device backup. Keep the core offline and account-free.
