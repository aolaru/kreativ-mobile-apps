extends RefCounted
## This entire file is excluded from both Android export presets.
static func show_menu(app: Control) -> void:
	if not OS.is_debug_build(): return
	var box: VBoxContainer = app._sheet("Developer boards", "Editor only. QA boards never change player statistics.")
	for preset in ["Cross clear", "SHIFT wrap", "SHIFT rescue", "Crowded", "New hand"]:
		app._button(preset, func() -> void: load_preset(app, preset), false, box)
	app._button("Close", app._close_modal, false, box)

static func load_preset(app: Control, preset: String) -> void:
	if not OS.is_debug_build(): return
	app._checkpoint()
	app.tutorial_active = false
	app.qa_active = true
	app.session = GameSession.new("shift", 55)
	app.session.shift.energy = app.session.shift.capacity()
	if preset == "Cross clear":
		for i in 8:
			if i != 4:
				app.session.board.cells[4 * 8 + i] = 2
				app.session.board.cells[i * 8 + 4] = 3
		app.session.pieces = [app.session.generator.make_piece(0), app.session.generator.make_piece(1), app.session.generator.make_piece(5)]
	elif preset == "SHIFT wrap":
		for y in 8:
			if y != 3: app.session.board.cells[y * 8] = 2
		app.session.board.cells[31] = 1
	elif preset == "SHIFT rescue":
		for y in 8:
			for x in 8: app.session.board.cells[y * 8 + x] = 1 if (x + y) % 2 == 0 else 0
		app.session.pieces = [app.session.generator.make_piece(1), {}, {}]
	elif preset == "Crowded":
		for i in 64: app.session.board.cells[i] = 0 if i % 5 == 0 else i % 4 + 1
	app.session.update_status()
	app.armed = false
	app.show_game()
	app._toast("QA: " + preset + " · Progress is isolated")
