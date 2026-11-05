extends SceneTree
## Destructive cases use disposable user:// files, never the player's save.
var assertions: int = 0
var failures: int = 0
var path: String = "user://qa-transfer-%s.json" % OS.get_process_id()

func check(ok: bool, message: String) -> void:
	assertions += 1
	if not ok:
		failures += 1
		printerr("TRANSFER FAIL: " + message)

func canonical(value: Variant) -> Variant:
	return JSON.parse_string(JSON.stringify(value))

func _initialize() -> void:
	var store: SaveManager = SaveManager.new(path)
	var preferences: SettingsManager = SettingsManager.new()
	preferences.values.tutorial_done = true
	preferences.values.ambience = 0.48
	store.data.settings = preferences.values.duplicate(true)
	for mode in GameModes.IDS:
		var game: GameSession = GameSession.new(mode, 729, "2026-10-26")
		game.score.total = 120 + mode.length()
		game.elapsed = 45.25
		game.shift.energy = 2
		check(store.start(game), "checkpoint mode " + mode)
	store.data.daily["2026-10-25"] = {"completed": true, "best": 801, "lines": 8, "rules_version": 2}
	check(store.write(), "persist daily and settings")
	var original: Dictionary = canonical(store.data)
	var encoded: String = SaveTransfer.encode(store.data)
	var decoded: Dictionary = SaveTransfer.decode(encoded)
	check(decoded.ok and decoded.data == original, "all modes, settings, counters and history survive portable encoding")
	check(SaveTransfer.export_file(path + ".blockshift", store.data), "export is read back and verified")
	check(SaveTransfer.inspect_file(path + ".blockshift").data == original, "file round trip")
	var many_records: Dictionary = original.duplicate(true)
	many_records.settings["test_padding"] = "x".repeat(30000)
	check(SaveTransfer.export_file(path + ".chunks.blockshift", many_records), "bounded stream reader spans multiple chunks")
	check(SaveTransfer.inspect_file(path + ".chunks.blockshift").get("ok", false), "multi-chunk backup reads to EOF")
	for mode in GameModes.IDS:
		var a: GameSession = GameSession.from_snapshot(original.runs[mode])
		var b: GameSession = GameSession.from_snapshot(decoded.data.runs[mode])
		check(canonical(a.snapshot()) == canonical(b.snapshot()), "board and held pieces match " + mode)
		check(canonical(a.generator.hand(a.board)) == canonical(b.generator.hand(b.board)), "future RNG hand matches " + mode)
	var envelope: Dictionary = JSON.parse_string(encoded)
	for mutation in ["checksum", "format", "version", "save_version", "payload"]:
		var broken: Dictionary = envelope.duplicate(true)
		if mutation == "checksum": broken.sha256 = "bad"
		elif mutation == "format": broken.format = "another.game"
		elif mutation == "payload": broken.payload = "{}"
		else: broken[mutation] = 999
		check(not SaveTransfer.decode(JSON.stringify(broken)).ok, "reject " + mutation)
	check(not SaveTransfer.decode("{").ok, "reject truncated JSON")
	check(not SaveTransfer.decode("x".repeat(SaveTransfer.MAX_BYTES + 1)).ok, "reject oversized input")
	check(not SaveTransfer.inspect_file(path + ".missing").ok, "reject missing input")
	for mutation in ["board", "piece", "mode", "counter", "elapsed", "daily", "stats", "last_mode"]:
		var broken: Dictionary = original.duplicate(true)
		match mutation:
			"board": broken.runs.shift.board[0] = 8
			"piece": broken.runs.shift.pieces[0] = {"id": 999}
			"mode": broken.runs.shift.mode = "classic"
			"counter": broken.runs.shift.score.total = -1
			"elapsed": broken.runs.shift.elapsed = "invalid"
			"daily": broken.daily["2026-10-25"].lines = -1
			"stats": broken.stats.shift.best = "broken"
			"last_mode": broken.last_mode = "unknown"
		check(not SaveTransfer.decode(SaveTransfer.encode(broken)).ok, "reject structurally invalid " + mutation)
	var prior: Dictionary = original.duplicate(true)
	prior.runs.erase("classic")
	prior.stats.shift.best = 999
	prior = canonical(prior)
	store.data = prior.duplicate(true)
	check(store.write(), "prepare existing progress")
	check(store.import_progress(decoded.data), "restore transaction succeeds")
	check(canonical(store.data) == original, "restored memory exactly matches backup")
	var reloaded: SaveManager = SaveManager.new(path)
	check(canonical(reloaded.data) == original, "restored progress survives restart")
	var undo: Dictionary = SaveTransfer.inspect_file(path + ".before-import.blockshift")
	check(undo.ok and undo.data == prior, "previous progress survives in verified Undo file")
	check(reloaded.import_progress(undo.data), "undo restore succeeds")
	check(canonical(SaveManager.new(path).data) == prior, "undo survives restart")
	var before_failure: String = FileAccess.get_file_as_string(path)
	var bad_data: Dictionary = decoded.data.duplicate(true)
	bad_data.runs.shift.board = []
	check(not reloaded.import_progress(bad_data), "invalid restore is refused")
	check(FileAccess.get_file_as_string(path) == before_failure and canonical(reloaded.data) == prior, "invalid restore leaves disk and memory unchanged")
	DirAccess.make_dir_absolute(path + ".import")
	check(not reloaded.import_progress(decoded.data), "unwritable stage is refused")
	check(FileAccess.get_file_as_string(path) == before_failure and canonical(reloaded.data) == prior, "storage failure leaves current progress unchanged")
	DirAccess.remove_absolute(path + ".import")
	reloaded.read_only = true
	check(not reloaded.import_progress(decoded.data), "future-version read-only state cannot be overwritten")
	reloaded.read_only = false
	var stage: FileAccess = FileAccess.open(path + ".import", FileAccess.WRITE)
	stage.store_string("interrupted restore")
	stage.close()
	check(canonical(SaveManager.new(path).data) == prior, "interrupted import is never selected on startup")
	check(reloaded.reset_progress(preferences.values), "explicit reset succeeds")
	check(not FileAccess.file_exists(path + ".before-import.blockshift") and not FileAccess.file_exists(path + ".import"), "explicit privacy reset removes transfer recovery records")
	for suffix in ["", ".tmp", ".bak", ".import", ".before-import.blockshift", ".blockshift", ".chunks.blockshift"]:
		DirAccess.remove_absolute(path + suffix)
	print("TRANSFER: %d assertions; %d failures" % [assertions, failures])
	quit(0 if failures == 0 else 1)
