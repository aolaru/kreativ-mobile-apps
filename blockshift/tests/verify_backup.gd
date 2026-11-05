extends SceneTree
## Verify a supplied backup using a disposable copy; never write the original.
func canonical(value: Variant) -> Variant:
	return JSON.parse_string(JSON.stringify(value))

func _initialize() -> void:
	var args: PackedStringArray = OS.get_cmdline_user_args()
	if args.size() != 1 or not FileAccess.file_exists(args[0]):
		printerr("Supply one saved progress envelope after --.")
		quit(2)
		return
	var original: String = FileAccess.get_file_as_string(args[0])
	var envelope: Variant = JSON.parse_string(original)
	if not envelope is Dictionary or not envelope.get("payload") is String or str(envelope.get("sha256", "")) != envelope.payload.sha256_text():
		printerr("Backup envelope checksum failed.")
		quit(1)
		return
	var payload: Dictionary = JSON.parse_string(envelope.payload)
	var temporary: String = "user://qa-backup-check-%s.json" % OS.get_process_id()
	var file: FileAccess = FileAccess.open(temporary, FileAccess.WRITE)
	file.store_string(original)
	file.close()
	var restored: SaveManager = SaveManager.new(temporary)
	var ok: bool = canonical(restored.data) == canonical(payload)
	for mode in payload.get("runs", {}):
		var saved: Dictionary = restored.saved(mode)
		var session: GameSession = GameSession.from_snapshot(saved)
		ok = ok and session != null
		if session != null:
			var snapshot: Dictionary = session.snapshot()
			for key in ["mode", "date", "board", "pieces", "score", "energy", "rng_state", "elapsed", "moves", "zen_resets", "over", "completed"]:
				ok = ok and canonical(snapshot[key]) == canonical(saved[key])
	ok = ok and restored.write()
	var reloaded: SaveManager = SaveManager.new(temporary)
	ok = ok and canonical(reloaded.data) == canonical(restored.data)
	var unchanged: bool = FileAccess.get_file_as_string(args[0]) == original
	ok = ok and unchanged
	for suffix in ["", ".tmp", ".bak"]: DirAccess.remove_absolute(temporary + suffix)
	print(JSON.stringify({"backup_version": envelope.version, "mode_count": payload.get("runs", {}).size(), "input_unmodified": unchanged, "save_and_session_roundtrip_passed": ok}, "  "))
	quit(0 if ok else 1)
