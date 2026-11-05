class_name GameModes
extends RefCounted
const IDS: Array[String] = ["classic", "shift", "zen", "daily"]

static func config(mode: String) -> Dictionary:
	return Rules.config().modes.get(mode, Rules.config().modes.classic)
