class_name VisualConfig
extends RefCounted
## Presentation tuning never changes daily seeds or board/scoring rules.
static var values: Dictionary = {}

static func value(key: String) -> Variant:
	if values.is_empty():
		values = JSON.parse_string(FileAccess.get_file_as_string("res://config/visuals.json"))
	return values[key]
