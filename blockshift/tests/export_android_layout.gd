extends SceneTree
## Generate emulator touch targets from the real UI instead of copying coordinates.
var app: Control
const QA_PATH: String = "user://qa-layout.json"

func _initialize() -> void:
	_run.call_deferred()

func settle() -> void:
	await process_frame
	await process_frame

func point(control: Control) -> Array:
	var center: Vector2 = control.get_global_rect().get_center()
	return [center.x, center.y]

func button(prefix: String, parent: Node) -> Button:
	for child in parent.get_children():
		if child is Button and child.text.begins_with(prefix): return child
		var found: Button = button(prefix, child)
		if found != null: return found
	return null

func board_geometry() -> Dictionary:
	var canvas: GameCanvas = app.canvas
	var origin: Vector2 = canvas.global_position + canvas.board_rect.position
	var tray: Vector2 = canvas.global_position + canvas.tray_rects[0].get_center()
	return {"origin": [origin.x, origin.y], "cell": canvas.cell, "tray": [tray.x, tray.y], "lift": app.preferences.values.drag_lift}

func _run() -> void:
	root.content_scale_size = Vector2i(480, 960)
	root.size = Vector2i(480, 1067)
	for suffix in ["", ".tmp", ".bak"]: DirAccess.remove_absolute(QA_PATH + suffix)
	app = load("res://scenes/main.tscn").instantiate()
	app.set_meta("qa_save_path", QA_PATH)
	app.set_meta("qa_controlled_focus", true) # Lifecycle events are exercised explicitly; host focus is nondeterministic.
	root.add_child(app)
	await create_timer(0.65).timeout
	var layout: Dictionary = {"version": ProjectSettings.get_setting("application/config/version"), "viewport": [480, 1067]}
	layout.home = {"shift": point(app.find_child("PlayShift", true, false)), "classic": point(button("Classic", app.page))}
	app.start_tutorial()
	await settle()
	app._close_modal()
	layout.tutorial = board_geometry()
	for line in 4: app._place(0, Vector2i(6, 7 - line))
	await create_timer(0.8).timeout
	layout.shift_button = point(app.shift_button)
	app._toggle_shift()
	await settle()
	layout.armed = board_geometry()
	app._preview_shift("row", 3, 1)
	await settle()
	layout.shift_apply = point(app.shift_button)
	app._shift_button_pressed()
	await create_timer(0.8).timeout
	layout.tutorial_menu = point(button("Back to menu", app.modal))
	app.show_home()
	app.start_game("classic")
	await settle()
	app._close_modal()
	layout.classic = board_geometry()
	app.show_home()
	await settle()
	layout.resume = point(app.find_child("Continue", true, false))
	var file: FileAccess = FileAccess.open("res://review/android-layout.json", FileAccess.WRITE)
	file.store_string(JSON.stringify(layout, "  ") + "\n")
	file.close()
	app.session = null
	app.queue_free()
	await settle()
	for suffix in ["", ".tmp", ".bak"]: DirAccess.remove_absolute(QA_PATH + suffix)
	print("Android layout: generated from real version %s controls" % layout.version)
	quit()
