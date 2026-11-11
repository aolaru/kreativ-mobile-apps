extends SceneTree
var app: Control
var assertions: int = 0
var failures: int = 0
var save_path: String = "user://qa-ui.json"

func _initialize() -> void:
	_run.call_deferred()

func check(condition: bool, description: String) -> void:
	assertions += 1
	if not condition:
		failures += 1
		printerr("UI FAIL: " + description)

func settle() -> void:
	await process_frame
	await process_frame

func capture(name_value: String) -> void:
	await settle()
	await create_timer(0.24).timeout # Capture settled page/modal fades, not a half-transparent frame.
	if DisplayServer.get_name() != "headless":
		await RenderingServer.frame_post_draw
		root.get_texture().get_image().save_png("res://review/" + name_value + ".png")

func touch(index: int, position: Vector2, pressed: bool) -> void:
	var event: InputEventScreenTouch = InputEventScreenTouch.new()
	event.index = index
	event.position = position
	event.pressed = pressed
	root.push_input(event, true)
	await process_frame

func drag(index: int, position: Vector2) -> void:
	var event: InputEventScreenDrag = InputEventScreenDrag.new()
	event.index = index
	event.position = position
	root.push_input(event, true)
	await process_frame

func click(button: Control) -> void:
	await settle() # Containers lay out newly created controls on subsequent frames.
	var ancestor: Node = button.get_parent()
	while ancestor != null:
		if ancestor is ScrollContainer: ancestor.ensure_control_visible(button)
		ancestor = ancestor.get_parent()
	await settle()
	await create_timer(0.12).timeout # Wait for native layout/hover changes before hit-testing.
	var position: Vector2 = button.get_global_rect().get_center()
	var motion: InputEventMouseMotion = InputEventMouseMotion.new()
	motion.position = position
	motion.global_position = position
	root.notify_mouse_entered()
	root.push_input(motion, true)
	# Desktop native controls use explicit mouse events; board gestures below use touch.
	# This avoids OS touch-to-mouse emulation competing with the host pointer.
	for pressed in [true, false]:
		var event: InputEventMouseButton = InputEventMouseButton.new()
		event.button_index = MOUSE_BUTTON_LEFT
		event.button_mask = MOUSE_BUTTON_MASK_LEFT if pressed else 0
		event.position = position
		event.global_position = position
		event.pressed = pressed
		root.push_input(event, true)
	await settle()

func pixel_span(image_value: Image, center: Vector2, direction: Vector2, color: Color, radius: int, material: bool = false) -> Vector2i:
	var bounds: Vector2i = Vector2i(radius, -radius)
	for offset in range(-radius, radius + 1):
		var position: Vector2i = Vector2i((center + direction * offset).round())
		var pixel: Color = image_value.get_pixelv(position)
		var matches: bool = maxf(pixel.r, maxf(pixel.g, pixel.b)) > 0.4 if material else absf(pixel.r - color.r) + absf(pixel.g - color.g) + absf(pixel.b - color.b) < 0.03
		if matches:
			bounds.x = mini(bounds.x, offset)
			bounds.y = maxi(bounds.y, offset)
	return bounds

func _run() -> void:
	for suffix in ["", ".tmp", ".bak"]: DirAccess.remove_absolute(save_path + suffix)
	app = load("res://scenes/main.tscn").instantiate()
	app.set_meta("qa_save_path", save_path)
	app.set_meta("qa_controlled_focus", true) # Lifecycle events are exercised explicitly; host focus is nondeterministic.
	root.add_child(app)
	if DisplayServer.get_name() != "headless":
		root.grab_focus()
	await create_timer(0.65).timeout
	check(app.screen == "home", "startup reaches home without a wake-up touch")
	check(not OS.low_processor_usage_mode and Engine.max_fps == 30, "menus advance timers at a bounded frame rate")
	check(not quit_on_go_back, "Android back does not use the engine default exit")
	check(app.audio.ambience.playing and app.audio.ambience.stream is AudioStreamOggVorbis and app.audio.ambience.stream.loop, "rain starts as a continuous compressed loop")
	app.preferences.values.ambience = 0.0
	app.audio.apply_settings()
	check(AudioServer.is_bus_mute(AudioServer.get_bus_index("Ambience")) and app.audio.ambience.stream_paused and not app.audio.preview_ambience(), "muted ambience stays silent including preview")
	app.preferences.values.ambience = 0.35
	app.audio.apply_settings()
	check(app.audio.ambience.playing and not app.audio.ambience.stream_paused and not AudioServer.is_bus_mute(AudioServer.get_bus_index("Ambience")), "raising ambience restores playback")
	app.audio.set_meta("qa_controlled_focus", false)
	app.audio._notification(Node.NOTIFICATION_APPLICATION_FOCUS_OUT)
	app.audio.apply_settings()
	check(app.audio.ambience.stream_paused, "settings never resume audio while backgrounded")
	app.audio._notification(Node.NOTIFICATION_APPLICATION_FOCUS_IN)
	check(not app.audio.ambience.stream_paused and app.audio.preview_ambience(), "foreground and preview restore ambience")
	app.audio.set_meta("qa_controlled_focus", true)
	await capture("01-home")
	app.start_game("classic")
	await settle()
	app._close_modal()
	check(app.canvas.board_rect.size.x > 300, "board has comfortable touch targets")
	check(app.canvas.get_global_rect().end.y < app.size.y, "canvas fits portrait")
	check(app.canvas.board_rect.end.y < app.canvas.tray_rects[0].position.y, "tray and board separated")
	check(app.canvas.tray_rects[0].position.y - app.canvas.board_rect.end.y <= 36, "board and hand stay together on tall screens")
	check(app.canvas.tray_rects[0].end.y <= app.canvas.size.y, "complete hand fits within canvas")
	var before: Array[int] = app.session.board.cells.duplicate()
	var canvas: GameCanvas = app.canvas
	var tray: Vector2 = canvas.global_position + canvas.tray_rects[0].get_center()
	await touch(0, tray, true)
	await touch(1, tray + Vector2(20, 0), true)
	await touch(1, tray, false)
	check(canvas.dragging == 0 and app.session.board.cells == before, "second finger cannot steal or place drag")
	await touch(0, Vector2(10, 10), false)
	check(app.session.board.cells == before and canvas.dragging == -1, "outside drop cancels")
	await touch(0, tray, true)
	var cancel: InputEventScreenTouch = InputEventScreenTouch.new()
	cancel.index = 0
	cancel.position = tray
	cancel.pressed = false
	cancel.canceled = true
	root.push_input(cancel, true)
	await settle()
	check(canvas.dragging == -1 and canvas.pointer_id == -99, "OS touch cancellation releases pointer")
	for i in 100:
		await touch(0, tray, true)
		await touch(0, tray, false)
	check(app.session.board.cells == before, "100 rapid invalid drops preserve board")
	app._close_modal() # Host window focus changes may have opened the real pause sheet.
	app.session.pieces = [app.session.generator.make_piece(0), {}, {}]
	app._place(0, Vector2i(2, 2))
	await create_timer(0.55).timeout
	check(not canvas.is_processing() and not canvas.effects.active(), "placement presentation stops when settled")
	await capture("19-tile-grid-alignment")
	if DisplayServer.get_name() != "headless":
		var rendered: Image = root.get_texture().get_image()
		var scale_factor: float = float(rendered.get_width()) / root.get_visible_rect().size.x
		var occupied: Vector2 = (canvas.global_position + canvas.cell_rect(Vector2(2, 2)).get_center()) * scale_factor
		var empty: Vector2 = (canvas.global_position + canvas.cell_rect(Vector2(3, 2)).get_center()) * scale_factor
		var tint: Color = ThemeManager.palette("orbit")[app.session.board.cells[18] - 1]
		var radius: int = ceili(canvas.cell * scale_factor / 2)
		for axis in [Vector2.RIGHT, Vector2.DOWN]:
			var tile_bounds: Vector2i = pixel_span(rendered, occupied, axis, tint, radius, true)
			var slot_bounds: Vector2i = pixel_span(rendered, empty, axis, Color("152137"), radius)
			check(abs(tile_bounds.x - slot_bounds.x) <= 1 and abs(tile_bounds.y - slot_bounds.y) <= 1 and tile_bounds.y > tile_bounds.x, "placed tile and empty slot have matching rendered bounds")
	app.start_tutorial()
	await settle()
	app._close_modal()
	canvas = app.canvas
	await capture("02-tutorial")
	tray = canvas.global_position + canvas.tray_rects[0].get_center()
	var target: Vector2 = canvas.global_position + canvas.board_rect.position + Vector2(7, 7.5) * canvas.cell + Vector2(0, 88)
	await touch(0, tray, true)
	await drag(0, target)
	check(canvas.valid and canvas.origin == Vector2i(6, 7), "drag preview snaps to intended cells")
	await capture("03-drag-preview")
	await touch(0, target, false)
	check(app.tutorial_step == 1 and app.session.shift.energy == 1 and not app.session.shift.ready(), "first practice clear earns partial charge from an empty meter")
	for line in range(1, 4):
		tray = canvas.global_position + canvas.tray_rects[0].get_center()
		target = canvas.global_position + canvas.board_rect.position + Vector2(7, 7.5 - line) * canvas.cell + Vector2(0, 88)
		await touch(0, tray, true)
		await drag(0, target)
		await touch(0, target, false)
		check(app.tutorial_lines == line + 1 and app.session.shift.energy == line + 1, "charge accumulates across separate practice turns")
		if line == 1: await capture("41-tutorial-half-charge")
	check(app.tutorial_step == 1 and app.session.shift.ready() and app.tutorial_stage_wait > 0, "full meter is shown before transitioning to movement practice")
	app._toggle_shift()
	check(not app.armed and app.shift_button.disabled, "last charging animation cannot enter movement practice early")
	await create_timer(0.8).timeout
	check(app.tutorial_step == 2 and app.session.shift.ready(), "four real clears unlock SHIFT movement practice")
	await click(app.shift_button)
	check(app.armed, "SHIFT button arms selection")
	var selected: Vector2 = canvas.global_position + canvas.board_rect.position + Vector2(4.0, 3.5) * canvas.cell
	await touch(0, selected, true)
	await touch(0, selected, false)
	check(app.selected_index == 3 and app.selected_axis == "row", "touch selects row")
	await capture("04-shift-selection")
	var before_shift: int = app.session.score.total
	await touch(0, selected, true)
	await drag(0, selected + Vector2(60, 0))
	await touch(0, selected + Vector2(60, 0), false)
	check(app.session.score.total == before_shift and app.session.shift.ready() and app.shift_preview.lines == 1, "swipe previews the clear without spending charge or score")
	await capture("37-shift-preview")
	# Desktop host focus may open the real pause sheet while captures are inspected.
	root.grab_focus()
	await settle()
	app._close_modal()
	await click(app.shift_button)
	check(app.session.board.occupied() == 0 and app.session.score.total == before_shift + 200, "SHIFT swipe clears column with the earned practice streak")
	check(app.tutorial_step == 3 and app.modal == null and not canvas.effects.motion.is_empty(), "tutorial shows its committed wrap before opening completion")
	await create_timer(0.8).timeout
	check(app.preferences.values.tutorial_done and app.modal != null, "tutorial completes")
	await capture("05-tutorial-complete")
	root.go_back_requested.emit()
	await settle()
	check(app.session == null and app.store.saved().mode == "classic", "Back exits the finished lesson without replacing the live run")
	await capture("23-home-saved-board")
	app._resume()
	await settle()
	app._close_modal()
	app.session.board.restore([0,0,0,0,0,0,0,0,0,0,2,2,2,0,0,0,0,0,2,0,0,0,0,0,0,0,0,0,1,1,0,0,3,3,0,0,1,0,0,0,3,0,0,0,4,4,4,0,3,0,0,0,0,0,4,0,0,0,2,2,0,0,0,0])
	app.session.pieces = [app.session.generator.make_piece(5), app.session.generator.make_piece(12), app.session.generator.make_piece(14)]
	app.session.score.total = 1248
	app._refresh_game()
	await capture("06-classic")
	root.go_back_requested.emit()
	root.go_back_requested.emit()
	await settle()
	check(app.modal != null and app.canvas.dragging == -1, "duplicate Android Back deliveries open pause only once and cancel input")
	await capture("07-pause")
	app._close_modal()
	app.set_meta("qa_controlled_focus", false)
	app._notification(Node.NOTIFICATION_APPLICATION_FOCUS_OUT)
	await settle()
	check(app.modal != null and app.canvas.dragging == -1, "real focus-loss policy pauses and checkpoints the game")
	app.set_meta("qa_controlled_focus", true)
	app._close_modal()
	app._close_modal()
	await click(app.find_child("GameHelp", true, false))
	check(app.modal != null and app.modal.find_child("DismissHelp", true, false) != null and app.canvas.dragging == -1, "in-game help stops input while preserving the board")
	await capture("24-game-help")
	app._close_modal()
	app.show_settings("game", "sound")
	app.audio._notification(Node.NOTIFICATION_APPLICATION_FOCUS_IN)
	app.audio.set_meta("qa_controlled_focus", true)
	app._checkpoint()
	var soundscape_runs: Dictionary = app.store.data.runs.duplicate(true)
	for sound in ["forest", "ocean", "rain"]:
		await click(app.page.find_child("Ambience" + sound.capitalize(), true, false))
		check(app.preferences.values.ambience_sound == sound and app.audio.ambience.stream == app.audio.streams["ambience_" + sound], "native soundscape selection: " + sound)
		var saved_sound: SaveManager = SaveManager.new(save_path)
		check(saved_sound.data.settings.ambience_sound == sound and JSON.parse_string(JSON.stringify(saved_sound.data.runs)) == JSON.parse_string(JSON.stringify(soundscape_runs)), "soundscape persists without changing saved boards: " + sound)
		await click(app.page.find_child("PreviewAmbience", true, false))
		check(app.modal != null and app.audio.ambience.playing, "selected soundscape previews: " + sound)
		app._close_modal()
	await click(app.page.find_child("AmbienceOff", true, false))
	await create_timer(0.75).timeout
	check(app.audio.ambience.stream == null and not app.audio.fading_ambience.playing and not app.audio.preview_ambience() and app.preferences.values.ambience > 0, "Off stops both ambience voices and remembers volume")
	await click(app.page.find_child("AmbienceRain", true, false))
	for sound in ["forest", "ocean", "off", "rain", "ocean"]:
		app.preferences.values.ambience_sound = sound
		app.audio.apply_settings()
	app.audio.set_meta("qa_controlled_focus", false)
	app.audio._notification(Node.NOTIFICATION_APPLICATION_FOCUS_OUT)
	check(app.audio.ambience.stream_paused and app.audio.fading_ambience.stream_paused, "backgrounding pauses both sides of a rapid soundscape transition")
	app.audio._notification(Node.NOTIFICATION_APPLICATION_FOCUS_IN)
	app.audio.set_meta("qa_controlled_focus", true)
	await create_timer(0.8).timeout
	check(app.audio.ambience.stream == app.audio.streams.ambience_ocean and not app.audio.fading_ambience.playing, "rapid changes settle on the latest sound with one active voice")
	await click(app.page.find_child("AmbienceRain", true, false))
	await click(app.page.find_child("PreviewAmbience", true, false))
	if app.modal == null or not app.audio.ambience.playing:
		print("Audio UI diagnostic: screen=", app.screen, " modal=", app.modal != null, " playing=", app.audio.ambience.playing, " suspended=", app.audio.suspended, " value=", app.preferences.values.ambience)
	check(app.modal != null and app.audio.ambience.playing, "native ambience preview offers audible playback")
	app._close_modal()
	await capture("08-settings")
	await click(app.page.find_child("SettingsPlay", true, false))
	check(app.settings_section == "play" and app.page.find_child("drag_liftSlider", true, false) != null, "settings tabs expose play configuration")
	await click(app.page.find_child("battery_saver", true, false))
	var height_slider: HSlider = app.page.find_child("drag_liftSlider", true, false)
	height_slider.value = 112
	check(app.preferences.values.drag_lift == 112 and app.preferences.values.battery_saver, "drag height and battery configuration update immediately")
	await capture("20-settings-play")
	await click(app.page.find_child("text_scale", true, false))
	check(app.preferences.values.text_scale == 1.2 and app.screen == "settings", "text scaling safely rebuilds native controls")
	app._leave_settings()
	await settle()
	app._close_modal()
	if Engine.max_fps != 30 or app.store.data.settings.drag_lift != 112:
		print("Settings UI diagnostic: fps=", Engine.max_fps, " lift=", app.store.data.settings.drag_lift, " battery=", app.preferences.values.battery_saver)
	check(Engine.max_fps == 30 and app.store.data.settings.drag_lift == 112, "battery frame budget and control setting survive return to gameplay")
	canvas = app.canvas
	var shape: Array = app.session.pieces[0].cells
	var legal: Vector2i = app.session.board.positions(shape)[0]
	var adjusted_target: Vector2 = canvas.global_position + canvas.board_rect.position + (Vector2(legal) + canvas._extent(shape) / 2) * canvas.cell + Vector2(0, 112)
	await touch(0, canvas.global_position + canvas.tray_rects[0].get_center(), true)
	await drag(0, adjusted_target)
	check(canvas.valid and canvas.origin == legal, "configured lift uses the same target for visual and placement preview")
	canvas.cancel_drag()
	await touch(0, adjusted_target, false)
	check(app.canvas.get_global_rect().end.y < app.size.y - 15, "large text gameplay fits")
	await capture("09-large-text")
	# Narrow / short portrait viewport with larger text.
	root.content_scale_size = Vector2i(400, 800)
	root.size = Vector2i(400, 800)
	await settle()
	check(app.canvas.get_global_rect().end.y <= app.size.y, "short portrait fits")
	await capture("10-short-portrait")
	app.preferences.values.text_scale = 1.0
	app._apply_theme()
	root.content_scale_size = Vector2i(480, 960)
	root.size = Vector2i(480, 960)
	app.show_daily()
	await capture("11-daily")
	app.show_stats()
	await capture("12-statistics")
	app.show_privacy()
	check(app.page.find_child("PrivacySummary", true, false).text == ReleaseInfo.privacy_text(), "release privacy describes actual local storage and reset behavior")
	check(app.page.find_child("ContactSupport", true, false) != null and "info@madebykreativ.com" in ReleaseInfo.privacy_text(), "privacy provides the confirmed publisher contact")
	check(app.page.find_child("PrivacyPolicyLink", true, false) == null or str(ReleaseInfo.details().privacy_policy_url).begins_with("https://"), "no unconfigured hosted policy link is exposed")
	await capture("13-privacy")
	app.show_settings("game", "about")
	await capture("36-about-navigation")
	check(app.page.find_child("DiscoverPro", true, false) == null, "internal release has no unavailable purchase promotion")
	app.show_pro("settings")
	check(app.screen == "settings" and app.settings_section == "about", "legacy PRO navigation safely returns to About")
	app.show_privacy()
	root.go_back_requested.emit()
	await settle()
	check(app.screen == "settings" and app.settings_section == "about", "Android Back returns to the actual parent page")
	app.show_modes()
	await capture("14-modes")
	app.start_game("zen")
	await settle()
	app._close_modal()
	app.session.board.cells.fill(0)
	await capture("15-zen")
	app.start_game("shift")
	await settle()
	app._close_modal()
	check(app.shift_button.disabled and app.shift_button.text == "Charging" and "Clear 4 more lines" in app.energy_label.text, "empty meter explains when SHIFT unlocks")
	app.session.pieces = [app.session.generator.make_piece(0), {}, {}]
	app._place(0, Vector2i(0, 0))
	check(app.session.shift.energy == 0 and app.shift_button.disabled, "placing blocks alone does not charge SHIFT")
	app.session.board.cells.fill(0)
	for x in 7: app.session.board.cells[x] = 1
	app.session.pieces = [app.session.generator.make_piece(0), {}, {}]
	app._place(0, Vector2i(7, 0))
	check(app.shift_button.disabled and "25% charged" in app.energy_label.text and "3 more lines" in app.energy_label.text, "line clear updates remaining charge instructions")
	app.session.shift.earn(3)
	app._refresh_game()
	check(not app.shift_button.disabled and app.shift_button.text == "Use SHIFT" and "SHIFT READY" in app.energy_label.text, "full meter offers an explicit available action")
	await capture("17-shift-charge-ready")
	app.preferences.values.text_scale = 1.2
	app._apply_theme()
	root.content_scale_size = Vector2i(400, 800)
	root.size = Vector2i(400, 800)
	app.show_game()
	await settle()
	check(app.shift_button.get_global_rect().end.x <= app.size.x and app.page.get_global_rect().end.y <= app.size.y, "SHIFT meter fits short portrait with larger text")
	await capture("18-shift-large-text")
	app._toggle_shift()
	app._toggle_axis()
	await settle()
	check(app.page.get_global_rect().end.y <= app.size.y and app.shift_positive_button.get_global_rect().end.x <= app.size.x, "armed column controls fit short portrait with larger text")
	check(app.canvas.tray_rects[0].end.y <= app.canvas.size.y, "short armed layout keeps the complete tray visible")
	await capture("22-shift-compact-controls")
	app._toggle_shift()
	app.preferences.values.text_scale = 1.0
	app._apply_theme()
	root.content_scale_size = Vector2i(480, 960)
	root.size = Vector2i(480, 960)
	app.show_game()
	await settle()
	app._toggle_shift()
	await settle()
	check(app.shift_axis_button.text == "Columns ↕" and app.shift_negative_button.text == "↑ Up" and app.shift_positive_button.text == "Down ↓", "SHIFT controls name their axis and directions")
	await capture("21-shift-controls")
	app._toggle_shift()
	var saved_run: Dictionary = app.store.saved().duplicate(true)
	app.show_settings("game", "play")
	await click(app.page.find_child("RestoreSettings", true, false))
	check(app.modal != null and app.preferences.values.drag_lift == 112, "default restoration requires its explicit action")
	await click(app.modal.find_child("ConfirmRestoreSettings", true, false))
	check(app.preferences.values.drag_lift == 88 and not app.preferences.values.battery_saver and app.preferences.values.tutorial_done and app.store.saved().board == saved_run.board, "restore settings preserves the saved board and tutorial")
	app._leave_settings()
	await settle()
	app.session.over = true
	app.show_game_over()
	await capture("16-game-over")
	# Debug fixtures are usable in the editor and never replace the player's run.
	var stored_run: Dictionary = app.store.saved().duplicate(true)
	var debug_menu: GDScript = load("res://scripts/debug/debug_menu.gd")
	debug_menu.load_preset(app, "SHIFT wrap")
	await settle()
	app._close_modal()
	app.armed = true
	app._shift("row", 3, 1)
	check(app.session.board.occupied() == 0 and app.store.saved() == stored_run, "QA fixture SHIFT works without changing player data")
	app.preferences.values.reduce_motion = true
	app._feedback({"shifted": false, "lines": 0, "points": 0, "zen_reset": false, "cleared": [0, 1]})
	check(not app.canvas.is_processing() and not app.canvas.effects.active() and app.canvas.effects.ghosts.is_empty(), "reduce motion bypasses visual animation")
	app.show_home()
	app.preferences.values.tutorial_done = false
	await settle()
	await click(app.find_child("PlayShift", true, false))
	if not app.tutorial_active: print("Navigation diagnostic: screen=", app.screen, " mode=", app.session.mode if app.session != null else "none", " modal=", app.modal != null)
	check(app.tutorial_active and app.screen == "game", "native button safely navigates after input dispatch")
	# A completed lesson can open another mode without replacing Classic.
	app.start_game("classic")
	var preserved_run: Dictionary = app.store.saved("classic").duplicate(true)
	app.start_tutorial()
	app.tutorial_step = 3
	app.preferences.values.tutorial_done = true
	app._show_tutorial_complete()
	app._toast("Lesson complete")
	app._request_tutorial_finish()
	app._process(3.1)
	check(app.screen == "game" and app.session != null and app.session.mode == "shift" and app.store.saved("classic") == preserved_run, "lesson Play SHIFT leaves a valid game and preserves the other mode")
	app.show_home()
	var shift_saved: Dictionary = app.store.saved("shift").duplicate(true)
	app._request_start("classic", true)
	app._process(3.1)
	check(app.screen == "home" and app.modal != null and app.store.saved("classic") == preserved_run, "explicit new-game confirmation keeps the existing mode until confirmed")
	app._close_modal()
	app._request_start("classic")
	await settle()
	check(app.screen == "game" and app.session.mode == "classic" and app.session.board.cells == preserved_run.board and app.store.saved("shift") == shift_saved, "mode switch resumes Classic and preserves SHIFT")
	app.show_home()
	app._request_start("zen")
	await settle()
	check(app.session.mode == "zen" and app.store.saved("classic").board == preserved_run.board and app.store.saved("shift") == shift_saved, "starting Zen preserves both other mode slots")
	app.show_home()
	app.show_modes()
	await capture("38-mode-saves")
	check(app.page.find_child("NewClassic", true, false) != null and app.page.find_child("ModeShift", true, false).text.begins_with("Continue"), "mode cards expose independent Continue and New game actions")
	app._request_start("classic", true)
	await click(app.modal.find_child("ConfirmNewGame", true, false))
	if app.session.mode != "classic" or app.session.board.occupied() != 0 or app.store.saved("shift") != shift_saved:
		print("New game diagnostic: mode=", app.session.mode, " occupied=", app.session.board.occupied(), " shift intact=", app.store.saved("shift") == shift_saved, " modal=", app.modal != null)
	check(app.session.mode == "classic" and app.session.board.occupied() == 0 and app.store.saved("shift") == shift_saved and not app.store.saved("zen").is_empty(), "confirmed New Classic changes only its own slot")
	app.show_home()
	var old_daily: GameSession = GameSession.new("daily", 1, "2026-10-15")
	app.store.start(old_daily)
	app.show_daily()
	check(app.page.get_child_count() > 0 and app._has_saved("daily"), "older Daily remains available alongside today's challenge")
	app._request_start("daily")
	check(app.modal != null and app.store.saved("daily").date == old_daily.date, "today's new Daily asks before replacing an unfinished older Daily")
	app._close_modal()
	app._resume("daily")
	check(app.session.date == old_daily.date and app.session.mode == "daily", "Continue Daily preserves its locked date")
	# Blocked board: automatic rescue suggestion is a preview, never a spent charge.
	app.start_game("shift")
	for y in 8:
		for x in 8: app.session.board.cells[y * 8 + x] = 1 if (x + y) % 2 == 0 else 0
	app.session.pieces = [app.session.generator.make_piece(1), {}, {}]
	app.session.shift.energy = app.session.shift.capacity()
	app.session.update_status()
	app._refresh_game()
	check(not app.session.over and app.status_label.text.contains("open space"), "blocked board explains its charged rescue")
	var rescue_before: Dictionary = app.session.snapshot()
	app._toggle_shift()
	check(app.shift_preview.allowed and app.session.snapshot() == rescue_before and app.shift_button.text == "Apply SHIFT", "rescue suggestion previews a useful move without changing the game")
	await capture("39-rescue-preview")
	app._cancel_shift()
	check(app.session.board.cells == rescue_before.board and app.session.shift.energy == rescue_before.energy and app.session.score.data() == rescue_before.score and app.session.moves == rescue_before.moves and not app.armed, "Cancel keeps the charged blocked game intact")
	app._toggle_shift()
	await click(app.shift_button)
	check(not app.session.over and app.session.board.has_move(app.session.pieces) and app.session.shift.energy == 0, "Apply rescue enables an actual placement and consumes one charge")
	app.start_game("shift")
	app.session.board.cells.fill(1)
	for i in 8: app.session.board.cells[i * 8 + i] = 0
	for y in 3:
		for x in 3: app.session.board.cells[y * 8 + x] = 0
	app.session.pieces = [app.session.generator.make_piece(19), {}, {}]
	app.session.shift.energy = app.session.shift.capacity()
	app.session.update_status()
	app._toggle_shift()
	app._preview_shift("row", 1, 1)
	check(app.shift_preview.allowed and app.status_label.text.contains("end the run") and app.session.shift.ready() and not app.session.over, "risky SHIFT preview clearly warns before a run-ending move")
	await capture("40-shift-risk-preview")
	app._cancel_shift()
	app.show_home()
	app.session = null
	app.queue_free()
	await settle()
	for suffix in ["", ".tmp", ".bak"]: DirAccess.remove_absolute(save_path + suffix)
	print("UI: %d assertions; %d failures" % [assertions, failures])
	quit(0 if failures == 0 else 1)
