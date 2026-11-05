class_name SaveTransfer
extends RefCounted
## Portable local backups. No accounts, networking, or executable content.
const FORMAT: String = "com.madebykreativ.blockshift.backup"
const VERSION: int = 1
const MAX_BYTES: int = 1048576

static func encode(data: Dictionary) -> String:
	var payload: String = JSON.stringify(data)
	return JSON.stringify({"format": FORMAT, "version": VERSION, "save_version": SaveManager.VERSION,
		"created": Time.get_datetime_string_from_system(true), "payload": payload, "sha256": payload.sha256_text()})

static func inspect_file(path: String) -> Dictionary:
	var file: FileAccess = FileAccess.open(path, FileAccess.READ)
	if file == null:
		return {"ok": false, "error": "This file could not be opened. Choose the backup again."}
	if file.get_length() > MAX_BYTES:
		return {"ok": false, "error": "This file is too large to be a BLOCKSHIFT backup."}
	# SAF providers can return a stream whose reported size is zero or whose
	# reads are partial. Read to EOF with a hard cap rather than sizing a text
	# read from provider metadata. This also avoids trusting an external size.
	var bytes: PackedByteArray = PackedByteArray()
	while bytes.size() <= MAX_BYTES:
		var chunk: PackedByteArray = file.get_buffer(mini(8192, MAX_BYTES + 1 - bytes.size()))
		if chunk.is_empty(): break
		bytes.append_array(chunk)
	file.close()
	if bytes.size() > MAX_BYTES:
		return {"ok": false, "error": "This file is too large to be a BLOCKSHIFT backup."}
	return decode(bytes.get_string_from_utf8())

static func decode(text: String) -> Dictionary:
	if text.to_utf8_buffer().size() > MAX_BYTES:
		return {"ok": false, "error": "This backup is too large."}
	var parser: JSON = JSON.new()
	if parser.parse(text) != OK:
		return {"ok": false, "error": "This backup is incomplete or unreadable. Your progress is unchanged."}
	var envelope: Variant = parser.data
	if not envelope is Dictionary or envelope.get("format") != FORMAT:
		return {"ok": false, "error": "Choose a BLOCKSHIFT progress backup (.blockshift)."}
	if envelope.get("version") != VERSION or envelope.get("save_version") != SaveManager.VERSION:
		return {"ok": false, "error": "This backup needs a different game version. Update BLOCKSHIFT first."}
	if not envelope.get("payload") is String or envelope.get("sha256") != envelope.payload.sha256_text():
		return {"ok": false, "error": "Backup verification failed. Your current progress is unchanged."}
	if parser.parse(envelope.payload) != OK:
		return {"ok": false, "error": "This backup's progress is unreadable. Your progress is unchanged."}
	var data: Variant = parser.data
	if not valid_data(data):
		return {"ok": false, "error": "This backup contains invalid progress. Your current progress is unchanged."}
	return {"ok": true, "data": data, "created": str(envelope.get("created", "Unknown date")), "sha256": envelope.sha256}

static func _counter(value: Variant, integer: bool = true) -> bool:
	return (value is int or value is float) and is_finite(float(value)) and value >= 0 and value <= 9007199254740991 and (not integer or int(value) == value)

static func valid_data(value: Variant) -> bool:
	if not value is Dictionary or value.get("last_mode") not in GameModes.IDS:
		return false
	for key in ["settings", "stats", "daily", "runs"]:
		if not value.get(key) is Dictionary: return false
	if value.daily.size() > 10000 or value.runs.size() > GameModes.IDS.size(): return false
	for mode in value.runs:
		if mode not in GameModes.IDS or not value.runs[mode] is Dictionary: return false
		var run: Dictionary = value.runs[mode]
		if run.get("mode") != mode or not run.get("score") is Dictionary: return false
		for key in ["total", "streak", "best_combo", "lines"]:
			if not _counter(run.score.get(key)): return false
		for key in ["energy", "moves", "zen_resets"]:
			if not _counter(run.get(key)): return false
		if not _counter(run.get("elapsed"), false): return false
		if not run.get("over") is bool or not run.get("completed") is bool: return false
		if GameSession.from_snapshot(run) == null: return false
	for mode in value.stats:
		if mode not in GameModes.IDS or not value.stats[mode] is Dictionary: return false
		for key in SaveManager.empty_stats():
			if not _counter(value.stats[mode].get(key), key != "playtime"): return false
	for date in value.daily:
		if not date is String or date.length() != 10 or not value.daily[date] is Dictionary: return false
		var record: Dictionary = value.daily[date]
		if not record.get("completed") is bool: return false
		for key in ["best", "lines", "rules_version"]:
			if not _counter(record.get(key)): return false
	return true

static func export_file(path: String, data: Dictionary) -> bool:
	if not valid_data(data): return false
	var text: String = encode(data)
	if text.to_utf8_buffer().size() > MAX_BYTES: return false
	var file: FileAccess = FileAccess.open(path, FileAccess.WRITE)
	if file == null: return false
	file.store_string(text)
	file.flush()
	var ok: bool = file.get_error() == OK
	file.close()
	# Read the document back before declaring it safe to keep outside the app.
	var verified: Dictionary = inspect_file(path)
	return ok and verified.get("ok", false) and verified.sha256 == JSON.stringify(data).sha256_text()
