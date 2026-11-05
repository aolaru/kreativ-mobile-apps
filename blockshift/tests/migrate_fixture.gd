extends SceneTree
## Optional read-only check against a supplied legacy envelope; uses a disposable copy.
func canonical(value: Variant) -> Variant:
	return JSON.parse_string(JSON.stringify(value))

func _initialize() -> void:
	var args: PackedStringArray = OS.get_cmdline_user_args()
	if args.size() != 1 or not FileAccess.file_exists(args[0]):
		printerr("Supply a legacy save envelope after --. The original is never modified.")
		quit(2)
		return
	var original_text: String = FileAccess.get_file_as_string(args[0])
	var envelope: Dictionary = JSON.parse_string(original_text)
	var old: Dictionary = JSON.parse_string(envelope.payload) if envelope.has("payload") else envelope.data
	var path: String = "user://qa-real-migration-v4.json"
	for suffix in ["", ".tmp", ".bak"]: DirAccess.remove_absolute(path + suffix)
	var file: FileAccess = FileAccess.open(path, FileAccess.WRITE)
	file.store_string(original_text)
	file.close()
	var migrated: SaveManager = SaveManager.new(path)
	var ok: bool = true
	for section in ["settings", "stats", "daily"]:
		ok = ok and canonical(migrated.data[section]) == canonical(old[section])
	var mode: String = str(old.active.mode)
	ok = ok and canonical(migrated.saved(mode)) == canonical(old.active) and migrated.data.last_mode == mode
	ok = ok and migrated.write()
	var reloaded: SaveManager = SaveManager.new(path)
	ok = ok and canonical(reloaded.data) == canonical(migrated.data)
	var session: GameSession = GameSession.from_snapshot(reloaded.saved(mode))
	ok = ok and session != null
	if session != null:
		var snapshot: Dictionary = session.snapshot()
		for key in ["mode", "date", "board", "pieces", "score", "energy", "rng_state", "elapsed", "moves", "zen_resets", "over", "completed"]:
			ok = ok and canonical(snapshot[key]) == canonical(old.active[key])
	ok = ok and FileAccess.get_file_as_string(args[0]) == original_text
	for suffix in ["", ".tmp", ".bak"]: DirAccess.remove_absolute(path + suffix)
	print(JSON.stringify({"legacy_version": envelope.version, "new_version": SaveManager.VERSION, "mode": mode, "input_unmodified": FileAccess.get_file_as_string(args[0]) == original_text, "migration_and_session_roundtrip_passed": ok}, "  "))
	quit(0 if ok else 1)
