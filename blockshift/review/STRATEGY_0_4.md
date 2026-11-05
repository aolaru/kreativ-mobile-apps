# SHIFT and independent saves — 0.4.0

A charged SHIFT now keeps a blocked run alive when a move can open space or complete Daily. Useful rescue directions are derived from pure board simulations. If clears recharge SHIFT, continuation can include another charged move; recursion only follows clearing moves and therefore strictly reduces occupied cells. No-op and unhelpful blocked-board moves keep the charge. Historical finished runs stay finished.

SHIFT now follows Use SHIFT → choose a line → choose a direction → inspect preview → Apply. Swipes preview too. The board shows prospective tile positions, vector direction arrows and white plus marks on cells that will clear. Piece captions identify pieces that will fit afterward. Cancel keeps the board and charge. A rescue suggestion appears automatically when blocked. An ordinary move that would end the run gets an explicit warning before applying. The interactive lesson explains the four-line charge and creating space.

Save envelope version 3 stores one run under each mode plus a last_mode pointer. Legacy versions 1/2 migrate the old active board into its mode slot, retaining settings, scores, statistics and daily history. Mode selection resumes by default. Explicit New game only replaces that mode, after confirmation. An unfinished older Daily retains its date and has a Continue action; starting today’s challenge asks before replacing it. Fresh Daily seeds and snapshots use rule version 2.

## Verification

- 7,111 core assertions, 69 rendered UI assertions and 16 graphics assertions passed with zero failures; 80 seeded stress games / 1,285 placements and audio checks passed.
- New deterministic cases cover row/column rescue, insufficient charge, no rescue, unchanged charge on rejected moves, clear scoring, Daily completion, historical terminal saves, preview isolation, risky moves, legacy migration, all four slots, repeated checkpoints, per-mode replacement and reload.
- Rendered tests exercise preview-before-apply, cancellation, actual post-rescue placement availability, independent mode switching, mode cards, new-game confirmation, older Daily continuation, compact/large-text controls and explicit focus/audio lifecycle behavior. Screens 37–40 show the new flow.
- A disposable copy of the last captured real 0.3.1 phone save also migrated and round-tripped successfully. Its board, hand, score, energy, RNG, date, moves, elapsed time, settings, aggregate statistics and daily history were preserved. The input fixture was unchanged. See logs/strategy-migration-fixture.log. This uses historical captured data and does not certify the current phone installation.
- The harness uses the documented [Godot viewport input API](https://docs.godotengine.org/en/stable/classes/class_viewport.html#class-viewport-method-push-input) with local coordinates. Native controls receive mouse events in the desktop harness; board interactions receive touch events. Host focus is controlled in debug-only isolated tests, while lifecycle behavior is exercised explicitly. No UI button signal is emitted as a substitute for hit testing.

## Android scope

The phone was disconnected and the user could not reconnect it. Version 0.4.0 installation and native checks are pending. The prior Galaxy A55 evidence belongs to 0.3.1. Current Android build results and package inspections are recorded in VALIDATION.md and BUILD_MANIFEST.json. Billing/ads remain disabled and release signing remains external.
