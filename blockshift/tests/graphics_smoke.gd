extends SceneTree
## Real Godot renderer, isolated save, controlled visual clock. Not an FPS benchmark.
const QA_PATH: String = "user://qa-graphics.json"
var app: Control
var assertions: int = 0
var failures: int = 0
var frame_number: int = 0

func _initialize() -> void:
	_run.call_deferred()

func check(condition: bool, description: String) -> void:
	assertions += 1
	if not condition:
		failures += 1
		printerr("GRAPHICS FAIL: " + description)

func settle() -> void:
	await process_frame
	await process_frame

func capture(name_value: String) -> void:
	app.canvas.queue_redraw()
	await process_frame
	await RenderingServer.frame_post_draw
	root.get_texture().get_image().save_png("res://review/" + name_value + ".png")

func record(count: int, advance: bool = true) -> void:
	for i in count:
		app.canvas.set_process(false)
		if advance: app.canvas.effects.advance(1.0 / 30.0)
		app.canvas.queue_redraw()
		await process_frame
		await RenderingServer.frame_post_draw
		root.get_texture().get_image().save_png("res://build/preview_frames/%04d.png" % frame_number)
		frame_number += 1

func pointer(position_value: Vector2, pressed: bool) -> void:
	var event: InputEventScreenTouch = InputEventScreenTouch.new()
	event.index = 0
	event.position = position_value
	event.pressed = pressed
	root.push_input(event, true)
	app.canvas.set_process(false)
	await process_frame

func drag(position_value: Vector2) -> void:
	var event: InputEventScreenDrag = InputEventScreenDrag.new()
	event.index = 0
	event.position = position_value
	root.push_input(event, true)
	app.canvas.set_process(false)
	await process_frame

func clear_fixture() -> void:
	app.session.board = Board.new()
	for index in [9, 10, 18, 19, 28, 29, 36, 44]: app.session.board.cells[index] = 2 + int(index) % 3
	for x in 6: app.session.board.cells[56 + x] = 1 + x % 4
	app.session.pieces = [app.session.generator.make_piece(1), app.session.generator.make_piece(12), app.session.generator.make_piece(14)]
	app.session.over = false
	app.previous_best = 0
	app.canvas.finish_effects()
	app._refresh_game()

func wrap_fixture() -> void:
	app.session.board = Board.new()
	for y in 8:
		if y != 3: app.session.board.cells[y * 8] = 2
	app.session.board.cells[31] = 1
	app.session.pieces = [app.session.generator.make_piece(0), {}, {}]
	app.session.shift.energy = app.session.shift.capacity()
	app.session.score.streak = 1
	app.session.over = false
	app.previous_best = 0
	app.canvas.finish_effects()
	app._refresh_game()

func _run() -> void:
	if DisplayServer.get_name() == "headless":
		printerr("Graphics smoke requires a graphical display.")
		quit(2)
		return
	DirAccess.make_dir_recursive_absolute("res://build/preview_frames")
	for file in DirAccess.get_files_at("res://build/preview_frames"):
		if file.ends_with(".png"): DirAccess.remove_absolute("res://build/preview_frames/" + file)
	for suffix in ["", ".tmp", ".bak"]: DirAccess.remove_absolute(QA_PATH + suffix)
	app = load("res://scenes/main.tscn").instantiate()
	app.set_meta("qa_save_path", QA_PATH)
	app.set_meta("qa_controlled_focus", true) # Lifecycle events are exercised explicitly; host focus is nondeterministic.
	root.add_child(app)
	root.grab_focus()
	await create_timer(0.7).timeout
	app.start_game("shift")
	await create_timer(0.2).timeout
	app._close_modal()
	clear_fixture()
	await settle()
	await capture("25-materials-board")
	await record(9, false)
	var canvas: GameCanvas = app.canvas
	var tray: Vector2 = canvas.global_position + canvas.tray_rects[0].get_center()
	var target: Vector2 = canvas.global_position + canvas.board_rect.position + Vector2(7, 7.5) * canvas.cell + Vector2(0, 88)
	await pointer(tray, true)
	for i in 12:
		await drag(tray.lerp(target, float(i + 1) / 12))
		await record(1)
	check(canvas.valid and canvas.origin == Vector2i(6, 7), "material and pickup effect retain precise placement")
	await capture("26-piece-pickup")
	await record(6)
	await pointer(target, false)
	check(app.session.score.lines == 1 and app.session.board.occupied() == 8, "line clear commits before the visual timeline finishes")
	check(canvas.effects.ghosts.size() == 8 and not canvas.effects.landing.is_empty(), "clear retains all outgoing tile colors after a real drag")
	var snapshot: Dictionary = app.session.snapshot()
	await record(2)
	await capture("27-placement-settle")
	await record(8)
	await capture("28-line-dissolve")
	await record(8)
	await capture("29-clear-reward")
	await record(20)
	for key in ["board", "pieces", "score", "rng_state", "moves"]:
		check(app.session.snapshot()[key] == snapshot[key], "visual frames never mutate " + key)
	check(not canvas.effects.active(), "clear timeline ends without persistent redraw")
	wrap_fixture()
	await record(12, false)
	app._toggle_shift()
	await settle()
	app._shift("row", 3, 1)
	canvas.set_process(false)
	check(app.session.board.occupied() == 0 and not canvas.effects.motion.is_empty(), "SHIFT model clears immediately while wrap remains purely visual")
	await record(4)
	await capture("30-shift-wrap")
	await record(12)
	await capture("31-shift-combo")
	await record(20)
	check(not canvas.effects.active(), "SHIFT and combo presentation settle completely")
	canvas.celebrate("PERSONAL BEST", Color("efba70"))
	await record(10)
	await capture("32-personal-best")
	app.pause_game()
	check(not canvas.effects.active() and not canvas.is_processing(), "pause discards every transient effect")
	app._close_modal()
	app.preferences.values.reduce_motion = true
	wrap_fixture()
	app.armed = true
	app._shift("row", 3, 1)
	check(not canvas.is_processing() and not canvas.effects.active() and app.session.board.occupied() == 0, "reduced motion renders final SHIFT state immediately")
	await capture("33-reduced-motion")
	app.preferences.values.reduce_motion = false
	app.session.board = Board.new()
	app.session.pieces = [app.session.generator.make_piece(0), {}, {}]
	app._place(0, Vector2i(3, 3))
	check(canvas.effects.deal and not app.session.pieces[0].is_empty() and not app.session.pieces[1].is_empty() and not app.session.pieces[2].is_empty(), "new pieces are playable immediately during their visual reveal")
	await record(3)
	await capture("35-new-hand")
	await record(12)
	app.start_game("daily")
	await settle()
	app._close_modal()
	app.session.score.lines = DailyChallenge.target() - 1
	clear_fixture()
	app._place(0, Vector2i(6, 7))
	await create_timer(0.25).timeout
	check(app.session.completed and app.modal == null and app.canvas.effects.active(), "final Daily clear is visible before the completion sheet")
	await capture("42-daily-final-clear")
	await create_timer(1.1).timeout
	check(app.session.completed and app.modal != null and not app.canvas.is_processing(), "daily badge modal is shown without background effects running")
	await capture("34-daily-success")
	await record(12, false)
	app.show_home()
	await create_timer(0.2).timeout
	check(not app.find_child("GameCanvas", true, false), "navigation frees the effect renderer")
	app.session = null
	app.queue_free()
	await settle()
	for suffix in ["", ".tmp", ".bak"]: DirAccess.remove_absolute(QA_PATH + suffix)
	print("Graphics: %d assertions; %d failures; %d recorded frames" % [assertions, failures, frame_number])
	quit(0 if failures == 0 else 1)
