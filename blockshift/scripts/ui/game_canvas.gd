class_name GameCanvas
extends Control
## Unified pointer owner. A second finger never steals a drag. All positions are local.
signal piece_picked
signal placement_requested(slot: int, origin: Vector2i)
signal invalid_drop
signal line_selected(axis: String, index: int)
signal shift_swiped(axis: String, index: int, direction: int)
var session: GameSession
var preferences: SettingsManager
var colors: Array = []
var board_rect: Rect2
var cell: float = 48.0
var dragging: int = -1
var pointer_id: int = -99
var pointer: Vector2
var origin: Vector2i = Vector2i(-10, -10)
var valid: bool = false
var shift_armed: bool = false
var shift_axis: String = "row"
var selected_line: int = 3
var shift_preview: Dictionary = {}
var lesson_targets: Array[int] = []
var swipe_start: Vector2
var shift_pointer: bool = false
var effects: BoardEffects = BoardEffects.new()
var pending_landing: Dictionary = {}
var tray_rects: Array[Rect2] = []
var styles: Dictionary = {}
const TILE: Texture2D = preload("res://assets/graphics/block.svg")
const SHEEN: Texture2D = preload("res://assets/graphics/block_sheen.svg")

func _ready() -> void:
	mouse_filter = Control.MOUSE_FILTER_STOP
	resized.connect(_layout)
	_layout()
	set_process(false)

func _layout() -> void:
	var tray_height: float = clampf(size.y - 244.0, 96.0, 124.0)
	var side: float = minf(size.x - 12.0, size.y - tray_height - 44.0)
	side = maxf(200.0, side)
	cell = side / 8.0
	var top: float = maxf(6, (size.y - side - tray_height - 32) / 2)
	board_rect = Rect2(Vector2((size.x - side) / 2, top), Vector2.ONE * side)
	tray_rects.clear()
	for i in 3:
		tray_rects.append(Rect2(Vector2(i * size.x / 3.0, board_rect.end.y + 32), Vector2(size.x / 3.0, tray_height)))
	queue_redraw()

func _draw() -> void:
	if session == null: return
	colors = ThemeManager.palette(str(preferences.values.theme), session.mode == "zen")
	_draw_board()
	_draw_shift_preview()
	_draw_preview()
	_draw_trays()
	_draw_outgoing()
	_draw_motion()
	_draw_landing()
	_draw_particles_and_points()
	if dragging >= 0 and not session.pieces[dragging].is_empty():
		var piece: Dictionary = session.pieces[dragging]
		var start: Vector2 = drag_top_left(piece.cells)
		var lift: float = clampf(1.0 - effects.pickup_left / float(VisualConfig.value("pickup_seconds")), 0, 1)
		for offset in piece.cells:
			var rect: Rect2 = Rect2(start + Vector2(offset[0], offset[1]) * cell + Vector2.ONE * 3, Vector2.ONE * (cell - 6))
			draw_style_box(_box(Color(0, 0, 0, 0.16 + lift * 0.10), 10), Rect2(rect.position + Vector2(0, 5), rect.size).grow(3))
			draw_style_box(_box(Color(0.32, 0.87, 0.82, 0.06), 12), rect.grow(4))
			_block(rect, colors[int(piece.color) - 1])

func _box(color: Color, radius: int, border: Color = Color.TRANSPARENT) -> StyleBoxFlat:
	var key: String = color.to_html() + str(radius) + border.to_html()
	if not styles.has(key):
		if styles.size() >= 256: styles.clear()
		var style: StyleBoxFlat = ThemeManager.panel(color, radius, border)
		style.content_margin_left = 0
		style.content_margin_right = 0
		style.content_margin_top = 0
		style.content_margin_bottom = 0
		styles[key] = style
	return styles[key]

func _draw_board() -> void:
	var displayed: Array = shift_preview.shifted if shift_armed and bool(shift_preview.get("ok", false)) else session.board.cells
	var glow: float = 0.24 * maxf(0, 1.0 - effects.elapsed / maxf(0.001, effects.ring_seconds)) if effects.energy_gain else 0.0
	var ready: bool = bool(GameModes.config(session.mode).shift) and session.shift.ready()
	var accent: Color = Color("70dcd4") if ready or glow > 0 else Color("536792")
	for spread in [10, 8]:
		draw_style_box(_box(Color.TRANSPARENT, 25, Color(accent, 0.04 + glow * 0.15)), board_rect.grow(spread))
	draw_style_box(_box(Color(0, 0, 0, 0.18), 24), Rect2(board_rect.position + Vector2(0, 7), board_rect.size).grow(7))
	draw_style_box(_box(Color("0b1527"), 22, Color("273b56")), board_rect.grow(6))
	draw_line(board_rect.position + Vector2(22, -5), Vector2(board_rect.end.x - 22, board_rect.position.y - 5), Color(accent, 0.16 + glow), 1, true)
	for y in 8:
		for x in 8:
			var index: int = y * 8 + x
			var rect: Rect2 = cell_rect(Vector2(x, y))
			var moving: bool = not effects.motion.is_empty() and (y == int(effects.motion.index) if effects.motion.axis == "row" else x == int(effects.motion.index))
			var arriving: bool = not effects.landing.is_empty() and index in effects.landing.indices
			draw_style_box(_box(Color("152137"), int(rect.size.x * 0.172), Color("1c2b43")), rect)
			if index in lesson_targets:
				draw_style_box(_box(Color(0.48, 0.96, 0.87, 0.08), int(rect.size.x * 0.172), Color("eef3ff")), rect)
				draw_circle(rect.get_center(), 2.5, Color("eef3ff"))
			if displayed[index] > 0 and not moving and not arriving:
				_block(rect, colors[int(displayed[index]) - 1])
			else:
				draw_circle(rect.get_center(), 0.7, Color("263650"))
	if shift_armed:
		var selected: Rect2 = Rect2(board_rect.position + Vector2(0, selected_line * cell), Vector2(board_rect.size.x, cell)) if shift_axis == "row" else Rect2(board_rect.position + Vector2(selected_line * cell, 0), Vector2(cell, board_rect.size.y))
		draw_style_box(_box(Color(0.32, 0.87, 0.82, 0.035), 8, Color("7af5dd")), selected.grow(1))

func _draw_shift_preview() -> void:
	if not shift_armed or not bool(shift_preview.get("ok", false)): return
	var tint: Color = Color("7af5dd") if bool(shift_preview.get("allowed", true)) and bool(shift_preview.get("can_continue", true)) else Color("efba70")
	draw_string(get_theme_default_font(), board_rect.position + Vector2(4, -13), "PREVIEW · CHARGE KEPT UNTIL APPLY", HORIZONTAL_ALIGNMENT_LEFT, board_rect.size.x, 10, tint)
	for index in shift_preview.cleared:
		var rect: Rect2 = cell_rect(Vector2(int(index) % 8, int(int(index) / 8)))
		draw_style_box(_box(Color(0.8, 1, 0.95, 0.10), int(rect.size.x * 0.172), Color("eef3ff")), rect.grow(-1))
		# A white plus identifies cells that will clear without relying on color.
		var center: Vector2 = rect.get_center()
		draw_line(center - Vector2(4, 0), center + Vector2(4, 0), Color.WHITE, 2, true)
		draw_line(center - Vector2(0, 4), center + Vector2(0, 4), Color.WHITE, 2, true)
	var direction: int = int(shift_preview.direction)
	var vector: Vector2 = Vector2(direction, 0) if shift_axis == "row" else Vector2(0, direction)
	var center: Vector2 = board_rect.position + (Vector2(board_rect.size.x / 2, (selected_line + 0.5) * cell) if shift_axis == "row" else Vector2((selected_line + 0.5) * cell, board_rect.size.y / 2))
	draw_style_box(_box(Color("0b1527"), 8, tint), Rect2(center - Vector2(16, 15), Vector2(32, 30)))
	var tip: Vector2 = center + vector * 8
	var side: Vector2 = vector.rotated(PI / 2) * 5
	draw_line(center - vector * 8, tip, tint, 2, true)
	draw_line(tip, tip - vector * 6 + side, tint, 2, true)
	draw_line(tip, tip - vector * 6 - side, tint, 2, true)

func _draw_preview() -> void:
	if dragging < 0 or session.pieces[dragging].is_empty(): return
	var shape: Array = session.pieces[dragging].cells
	var tint: Color = Color("7af5dd") if valid else Color("ec8e9b")
	for offset in shape:
		var target: Vector2i = origin + Vector2i(offset[0], offset[1])
		if target.x >= 0 and target.x < 8 and target.y >= 0 and target.y < 8:
			var rect: Rect2 = cell_rect(Vector2(target))
			draw_style_box(_box(Color(tint, 0.22), int(rect.size.x * 0.172), tint), rect)
			if not valid:
				var inset: float = rect.size.x * 0.28
				draw_line(rect.position + Vector2.ONE * inset, rect.end - Vector2.ONE * inset, tint, 2, true)
				draw_line(rect.position + Vector2(inset, rect.size.y - inset), rect.position + Vector2(rect.size.x - inset, inset), tint, 2, true)
	if valid:
		var simulation: Board = Board.new()
		simulation.cells = session.board.cells.duplicate()
		simulation.place(shape, origin, 1)
		var lines: Dictionary = simulation.full_lines()
		for row in lines.rows:
			draw_style_box(_box(Color(tint, 0.035), 8, Color(tint, 0.7)), Rect2(board_rect.position + Vector2(0, row * cell), Vector2(cell * 8, cell)))
		for column in lines.columns:
			draw_style_box(_box(Color(tint, 0.035), 8, Color(tint, 0.7)), Rect2(board_rect.position + Vector2(column * cell, 0), Vector2(cell, cell * 8)))

func _draw_trays() -> void:
	var placement_board: Board = session.board
	var previewing: bool = shift_armed and bool(shift_preview.get("ok", false))
	if previewing:
		placement_board = Board.new()
		placement_board.cells = shift_preview.after.duplicate()
	var remaining: int = 0
	for piece in session.pieces:
		if not piece.is_empty(): remaining += 1
	draw_string(get_theme_default_font(), Vector2(8, tray_rects[0].position.y - 8), "%d PIECE%s LEFT  ·  USE ALL THREE FOR A NEW HAND" % [remaining, "" if remaining == 1 else "S"], HORIZONTAL_ALIGNMENT_LEFT, size.x - 16, 10, ThemeManager.MUTED)
	for i in 3:
		var rect: Rect2 = tray_rects[i]
		var border: Color = Color("263751")
		if i == dragging: border = Color("6ce5d5")
		if i == effects.reject_slot: border = Color("ec8e9b").lerp(border, 1.0 - effects.reject_left / 0.22)
		draw_style_box(_box(Color("0e192b"), 16, border), rect.grow(-5))
		if session.pieces[i].is_empty():
			draw_arc(rect.get_center(), 3, 0, TAU, 12, Color("293c56"), 1, true)
		elif i != dragging:
			var shape: Array = session.pieces[i].cells
			var extent: Vector2 = _extent(shape)
			var fits: bool = not placement_board.positions(shape).is_empty()
			var caption: String = "FITS AFTER SHIFT" if previewing and fits else ("NO SPACE" if not fits else "")
			var small: float = minf(25, minf((rect.size.x - 24) / extent.x, (rect.size.y - 24 - (14 if not caption.is_empty() else 0)) / extent.y))
			var reveal: float = effects.deal_progress(i)
			small *= 0.88 + 0.12 * (1.0 - pow(1.0 - reveal, 3))
			var start: Vector2 = rect.get_center() - extent * small / 2 - Vector2(0, 7 if not caption.is_empty() else 0)
			for offset in shape:
				var color: Color = colors[int(session.pieces[i].color) - 1]
				if not fits: color = color.darkened(0.65)
				color.a = 0.4 + reveal * 0.6
				_block(Rect2(start + Vector2(offset[0], offset[1]) * small + Vector2.ONE, Vector2.ONE * (small - 2)), color)
			if not caption.is_empty():
				draw_string(get_theme_default_font(), rect.position + Vector2(12, rect.size.y - 12), caption, HORIZONTAL_ALIGNMENT_CENTER, rect.size.x - 24, 9, Color("7af5dd") if previewing and fits else ThemeManager.MUTED)

func _draw_outgoing() -> void:
	var progress: float = effects.clear_progress()
	for ghost in effects.ghosts:
		var index: int = int(ghost.index)
		if not effects.landing.is_empty() and index in effects.landing.indices: continue
		if not effects.motion.is_empty() and (int(index / 8) == int(effects.motion.index) if effects.motion.axis == "row" else index % 8 == int(effects.motion.index)): continue
		var rect: Rect2 = cell_rect(Vector2(index % 8, int(index / 8)))
		var inset: float = rect.size.x * 0.16 * progress * progress
		var color: Color = colors[int(ghost.color) - 1].lightened(sin(progress * PI) * 0.22)
		color.a = 1.0 - progress
		_block(rect.grow(-inset), color)
	if effects.elapsed >= effects.move_seconds:
		for sweep in effects.sweeps:
			var horizontal: bool = sweep.axis == "row"
			var position_value: Vector2 = board_rect.position + (Vector2(progress * board_rect.size.x, (int(sweep.index) + 0.5) * cell) if horizontal else Vector2((int(sweep.index) + 0.5) * cell, progress * board_rect.size.y))
			var half: Vector2 = Vector2(cell * 0.32, cell * 0.35) if horizontal else Vector2(cell * 0.35, cell * 0.32)
			var rect: Rect2 = Rect2(position_value - half, half * 2).intersection(board_rect)
			if rect.has_area(): draw_style_box(_box(Color(0.7, 1, 0.96, sin(progress * PI) * 0.22), 5), rect)
	for index in effects.ring_indices:
		var alpha: float = maxf(0, 1.0 - effects.elapsed / effects.ring_seconds)
		var rect: Rect2 = cell_rect(Vector2(int(index) % 8, int(int(index) / 8)))
		draw_style_box(_box(Color.TRANSPARENT, int(rect.size.x * 0.172), Color(0.65, 1, 0.94, alpha * 0.55)), rect.grow(1 + (1.0 - alpha) * 2))

func _draw_motion() -> void:
	if effects.motion.is_empty(): return
	var motion: Dictionary = effects.motion
	var t: float = effects.move_progress()
	var progress: float = t * t * (3.0 - 2.0 * t)
	for i in 8:
		var index: int = int(motion.index) * 8 + i if motion.axis == "row" else i * 8 + int(motion.index)
		if int(motion.before[index]) == 0: continue
		var position_value: Vector2 = Vector2(i, motion.index) if motion.axis == "row" else Vector2(motion.index, i)
		var offset: Vector2 = Vector2(progress * int(motion.direction), 0) if motion.axis == "row" else Vector2(0, progress * int(motion.direction))
		for wrap in [-8, 0, 8]:
			var wrapped: Vector2 = position_value + offset + (Vector2(wrap, 0) if motion.axis == "row" else Vector2(0, wrap))
			_block(cell_rect(wrapped), colors[int(motion.before[index]) - 1], board_rect)

func _draw_landing() -> void:
	if effects.landing.is_empty(): return
	var landing: Dictionary = effects.landing
	var t: float = effects.move_progress()
	var start: Vector2 = Vector2(landing.from).lerp(Vector2(landing.to), 1.0 - pow(1.0 - t, 3))
	for offset in landing.cells:
		var rect: Rect2 = Rect2(start + Vector2(offset[0], offset[1]) * cell + Vector2.ONE * 3, Vector2.ONE * (cell - 6))
		_block(rect.grow(-sin(t * PI) * rect.size.x * 0.025), colors[int(landing.color) - 1])

func _draw_particles_and_points() -> void:
	if effects.elapsed < effects.move_seconds: return
	var t: float = effects.points_progress()
	var particle_t: float = effects.particle_progress()
	var elapsed: float = maxf(0, effects.elapsed - effects.move_seconds)
	for spark in effects.sparks:
		var p: Vector2 = Vector2(spark.position) + Vector2(spark.velocity) * elapsed + Vector2(0, elapsed * elapsed * 0.7)
		var position_value: Vector2 = board_rect.position + p * cell
		if not board_rect.grow(8).has_point(position_value): continue
		var color: Color = Color(colors[int(spark.shade)], (1.0 - particle_t) * 0.7)
		var radius: float = 1.3 * (1.0 - particle_t)
		draw_line(position_value - Vector2(radius * 2, 0), position_value + Vector2(radius * 2, 0), color, 1, true)
		draw_line(position_value - Vector2(0, radius * 2), position_value + Vector2(0, radius * 2), color, 1, true)
	if not effects.banner.is_empty():
		var banner: Dictionary = effects.banner
		var alpha: float = minf(1, t * 10) * minf(1, (1.0 - t) * 4)
		var y: float = clampf(board_rect.position.y + Vector2(banner.center).y * cell - t * 18, board_rect.position.y + 34, board_rect.end.y - 18)
		var color: Color = Color(banner.color, alpha)
		var title_size: int = int(12 * float(preferences.values.text_scale))
		draw_string(get_theme_default_font(), Vector2(board_rect.position.x, y - 16), str(banner.title), HORIZONTAL_ALIGNMENT_CENTER, board_rect.size.x, title_size, Color(0.02, 0.04, 0.08, alpha))
		draw_string(get_theme_default_font(), Vector2(board_rect.position.x, y - 17), str(banner.title), HORIZONTAL_ALIGNMENT_CENTER, board_rect.size.x, title_size, color)
		draw_string(get_theme_default_font(), Vector2(board_rect.position.x, y + 5), str(banner.points), HORIZONTAL_ALIGNMENT_CENTER, board_rect.size.x, 24, Color(0.95, 1, 1, alpha))

func cell_rect(position_value: Vector2) -> Rect2:
	return Rect2(board_rect.position + position_value * cell + Vector2.ONE * 3, Vector2.ONE * (cell - 6))

func drag_top_left(shape: Array) -> Vector2:
	return pointer - Vector2(0, float(preferences.values.drag_lift)) - _extent(shape) * cell / 2

func _block(rect: Rect2, color: Color, clip: Rect2 = Rect2()) -> void:
	# One material and one full footprint for board, held pieces and wrap fragments.
	if rect.size.x <= 0 or rect.size.y <= 0: return
	_tile_layer(TILE, rect, color, clip)
	_tile_layer(SHEEN, rect, Color(1, 1, 1, color.a * (0.25 if color.v < 0.4 else 0.7)), clip)

func _tile_layer(texture: Texture2D, rect: Rect2, color: Color, clip: Rect2) -> void:
	if clip.has_area():
		var intersection: Rect2 = rect.intersection(clip)
		if not intersection.has_area(): return
		var source: Rect2 = Rect2((intersection.position - rect.position) / rect.size * texture.get_size(), intersection.size / rect.size * texture.get_size())
		draw_texture_rect_region(texture, intersection, source, color)
	else:
		draw_texture_rect(texture, rect, false, color)

func _extent(shape: Array) -> Vector2:
	var extent: Vector2 = Vector2.ONE
	for offset in shape:
		extent = extent.max(Vector2(offset[0] + 1, offset[1] + 1))
	return extent

func _gui_input(event: InputEvent) -> void:
	if event is InputEventScreenTouch and event.canceled and event.index == pointer_id:
		cancel_drag()
		return
	if event.device == -1:
		return # Native touches own their pointer; ignore the emulated mouse.
	if session == null or session.over:
		return
	var id: int = -1
	var position_value: Vector2
	var pressed: bool = false
	var released: bool = false
	if event is InputEventScreenTouch:
		id = event.index
		position_value = event.position
		pressed = event.pressed
		released = not event.pressed
	elif event is InputEventScreenDrag:
		id = event.index
		position_value = event.position
	elif event is InputEventMouseButton and event.button_index == MOUSE_BUTTON_LEFT:
		position_value = event.position
		pressed = event.pressed
		released = not event.pressed
	elif event is InputEventMouseMotion:
		position_value = event.position
	else:
		return
	if pressed and pointer_id == -99:
		pointer_id = id
		pointer = position_value
		if shift_armed and board_rect.has_point(pointer):
			finish_effects()
			shift_pointer = true
			swipe_start = pointer
			selected_line = clampi(int((pointer.y - board_rect.position.y) / cell) if shift_axis == "row" else int((pointer.x - board_rect.position.x) / cell), 0, 7)
			line_selected.emit(shift_axis, selected_line)
		elif not shift_armed:
			for i in 3:
				if tray_rects[i].has_point(pointer) and not session.pieces[i].is_empty():
					dragging = i
					effects.pickup(not bool(preferences.values.reduce_motion))
					set_process(effects.active())
					piece_picked.emit()
		_update_origin()
	elif id == pointer_id:
		pointer = position_value
		_update_origin()
		if released:
			if shift_pointer:
				var delta: Vector2 = pointer - swipe_start
				var distance: float = delta.x if shift_axis == "row" else delta.y
				if absf(distance) > 22:
					shift_swiped.emit(shift_axis, selected_line, 1 if distance > 0 else -1)
			elif dragging >= 0:
				var slot: int = dragging
				var target: Vector2i = origin
				var allowed: bool = valid
				if allowed and not bool(preferences.values.reduce_motion):
					var piece: Dictionary = session.pieces[slot]
					var indices: Array[int] = []
					for offset in piece.cells: indices.append((target.y + int(offset[1])) * 8 + target.x + int(offset[0]))
					pending_landing = {"cells": piece.cells.duplicate(true), "color": piece.color, "indices": indices, "from": drag_top_left(piece.cells), "to": board_rect.position + Vector2(target) * cell}
				cancel_drag()
				if allowed:
					placement_requested.emit(slot, target)
				else:
					effects.reject(slot, not bool(preferences.values.reduce_motion))
					set_process(effects.active())
					invalid_drop.emit()
				pending_landing.clear()
			pointer_id = -99
			shift_pointer = false
	queue_redraw()
	accept_event()

func _update_origin() -> void:
	if dragging < 0 or session.pieces[dragging].is_empty():
		return
	var top_left: Vector2 = drag_top_left(session.pieces[dragging].cells)
	origin = Vector2i(((top_left - board_rect.position) / cell).round())
	valid = board_rect.grow(cell * 0.3).has_point(pointer - Vector2(0, float(preferences.values.drag_lift))) and session.board.can_place(session.pieces[dragging].cells, origin)

func cancel_drag() -> void:
	dragging = -1
	pointer_id = -99
	shift_pointer = false
	valid = false
	queue_redraw()

func animate_result(result: Dictionary) -> void:
	effects.begin(result, pending_landing, preferences, session.mode, session.score.streak)
	pending_landing.clear()
	set_process(effects.active())
	queue_redraw()

func celebrate(title: String, color: Color) -> void:
	effects.celebrate(title, color)
	set_process(effects.active())
	queue_redraw()

func finish_effects() -> void:
	effects.clear()
	set_process(false)
	queue_redraw()

func _process(delta: float) -> void:
	if bool(preferences.values.reduce_motion):
		finish_effects()
		return
	effects.advance(delta)
	queue_redraw()
	set_process(effects.active())
