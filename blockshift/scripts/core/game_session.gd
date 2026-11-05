class_name GameSession
extends RefCounted
## Sole gameplay transaction boundary. Rendering never owns or delays state changes.
var board: Board = Board.new()
var generator: PieceGenerator
var score: ScoreManager = ScoreManager.new()
var shift: ShiftSystem = ShiftSystem.new()
var mode: String = "classic"
var pieces: Array = []
var over: bool = false
var completed: bool = false
var date: String = ""
var elapsed: float = 0.0
var moves: int = 0
var zen_resets: int = 0
var recorded: bool = false
var end_reason: String = ""
var rescue_options: Array[Dictionary] = []

func _init(mode_id: String = "classic", seed_value: int = 1, daily_date: String = "") -> void:
	mode = mode_id if mode_id in GameModes.IDS else "classic"
	date = daily_date if not daily_date.is_empty() else DailyChallenge.today()
	generator = PieceGenerator.new(DailyChallenge.seed_for(date) if mode == "daily" else seed_value)
	pieces = generator.hand(board)

func place(slot: int, origin: Vector2i) -> Dictionary:
	if over or slot < 0 or slot >= pieces.size() or pieces[slot].is_empty():
		return {"ok": false}
	var piece: Dictionary = pieces[slot]
	if not board.place(piece.cells, origin, int(piece.color)):
		return {"ok": false}
	pieces[slot] = {}
	moves += 1
	return _resolve(piece.cells.size(), false)

func move_line(axis: String, index: int, direction: int) -> Dictionary:
	if not board.has_move(pieces) and not preview_shift(axis, index, direction).get("can_continue", false):
		return {"ok": false, "reason": "no_rescue"}
	var before: Array[int] = board.cells.duplicate()
	if not bool(GameModes.config(mode).shift) or not shift.apply(board, axis, index, direction, not over):
		return {"ok": false}
	moves += 1
	var result: Dictionary = _resolve(0, true)
	result.motion = {"before": before, "axis": axis, "index": index, "direction": direction, "life": 1.0}
	return result

func _resolve(placed: int, shifted: bool) -> Dictionary:
	var clear: Dictionary = board.clear_lines()
	var points: int = score.award(placed, int(clear.count))
	if bool(GameModes.config(mode).shift):
		shift.earn(int(clear.count))
	var empty: bool = true
	for piece in pieces:
		empty = empty and piece.is_empty()
	if empty:
		pieces = generator.hand(board)
	update_status()
	var reset: bool = false
	if over and mode == "zen":
		board = Board.new()
		pieces = generator.hand(board)
		zen_resets += 1
		over = false
		end_reason = ""
		reset = true
	return {"ok": true, "cleared": clear.cells, "lines": clear.count, "points": points, "shifted": shifted, "zen_reset": reset}

func preview_shift(axis: String, index: int, direction: int) -> Dictionary:
	var result: Dictionary = ShiftSystem.preview(board, pieces, axis, index, direction)
	if not result.ok: return result
	var after: Board = Board.new()
	after.cells = result.after.duplicate()
	var earned: int = mini(shift.capacity(), int(result.lines) * int(Rules.value("shift_per_line")))
	var needed: int = DailyChallenge.target() - score.lines - int(result.lines) if mode == "daily" else 999
	result.can_continue = _has_continuation(after, earned, needed)
	return result

func _has_continuation(candidate: Board, energy: int, needed: int) -> bool:
	if candidate.has_move(pieces) or (mode == "daily" and needed <= 0): return true
	if not bool(GameModes.config(mode).shift) or energy < shift.capacity(): return false
	for axis in ["row", "column"]:
		for index in 8:
			for direction in [-1, 1]:
				var move: Dictionary = ShiftSystem.preview(candidate, pieces, axis, index, direction)
				if not move.ok: continue
				if move.can_place or (mode == "daily" and int(move.lines) >= needed): return true
				var earned: int = mini(shift.capacity(), int(move.lines) * int(Rules.value("shift_per_line")))
				# A further charged step requires clears, so occupied cells strictly decrease.
				if earned >= shift.capacity() and not move.cleared.is_empty():
					var after: Board = Board.new()
					after.cells = move.after.duplicate()
					if _has_continuation(after, earned, needed - int(move.lines)): return true
	return false

func update_status() -> void:
	completed = mode == "daily" and score.lines >= DailyChallenge.target()
	rescue_options.clear()
	var fits: bool = board.has_move(pieces)
	if not completed and not fits and bool(GameModes.config(mode).shift) and shift.ready():
		for axis in ["row", "column"]:
			for index in 8:
				for direction in [-1, 1]:
					if preview_shift(axis, index, direction).get("can_continue", false):
						rescue_options.append({"axis": axis, "index": index, "direction": direction})
	over = completed or (not fits and rescue_options.is_empty())
	end_reason = "daily_complete" if completed else ("no_moves" if over else "")

func finish() -> void:
	over = true
	end_reason = "manual"

func snapshot() -> Dictionary:
	return {"mode": mode, "date": date, "board": board.cells.duplicate(), "pieces": pieces.duplicate(true),
		"score": score.data(), "energy": shift.energy, "rng_state": str(generator.rng.state),
		"elapsed": elapsed, "moves": moves, "zen_resets": zen_resets, "over": over,
		"completed": completed, "recorded": recorded, "end_reason": end_reason, "rules_version": 2}

static func from_snapshot(data: Dictionary) -> GameSession:
	if (data.get("rules_version", 0) != 1 and data.get("rules_version", 0) != 2) or data.get("mode", "") not in GameModes.IDS:
		return null
	var session: GameSession = GameSession.new(data.mode, 1, str(data.get("date", "")))
	if not session.board.restore(data.get("board")):
		return null
	var saved_pieces: Variant = data.get("pieces")
	if not saved_pieces is Array or saved_pieces.size() != 3:
		return null
	var rebuilt: Array = []
	for piece in saved_pieces:
		if not piece is Dictionary:
			return null
		if piece.is_empty():
			rebuilt.append({})
		else:
			var raw_id: Variant = piece.get("id", -1)
			if not (raw_id is int or raw_id is float) or not is_finite(float(raw_id)):
				return null
			var id: int = int(raw_id)
			if id != raw_id:
				return null
			if id < 0 or id >= session.generator.catalog.size():
				return null
			rebuilt.append(session.generator.make_piece(id))
	if not data.get("score") is Dictionary:
		return null
	session.pieces = rebuilt
	session.score.restore(data.score)
	session.shift.energy = clampi(ScoreManager.counter(data, "energy"), 0, session.shift.capacity())
	var state: Variant = data.get("rng_state", "")
	if not state is String or not state.is_valid_int():
		return null
	session.generator.rng.state = int(state)
	var elapsed_value: Variant = data.get("elapsed", 0)
	if (elapsed_value is int or elapsed_value is float) and is_finite(float(elapsed_value)):
		session.elapsed = maxf(0, float(elapsed_value))
	session.moves = ScoreManager.counter(data, "moves")
	session.zen_resets = ScoreManager.counter(data, "zen_resets")
	session.update_status()
	# Completed historical runs stay completed when rules change.
	session.over = session.over or bool(data.get("over", false))
	if session.over and data.get("end_reason", "") in ["manual", "no_moves", "daily_complete"]:
		session.end_reason = data.end_reason
	session.recorded = bool(data.get("recorded", false))
	return session
