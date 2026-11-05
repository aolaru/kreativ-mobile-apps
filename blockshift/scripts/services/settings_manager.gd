class_name SettingsManager
extends RefCounted
const DRAG_MIN: float = 56.0
const DRAG_MAX: float = 120.0
const DEFAULTS: Dictionary = {"sfx": 0.75, "ambience": 0.35, "particles": true, "reduce_motion": false, "haptics": true, "text_scale": 1.0, "theme": "orbit", "tutorial_done": false, "drag_lift": 88.0, "battery_saver": false}
var values: Dictionary = DEFAULTS.duplicate(true)

func game_fps() -> int:
	return 30 if bool(values.battery_saver) else 60

func reset_preferences() -> void:
	var learned: bool = bool(values.tutorial_done)
	values = DEFAULTS.duplicate(true)
	values.tutorial_done = learned

func restore(data: Variant) -> void:
	values = DEFAULTS.duplicate(true)
	if not data is Dictionary:
		return
	for key in DEFAULTS:
		if not data.has(key):
			continue
		if DEFAULTS[key] is bool and data[key] is bool:
			values[key] = data[key]
		elif DEFAULTS[key] is float and (data[key] is float or data[key] is int):
			var number: float = float(data[key])
			if not is_finite(number):
				continue
			if key == "drag_lift":
				values[key] = clampf(number, DRAG_MIN, DRAG_MAX)
			else:
				values[key] = clampf(number, 1.0 if key == "text_scale" else 0.0, 1.3 if key == "text_scale" else 1.0)
		elif key == "theme" and data[key] in ["orbit", "dusk", "lagoon"]:
			values[key] = data[key]
