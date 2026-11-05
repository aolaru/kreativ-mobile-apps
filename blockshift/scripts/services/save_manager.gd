class_name SaveManager
extends RefCounted
## Checksummed, versioned envelope; highest valid sequence wins across main/tmp/bak.
const VERSION: int = 3
var path: String
var data: Dictionary
var sequence: int = 0
var read_only: bool = false
var recovered: bool = false
var last_error: String = ""

func _init(save_path: String = "user://progress.json") -> void:
	path = save_path
	data = {"settings": {}, "stats": {}, "daily": {}, "runs": {}, "last_mode": "shift"}
	load_progress()

func _read(candidate: String) -> Dictionary:
	if not FileAccess.file_exists(candidate):
		return {}
	var text: String = FileAccess.get_file_as_string(candidate)
	var parser: JSON = JSON.new()
	if parser.parse(text) != OK:
		return {}
	var envelope: Variant = parser.data
	if not envelope is Dictionary:
		return {}
	if not (envelope.get("version") is int or envelope.get("version") is float):
		return {}
	if int(envelope.get("version", 0)) > VERSION:
		read_only = true
		last_error = "Save belongs to a newer version. Progress writes are paused."
		return {}
	# Version 1 was an unsigned plain dictionary, migrated without losing scores.
	if int(envelope.get("version", 0)) == 1 and envelope.get("data") is Dictionary:
		return {"sequence": ScoreManager.counter(envelope, "sequence"), "data": envelope.data}
	if (envelope.get("version") != 2 and envelope.get("version") != VERSION) or not envelope.get("payload") is String:
		return {}
	if str(envelope.get("sha256", "")) != str(envelope.payload).sha256_text():
		return {}
	var payload_parser: JSON = JSON.new()
	if payload_parser.parse(envelope.payload) != OK:
		return {}
	var payload: Variant = payload_parser.data
	if not payload is Dictionary:
		return {}
	return {"sequence": ScoreManager.counter(envelope, "sequence"), "data": payload}

func load_progress() -> void:
	var best: Dictionary = {}
	for suffix in ["", ".tmp", ".bak"]:
		var candidate: Dictionary = _read(path + suffix)
		if not candidate.is_empty() and (best.is_empty() or candidate.sequence > best.sequence):
			best = candidate
			recovered = suffix != ""
	if best.is_empty():
		return
	sequence = int(best.sequence)
	for key in data:
		if best.data.get(key) is Dictionary:
			data[key] = best.data[key]
	var legacy: Variant = best.data.get("active", {})
	if legacy is Dictionary and legacy.get("mode", "") in GameModes.IDS and not data.runs.has(legacy.mode):
		data.runs[legacy.mode] = legacy.duplicate(true)
	var clean_runs: Dictionary = {}
	for mode in GameModes.IDS:
		if data.runs.get(mode) is Dictionary and data.runs[mode].get("mode") == mode:
			clean_runs[mode] = data.runs[mode]
	data.runs = clean_runs
	var last: Variant = best.data.get("last_mode", legacy.get("mode", "shift") if legacy is Dictionary else "shift")
	data.last_mode = last if last in GameModes.IDS else "shift"
	# Never expose malformed counters to the UI.
	for mode in GameModes.IDS:
		var stats_value: Variant = data.stats.get(mode)
		var clean: Dictionary = empty_stats()
		if stats_value is Dictionary:
			for key in clean:
				if stats_value.get(key) is int or stats_value.get(key) is float:
					clean[key] = maxf(0, float(stats_value[key])) if key == "playtime" else maxi(0, int(stats_value[key]))
		data.stats[mode] = clean
	var history: Dictionary = {}
	for date_value in data.daily:
		var record: Variant = data.daily[date_value]
		if date_value is String and date_value.length() == 10 and record is Dictionary:
			history[date_value] = {"completed": record.get("completed", false) == true, "best": ScoreManager.counter(record, "best"), "lines": ScoreManager.counter(record, "lines"), "rules_version": ScoreManager.counter(record, "rules_version")}
	data.daily = history

func saved(mode: String = "") -> Dictionary:
	var id: String = str(data.last_mode) if mode.is_empty() else mode
	return data.runs.get(id, {})

func clear_run(mode: String) -> void:
	data.runs.erase(mode)
	write()

static func empty_stats() -> Dictionary:
	return {"best": 0, "games": 0, "lines": 0, "combo": 0, "playtime": 0.0}

func stats(mode: String) -> Dictionary:
	if not data.stats.get(mode) is Dictionary:
		data.stats[mode] = empty_stats()
	return data.stats[mode]

func start(session: GameSession) -> bool:
	var mode_stats: Dictionary = stats(session.mode)
	mode_stats.games = int(mode_stats.games) + 1
	data.runs.erase(session.mode)
	return checkpoint(session)

func checkpoint(session: GameSession) -> bool:
	var previous: Dictionary = saved(session.mode)
	var previous_lines: int = 0
	var previous_elapsed: float = 0.0
	if previous.get("mode") == session.mode and previous.get("score") is Dictionary:
		previous_lines = ScoreManager.counter(previous.score, "lines")
		var previous_time: Variant = previous.get("elapsed", 0)
		if previous_time is int or previous_time is float: previous_elapsed = maxf(0, float(previous_time))
	var mode_stats: Dictionary = stats(session.mode)
	mode_stats.best = maxi(int(mode_stats.best), session.score.total)
	mode_stats.lines = int(mode_stats.lines) + maxi(0, session.score.lines - previous_lines)
	mode_stats.combo = maxi(int(mode_stats.combo), session.score.best_combo)
	mode_stats.playtime = float(mode_stats.playtime) + maxf(0, session.elapsed - previous_elapsed)
	data.runs[session.mode] = session.snapshot()
	data.last_mode = session.mode
	if session.mode == "daily" and session.over:
		var record: Variant = data.daily.get(session.date, {})
		if not record is Dictionary:
			record = {}
		data.daily[session.date] = {"completed": bool(record.get("completed", false)) or session.completed,
			"best": maxi(int(record.get("best", 0)), session.score.total), "lines": maxi(int(record.get("lines", 0)), session.score.lines), "rules_version": DailyChallenge.RULE_VERSION}
	return write()

func write() -> bool:
	if read_only:
		last_error = "Save belongs to a newer version. Update the app to save progress."
		return false
	var payload: String = JSON.stringify(data)
	var envelope: Dictionary = {"version": VERSION, "sequence": sequence + 1, "payload": payload, "sha256": payload.sha256_text()}
	var file: FileAccess = FileAccess.open(path + ".tmp", FileAccess.WRITE)
	if file == null:
		last_error = "Progress could not be saved. Check available storage."
		return false
	file.store_string(JSON.stringify(envelope))
	file.flush()
	file.close()
	if _read(path + ".tmp").is_empty():
		last_error = "Progress verification failed. Previous save is intact."
		return false
	# A recovered backup must survive if main is corrupt or absent.
	if not _read(path).is_empty():
		if FileAccess.file_exists(path + ".bak"):
			DirAccess.remove_absolute(path + ".bak")
		if DirAccess.rename_absolute(path, path + ".bak") != OK:
			last_error = "Could not rotate progress backup."
			return false
	elif FileAccess.file_exists(path):
		DirAccess.remove_absolute(path)
	if DirAccess.rename_absolute(path + ".tmp", path) != OK:
		last_error = "Could not commit progress. Recovery copy is available."
		return false
	sequence += 1
	last_error = ""
	return true

func reset_progress(settings: Dictionary) -> bool:
	# Stage the reset separately: a failed reset must not replace in-memory progress,
	# truncate an interrupted save, or remove a usable recovery copy.
	if read_only:
		last_error = "Save belongs to a newer version. Update the app before resetting progress."
		return false
	var cleared: Dictionary = {"settings": settings.duplicate(true), "stats": {}, "daily": {}, "runs": {}, "last_mode": "shift"}
	var payload: String = JSON.stringify(cleared)
	var envelope: Dictionary = {"version": VERSION, "sequence": sequence + 1, "payload": payload, "sha256": payload.sha256_text()}
	var stage: String = path + ".reset"
	var file: FileAccess = FileAccess.open(stage, FileAccess.WRITE)
	if file == null:
		last_error = "Progress was not reset. Check available storage and try again."
		return false
	file.store_string(JSON.stringify(envelope))
	file.flush()
	file.close()
	if _read(stage).is_empty() or DirAccess.rename_absolute(stage, path) != OK:
		DirAccess.remove_absolute(stage)
		last_error = "Progress was not reset. Your previous save and backups are kept."
		return false
	data = cleared
	sequence += 1
	recovered = false
	last_error = ""
	# Main is now the verified reset with a newer sequence than every old copy.
	for suffix in [".tmp", ".bak", ".import", ".before-import.blockshift"]:
		if FileAccess.file_exists(path + suffix) and DirAccess.remove_absolute(path + suffix) != OK:
			last_error = "Progress reset, but an old recovery file could not be removed. Check available storage."
	return true

func import_progress(imported: Dictionary) -> bool:
	# Stage independently of .tmp/.bak; even an interrupted import keeps the old
	# save. The previous progress remains available for one-tap undo after restart.
	if read_only or not SaveTransfer.valid_data(imported):
		last_error = "Backup was not restored. Your current progress is kept."
		return false
	var payload: String = JSON.stringify(imported)
	var stage: String = path + ".import"
	var file: FileAccess = FileAccess.open(stage, FileAccess.WRITE)
	if file == null:
		last_error = "Backup was not restored. Check available storage."
		return false
	file.store_string(JSON.stringify({"version": VERSION, "sequence": sequence + 1, "payload": payload, "sha256": payload.sha256_text()}))
	file.flush()
	file.close()
	if _read(stage).is_empty():
		last_error = "Backup was not restored. Verification failed."
		return false
	# Capture the actual in-memory state, including a recovered main/tmp/bak.
	if not SaveTransfer.export_file(path + ".before-import.blockshift", data):
		DirAccess.remove_absolute(stage)
		last_error = "Backup was not restored. Could not preserve current progress."
		return false
	if DirAccess.rename_absolute(stage, path) != OK:
		last_error = "Backup was not restored. Previous progress and recovery files are kept."
		return false
	data = imported.duplicate(true)
	sequence += 1
	recovered = false
	last_error = ""
	# Older recovery copies cannot win against the newer committed main.
	return true
