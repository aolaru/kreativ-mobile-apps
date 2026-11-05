class_name Rules
extends RefCounted

static var values: Dictionary = {}

static func config() -> Dictionary:
	if values.is_empty():
		values = JSON.parse_string(FileAccess.get_file_as_string("res://config/rules.json"))
	return values

static func value(key: String) -> Variant:
	return config()[key]
