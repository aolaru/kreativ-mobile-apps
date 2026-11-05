extends SceneTree
## Convert a user-authorized private backup and verify restore via real services.
func _initialize() -> void:
	var args: PackedStringArray = OS.get_cmdline_user_args()
	if args.size() != 2:
		printerr("Supply raw save input and private portable backup output after --.")
		quit(2)
		return
	var raw: String = FileAccess.get_file_as_string(args[0])
	var envelope: Variant = JSON.parse_string(raw)
	if not envelope is Dictionary or not envelope.get("payload") is String or envelope.get("sha256") != envelope.payload.sha256_text():
		quit(1)
		return
	var store: SaveManager = SaveManager.new(args[0])
	var ok: bool = not store.read_only and SaveTransfer.export_file(args[1], store.data)
	var decoded: Dictionary = SaveTransfer.inspect_file(args[1])
	var temporary: String = "user://qa-real-transfer-%s.json" % OS.get_process_id()
	var destination: SaveManager = SaveManager.new(temporary)
	ok = ok and decoded.get("ok", false) and destination.import_progress(decoded.get("data", {}))
	var restarted: SaveManager = SaveManager.new(temporary)
	ok = ok and JSON.parse_string(JSON.stringify(store.data)) == JSON.parse_string(JSON.stringify(restarted.data))
	for mode in store.data.runs:
		var original: GameSession = GameSession.from_snapshot(store.saved(mode))
		var restored: GameSession = GameSession.from_snapshot(restarted.saved(mode))
		ok = ok and original != null and restored != null
		if original != null and restored != null:
			ok = ok and JSON.stringify(original.snapshot()) == JSON.stringify(restored.snapshot())
			ok = ok and JSON.stringify(original.generator.hand(original.board)) == JSON.stringify(restored.generator.hand(restored.board))
	ok = ok and FileAccess.get_file_as_string(args[0]) == raw
	for suffix in ["", ".tmp", ".bak", ".before-import.blockshift"]:
		DirAccess.remove_absolute(temporary + suffix)
	print(JSON.stringify({"portable_backup_verified": ok, "saved_modes": store.data.runs.size(), "original_unchanged": FileAccess.get_file_as_string(args[0]) == raw}, "  "))
	quit(0 if ok else 1)
