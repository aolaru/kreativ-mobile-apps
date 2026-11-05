extends SceneTree
## Regressions from the 0.4.0 audit. Real UI hit testing; disposable storage only.
const QA_PATH: String = "user://qa-reliability.json"
var app: Control
var assertions: int = 0
var failures: int = 0

func _initialize() -> void:
	_run.call_deferred()

func check(ok: bool, description: String) -> void:
	assertions += 1
	if not ok:
		failures += 1
		printerr("RELIABILITY FAIL: " + description)

func settle() -> void:
	await process_frame
	await process_frame

func button(title: String, parent: Node) -> Button:
	for child in parent.get_children():
		if child is Button and child.text == title: return child
		var found: Button = button(title, child)
		if found != null: return found
	return null

func labels(parent: Node) -> String:
	var value: String = parent.text if parent is Label else ""
	for child in parent.get_children(): value += "\n" + labels(child)
	return value

func click(target: Control) -> void:
	if target == null:
		check(false, "expected button exists")
		return
	await settle()
	var ancestor: Node = target.get_parent()
	while ancestor != null:
		if ancestor is ScrollContainer: ancestor.ensure_control_visible(target)
		ancestor = ancestor.get_parent()
	await settle()
	await create_timer(0.12).timeout
	var position: Vector2 = target.get_global_rect().get_center()
	root.notify_mouse_entered()
	var motion: InputEventMouseMotion = InputEventMouseMotion.new()
	motion.position = position
	motion.global_position = position
	root.push_input(motion, true)
	for pressed in [true, false]:
		var event: InputEventMouseButton = InputEventMouseButton.new()
		event.button_index = MOUSE_BUTTON_LEFT
		event.button_mask = MOUSE_BUTTON_MASK_LEFT if pressed else 0
		event.position = position
		event.global_position = position
		event.pressed = pressed
		root.push_input(event, true)
	await settle()

func back() -> void:
	app.last_back_at = -1000
	root.go_back_requested.emit()

func capture(name_value: String) -> void:
	await settle()
	await create_timer(0.24).timeout
	if DisplayServer.get_name() != "headless":
		await RenderingServer.frame_post_draw
		root.get_texture().get_image().save_png("res://review/" + name_value + ".png")

func winning_daily() -> void:
	app.start_game("daily")
	app.session.score.lines = DailyChallenge.target() - 1
	app.session.board.cells.fill(0)
	for x in 7: app.session.board.cells[x] = 1
	app.session.pieces = [app.session.generator.make_piece(0), {}, {}]
	app._place(0, Vector2i(7, 0))

func _run() -> void:
	for suffix in ["", ".tmp", ".bak", ".reset"]: DirAccess.remove_absolute(QA_PATH + suffix)
	app = load("res://scenes/main.tscn").instantiate()
	app.set_meta("qa_save_path", QA_PATH)
	app.set_meta("qa_controlled_focus", true)
	root.add_child(app)
	await create_timer(0.65).timeout
	app.preferences.values.tutorial_done = true
	app.start_game("classic")
	app.session.finish()
	app.show_home()
	app.show_settings("home", "play")
	await click(app.page.find_child("RestoreSettings", true, false))
	back()
	check(app.screen == "settings" and app.modal == null, "Back dismisses Settings confirmation even with a finished retained game")
	app.show_privacy()
	await click(button("Reset local progress", app.page))
	back()
	check(app.screen == "privacy" and app.modal == null, "Back dismisses reset confirmation without leaving Privacy")
	app.show_pro("settings")
	check(app.screen == "settings" and app.settings_section == "about" and app.modal == null, "unavailable commerce never opens a checkout or store dialog")
	app.show_game()
	back()
	check(app.screen == "home", "Back on game-over still returns home")

	app.start_game("shift")
	app.store.path = "user://qa-missing-reliability-dir/progress.json"
	app.session.pieces = [app.session.generator.make_piece(0), {}, {}]
	app._place(0, Vector2i(2, 2))
	var expected_board: Array = app.session.board.cells.duplicate()
	check(not app._checkpoint() and app.save_notice.visible, "failed saves return false and expose persistent warning")
	app.pause_game()
	check(labels(app.modal).contains("could not be saved") and not labels(app.modal).contains("Your board is saved"), "Pause never claims a failed save succeeded")
	await settle()
	check(app.modal.get_global_rect().end.y <= app.save_notice.get_global_rect().position.y, "Retry save remains outside the modal's input blocker")
	await capture("43-save-failure")
	await click(app.save_retry_button)
	check(app.save_notice.visible and app.modal != null, "failed Retry keeps warning and paused board")
	app.store.path = QA_PATH
	await click(app.save_retry_button)
	var loaded: SaveManager = SaveManager.new(QA_PATH)
	var restored: GameSession = GameSession.from_snapshot(loaded.saved("shift"))
	check(not app.save_notice.visible and restored != null and restored.board.cells == expected_board, "successful native Retry persists latest board and dismisses warning")
	check(loaded.stats("shift").games == 1 and loaded.stats("shift").best == app.session.score.total, "retry does not duplicate games or lose score")
	app._close_modal()
	app.store.path = "user://qa-missing-reliability-dir/progress.json"
	app._checkpoint()
	root.content_scale_size = Vector2i(400, 800)
	root.size = Vector2i(400, 800)
	app.preferences.values.text_scale = 1.2
	app._apply_theme()
	app.show_game()
	await settle()
	check(app.page.get_global_rect().end.y <= app.save_notice.get_global_rect().position.y and app.save_notice.get_global_rect().end.y <= app.size.y, "save warning and gameplay fit short portrait with larger text")
	await capture("44-save-warning-compact")
	app.store.path = QA_PATH
	app._process(5.1)
	check(not app.save_notice.visible and app.store.last_error.is_empty(), "automatic retry recovers on storage becoming available")
	app.preferences.values.text_scale = 1.0
	app._apply_theme()
	root.content_scale_size = Vector2i(480, 960)
	root.size = Vector2i(480, 960)
	app.show_game()

	app.store.read_only = true
	app._checkpoint()
	var before: Dictionary = app.store.data.duplicate(true)
	var main_text: String = FileAccess.get_file_as_string(QA_PATH)
	var backup_text: String = FileAccess.get_file_as_string(QA_PATH + ".bak")
	check(app.save_notice.visible and app.save_retry_button.disabled, "newer-version protection explains unavailable writes")
	app.show_privacy()
	await click(button("Reset local progress", app.page))
	await click(app.modal.find_child("ConfirmResetProgress", true, false))
	check(labels(app.modal).contains("Progress was not reset") and app.session != null, "failed reset reports failure and keeps live game")
	check(app.store.data == before and FileAccess.get_file_as_string(QA_PATH) == main_text and FileAccess.get_file_as_string(QA_PATH + ".bak") == backup_text, "failed reset preserves memory, main file and recovery copy")
	back()
	check(app.screen == "privacy", "Back from reset error returns to Privacy")
	app.store.read_only = false
	app._checkpoint()
	await click(button("Reset local progress", app.page))
	await click(app.modal.find_child("ConfirmResetProgress", true, false))
	loaded = SaveManager.new(QA_PATH)
	check(app.screen == "home" and app.session == null and loaded.data.runs.is_empty() and loaded.stats("shift").games == 0, "successful reset clears all boards and survives disk reload")
	check(app.preferences.values.tutorial_done, "progress reset retains preferences as described")

	app.start_game("shift")
	app.previous_best = 50
	app.session.score.total = 60
	app.session.score.streak = 1
	app.session.shift.energy = app.session.shift.capacity() - 1
	for x in 7: app.session.board.cells[x] = 2
	app.session.pieces = [app.session.generator.make_piece(0), {}, {}]
	app._place(0, Vector2i(7, 0))
	check(app.status_label.text.contains("COMBO ×2") and app.status_label.text.contains("+126") and app.status_label.text.contains("NEW BEST"), "clear, combo and new-best feedback are retained together")
	check(app.energy_label.text.contains("SHIFT READY") and app.shift_button.text == "Use SHIFT", "charge instructions remain visible in their dedicated meter")
	await capture("45-combo-and-charge")
	check(app.status_label.text.contains("COMBO ×2"), "combo feedback survives subsequent rendering frames")

	winning_daily()
	check(app.session.completed and app.modal == null and app.result_wait > 0 and app.canvas.effects.active(), "Daily result is saved while final effects remain visible")
	loaded = SaveManager.new(QA_PATH)
	check(loaded.data.daily[app.session.date].completed and loaded.saved("daily").over, "completion is durable before waiting for presentation")
	var moves: int = app.session.moves
	app._place(0, Vector2i(0, 0))
	check(app.session.moves == moves, "final animation cannot accept another placement")
	await create_timer(1.3).timeout
	check(app.modal != null and not app.canvas.effects.active() and labels(app.modal).contains("Daily complete"), "Daily result appears after final clear finishes")
	back()
	check(app.screen == "home", "Daily results use their explicit Back destination")
	app.preferences.values.reduce_motion = true
	winning_daily()
	check(app.modal != null and app.result_wait == 0 and not app.canvas.effects.active(), "reduced motion opens results immediately")
	app.preferences.values.reduce_motion = false

	app.start_game("daily")
	app.session.board.cells.fill(1)
	for i in 8: app.session.board.cells[i * 8 + i] = 0
	app.session.pieces = [app.session.generator.make_piece(19), {}, {}]
	app.session.score.lines = DailyChallenge.target() - 1
	app.session.shift.energy = app.session.shift.capacity()
	app.session.update_status()
	app._toggle_shift()
	app._preview_shift("row", 0, 1)
	app._shift_button_pressed()
	check(app.session.completed and app.modal == null and not app.canvas.effects.motion.is_empty(), "a winning SHIFT also plays wrap/clear before results")
	await create_timer(1.4).timeout
	check(app.modal != null and app.result_wait == 0, "winning SHIFT reaches results exactly once")

	app.start_game("shift")
	for y in 8:
		for x in 8: app.session.board.cells[y * 8 + x] = 1 if (x + y) % 2 == 0 else 0
	app.session.pieces = [app.session.generator.make_piece(1), {}, {}]
	app.session.shift.energy = app.session.shift.capacity()
	app.session.update_status()
	app.pause_game()
	await click(button("End this run", app.modal))
	check(app.session.end_reason == "manual" and labels(app.modal).contains("You chose to finish") and not labels(app.modal).contains("no charged SHIFT"), "voluntary finish has a truthful explanation even with available rescues")
	loaded = SaveManager.new(QA_PATH)
	check(GameSession.from_snapshot(loaded.saved("shift")).end_reason == "manual", "manual ending survives app restart")
	await capture("46-voluntary-finish")

	app.session = null
	app.queue_free()
	await settle()
	for suffix in ["", ".tmp", ".bak", ".reset"]: DirAccess.remove_absolute(QA_PATH + suffix)
	print("Reliability: %d assertions; %d failures" % [assertions, failures])
	quit(0 if failures == 0 else 1)
