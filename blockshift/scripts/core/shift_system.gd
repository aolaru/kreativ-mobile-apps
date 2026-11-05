class_name ShiftSystem
extends RefCounted
var energy: int = 0

func capacity() -> int:
	return int(Rules.value("shift_capacity"))

func ready() -> bool:
	return energy >= capacity()

func lines_remaining() -> int:
	return ceili(float(maxi(0, capacity() - energy)) / maxi(1, int(Rules.value("shift_per_line"))))

func earn(lines: int) -> void:
	energy = mini(capacity(), energy + lines * int(Rules.value("shift_per_line")))

static func preview(board: Board, pieces: Array, axis: String, index: int, direction: int) -> Dictionary:
	var copy: Board = Board.new()
	copy.cells = board.cells.duplicate()
	if not copy.shift(axis, index, direction):
		return {"ok": false}
	var shifted: Array[int] = copy.cells.duplicate()
	var clear: Dictionary = copy.clear_lines()
	return {"ok": true, "axis": axis, "index": index, "direction": direction,
		"shifted": shifted, "after": copy.cells.duplicate(), "cleared": clear.cells,
		"lines": clear.count, "can_place": copy.has_move(pieces)}

func apply(board: Board, axis: String, index: int, direction: int, active: bool) -> bool:
	if not active or not ready():
		return false
	if not board.shift(axis, index, direction):
		return false
	energy = 0
	return true
