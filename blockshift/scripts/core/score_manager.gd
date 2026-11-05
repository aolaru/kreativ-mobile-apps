class_name ScoreManager
extends RefCounted
var total: int = 0
var streak: int = 0
var best_combo: int = 0
var lines: int = 0

func award(placed: int, cleared: int) -> int:
	var points: int = placed * int(Rules.value("cell_points"))
	if cleared > 0:
		streak += 1
		lines += cleared
		best_combo = maxi(best_combo, streak)
		var table: Array = Rules.value("line_points")
		points += int(table[mini(cleared, table.size() - 1)])
		points += maxi(0, cleared - table.size() + 1) * int(Rules.value("extra_line_points"))
		points += (streak - 1) * cleared * int(Rules.value("streak_bonus"))
	else:
		streak = 0
	total += points
	return points

func data() -> Dictionary:
	return {"total": total, "streak": streak, "best_combo": best_combo, "lines": lines}

static func counter(data_value: Dictionary, key: String) -> int:
	var value: Variant = data_value.get(key, 0)
	if not (value is int or value is float) or not is_finite(float(value)):
		return 0
	return maxi(0, int(value))

func restore(data_value: Dictionary) -> void:
	total = counter(data_value, "total")
	streak = counter(data_value, "streak")
	best_combo = maxi(streak, counter(data_value, "best_combo"))
	lines = counter(data_value, "lines")
