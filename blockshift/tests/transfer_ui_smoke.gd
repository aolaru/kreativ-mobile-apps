extends SceneTree
const QA_PATH: String = "user://qa-transfer-ui.json"
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


func _run() -> void:
	for suffix in ["", ".tmp", ".bak", ".before-import.blockshift"]: DirAccess.remove_absolute(QA_PATH + suffix)
	app = load("res://scenes/main.tscn").instantiate()
	app.set_meta("qa_save_path", QA_PATH)
	app.set_meta("qa_controlled_focus", true)
	root.add_child(app)
	await create_timer(0.65).timeout
	app.start_game("classic")
	app.session.score.total = 64
	app._checkpoint()
	app.show_settings("game", "about")
	await click(button("Back up & restore progress", app))
	check(app.screen == "save_transfer", "backup page reachable from About")
	var original: Dictionary = JSON.parse_string(JSON.stringify(app.store.data))
	var exported: String = "user://qa-ui-export.blockshift"
	app._export_transfer(exported)
	check(labels(app).contains("Backup saved and verified"), "export confirmation follows read-back verification")
	check(SaveTransfer.inspect_file(exported).get("ok", false), "UI export is a valid portable file")
	await click(button("Done", app.modal))
	var fixture: Dictionary = original.duplicate(true)
	fixture.runs.classic.score.total = 901
	fixture.stats.classic.best = 901
	fixture.settings.tutorial_done = true
	var incoming: String = "user://qa-ui-incoming.blockshift"
	check(SaveTransfer.export_file(incoming, fixture), "test backup created")
	app._preview_transfer(incoming)
	await settle()
	check(labels(app.modal).contains("Classic") and labels(app.modal).contains("901"), "preview shows modes and scores before committing")
	check(app.store.saved("classic").score.total == 64, "preview does not modify current board")
	await capture("45-restore-preview")
	await click(button("Keep current progress", app.modal))
	check(app.store.saved("classic").score.total == 64 and app.modal == null, "cancel leaves current progress intact")
	app._preview_transfer(incoming)
	await click(button("Restore backup", app.modal))
	check(app.session == null and app.store.saved("classic").score.total == 901, "confirm restores and detaches stale live session")
	check(app.preferences.values.tutorial_done, "restored preferences applied")
	check(SaveManager.new(QA_PATH).saved("classic").score.total == 901, "restored state persists immediately")
	await click(button("Done", app.modal))
	check(button("Undo last restore", app) != null, "undo available after restore")
	await capture("46-progress-transfer")
	await click(button("Undo last restore", app))
	await click(button("Restore backup", app.modal))
	check(app.store.saved("classic").score.total == 64, "undo recovers original progress through UI")
	await click(button("Done", app.modal))
	app._preview_transfer("user://not-a-backup.blockshift")
	check(labels(app.modal).contains("Backup not restored") and app.store.saved("classic").score.total == 64, "bad file shows failure and keeps current progress")
	app.queue_free()
	await settle()
	for suffix in ["", ".tmp", ".bak", ".import", ".before-import.blockshift"]: DirAccess.remove_absolute(QA_PATH + suffix)
	DirAccess.remove_absolute(exported)
	DirAccess.remove_absolute(incoming)
	print("TRANSFER UI: %d assertions; %d failures" % [assertions, failures])
	quit(0 if failures == 0 else 1)
