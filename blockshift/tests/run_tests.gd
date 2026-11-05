extends SceneTree
class TestAds extends AdAdapter:
	var shown: int = 0
	func available() -> bool: return true
	func show_interstitial() -> bool:
		shown += 1
		return true

var assertions: int = 0
var failures: int = 0

func check(condition: bool, description: String) -> void:
	assertions += 1
	if not condition:
		failures += 1
		printerr("FAIL: " + description)

func _initialize() -> void:
	placement()
	line_clears()
	shifting()
	scoring()
	generation()
	sessions()
	shift_rescues()
	mode_saves()
	persistence()
	reset_transactions()
	defensive_loading()
	commerce()
	visual_effects()
	stress()
	print("BLOCKSHIFT: %d assertions; %d failures" % [assertions, failures])
	quit(0 if failures == 0 else 1)

func visual_effects() -> void:
	var preferences: SettingsManager = SettingsManager.new()
	var fx: BoardEffects = BoardEffects.new()
	var game: GameSession = GameSession.new("shift", 909)
	var original: Dictionary = game.snapshot()
	var cleared: Array = range(64)
	var visual: Array = []
	visual.resize(64)
	visual.fill(2)
	var result: Dictionary = {"cleared": cleared, "visual_cells": visual, "lines": 16, "points": 900, "shifted": false, "zen_reset": false}
	var pending: Dictionary = {"indices": [0, 1], "cells": [[0, 0], [1, 0]], "color": 2, "from": Vector2.ZERO, "to": Vector2.ONE}
	fx.begin(result, pending, preferences, "shift", 3)
	check(fx.active() and fx.ghosts.size() == 64 and fx.sweeps.size() == 16, "clear presentation retains outgoing snapshots and both line axes")
	check(fx.sparks.size() == 40 and fx.banner.title == "COMBO ×3", "combo effects are bounded and labeled")
	fx.advance(0.1)
	check(not fx.landing.is_empty() and fx.clear_progress() == 0, "placement settles before clear disappearance")
	fx.advance(0.1)
	check(fx.landing.is_empty() and fx.clear_progress() > 0 and not fx.ghosts.is_empty(), "clear follows landing without delaying game state")
	fx.advance(2)
	check(not fx.active() and fx.ghosts.is_empty() and fx.sparks.is_empty(), "presentation reaches a fully idle state")
	fx.celebrate("PERSONAL BEST", Color.WHITE)
	fx.advance(0.1)
	check(fx.active() and not fx.banner.is_empty(), "a celebration can start after an idle timeline")
	fx.advance(2)
	check(not fx.active(), "celebration expires")
	preferences.values.battery_saver = true
	fx.begin(result, pending, preferences, "shift", 2)
	check(fx.sparks.size() == 16, "battery saver lowers particle budget")
	fx.pickup(true)
	check(fx.motion.is_empty() and fx.ghosts.is_empty() and fx.landing.is_empty() and fx.pickup_left > 0, "new pickup reveals authoritative state and cancels stale overlays")
	fx.advance(1)
	check(not fx.active(), "pickup has no continuous idle redraw")
	fx.reject(2, true)
	check(fx.reject_slot == 2 and fx.active(), "invalid drop marks only its own tray")
	fx.advance(1)
	check(not fx.active(), "invalid-drop feedback expires")
	preferences.values.particles = false
	fx.begin(result, pending, preferences, "shift", 2)
	check(fx.sparks.is_empty() and not fx.ghosts.is_empty(), "particles off retains clean non-particle clears")
	preferences.values.particles = true
	fx.begin(result, pending, preferences, "zen", 2)
	check(fx.sparks.is_empty(), "Zen never emits particles")
	preferences.values.reduce_motion = true
	fx.begin(result, pending, preferences, "shift", 3)
	check(not fx.active() and fx.banner.is_empty() and fx.landing.is_empty(), "reduced motion eliminates the entire timeline")
	preferences.values.reduce_motion = false
	var motion_result: Dictionary = result.duplicate(true)
	motion_result.motion = {"before": visual.duplicate(), "axis": "row", "index": 3, "direction": 1}
	fx.begin(motion_result, {}, preferences, "shift", 2)
	check(fx.move_seconds == float(VisualConfig.value("shift_seconds")) and fx.clear_progress() == 0, "SHIFT wraps before its generated clear dissolves")
	fx.clear()
	check(not fx.active(), "pause/navigation can finish presentation immediately")
	for i in 100:
		fx.begin(result, pending, preferences, "shift", 2)
		fx.advance(0.01)
	check(fx.ghosts.size() == 64 and fx.sparks.size() == 16 and game.snapshot() == original, "rapid presentation replacement is bounded and never changes board, score or RNG")
	var zen_result: Dictionary = result.duplicate(true)
	zen_result.zen_reset = true
	zen_result.cleared = []
	zen_result.lines = 0
	fx.begin(zen_result, {}, preferences, "zen", 0)
	check(fx.ghosts.size() == 64 and fx.banner.title == "FRESH SPACE" and fx.sparks.is_empty(), "Zen renewal has a calm distinct visual transition")
	var deal_result: Dictionary = {"cleared": [], "lines": 0, "points": 1, "shifted": false, "dealt_hand": true}
	fx.begin(deal_result, {}, preferences, "classic", 0)
	check(fx.deal and fx.deal_progress(0) == 0 and fx.deal_progress(2) == 0, "new hand starts a staggered visual reveal")
	fx.advance(1)
	check(not fx.active() and fx.deal_progress(2) == 1, "deal is finite and never delays access to model pieces")

func placement() -> void:
	var board: Board = Board.new()
	var shape: Array = [[0, 0], [1, 0]]
	check(board.can_place(shape, Vector2i(6, 7)), "last valid boundary")
	check(not board.can_place(shape, Vector2i(7, 7)), "right boundary")
	check(not board.can_place(shape, Vector2i(-1, 0)), "negative boundary")
	check(not board.can_place([[0, 0], [0, 1]], Vector2i(0, 7)), "bottom boundary")
	check(not board.can_place([], Vector2i.ZERO), "empty piece")
	check(not board.place(shape, Vector2i.ZERO, 5), "invalid color rejected")
	check(board.place(shape, Vector2i.ZERO, 2), "valid placement")
	var before: Array[int] = board.cells.duplicate()
	check(not board.place(shape, Vector2i(1, 0), 1), "overlap rejected")
	check(board.cells == before, "invalid placement atomic")
	check(board.occupied() == 2, "placement count")

func line_clears() -> void:
	var board: Board = Board.new()
	for i in 8: board.cells[2 * 8 + i] = 1
	var result: Dictionary = board.clear_lines()
	check(result.count == 1 and result.cells.size() == 8 and board.occupied() == 0, "horizontal clear")
	for i in 8: board.cells[i * 8 + 4] = 2
	result = board.clear_lines()
	check(result.count == 1 and board.occupied() == 0, "vertical clear")
	for i in 8:
		board.cells[3 * 8 + i] = 1
		board.cells[i * 8 + 4] = 2
	result = board.clear_lines()
	check(result.count == 2 and result.cells.size() == 15 and board.occupied() == 0, "simultaneous cross union")
	board.cells.fill(1)
	result = board.clear_lines()
	check(result.count == 16 and result.cells.size() == 64, "all lines simultaneously")

func shifting() -> void:
	var board: Board = Board.new()
	board.cells[7] = 3
	check(board.shift("row", 0, 1) and board.cells[0] == 3 and board.cells[7] == 0, "row wrap right includes empty")
	check(board.shift("row", 0, -1) and board.cells[7] == 3, "row wrap left")
	board = Board.new()
	board.cells[7 * 8 + 2] = 4
	check(board.shift("column", 2, 1) and board.cells[2] == 4, "column down wrap")
	check(board.shift("column", 2, -1) and board.cells[58] == 4, "column up wrap")
	var before: Array[int] = board.cells.duplicate()
	check(not board.shift("bad", 0, 1) and not board.shift("row", 8, 1) and not board.shift("row", 0, 2), "invalid shifts")
	check(before == board.cells, "invalid shift atomic")
	var energy: ShiftSystem = ShiftSystem.new()
	check(energy.lines_remaining() == 4, "SHIFT starts four full line clears away")
	energy.earn(1)
	check(energy.lines_remaining() == 3 and not energy.ready(), "one line advances charge without unlocking")
	energy.earn(2)
	check(energy.lines_remaining() == 1 and not energy.ready(), "separate turns accumulate toward SHIFT")
	energy.earn(1)
	check(energy.lines_remaining() == 0 and energy.ready(), "four total lines unlock SHIFT")
	var per_line: int = int(Rules.value("shift_per_line"))
	Rules.values.shift_per_line = 2
	energy.energy = 1
	check(energy.lines_remaining() == 2, "remaining clears round up with configurable charge gain")
	Rules.values.shift_per_line = per_line
	energy.energy = energy.capacity()
	check(not energy.apply(board, "row", 4, 1, true), "empty row no-op")
	check(energy.ready(), "no-op preserves charge")
	check(not energy.apply(board, "column", 2, 1, false), "inactive blocked")
	check(energy.ready(), "inactive preserves charge")
	check(energy.apply(board, "column", 2, 1, true) and energy.energy == 0, "success consumes exactly one charge")
	check(not energy.apply(board, "column", 2, 1, true), "cannot spend twice")
	energy.earn(99)
	check(energy.energy == energy.capacity(), "energy clamped")
	var game: GameSession = GameSession.new("shift", 10)
	for y in 8:
		if y != 3: game.board.cells[y * 8] = 2
	game.board.cells[31] = 1
	game.shift.energy = game.shift.capacity()
	var result: Dictionary = game.move_line("row", 3, 1)
	check(result.ok and result.lines == 1 and game.board.occupied() == 0, "SHIFT creates line clear")
	check(game.shift.energy == 1 and game.score.total == 100, "SHIFT earns from resulting lines")
	game.shift.energy = game.shift.capacity()
	game.over = true
	check(not game.move_line("row", 0, 1).ok and game.shift.ready(), "no SHIFT after game over")

func scoring() -> void:
	var score: ScoreManager = ScoreManager.new()
	check(score.award(4, 0) == 4 and score.streak == 0, "cell points")
	check(score.award(2, 1) == 102 and score.streak == 1, "single clear")
	check(score.award(0, 2) == 300 and score.streak == 2, "two lines + streak")
	check(score.award(0, 3) == 600, "three lines + streak")
	check(score.best_combo == 3 and score.lines == 6, "combo and line stats")
	score.award(1, 0)
	check(score.streak == 0 and score.best_combo == 3, "non-clear resets streak")
	check(score.award(0, 4) == 700, "four lines")
	var extra: ScoreManager = ScoreManager.new()
	check(extra.award(0, 5) == 1000, "extra line scaling")

func generation() -> void:
	var a: PieceGenerator = PieceGenerator.new(12345)
	var b: PieceGenerator = PieceGenerator.new(12345)
	var board: Board = Board.new()
	var seen: Dictionary = {}
	for i in 500:
		var hand: Array = a.hand(board)
		check(hand == b.hand(board), "same seed same hand %d" % i)
		check(hand.size() == 3 and board.has_move(hand), "opening fairness %d" % i)
		var singles: int = 0
		for piece in hand:
			seen[piece.id] = true
			singles += int(piece.id == 0)
		check(singles <= 2, "singleton limit")
	check(seen.size() == a.catalog.size(), "all catalog pieces generated")
	board.cells.fill(1)
	board.cells[27] = 0
	for i in 100:
		check(board.has_move(a.hand(board)), "crowded board receives one fitting piece")
	check(DailyChallenge.seed_for("2026-10-16") == DailyChallenge.seed_for("2026-10-16"), "daily reproducible")
	check(DailyChallenge.seed_for("2026-10-16") != DailyChallenge.seed_for("2026-10-17"), "daily changes")

func sessions() -> void:
	var game: GameSession = GameSession.new("classic", 22)
	game.pieces = [game.generator.make_piece(0), {}, {}]
	check(game.place(0, Vector2i.ZERO).ok and game.pieces.size() == 3 and not game.pieces[0].is_empty(), "refill after three used")
	check(not game.move_line("row", 0, 1).ok, "Classic has no SHIFT")
	game.board.cells.fill(1)
	check(not game.board.has_move(game.pieces), "full board has no move")
	game = GameSession.new("classic", 22)
	# Checkerboard has no domino position but is not a full board.
	for y in 8:
		for x in 8: game.board.cells[y * 8 + x] = 1 if (x + y) % 2 == 0 else 0
	game.pieces = [game.generator.make_piece(1), {}, {}]
	check(not game.board.has_move(game.pieces), "game over checks all shapes, not fullness")
	var zen: GameSession = GameSession.new("zen", 2)
	for y in 8:
		for x in 8: zen.board.cells[y * 8 + x] = 1 if (x + y) % 2 == 0 else 0
	zen.pieces = [zen.generator.make_piece(0), zen.generator.make_piece(5), {}]
	zen.place(0, Vector2i(1, 0))
	check(not zen.over and zen.zen_resets == 1 and zen.board.occupied() == 0, "Zen renews when remaining pieces cannot fit")
	var daily: GameSession = GameSession.new("daily", 88, "2026-10-16")
	var again: GameSession = GameSession.new("daily", 99, "2026-10-16")
	check(daily.pieces == again.pieces, "daily ignores runtime seed")
	daily.score.lines = DailyChallenge.target() - 1
	for x in 7: daily.board.cells[x] = 1
	daily.pieces = [daily.generator.make_piece(0), {}, {}]
	daily.place(0, Vector2i(7, 0))
	check(daily.over and daily.completed, "daily objective ends run")
	var restored: GameSession = GameSession.from_snapshot(daily.snapshot())
	check(restored != null and restored.completed and restored.date == daily.date, "completed daily restored")
	check(GameSession.from_snapshot({"mode": "bogus"}) == null, "malformed snapshot rejected")
	var malformed: Dictionary = game.snapshot()
	malformed.board[0] = 22
	check(GameSession.from_snapshot(malformed) == null, "invalid board save rejected")
	var live: GameSession = GameSession.new("shift", 92)
	live.place(0, live.board.positions(live.pieces[0].cells)[0])
	var copy: GameSession = GameSession.from_snapshot(live.snapshot())
	check(copy != null and copy.board.cells == live.board.cells and copy.pieces == live.pieces and copy.score.total == live.score.total, "session round trip")
	check(copy.generator.hand(copy.board) == live.generator.hand(live.board), "RNG state round trip")

func persistence() -> void:
	var path: String = "user://qa-progress.json"
	for suffix in ["", ".tmp", ".bak"]: DirAccess.remove_absolute(path + suffix)
	var save: SaveManager = SaveManager.new(path)
	var game: GameSession = GameSession.new("classic", 1)
	check(save.start(game), "save initial")
	game.score.total = 123
	game.score.lines = 3
	game.elapsed = 65
	check(save.checkpoint(game), "save checkpoint")
	save.checkpoint(game)
	var shift_game: GameSession = GameSession.new("shift", 2)
	save.start(shift_game)
	shift_game.score.total = 456
	save.checkpoint(shift_game)
	var copy: SaveManager = SaveManager.new(path)
	check(copy.stats("classic").best == 123 and copy.stats("shift").best == 456, "mode-specific high scores")
	check(copy.stats("classic").lines == 3 and copy.stats("classic").playtime == 65, "checkpoint does not double-count stats")
	check(copy.saved().mode == "shift", "active session restored")
	var file: FileAccess = FileAccess.open(path, FileAccess.WRITE)
	file.store_string("{interrupted")
	file.close()
	copy = SaveManager.new(path)
	check(copy.recovered and copy.stats("classic").best == 123, "corrupt main recovers backup")
	check(copy.write(), "recovered save can commit")
	# Emulate a crash after writing the next complete temp but before renaming it.
	var env: Dictionary = JSON.parse_string(FileAccess.get_file_as_string(path))
	env.sequence = int(env.sequence) + 1
	var payload: Dictionary = JSON.parse_string(env.payload)
	payload.stats.classic.best = 777
	env.payload = JSON.stringify(payload)
	env.sha256 = str(env.payload).sha256_text()
	file = FileAccess.open(path + ".tmp", FileAccess.WRITE)
	file.store_string(JSON.stringify(env))
	file.close()
	copy = SaveManager.new(path)
	check(copy.recovered and copy.stats("classic").best == 777, "newer complete temp wins")
	var daily: GameSession = GameSession.new("daily", 1, "2026-10-16")
	copy.start(daily)
	daily.completed = true
	daily.over = true
	daily.score.lines = 8
	copy.checkpoint(daily)
	check(copy.data.daily["2026-10-16"].completed, "daily history completion")
	copy.start(GameSession.new("daily", 1, "2026-10-16"))
	check(copy.data.daily["2026-10-16"].completed, "retry preserves completion")
	for suffix in ["", ".tmp", ".bak"]: DirAccess.remove_absolute(path + suffix)
	file = FileAccess.open(path, FileAccess.WRITE)
	file.store_string(JSON.stringify({"version": 1, "data": {"stats": {"classic": {"best": 432}}}}))
	file.close()
	copy = SaveManager.new(path)
	check(copy.stats("classic").best == 432 and copy.write(), "v1 migration")
	file = FileAccess.open(path, FileAccess.WRITE)
	file.store_string('{"version":99}')
	file.close()
	copy = SaveManager.new(path)
	check(copy.read_only and not copy.write(), "future version never overwritten")
	for suffix in ["", ".tmp", ".bak"]: DirAccess.remove_absolute(path + suffix)
	var preferences: SettingsManager = SettingsManager.new()
	preferences.restore({"sfx": 10, "particles": "invalid", "theme": "bad", "text_scale": -5})
	check(preferences.values.sfx == 1 and preferences.values.particles == true and preferences.values.theme == "orbit" and preferences.values.text_scale == 1, "settings defensive loading")
	check(preferences.values.drag_lift == 88 and not preferences.values.battery_saver and preferences.game_fps() == 60, "older saves receive comfortable control defaults")
	preferences.restore({"drag_lift": -900, "battery_saver": true})
	check(preferences.values.drag_lift == SettingsManager.DRAG_MIN and preferences.game_fps() == 30, "drag height lower bound and battery frame limit")
	preferences.restore({"drag_lift": 900, "battery_saver": "invalid"})
	check(preferences.values.drag_lift == SettingsManager.DRAG_MAX and not preferences.values.battery_saver, "drag height upper bound and invalid boolean")
	preferences.restore({"drag_lift": NAN, "sfx": INF})
	check(preferences.values.drag_lift == 88 and preferences.values.sfx == 0.75, "nonfinite settings do not enter input or audio math")
	preferences.values.tutorial_done = true
	preferences.values.battery_saver = true
	preferences.values.drag_lift = 112
	preferences.reset_preferences()
	check(preferences.values.tutorial_done and preferences.values.drag_lift == 88 and preferences.game_fps() == 60, "restore defaults preserves learning progress")

func defensive_loading() -> void:
	var original: GameSession = GameSession.new("shift", 908)
	var snapshot: Dictionary = original.snapshot()
	snapshot.score = {"total": {}, "lines": "broken", "streak": null, "best_combo": []}
	snapshot.elapsed = {}
	snapshot.energy = []
	snapshot.moves = "bad"
	var restored: GameSession = GameSession.from_snapshot(snapshot)
	check(restored != null and restored.score.total == 0 and restored.elapsed == 0 and restored.shift.energy == 0, "malformed counters sanitized")
	for bad in [{}, [], null, "no", -1, 900, 1.5]:
		var corrupt: Dictionary = original.snapshot()
		corrupt.pieces[0].id = bad
		check(GameSession.from_snapshot(corrupt) == null, "invalid piece id rejected")
	for bad in [{}, [], null, "no"]:
		var corrupt: Dictionary = original.snapshot()
		corrupt.rng_state = bad
		check(GameSession.from_snapshot(corrupt) == null, "invalid RNG state rejected")
	var zen: GameSession = GameSession.new("zen", 908)
	zen.over = true
	check(GameSession.from_snapshot(zen.snapshot()).over, "finished Zen stays finished")

func commerce() -> void:
	var monetization: MonetizationManager = MonetizationManager.new()
	check(not monetization.purchases.available() and not monetization.ads.available(), "native integrations disabled")
	check(not monetization.verified_pro, "no fabricated entitlement")
	check(not monetization.try_interstitial(true, false, 99, 9999), "never ads during gameplay")
	check(not monetization.try_interstitial(false, true, 99, 9999), "never ads in tutorial")
	check(not monetization.try_interstitial(false, false, 1, 9999), "never ads after first session")
	monetization.ads = TestAds.new()
	check(not monetization.try_interstitial(false, false, 5, 9999), "consent required before SDK use")
	monetization.consent_granted = true
	check(not monetization.try_interstitial(true, false, 5, 9999), "active play blocked with available SDK")
	check(not monetization.try_interstitial(false, true, 5, 9999), "tutorial blocked with available SDK")
	check(not monetization.try_interstitial(false, false, 1, 9999), "first session blocked with available SDK")
	check(not monetization.try_interstitial(false, false, 5, 60), "short sessions blocked")
	check(monetization.try_interstitial(false, false, 5, 300), "eligible post-game slot")
	check(not monetization.try_interstitial(false, false, 5, 400), "ad spacing enforced")
	monetization.verified_pro = true
	check(not monetization.try_interstitial(false, false, 5, 900), "PRO blocks regular ads")
	var outcomes: Array = []
	monetization.purchases.purchase_resolved.connect(func(status: String, entitled: bool) -> void: outcomes.append([status, entitled]))
	monetization.purchases.purchase_pro()
	monetization.purchases.restore()
	check(outcomes == [["unavailable", false], ["unavailable", false]], "purchase and restore never fabricate success")

func stress() -> void:
	var turns: int = 0
	for seed_value in 80:
		var game: GameSession = GameSession.new("shift" if seed_value % 2 else "classic", seed_value + 500)
		for turn in 180:
			if game.over: break
			var moved: bool = false
			for slot in 3:
				if game.pieces[slot].is_empty(): continue
				var positions: Array[Vector2i] = game.board.positions(game.pieces[slot].cells)
				if not positions.is_empty():
					var result: Dictionary = game.place(slot, positions[(seed_value + turn) % positions.size()])
					check(result.ok, "stress legal move")
					turns += 1
					moved = true
					break
			check(moved or game.over, "stress no phantom game over")
			check(game.board.full_lines().rows.is_empty() and game.board.full_lines().columns.is_empty(), "resolved state contains no full lines")
			check(game.board.cells.size() == 64, "board invariant")
			if game.shift.ready() and not game.over and game.mode == "shift":
				game.move_line("row", turn % 8, 1)
			if turn % 7 == 0:
				var copy: GameSession = GameSession.from_snapshot(game.snapshot())
				check(copy != null and copy.snapshot() == game.snapshot(), "stress resume deterministic")
	print("Stress: 80 seeded games, %d placements" % turns)

func reset_transactions() -> void:
	var path: String = "user://qa-reset-transactions.json"
	for suffix in ["", ".tmp", ".bak", ".reset"]: DirAccess.remove_absolute(path + suffix)
	var save: SaveManager = SaveManager.new(path)
	var game: GameSession = GameSession.new("shift", 912)
	save.start(game)
	game.score.total = 123
	save.checkpoint(game)
	var previous: Dictionary = save.data.duplicate(true)
	var main_text: String = FileAccess.get_file_as_string(path)
	var backup_text: String = FileAccess.get_file_as_string(path + ".bak")
	save.read_only = true
	check(not save.reset_progress({"ambience": 0.6}), "read-only reset explicitly fails")
	check(save.data == previous and FileAccess.get_file_as_string(path) == main_text and FileAccess.get_file_as_string(path + ".bak") == backup_text, "failed reset keeps in-memory progress, main and backup byte-for-byte")
	save.read_only = false
	var blocked: String = path + "-directory"
	DirAccess.make_dir_absolute(blocked)
	save.path = blocked
	check(not save.reset_progress({}), "reset commit failure is reported")
	check(save.data == previous and not FileAccess.file_exists(blocked + ".reset"), "failed commit rolls back memory and discards reset stage")
	save.path = path
	DirAccess.remove_absolute(blocked)
	check(save.reset_progress({"ambience": 0.6}), "verified reset commits")
	var reloaded: SaveManager = SaveManager.new(path)
	check(reloaded.data.runs.is_empty() and reloaded.stats("shift").games == 0 and reloaded.data.settings.ambience == 0.6, "successful reset survives reload and preserves settings")
	check(not FileAccess.file_exists(path + ".bak") and not FileAccess.file_exists(path + ".tmp"), "old recovery copies are removed only after successful reset")
	save.start(GameSession.new("classic", 32))
	check(save.stats("classic").games == 1 and save.stats("shift").games == 0, "new game starts with clean statistics after reset")
	game = blocked_game(1)
	check(not game.rescue_options.is_empty(), "manual finish fixture has a real rescue")
	game.finish()
	check(game.over and game.end_reason == "manual", "voluntary finish has a distinct reason")
	var restored: GameSession = GameSession.from_snapshot(game.snapshot())
	check(restored.over and restored.end_reason == "manual", "voluntary finish reason survives save/reload")
	for suffix in ["", ".tmp", ".bak", ".reset"]: DirAccess.remove_absolute(path + suffix)

func blocked_game(piece_id: int, mode: String = "shift") -> GameSession:
	var game: GameSession = GameSession.new(mode, 912)
	for y in 8:
		for x in 8: game.board.cells[y * 8 + x] = 1 if (x + y) % 2 == 0 else 0
	game.pieces = [game.generator.make_piece(piece_id), {}, {}]
	game.shift.energy = game.shift.capacity()
	game.update_status()
	return game

func shift_rescues() -> void:
	var game: GameSession = blocked_game(1)
	check(not game.board.has_move(game.pieces) and not game.over and not game.rescue_options.is_empty(), "charged SHIFT keeps a rescuable blocked board alive")
	var before: Dictionary = game.snapshot()
	var preview: Dictionary = game.preview_shift("column", 0, 1)
	check(preview.ok and preview.can_place and preview.can_continue, "rescue preview finds space without requiring a line clear")
	check(game.snapshot() == before, "preview preserves board, charge, score, RNG and moves")
	check(not game.move_line("row", 0, 1).ok and game.snapshot() == before, "unhelpful move on blocked board preserves charge and run")
	var encoded: Dictionary = JSON.parse_string(JSON.stringify(before))
	var copy: GameSession = GameSession.from_snapshot(encoded)
	check(copy != null and not copy.over and not copy.rescue_options.is_empty(), "blocked charged rescue survives JSON save and restore")
	check(game.move_line("column", 0, 1).ok and game.board.has_move(game.pieces) and not game.over and game.shift.energy == 0, "successful rescue spends exactly one charge and allows placement")
	var position: Vector2i = game.board.positions(game.pieces[0].cells)[0]
	check(game.place(0, position).ok, "piece can actually be placed after rescue")
	game = blocked_game(1)
	game.shift.energy = game.shift.capacity() - 1
	game.update_status()
	check(game.over and game.rescue_options.is_empty(), "insufficient charge cannot postpone game over")
	game = blocked_game(19)
	check(game.over and game.rescue_options.is_empty(), "charged board with no rescuing move ends normally")
	game = blocked_game(1, "classic")
	check(game.over, "Classic game over is unchanged by stored energy")
	game = blocked_game(2)
	check(game.preview_shift("row", 0, -1).can_continue and not game.over, "row wrap can rescue a vertical piece")
	game = GameSession.new("shift", 3)
	game.board.cells.fill(1)
	for i in 8: game.board.cells[i * 8 + i] = 0
	game.pieces = [game.generator.make_piece(5), {}, {}]
	game.shift.energy = game.shift.capacity()
	game.update_status()
	check(not game.over and not game.board.has_move(game.pieces), "clearing rescue remains available on a crowded board")
	preview = game.preview_shift("row", 0, 1)
	check(preview.lines == 1 and preview.can_place and game.board.cells[0] == 0, "preview resolves full column and predicts resulting placement")
	var result: Dictionary = game.move_line("row", 0, 1)
	check(result.ok and result.lines == 1 and game.shift.energy == 1 and game.score.total == 100 and not game.over, "SHIFT clear rescue scores once and earns replacement energy")
	var daily: GameSession = GameSession.new("daily", 3, "2026-10-17")
	daily.board.cells.fill(1)
	for i in 8: daily.board.cells[i * 8 + i] = 0
	daily.pieces = [daily.generator.make_piece(19), {}, {}]
	daily.score.lines = DailyChallenge.target() - 1
	daily.shift.energy = daily.shift.capacity()
	daily.update_status()
	check(not daily.over, "a SHIFT that completes Daily is a rescue even when its remaining piece will not fit")
	check(daily.move_line("row", 0, 1).ok and daily.completed and daily.over, "rescue completes Daily immediately at its objective")
	game = blocked_game(1)
	var historical: Dictionary = game.snapshot()
	historical.rules_version = 1
	historical.over = true
	check(GameSession.from_snapshot(historical).over, "rule migration never revives a finished historical run")
	for axis in ["row", "column"]:
		for index in 8:
			for direction in [-1, 1]:
				game.preview_shift(axis, index, direction)
	check(game.snapshot() == historical.merged({"rules_version": 2, "over": false}, true), "all previews remain deterministic and leave saved state unchanged")

	game = GameSession.new("shift", 6)
	game.board.cells.fill(1)
	for i in 8: game.board.cells[i * 8 + i] = 0
	for y in 3:
		for x in 3: game.board.cells[y * 8 + x] = 0
	game.pieces = [game.generator.make_piece(19), {}, {}]
	game.shift.energy = game.shift.capacity()
	game.update_status()
	preview = game.preview_shift("row", 1, 1)
	check(not game.over and preview.ok and not preview.can_continue and game.board.has_move(game.pieces), "preview detects a losing SHIFT before a playable board changes")
	check(game.move_line("row", 1, 1).ok and game.over, "a deliberately applied losing SHIFT ends normally after spending the charge")

func mode_saves() -> void:
	var path: String = "user://qa-mode-saves.json"
	for suffix in ["", ".tmp", ".bak"]: DirAccess.remove_absolute(path + suffix)
	var old_game: GameSession = GameSession.new("classic", 818)
	old_game.place(0, old_game.board.positions(old_game.pieces[0].cells)[0])
	var original: Dictionary = old_game.snapshot()
	original.rules_version = 1
	var legacy: Dictionary = {"active": original, "settings": {"ambience": 0.6}, "stats": {"classic": {"best": 321, "lines": old_game.score.lines, "playtime": old_game.elapsed}}, "daily": {}}
	var payload: String = JSON.stringify(legacy)
	var file: FileAccess = FileAccess.open(path, FileAccess.WRITE)
	file.store_string(JSON.stringify({"version": 2, "sequence": 10, "payload": payload, "sha256": payload.sha256_text()}))
	file.close()
	var save: SaveManager = SaveManager.new(path)
	check(save.saved("classic") == JSON.parse_string(JSON.stringify(original)) and save.data.last_mode == "classic", "version 2 single run migrates into the correct mode slot")
	check(save.data.settings.ambience == 0.6 and save.stats("classic").best == 321, "save migration preserves preferences and scores")
	check(save.write(), "migrated data commits as version 3")
	var env: Dictionary = JSON.parse_string(FileAccess.get_file_as_string(path))
	check(env.version == 3 and not JSON.parse_string(env.payload).has("active"), "version 3 writes canonical per-mode saves without a duplicate active run")
	var snapshots: Dictionary = {"classic": save.saved("classic").duplicate(true)}
	for mode in ["shift", "zen", "daily"]:
		var game: GameSession = GameSession.new(mode, 20, "2026-10-16")
		save.start(game)
		game.place(0, game.board.positions(game.pieces[0].cells)[0])
		game.elapsed = 40
		save.checkpoint(game)
		snapshots[mode] = save.saved(mode).duplicate(true)
	var loaded: SaveManager = SaveManager.new(path)
	for mode in GameModes.IDS:
		check(loaded.saved(mode) == JSON.parse_string(JSON.stringify(snapshots[mode])), "switching modes preserves exact saved " + mode + " board")
		var game: GameSession = GameSession.from_snapshot(loaded.saved(mode))
		var lines: int = int(loaded.stats(mode).lines)
		var elapsed: float = float(loaded.stats(mode).playtime)
		loaded.checkpoint(game)
		loaded.checkpoint(game)
		check(loaded.stats(mode).lines == lines and loaded.stats(mode).playtime == elapsed, "resuming " + mode + " does not double-count playtime or lines")
	var zen_before: Dictionary = loaded.saved("zen").duplicate(true)
	var daily_before: Dictionary = loaded.saved("daily").duplicate(true)
	loaded.start(GameSession.new("classic", 99))
	check(loaded.saved("zen") == zen_before and loaded.saved("daily") == daily_before, "new Classic run replaces only Classic")
	check(loaded.saved("daily").date == "2026-10-16", "switching modes preserves an interrupted Daily's original date")
	loaded.clear_run("shift")
	check(loaded.saved("shift").is_empty() and loaded.saved("zen") == zen_before, "discarding one damaged mode cannot remove another mode")
	loaded = SaveManager.new(path)
	check(loaded.saved("zen") == JSON.parse_string(JSON.stringify(zen_before)) and loaded.saved("daily") == JSON.parse_string(JSON.stringify(daily_before)), "separate boards survive a second disk reload")
	for suffix in ["", ".tmp", ".bak"]: DirAccess.remove_absolute(path + suffix)
