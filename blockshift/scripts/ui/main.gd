extends Control
## Navigation and presentation. Gameplay transactions live in GameSession.
var store: SaveManager
var preferences: SettingsManager = SettingsManager.new()
var commerce: MonetizationManager = MonetizationManager.new()
var audio: AudioManager
var session: GameSession
var safe: MarginContainer
var page_host: MarginContainer
var save_notice: PanelContainer
var save_notice_label: Label
var save_retry_button: Button
var save_retry_elapsed: float = 0.0
var page: VBoxContainer
var modal: Control
var modal_back: Callable
var canvas: GameCanvas
var score_label: Label
var best_label: Label
var status_label: Label
var energy_label: Label
var energy_bar: ProgressBar
var shift_button: Button
var shift_controls: HBoxContainer
var shift_axis_button: Button
var shift_negative_button: Button
var shift_cancel_button: Button
var shift_direction: int = 0
var shift_preview: Dictionary = {}
var shift_positive_button: Button
var game_footer: Label
var screen: String = "splash"
var qa_active: bool = false
var tutorial_step: int = 0
var tutorial_active: bool = false
var autosave: float = 0.0
var previous_best: int = 0
var toast_timer: float = 0.0
var selected_axis: String = "row"
var selected_index: int = 3
var armed: bool = false
var settings_return: String = "home"
var settings_section: String = "play"
var page_back: Callable
var last_back_at: int = -1000
var score_tween: Tween
var tutorial_finish_wait: float = 0.0
var tutorial_lines: int = 0
var tutorial_charge_goal: int = 4
var tutorial_stage_wait: float = 0.0
var result_wait: float = 0.0
var transfer_dialog_open: bool = false
var pending_transfer: Dictionary = {}

func _ready() -> void:
	get_tree().auto_accept_quit = false
	get_tree().quit_on_go_back = false
	get_tree().root.go_back_requested.connect(_back)
	Engine.max_fps = 30
	OS.low_processor_usage_mode = false # Android must advance timers without waiting for input.
	store = SaveManager.new(str(get_meta("qa_save_path")) if OS.is_debug_build() and has_meta("qa_save_path") else "user://progress.json")
	preferences.restore(store.data.settings)
	_apply_theme()
	audio = AudioManager.new()
	audio.set_meta("qa_controlled_focus", OS.is_debug_build() and has_meta("qa_save_path") and bool(get_meta("qa_controlled_focus", false)))
	add_child(audio)
	audio.setup(preferences)
	var background: Atmosphere = Atmosphere.new()
	background.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	background.mouse_filter = Control.MOUSE_FILTER_IGNORE
	add_child(background)
	safe = MarginContainer.new()
	safe.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	add_child(safe)
	var shell: VBoxContainer = VBoxContainer.new()
	shell.add_theme_constant_override("separation", 8)
	safe.add_child(shell)
	page_host = MarginContainer.new()
	page_host.size_flags_vertical = Control.SIZE_EXPAND_FILL
	shell.add_child(page_host)
	save_notice = PanelContainer.new()
	save_notice.name = "SaveNotice"
	var notice_style: StyleBoxFlat = ThemeManager.panel(Color("332333"), 12, Color("efba70"))
	notice_style.content_margin_left = 10
	notice_style.content_margin_right = 10
	notice_style.content_margin_top = 6
	notice_style.content_margin_bottom = 6
	save_notice.add_theme_stylebox_override("panel", notice_style)
	shell.add_child(save_notice)
	save_notice.resized.connect(_layout_modal_save_notice)
	var notice_row: HBoxContainer = HBoxContainer.new()
	notice_row.add_theme_constant_override("separation", 8)
	save_notice.add_child(notice_row)
	save_notice_label = _label("", 12, Color("efba70"), HORIZONTAL_ALIGNMENT_LEFT, notice_row)
	save_retry_button = _button("Retry save", _checkpoint, false, notice_row)
	save_retry_button.name = "RetrySave"
	save_retry_button.size_flags_horizontal = Control.SIZE_SHRINK_END
	save_retry_button.add_theme_font_size_override("font_size", 12)
	_update_save_notice()
	resized.connect(_update_safe_area)
	_update_safe_area()
	_new_page(false)
	page.add_spacer(false)
	_label("B L O C K S H I F T", 30, ThemeManager.INK, HORIZONTAL_ALIGNMENT_CENTER)
	_label("M A D E  B Y  K R E A T I V", 12, ThemeManager.MUTED, HORIZONTAL_ALIGNMENT_CENTER)
	page.add_spacer(false)
	await get_tree().create_timer(0.45).timeout
	if screen == "splash":
		show_home()

func _apply_theme() -> void:
	var ui: Theme = Theme.new()
	var font: FontVariation = FontVariation.new()
	font.base_font = load("res://assets/fonts/Manrope.ttf")
	font.variation_opentype = {TextServerManager.get_primary_interface().name_to_tag("wght"): 550.0}
	ui.default_font = font
	ui.default_font_size = int(16 * float(preferences.values.text_scale))
	ui.set_color("font_color", "Label", ThemeManager.INK)
	ui.set_color("font_color", "Button", ThemeManager.INK)
	ui.set_color("font_hover_color", "Button", ThemeManager.INK)
	ui.set_color("font_pressed_color", "Button", ThemeManager.INK)
	ui.set_color("font_focus_color", "Button", ThemeManager.INK)
	ui.set_color("font_disabled_color", "Button", ThemeManager.MUTED)
	ui.set_stylebox("normal", "Button", ThemeManager.panel())
	ui.set_stylebox("hover", "Button", ThemeManager.panel(Color("1b2c45")))
	ui.set_stylebox("pressed", "Button", ThemeManager.panel(Color("253c54")))
	ui.set_stylebox("disabled", "Button", ThemeManager.panel(Color("111b2a")))
	ui.set_stylebox("focus", "Button", ThemeManager.panel(Color(0, 0, 0, 0), 18, Color("55ded2")))
	for state in ["normal", "hover", "pressed", "focus"]:
		var toggle_style: StyleBoxFlat = ThemeManager.panel(Color("1b2c45") if state in ["hover", "pressed"] else Color.TRANSPARENT, 12, Color("55ded2") if state == "focus" else Color.TRANSPARENT)
		toggle_style.content_margin_left = 12
		toggle_style.content_margin_right = 12
		toggle_style.content_margin_top = 8
		toggle_style.content_margin_bottom = 8
		ui.set_stylebox(state, "CheckButton", toggle_style)
	ui.set_icon("checked", "CheckButton", load("res://assets/icons/toggle_on.svg"))
	ui.set_icon("unchecked", "CheckButton", load("res://assets/icons/toggle_off.svg"))
	for pair in [["background", Color("23334c")], ["fill", Color("55ded2")]]:
		var bar_style: StyleBoxFlat = ThemeManager.panel(pair[1], 4)
		bar_style.content_margin_top = 0
		bar_style.content_margin_bottom = 0
		ui.set_stylebox(pair[0], "ProgressBar", bar_style)
	var slider_track: StyleBoxFlat = ThemeManager.panel(Color("273c55"), 3)
	slider_track.content_margin_top = 3
	slider_track.content_margin_bottom = 3
	ui.set_stylebox("slider", "HSlider", slider_track)
	var slider_fill: StyleBoxFlat = ThemeManager.panel(Color("55ded2"), 3)
	slider_fill.content_margin_top = 3
	slider_fill.content_margin_bottom = 3
	ui.set_stylebox("grabber_area", "HSlider", slider_fill)
	theme = ui

func _update_safe_area() -> void:
	if safe == null:
		return
	var top: int = 20
	var bottom: int = 18
	var left: int = 24
	var right: int = 24
	if OS.has_feature("android") or OS.has_feature("ios"):
		var area: Rect2i = DisplayServer.get_display_safe_area()
		var display: Vector2i = DisplayServer.screen_get_size()
		var factor: float = size.x / maxf(1.0, display.x)
		top = maxi(top, int(area.position.y * factor) + 8)
		bottom = maxi(bottom, int((display.y - area.end.y) * factor) + 8)
		left = maxi(left, int(area.position.x * factor) + 8)
		right = maxi(right, int((display.x - area.end.x) * factor) + 8)
	for pair in [["left", left], ["right", right], ["top", top], ["bottom", bottom]]:
		safe.add_theme_constant_override("margin_" + pair[0], pair[1])

func _new_page(scroll: bool = true) -> void:
	result_wait = 0
	if score_tween != null and score_tween.is_valid(): score_tween.kill()
	page_back = show_home
	Engine.max_fps = preferences.game_fps() if screen == "game" else 30
	_close_modal()
	if canvas != null and is_instance_valid(canvas):
		canvas.cancel_drag()
	canvas = null
	for child in page_host.get_children():
		child.hide()
		child.queue_free() # Keep event targets in-tree until Godot finishes dispatch.
	page = VBoxContainer.new()
	page.add_theme_constant_override("separation", 12)
	page.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	if scroll:
		var scroller: ScrollContainer = ScrollContainer.new()
		scroller.horizontal_scroll_mode = ScrollContainer.SCROLL_MODE_DISABLED
		scroller.size_flags_vertical = Control.SIZE_EXPAND_FILL
		page_host.add_child(scroller)
		scroller.add_child(page)
	else:
		page_host.add_child(page)
	if not bool(preferences.values.reduce_motion):
		page.modulate.a = 0
		page.create_tween().tween_property(page, "modulate:a", 1.0, float(VisualConfig.value("page_fade_seconds")))

func _label(text: String, font_size: int = 16, color: Color = ThemeManager.INK, alignment: HorizontalAlignment = HORIZONTAL_ALIGNMENT_LEFT, parent: Node = null) -> Label:
	var label: Label = Label.new()
	label.text = text
	label.add_theme_font_size_override("font_size", int(font_size * float(preferences.values.text_scale)))
	label.add_theme_color_override("font_color", color)
	label.horizontal_alignment = alignment
	label.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	label.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	(parent if parent != null else page).add_child(label)
	return label

func _button(text: String, action: Callable, primary: bool = false, parent: Node = null) -> Button:
	var button: Button = Button.new()
	button.text = text
	button.custom_minimum_size.y = 54
	button.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	button.mouse_default_cursor_shape = Control.CURSOR_POINTING_HAND
	if primary:
		button.add_theme_stylebox_override("normal", ThemeManager.primary_surface())
		button.add_theme_stylebox_override("hover", ThemeManager.panel(Color("89eee4")))
		button.add_theme_stylebox_override("pressed", ThemeManager.panel(Color("3cb5ad")))
		for state in ["font_color", "font_hover_color", "font_pressed_color", "font_focus_color"]:
			button.add_theme_color_override(state, Color("081a26"))
	button.pressed.connect(func() -> void:
		audio.play_event("menu")
		action.call_deferred())
	(parent if parent != null else page).add_child(button)
	return button

func _row(parent: Node = null) -> HBoxContainer:
	var row: HBoxContainer = HBoxContainer.new()
	row.add_theme_constant_override("separation", 10)
	(parent if parent != null else page).add_child(row)
	return row

func _card(parent: Node = null) -> VBoxContainer:
	var panel: PanelContainer = PanelContainer.new()
	panel.add_theme_stylebox_override("panel", ThemeManager.panel())
	(parent if parent != null else page).add_child(panel)
	var box: VBoxContainer = VBoxContainer.new()
	box.add_theme_constant_override("separation", 8)
	panel.add_child(box)
	return box

func _header(title: String, back: Callable = show_home) -> void:
	page_back = back
	var row: HBoxContainer = _row()
	var button: Button = _button("‹", back, false, row)
	button.size_flags_horizontal = Control.SIZE_SHRINK_BEGIN
	button.custom_minimum_size.x = 54
	button.tooltip_text = "Back"
	_label(title, 23, ThemeManager.INK, HORIZONTAL_ALIGNMENT_LEFT, row)

func show_home() -> void:
	_checkpoint()
	screen = "home"
	if tutorial_active or qa_active:
		session = null
	tutorial_active = false
	qa_active = false
	_new_page()
	var top: HBoxContainer = _row()
	_label("K /", 20, Color("55ded2"), HORIZONTAL_ALIGNMENT_LEFT, top)
	_label("MADE BY KREATIV", 10, ThemeManager.MUTED, HORIZONTAL_ALIGNMENT_RIGHT, top)
	var hero: TextureRect = TextureRect.new()
	hero.texture = load("res://assets/icons/hero.svg")
	hero.custom_minimum_size.y = 104
	hero.expand_mode = TextureRect.EXPAND_IGNORE_SIZE
	hero.stretch_mode = TextureRect.STRETCH_KEEP_ASPECT_CENTERED
	page.add_child(hero)
	_label("BLOCKSHIFT", 38)
	_label("A little space. A new possibility.", 15, ThemeManager.MUTED)
	_space(2)
	var saved_session: GameSession = GameSession.from_snapshot(store.saved())
	var has_run: bool = saved_session != null and not saved_session.over
	if has_run:
		var resume_box: VBoxContainer = _card()
		_label("CONTINUE YOUR LAST GAME", 10, Color("55ded2"), HORIZONTAL_ALIGNMENT_LEFT, resume_box)
		_label("%s · %d %s · %d %s" % [GameModes.config(saved_session.mode).title, saved_session.score.total, "point" if saved_session.score.total == 1 else "points", saved_session.score.lines, "line" if saved_session.score.lines == 1 else "lines"], 15, ThemeManager.INK, HORIZONTAL_ALIGNMENT_LEFT, resume_box)
		_button("Continue playing  →", func() -> void: _resume(), true, resume_box).name = "Continue"
	_button("Continue SHIFT  →" if _has_saved("shift") else "Play SHIFT    →", func() -> void: _request_start("shift"), not has_run).name = "PlayShift"
	var quick: HBoxContainer = _row()
	_button("Classic", func() -> void: _request_start("classic"), false, quick)
	_button("Zen", func() -> void: _request_start("zen"), false, quick)
	var daily_card: VBoxContainer = _card()
	_label("DAILY ORBIT    /    " + DailyChallenge.today(), 11, Color("efba70"), HORIZONTAL_ALIGNMENT_LEFT, daily_card)
	var completed_today: bool = bool(store.data.daily.get(DailyChallenge.today(), {}).get("completed", false))
	_label("✓ Today's orbit complete" if completed_today else "Clear %d lines. A fresh goal each day." % DailyChallenge.target(), 16, ThemeManager.INK, HORIZONTAL_ALIGNMENT_LEFT, daily_card)
	_button("View daily challenge  →", show_daily, false, daily_card)
	var links: HBoxContainer = _row()
	_button("Modes", show_modes, false, links)
	_button("Stats", show_stats, false, links)
	_button("Settings", func() -> void: show_settings("home"), false, links)
	_label("EACH MODE KEEPS ITS BOARD · OFFLINE", 10, ThemeManager.MUTED, HORIZONTAL_ALIGNMENT_CENTER)
	if store.last_error.is_empty() and store.recovered:
		_label("Your progress was recovered from a backup.", 13, Color("55ded2"))

func _space(height: float) -> void:
	var spacer: Control = Control.new()
	spacer.custom_minimum_size.y = height
	page.add_child(spacer)

func show_modes() -> void:
	screen = "modes"
	_new_page()
	_header("Find your rhythm")
	_label("Each mode keeps its own board. Switch freely.", 16, ThemeManager.MUTED)
	for mode in GameModes.IDS:
		var box: VBoxContainer = _card()
		_label(str(GameModes.config(mode).title), 27, ThemeManager.INK, HORIZONTAL_ALIGNMENT_LEFT, box)
		_label(str(GameModes.config(mode).subtitle), 15, ThemeManager.MUTED, HORIZONTAL_ALIGNMENT_LEFT, box)
		var description: String = {"classic": "Place pieces. Clear full rows or columns. No timer.", "shift": "Every %d full line clears earn one optional board move." % ShiftSystem.new().lines_remaining(), "zen": "When no piece fits, the board renews and your score continues.", "daily": "Clear %d lines from today's shared starting deal." % DailyChallenge.target()}[mode]
		_label(description, 14, ThemeManager.MUTED, HORIZONTAL_ALIGNMENT_LEFT, box)
		_label("BEST  %d" % int(store.stats(mode).best), 11, Color("efba70"), HORIZONTAL_ALIGNMENT_LEFT, box)
		var saved_game: GameSession = GameSession.from_snapshot(store.saved(mode))
		if saved_game != null and not saved_game.over:
			_label("SAVED · %d points%s" % [saved_game.score.total, " · " + saved_game.date if mode == "daily" else ""], 12, Color("55ded2"), HORIZONTAL_ALIGNMENT_LEFT, box)
		_button(("Continue  →" if _has_saved(mode) else "Play  →") if mode != "daily" else "View challenge  →", func() -> void:
			if mode == "daily": show_daily()
			else: _request_start(mode), false, box).name = "Mode" + mode.capitalize()
		if mode != "daily" and _has_saved(mode):
			_button("New game", func() -> void: _request_start(mode, true), false, box).name = "New" + mode.capitalize()

func _has_saved(mode: String) -> bool:
	var saved_game: GameSession = GameSession.from_snapshot(store.saved(mode))
	return saved_game != null and not saved_game.over

func _request_start(mode: String, fresh: bool = false) -> void:
	_checkpoint()
	var existing: GameSession = GameSession.from_snapshot(store.saved(mode))
	if existing != null and not existing.over:
		if not fresh and (mode != "daily" or existing.date == DailyChallenge.today()):
			_resume(mode)
			return
		var box: VBoxContainer = _sheet("Start a new %s game?" % GameModes.config(mode).title, "Only your %s board will be replaced. Other modes, high scores and statistics are kept." % GameModes.config(mode).title)
		_button("Start new game", func() -> void: start_game(mode), true, box).name = "ConfirmNewGame"
		_button("Keep saved board", _close_modal, false, box).name = "KeepSavedGame"
		return
	if mode == "shift" and not bool(preferences.values.tutorial_done):
		start_tutorial()
	else:
		start_game(mode)

func start_game(mode: String) -> void:
	_checkpoint()
	tutorial_active = false
	qa_active = false
	session = GameSession.new(mode, int(Time.get_unix_time_from_system()) ^ Time.get_ticks_usec())
	previous_best = int(store.stats(mode).best)
	_save_result(store.start(session))
	armed = false
	selected_axis = "row"
	selected_index = 3
	show_game()

func _resume(mode: String = "") -> void:
	_checkpoint()
	var id: String = str(store.data.last_mode) if mode.is_empty() else mode
	session = GameSession.from_snapshot(store.saved(id))
	if session == null:
		store.clear_run(id)
		show_home()
		var box: VBoxContainer = _sheet("A fresh start", "This mode's interrupted board could not be restored. Other boards and high scores are safe.")
		_button("Continue", _close_modal, true, box)
		return
	tutorial_active = false
	qa_active = false
	store.data.last_mode = id
	_save_result(store.write())
	previous_best = int(store.stats(session.mode).best)
	armed = false
	shift_preview.clear()
	shift_direction = 0
	show_game()

func show_game() -> void:
	screen = "game"
	_new_page(false)
	page.add_theme_constant_override("separation", 8)
	var header: HBoxContainer = _row()
	var home: Button = _button("?", show_game_help, false, header)
	home.size_flags_horizontal = Control.SIZE_SHRINK_BEGIN
	home.custom_minimum_size.x = 52
	home.tooltip_text = "How to play"
	home.name = "GameHelp"
	_label("LEARN TO SHIFT" if tutorial_active else str(GameModes.config(session.mode).title).to_upper(), 14, ThemeManager.INK, HORIZONTAL_ALIGNMENT_CENTER, header)
	var pause: Button = _button("Ⅱ", pause_game, false, header)
	pause.custom_minimum_size.x = 52
	pause.size_flags_horizontal = Control.SIZE_SHRINK_END
	pause.tooltip_text = "Pause game"
	pause.name = "Pause"
	var score_row: HBoxContainer = _row()
	var score_box: VBoxContainer = VBoxContainer.new()
	score_box.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	score_row.add_child(score_box)
	_label("SCORE", 10, ThemeManager.MUTED, HORIZONTAL_ALIGNMENT_LEFT, score_box)
	score_label = _label(str(session.score.total), 42, ThemeManager.INK, HORIZONTAL_ALIGNMENT_LEFT, score_box)
	score_label.name = "Score"
	var best_box: VBoxContainer = VBoxContainer.new()
	best_box.custom_minimum_size.x = 145 * float(preferences.values.text_scale)
	score_row.add_child(best_box)
	_label("PERSONAL BEST", 10, ThemeManager.MUTED, HORIZONTAL_ALIGNMENT_RIGHT, best_box)
	best_label = _label(str(store.stats(session.mode).best), 24, Color("efba70"), HORIZONTAL_ALIGNMENT_RIGHT, best_box)
	status_label = _label("", 13, ThemeManager.MUTED, HORIZONTAL_ALIGNMENT_CENTER)
	status_label.custom_minimum_size.y = 36
	canvas = GameCanvas.new()
	canvas.name = "GameCanvas"
	canvas.session = session
	canvas.preferences = preferences
	canvas.size_flags_vertical = Control.SIZE_EXPAND_FILL
	canvas.custom_minimum_size.y = 256 if not store.last_error.is_empty() else 334
	page.add_child(canvas)
	canvas.piece_picked.connect(func() -> void: audio.play_event("pickup"); audio.haptic(8))
	canvas.placement_requested.connect(_place)
	canvas.invalid_drop.connect(func() -> void: audio.play_event("invalid"); _toast("Find a little more room for this piece."))
	canvas.line_selected.connect(func(axis: String, index: int) -> void:
		selected_axis = axis
		selected_index = index
		if shift_direction != 0: _preview_shift(axis, index, shift_direction)
		else: _refresh_game())
	canvas.shift_swiped.connect(_preview_shift)
	if bool(GameModes.config(session.mode).shift):
		var energy_row: HBoxContainer = _row()
		var meter: VBoxContainer = VBoxContainer.new()
		meter.size_flags_horizontal = Control.SIZE_EXPAND_FILL
		energy_row.add_child(meter)
		energy_label = _label("", 12, Color("55ded2"), HORIZONTAL_ALIGNMENT_LEFT, meter)
		energy_bar = ProgressBar.new()
		energy_bar.custom_minimum_size.y = 7
		energy_bar.show_percentage = false
		energy_bar.max_value = session.shift.capacity()
		meter.add_child(energy_bar)
		shift_button = _button("SHIFT", _shift_button_pressed, true, energy_row)
		shift_button.size_flags_horizontal = Control.SIZE_SHRINK_END
		shift_button.custom_minimum_size.x = 132
		shift_button.add_theme_font_size_override("font_size", int(14 * float(preferences.values.text_scale)))
		shift_button.name = "Shift"
		shift_controls = _row()
		shift_axis_button = _button("", _toggle_axis, false, shift_controls)
		shift_axis_button.name = "ShiftAxis"
		shift_axis_button.tooltip_text = "Switch between rows and columns"
		shift_negative_button = _button("", func() -> void: _preview_shift(selected_axis, selected_index, -1), false, shift_controls)
		shift_positive_button = _button("", func() -> void: _preview_shift(selected_axis, selected_index, 1), false, shift_controls)
		shift_cancel_button = _button("Cancel", _cancel_shift, false, shift_controls)
		shift_cancel_button.name = "CancelShift"
		for control in [shift_axis_button, shift_negative_button, shift_positive_button, shift_cancel_button]:
			control.add_theme_font_size_override("font_size", int(13 * float(preferences.values.text_scale)))
			for state in ["normal", "hover", "pressed", "disabled", "focus"]:
				var compact: StyleBoxFlat = ThemeManager.panel(Color.TRANSPARENT if state == "focus" else ThemeManager.SURFACE, 18, Color("55ded2") if state == "focus" else Color.TRANSPARENT)
				compact.content_margin_left = 6
				compact.content_margin_right = 6
				control.add_theme_stylebox_override(state, compact)
	game_footer = _label("Charge builds across turns · SHIFT is optional" if bool(GameModes.config(session.mode).shift) else "Drag a piece · Fill a row or column · Make space", 11, ThemeManager.MUTED, HORIZONTAL_ALIGNMENT_CENTER)
	_refresh_game()
	if session.over:
		show_game_over()

func _refresh_game() -> void:
	if screen != "game" or canvas == null or session == null:
		return
	score_label.text = str(session.score.total)
	var number_size: float = minf(42 * float(preferences.values.text_scale), size.x * 0.37 / (maxi(1, score_label.text.length()) * 0.62))
	score_label.add_theme_font_size_override("font_size", int(maxf(18, number_size)))
	best_label.text = str(maxi(int(store.stats(session.mode).best), session.score.total))
	game_footer.visible = not armed
	canvas.shift_armed = armed
	canvas.shift_axis = selected_axis
	canvas.selected_line = selected_index
	canvas.shift_preview = shift_preview if armed else {}
	canvas.lesson_targets.clear()
	if tutorial_active and tutorial_step == 1 and tutorial_stage_wait <= 0:
		var target_row: int = 7 - tutorial_lines % 4
		canvas.lesson_targets.assign([target_row * 8 + 6, target_row * 8 + 7])
	canvas.queue_redraw()
	if bool(GameModes.config(session.mode).shift):
		energy_bar.value = session.shift.energy
		var remaining: int = session.shift.lines_remaining()
		energy_label.text = "SHIFT READY\nTap to move one row or column" if session.shift.ready() else "SHIFT · %d%% charged\nClear %d more %s to unlock" % [roundi(100.0 * session.shift.energy / session.shift.capacity()), remaining, "line" if remaining == 1 else "lines"]
		if armed:
			energy_label.text = "PREVIEW · No charge spent\nApply when you like the result" if not shift_preview.is_empty() else "1 · Choose a line\n2 · Tap a direction to preview"
		shift_button.disabled = not session.shift.ready() or session.over or (tutorial_active and tutorial_step != 2) or (armed and not bool(shift_preview.get("allowed", false)))
		shift_button.text = "Apply SHIFT" if armed else ("Use SHIFT" if session.shift.ready() else "Charging")
		shift_controls.visible = armed
		shift_axis_button.text = "Rows ↔" if selected_axis == "row" else "Columns ↕"
		shift_negative_button.text = "← Left" if selected_axis == "row" else "↑ Up"
		shift_positive_button.text = "Right →" if selected_axis == "row" else "Down ↓"
	if tutorial_active:
		if tutorial_step == 1:
			status_label.text = "CHARGE %d / %d · %s" % [tutorial_lines, tutorial_charge_goal, "SHIFT is ready! Your charge stays until you use it." if tutorial_stage_wait > 0 else ("Drag the pair to the outlined gaps in row %d. Only full lines earn charge." % (8 - tutorial_lines % 4) if tutorial_lines == 0 else "Charge kept between turns. Clear the outlined gaps in row %d." % (8 - tutorial_lines % 4))]
		else:
			status_label.text = "MOVE · Use SHIFT → row 4 → Right → Apply. Preview is free; edge blocks wrap." if tutorial_step == 2 else "The block wrapped and cleared the column. SHIFT makes room for your next piece."
		if tutorial_step == 2 and not shift_preview.is_empty(): status_label.text = _preview_description()
	elif armed:
		status_label.text = _preview_description() if not shift_preview.is_empty() else "%s %d selected · Tap a direction to preview" % [selected_axis.capitalize(), selected_index + 1]
	elif not session.rescue_options.is_empty():
		status_label.text = "No piece fits yet. Your charged SHIFT can open space. Tap Use SHIFT."
	elif session.mode == "daily":
		status_label.text = "%s   ·   %d / %d lines" % [session.date, session.score.lines, DailyChallenge.target()]
	elif session.mode == "zen":
		status_label.text = "Breathe. If the board fills, a fresh one unfolds."
	elif session.mode == "shift":
		status_label.text = "SHIFT is ready whenever you need it." if session.shift.ready() else "Clear full rows or columns to charge SHIFT."
	else:
		status_label.text = "Three pieces. One possibility at a time."

func _place(slot: int, origin: Vector2i) -> void:
	if modal != null:
		return
	if tutorial_active and (tutorial_step == 3 or tutorial_stage_wait > 0):
		return
	if tutorial_active and (tutorial_step != 1 or slot != 0 or origin != Vector2i(6, 7 - tutorial_lines % 4)):
		_toast("Drag the pair to the two outlined gaps in row %d." % (8 - tutorial_lines % 4))
		return
	var visual: Array[int] = session.board.cells.duplicate()
	var piece: Dictionary = session.pieces[slot].duplicate(true) if slot >= 0 and slot < session.pieces.size() else {}
	var remaining: int = 0
	for available in session.pieces:
		if not available.is_empty(): remaining += 1
	var result: Dictionary = session.place(slot, origin)
	if not result.ok:
		audio.play_event("invalid")
		return
	for offset in piece.cells:
		visual[(origin.y + int(offset[1])) * 8 + origin.x + int(offset[0])] = int(piece.color)
	result.visual_cells = visual
	result.dealt_hand = remaining == 1 or bool(result.zen_reset)
	armed = false
	shift_preview.clear()
	shift_direction = 0
	_feedback(result)
	if tutorial_active and tutorial_step == 1:
		tutorial_lines += int(result.lines)
		session.pieces = [session.generator.make_piece(1), {}, {}]
		if session.shift.ready():
			tutorial_stage_wait = 0.01 if bool(preferences.values.reduce_motion) else canvas.effects.move_seconds + canvas.effects.clear_seconds + 0.12
		else:
			_prepare_charge_row()
	if tutorial_active: _refresh_game()
	_checkpoint()
	if session.over:
		_queue_game_over()

func _feedback(result: Dictionary) -> void:
	audio.play_event("shift" if bool(result.shifted) else "place")
	audio.haptic(14)
	canvas.animate_result(result)
	_refresh_game()
	if not bool(preferences.values.reduce_motion):
		if score_tween != null and score_tween.is_valid(): score_tween.kill()
		score_label.modulate = Color("55ded2")
		score_tween = score_label.create_tween()
		score_tween.tween_property(score_label, "modulate", Color.WHITE, 0.35)
	if int(result.lines) > 0:
		audio.play_event("combo" if int(result.lines) > 1 or session.score.streak > 1 else "clear")
		# Score/combo feedback stays visible; the dedicated meter explains charge.
		_toast("%d %s · +%d%s" % [result.lines, "line" if int(result.lines) == 1 else "lines", result.points, " · COMBO ×%d" % session.score.streak if session.score.streak > 1 else ""])
	elif bool(result.zen_reset):
		_toast("A fresh board. Your journey continues.")
	if session.score.total > previous_best and previous_best > 0:
		audio.play_event("achievement")
		previous_best = 0
		_toast(status_label.text + " · NEW BEST" if int(result.lines) > 0 else "A new personal best. Keep your flow.")
		if int(result.lines) == 0: canvas.celebrate("PERSONAL BEST", Color("efba70"))

func _shift_button_pressed() -> void:
	if armed:
		if bool(shift_preview.get("allowed", false)): _shift(selected_axis, selected_index, shift_direction)
	else:
		_toggle_shift()

func _toggle_shift() -> void:
	if session == null or not session.shift.ready() or session.over: return
	if tutorial_active and tutorial_step != 2: return
	if armed:
		_cancel_shift()
		return
	armed = true
	shift_direction = 0
	shift_preview.clear()
	canvas.cancel_drag()
	canvas.finish_effects()
	if not session.rescue_options.is_empty():
		var suggestion: Dictionary = session.rescue_options[0]
		selected_axis = suggestion.axis
		selected_index = int(suggestion.index)
		_preview_shift(selected_axis, selected_index, int(suggestion.direction))
	else:
		_refresh_game()

func _cancel_shift() -> void:
	armed = false
	shift_direction = 0
	shift_preview.clear()
	_refresh_game()

func _toggle_axis() -> void:
	selected_axis = "column" if selected_axis == "row" else "row"
	shift_direction = 0
	shift_preview.clear()
	_refresh_game()

func _preview_shift(axis: String, index: int, direction: int) -> void:
	if not armed or session == null or modal != null: return
	selected_axis = axis
	selected_index = index
	shift_direction = direction
	shift_preview = session.preview_shift(axis, index, direction)
	shift_preview.allowed = bool(shift_preview.ok) and (session.board.has_move(session.pieces) or bool(shift_preview.get("can_continue", false)))
	if tutorial_active and (axis != "row" or index != 3 or direction != 1): shift_preview.allowed = false
	canvas.finish_effects()
	_refresh_game()

func _preview_description() -> String:
	if not bool(shift_preview.get("ok", false)): return "That line stays the same. Choose another move; your charge is safe."
	if tutorial_active and not bool(shift_preview.allowed): return "Try row 4 → Right. It wraps the cyan block into the column."
	if not bool(shift_preview.allowed): return "This move cannot open space. Choose another line or direction; charge kept."
	if not bool(shift_preview.can_continue): return "PREVIEW · No piece will fit after this move. Applying will end the run."
	var direction: String = ("right" if shift_direction > 0 else "left") if selected_axis == "row" else ("down" if shift_direction > 0 else "up")
	var outcome: String = "%d line%s will clear" % [int(shift_preview.lines), "" if int(shift_preview.lines) == 1 else "s"] if int(shift_preview.lines) > 0 else ("opens space for a piece" if not session.board.has_move(session.pieces) else "no lines clear")
	return "PREVIEW · %s %d %s · %s. Tap Apply SHIFT." % [selected_axis.capitalize(), selected_index + 1, direction, outcome]

func _shift(axis: String, index: int, direction: int) -> void:
	if not armed or modal != null:
		return
	if tutorial_active and tutorial_step != 2: return
	if tutorial_active and (axis != "row" or index != 3 or direction != 1):
		_toast("Select row 4 and move right to wrap the cyan block.")
		return
	var result: Dictionary = session.move_line(axis, index, direction)
	if not result.ok:
		_toast("Choose a move that opens space. Your charge is safe." if result.get("reason") == "no_rescue" else "That line would stay the same. Your charge is safe.")
		return
	var visual: Board = Board.new()
	visual.restore(result.motion.before)
	visual.shift(axis, index, direction)
	result.visual_cells = visual.cells.duplicate()
	armed = false
	shift_preview.clear()
	shift_direction = 0
	_feedback(result)
	_checkpoint()
	if tutorial_active:
		preferences.values.tutorial_done = true
		store.data.settings = preferences.values.duplicate(true)
		_save_result(store.write())
		tutorial_step = 3
		tutorial_finish_wait = float(VisualConfig.value("shift_seconds")) + float(VisualConfig.value("clear_seconds")) + 0.04
		_refresh_game()
		if bool(preferences.values.reduce_motion): _show_tutorial_complete()
	elif session.over:
		_queue_game_over()

func _request_tutorial_finish() -> void:
	# Leave the lesson before a replacement confirmation can keep it on-screen.
	# show_home checkpoints settings without recording the tutorial as a real run.
	show_home()
	_request_start("shift")

func _show_tutorial_complete() -> void:
	tutorial_finish_wait = 0
	var box: VBoxContainer = _sheet("Make room with SHIFT.", "You earned a charge over %d line clears. It stayed between turns.\n\nUse SHIFT → choose Rows or Columns → tap a line and direction → Preview → Apply. Preview and Cancel are free. Applying spends one charge; blocks wrap at the edge.\n\nSave a charge for a tight spot: it can rescue a blocked board." % tutorial_charge_goal, false, show_home)
	_button("Play SHIFT", _request_tutorial_finish, true, box)
	_button("Back to menu", show_home, false, box)

func _toast(message: String) -> void:
	if status_label != null and is_instance_valid(status_label):
		status_label.text = message
		toast_timer = 3.0

func _save_result(saved: bool) -> bool:
	_update_save_notice()
	return saved

func _update_save_notice() -> void:
	if save_notice == null: return
	save_notice.visible = not store.last_error.is_empty()
	save_notice_label.text = store.last_error
	save_retry_button.disabled = store.read_only
	save_retry_button.text = "Update needed" if store.read_only else "Retry save"
	if canvas != null and is_instance_valid(canvas):
		canvas.custom_minimum_size.y = 256 if save_notice.visible else 334
	_layout_modal_save_notice()

func _layout_modal_save_notice() -> void:
	if modal != null and is_instance_valid(modal) and save_notice != null:
		# Keep Retry reachable even when a pause/confirmation sheet is open.
		modal.offset_bottom = -(maxf(64, save_notice.size.y) + safe.get_theme_constant("margin_bottom") + 8) if save_notice.visible else 0.0

func _checkpoint() -> bool:
	if store == null:
		return false
	store.data.settings = preferences.values.duplicate(true)
	if session != null and not tutorial_active and not qa_active:
		return _save_result(store.checkpoint(session))
	else:
		return _save_result(store.write())

func show_game_help() -> void:
	var description: String = "Drag any piece onto empty cells. Fill an entire row or column to clear it. Use all three pieces to receive a new hand. The game ends when none of your pieces fits."
	if session.mode == "zen":
		description = "Drag pieces onto empty cells. Full rows and columns clear. When no remaining piece fits, the board renews and your score continues. Finish your session from Pause."
	elif bool(GameModes.config(session.mode).shift):
		description = "Place pieces on empty cells. Full rows and columns clear. Every %d full lines earn one SHIFT charge.\n\nTap Use SHIFT, choose a row or column, then tap a direction to PREVIEW. Outlined lines will clear. Tap Apply SHIFT to spend the charge, or Cancel to keep it. A swipe also previews.\n\nA charged SHIFT can rescue you when no piece fits. The run ends only when no placement or rescuing SHIFT remains. During ordinary play, SHIFT is optional." % ShiftSystem.new().lines_remaining()
	if session.mode == "daily":
		description += "\n\nToday's goal: clear %d lines." % DailyChallenge.target()
	var box: VBoxContainer = _sheet("How to play", description)
	_button("Back to game", _close_modal, true, box).name = "DismissHelp"
	if bool(GameModes.config(session.mode).shift):
		_button("Practice SHIFT", start_tutorial, false, box)

func pause_game() -> void:
	if screen != "game" or modal != null:
		return
	if session.over:
		show_game_over()
		return
	canvas.cancel_drag()
	var saved: bool = _checkpoint()
	var description: String = "Your board is saved. Resume whenever you're ready." if saved else "Your latest progress could not be saved. Use Retry save below before closing the app."
	if tutorial_active: description = "Practice is paused. Your other games are kept."
	var box: VBoxContainer = _sheet("Game paused", description)
	_button("Continue playing", _close_modal, true, box).name = "Resume"
	_button("Settings", func() -> void: show_settings("game"), false, box)
	_button("How to play", show_game_help, false, box)
	if not session.rescue_options.is_empty() and not tutorial_active:
		_button("End this run", func() -> void:
			session.finish()
			_checkpoint()
			show_game_over(), false, box)
	if OS.is_debug_build() and ResourceLoader.exists("res://scripts/debug/debug_menu.gd"):
		_button("Developer boards", show_debug, false, box)
	if session.mode == "zen" and not tutorial_active:
		_button("Finish Zen session", func() -> void:
			session.finish()
			_checkpoint()
			show_game_over(), false, box)
	_button("Main menu", show_home, false, box)

func _sheet(title: String, description: String, achievement: bool = false, on_back: Callable = Callable()) -> VBoxContainer:
	_close_modal()
	modal_back = on_back if on_back.is_valid() else _close_modal
	if canvas != null and is_instance_valid(canvas):
		canvas.cancel_drag()
		canvas.finish_effects()
	modal = Control.new()
	modal.name = "Modal"
	modal.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	add_child(modal)
	_layout_modal_save_notice()
	var dim: ColorRect = ColorRect.new()
	dim.color = Color(0.01, 0.02, 0.05, 0.88)
	dim.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	modal.add_child(dim)
	var scroller: ScrollContainer = ScrollContainer.new()
	scroller.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	scroller.offset_left = 20
	scroller.offset_right = -20
	scroller.offset_top = 24
	scroller.offset_bottom = -24
	scroller.horizontal_scroll_mode = ScrollContainer.SCROLL_MODE_DISABLED
	modal.add_child(scroller)
	var center: CenterContainer = CenterContainer.new()
	center.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	center.size_flags_vertical = Control.SIZE_EXPAND_FILL
	scroller.add_child(center)
	var panel: PanelContainer = PanelContainer.new()
	panel.custom_minimum_size.x = minf(size.x - 40, 420)
	var surface: StyleBoxFlat = ThemeManager.panel(Color("15223a"), 26, Color("2a3e59"))
	surface.shadow_color = Color(0, 0, 0, 0.28)
	surface.shadow_size = 16
	surface.shadow_offset = Vector2(0, 8)
	panel.add_theme_stylebox_override("panel", surface)
	center.add_child(panel)
	var box: VBoxContainer = VBoxContainer.new()
	box.custom_minimum_size.x = minf(size.x - 80, 380)
	box.add_theme_constant_override("separation", 14)
	panel.add_child(box)
	if achievement:
		var badge: TextureRect = TextureRect.new()
		badge.texture = load("res://assets/graphics/orbit_badge.svg")
		badge.custom_minimum_size = Vector2(80, 80)
		badge.expand_mode = TextureRect.EXPAND_IGNORE_SIZE
		badge.stretch_mode = TextureRect.STRETCH_KEEP_ASPECT_CENTERED
		box.add_child(badge)
	_label(title, 27, ThemeManager.INK, HORIZONTAL_ALIGNMENT_LEFT, box)
	_label(description, 15, ThemeManager.MUTED, HORIZONTAL_ALIGNMENT_LEFT, box)
	if not bool(preferences.values.reduce_motion):
		modal.modulate.a = 0
		modal.create_tween().tween_property(modal, "modulate:a", 1.0, float(VisualConfig.value("sheet_fade_seconds")))
	return box

func _close_modal() -> void:
	if modal != null and is_instance_valid(modal):
		modal.hide()
		modal.queue_free()
	modal = null
	modal_back = Callable()

func _queue_game_over() -> void:
	# State and save are already terminal. Only presentation waits; input is rejected
	# by GameSession and GameCanvas while the final clear is on screen.
	if bool(preferences.values.reduce_motion) or not canvas.effects.active():
		show_game_over()
	else:
		result_wait = canvas.effects.move_seconds + maxf(canvas.effects.clear_seconds, canvas.effects.points_seconds) + 0.06

func show_game_over() -> void:
	result_wait = 0
	var saved: bool = _checkpoint()
	audio.play_event("achievement" if session.completed else "game_over")
	var description: String
	if session.completed:
		description = "%d lines cleared. Your daily challenge is complete." % session.score.lines
	elif session.end_reason == "manual":
		description = "You chose to finish this run." if session.mode != "zen" else "A little space, at your own pace. Your Zen session is finished."
	elif session.mode == "zen":
		description = "Your Zen session is finished. Come back for a fresh board anytime."
	else:
		description = "No piece fits, and no charged SHIFT can open space." if bool(GameModes.config(session.mode).shift) else "None of the remaining pieces fits."
	description += " Your score is saved." if saved else " Your latest progress could not be saved. Use Retry save below."
	var box: VBoxContainer = _sheet("Daily complete" if session.completed else "Session complete", description, session.completed, show_home)
	_label(str(session.score.total), 50, Color("55ded2"), HORIZONTAL_ALIGNMENT_CENTER, box)
	_label("%d lines   ·   Best combo ×%d" % [session.score.lines, session.score.best_combo], 15, ThemeManager.MUTED, HORIZONTAL_ALIGNMENT_CENTER, box)
	_button("Play again", func() -> void: start_game(session.mode), true, box)
	_button("Main menu", show_home, false, box)

func start_tutorial() -> void:
	_checkpoint()
	tutorial_active = true
	qa_active = false
	tutorial_step = 1
	tutorial_lines = 0
	tutorial_charge_goal = ShiftSystem.new().lines_remaining()
	tutorial_stage_wait = 0
	tutorial_finish_wait = 0
	session = GameSession.new("shift", 42)
	for line in mini(tutorial_charge_goal, 4):
		for x in 6: session.board.cells[(7 - line) * 8 + x] = 2 + line % 3
	session.pieces = [session.generator.make_piece(1), {}, {}]
	session.shift.energy = 0
	armed = false
	selected_axis = "row"
	selected_index = 3
	show_game()

func _prepare_charge_row() -> void:
	var row: int = 7 - tutorial_lines % 4
	for x in 6: session.board.cells[row * 8 + x] = 2 + tutorial_lines % 3

func _prepare_shift_lesson() -> void:
	tutorial_stage_wait = 0
	armed = false
	shift_preview.clear()
	shift_direction = 0
	selected_axis = "row"
	selected_index = 3
	canvas.finish_effects()
	tutorial_step = 2
	session.board = Board.new()
	for y in 8:
		if y != 3: session.board.cells[y * 8] = 2
	session.board.cells[3 * 8 + 7] = 1
	session.pieces = [session.generator.make_piece(1), {}, {}]
	session.over = false
	session.end_reason = ""
	_refresh_game()

func show_daily() -> void:
	screen = "daily"
	_new_page()
	_header("Daily orbit")
	_label(DailyChallenge.today(), 14, Color("efba70"))
	_label("A fresh perspective,
every day.", 34)
	var box: VBoxContainer = _card()
	_label("THE OBJECTIVE", 11, Color("55ded2"), HORIZONTAL_ALIGNMENT_LEFT, box)
	_label("Clear %d lines." % DailyChallenge.target(), 25, ThemeManager.INK, HORIZONTAL_ALIGNMENT_LEFT, box)
	_label("SHIFT charges as you clear full lines. No timer, no account. Every retry starts with the same seed and rules. Your choices shape the rest.", 16, ThemeManager.MUTED, HORIZONTAL_ALIGNMENT_LEFT, box)
	_label("Uses your device's local calendar. The date stays fixed during a run.", 13, ThemeManager.MUTED, HORIZONTAL_ALIGNMENT_LEFT, box)
	var saved_daily: GameSession = GameSession.from_snapshot(store.saved("daily"))
	if saved_daily != null and not saved_daily.over and saved_daily.date != DailyChallenge.today():
		_button("Continue saved orbit · " + saved_daily.date, func() -> void: _resume("daily"))
	_button("Continue today's orbit  →" if _has_saved("daily") and saved_daily.date == DailyChallenge.today() else "Begin today's orbit  →", func() -> void: _request_start("daily"), true)
	_label("YOUR RECENT ORBITS", 12, ThemeManager.MUTED)
	var dates: Array = store.data.daily.keys()
	dates.sort()
	dates.reverse()
	if dates.is_empty():
		_label("Your first orbit is waiting.", 16, ThemeManager.MUTED)
	for date_value in dates.slice(0, 31):
		var record: Variant = store.data.daily[date_value]
		if not record is Dictionary:
			continue
		var history: VBoxContainer = _card()
		_label(str(date_value) + ("  ✓ Complete" if bool(record.get("completed", false)) else "  · Attempted"), 16, ThemeManager.INK, HORIZONTAL_ALIGNMENT_LEFT, history)
		_label("Best %d   ·   %d lines" % [int(record.get("best", 0)), int(record.get("lines", 0))], 13, ThemeManager.MUTED, HORIZONTAL_ALIGNMENT_LEFT, history)

func show_stats() -> void:
	screen = "stats"
	_new_page()
	_header("Your progress")
	_label("Small moves add up.", 30)
	for mode in GameModes.IDS:
		var record: Dictionary = store.stats(mode)
		var box: VBoxContainer = _card()
		_label(str(GameModes.config(mode).title), 22, Color("55ded2"), HORIZONTAL_ALIGNMENT_LEFT, box)
		_label("%d personal best" % int(record.best), 25, ThemeManager.INK, HORIZONTAL_ALIGNMENT_LEFT, box)
		_label("%d games   ·   %d lines   ·   Combo ×%d
%d minutes of play" % [int(record.games), int(record.lines), int(record.combo), int(float(record.playtime) / 60)], 15, ThemeManager.MUTED, HORIZONTAL_ALIGNMENT_LEFT, box)
	_button("Daily history", show_daily)

func show_settings(return_to: String = "home", section: String = "") -> void:
	_checkpoint()
	settings_return = return_to
	if section in ["play", "sound", "about"]:
		settings_section = section
	screen = "settings"
	_new_page()
	_header("Settings", _leave_settings)
	var tabs: HBoxContainer = _row()
	for id in ["play", "sound", "about"]:
		_button(id.capitalize(), func() -> void: show_settings(settings_return, id), id == settings_section, tabs).name = "Settings" + id.capitalize()
	if settings_section == "sound":
		_label("Set the mood.", 28)
		_slider("Sound effects", "sfx")
		_slider("Ambience", "ambience")
		var sounds: VBoxContainer = _card()
		_label("YOUR SOUNDSCAPE", 11, Color("55ded2"), HORIZONTAL_ALIGNMENT_LEFT, sounds)
		for row_index in 2:
			var choices: HBoxContainer = _row(sounds)
			for sound in SettingsManager.AMBIENCE_SOUNDS.slice(row_index * 2, row_index * 2 + 2):
				var selected: bool = preferences.values.ambience_sound == sound
				var choice: Button = _button(("✓ " if selected else "") + AudioManager.AMBIENCE_LABELS[sound], func() -> void:
					preferences.values.ambience_sound = sound
					audio.apply_settings()
					show_settings(settings_return, "sound"), selected, choices)
				choice.name = "Ambience" + sound.capitalize()
				choice.tooltip_text = AudioManager.AMBIENCE_DESCRIPTIONS[sound]
		_label(AudioManager.AMBIENCE_DESCRIPTIONS[preferences.values.ambience_sound], 14, ThemeManager.MUTED, HORIZONTAL_ALIGNMENT_LEFT, sounds)
		_label("Plays softly in the background. Uses your phone's media volume. Your choice saves automatically.", 13, ThemeManager.MUTED)
		var selected_title: String = AudioManager.AMBIENCE_LABELS[preferences.values.ambience_sound]
		_button("Preview " + selected_title if preferences.values.ambience_sound != "off" else "Preview ambience", func() -> void:
			var played: bool = audio.preview_ambience()
			var box: VBoxContainer = _sheet(selected_title + " is playing" if played else "Ambience is off" if preferences.values.ambience_sound == "off" else "Ambience is muted", AudioManager.AMBIENCE_DESCRIPTIONS[preferences.values.ambience_sound] + " Adjust your phone's media volume if needed." if played else "Choose Rain, Forest or Ocean and raise the Ambience slider to preview.")
			_button("Done", _close_modal, true, box)).name = "PreviewAmbience"
	elif settings_section == "play":
		_label("Make it comfortable.", 27)
		var controls: VBoxContainer = _card()
		_toggle("Battery saver", "battery_saver", false, controls)
		_toggle("Particles", "particles", false, controls)
		_toggle("Reduce motion", "reduce_motion", false, controls)
		_toggle("Haptic feedback", "haptics", false, controls)
		_toggle("Larger interface text", "text_scale", true, controls)
		_slider("Drag height", "drag_lift")
		_label("Lift pieces farther above your finger, or closer for easier reach. Progress saves automatically; any storage problem is shown below.", 13, ThemeManager.MUTED)
		_button("Restore default settings", _confirm_settings_reset).name = "RestoreSettings"
	else:
		_label("Made for a little space.", 26)
		_label("VISUAL THEME", 11, Color("55ded2"))
		_label("Orbit · Included", 17, ThemeManager.MUTED)
		_button("Learn SHIFT · interactive tutorial", start_tutorial)
		_button("Back up & restore progress", show_save_transfer).name = "SaveTransfer"
		_button("Privacy & local data", show_privacy)
		_button("Credits & licenses", show_licenses)
		_label("BLOCKSHIFT %s\nMade by Kreativ · Internal testing" % ProjectSettings.get_setting("application/config/version"), 12, ThemeManager.MUTED, HORIZONTAL_ALIGNMENT_CENTER)

func _confirm_settings_reset() -> void:
	var box: VBoxContainer = _sheet("Restore default settings?", "Audio, controls and appearance will return to their defaults. Your board, scores and tutorial progress are kept.")
	_button("Restore settings", func() -> void:
		preferences.reset_preferences()
		audio.apply_settings()
		_apply_theme()
		_checkpoint()
		show_settings(settings_return, "play"), true, box).name = "ConfirmRestoreSettings"
	_button("Keep my settings", _close_modal, false, box)

func _leave_settings() -> void:
	_checkpoint()
	_apply_theme()
	if settings_return == "game" and session != null:
		show_game()
	else:
		show_home()

func show_save_transfer() -> void:
	_checkpoint()
	screen = "save_transfer"
	_new_page()
	_header("Keep your progress", func() -> void: show_settings(settings_return, "about"))
	_label("A little peace of mind.", 26)
	_label("Export all saved boards, scores, daily results and preferences to a file you choose. Keep it outside the app before changing phones or uninstalling.", 16, ThemeManager.MUTED)
	_label("A restore replaces this installation's progress after you review it. Your previous progress is kept for Undo restore. Nothing is sent to Made by Kreativ.", 14, ThemeManager.MUTED)
	_button("Export progress backup", func() -> void: _choose_transfer(true), true).name = "ExportProgress"
	_button("Restore from backup", func() -> void: _choose_transfer(false)).name = "ImportProgress"
	if FileAccess.file_exists(store.path + ".before-import.blockshift"):
		_button("Undo last restore", func() -> void: _preview_transfer(store.path + ".before-import.blockshift")).name = "UndoImport"
	_label("Choose on-device storage for an offline backup. If you choose a cloud provider, that provider handles the file under its own policy. A backup file is not encrypted; keep it private.", 13, ThemeManager.MUTED)

func _transfer_message(title: String, message: String) -> void:
	var box: VBoxContainer = _sheet(title, message)
	_button("Done", _close_modal, true, box).name = "DismissTransfer"

func _choose_transfer(exporting: bool) -> void:
	if transfer_dialog_open: return
	if not _checkpoint():
		_transfer_message("Save needs attention", "Your current progress could not be saved. Free some storage and retry before transferring it.")
		return
	if not DisplayServer.has_feature(DisplayServer.FEATURE_NATIVE_DIALOG_FILE):
		_transfer_message("File picker unavailable", "Progress transfer needs the system file picker (Android 10 or later). Your local game is kept.")
		return
	transfer_dialog_open = true
	var filename: String = "BLOCKSHIFT-%s.blockshift" % DailyChallenge.today() if exporting else ""
	var error: Error = DisplayServer.file_dialog_show("Export BLOCKSHIFT backup" if exporting else "Restore BLOCKSHIFT backup", "", filename, false,
		DisplayServer.FILE_DIALOG_MODE_SAVE_FILE if exporting else DisplayServer.FILE_DIALOG_MODE_OPEN_FILE,
		PackedStringArray(["*.blockshift;BLOCKSHIFT progress;application/octet-stream"]),
		func(accepted: bool, paths: PackedStringArray, _filter: int) -> void:
			transfer_dialog_open = false
			if not accepted or paths.is_empty(): return
			if exporting: _export_transfer(paths[0])
			else: _preview_transfer(paths[0]))
	if error != OK:
		transfer_dialog_open = false
		_transfer_message("File picker unavailable", "No progress was changed. Please try again.")

func _export_transfer(path: String) -> void:
	if not SaveTransfer.export_file(path, store.data):
		_transfer_message("Backup not verified", "Keep your current game installed. The file could not be written and read back correctly. Choose a different location and try again.")
		return
	_transfer_message("Backup saved and verified", "Your saved modes, scores and preferences are in the selected file. Keep this file safe. You can restore it in another BLOCKSHIFT installation.")

func _preview_transfer(path: String) -> void:
	pending_transfer = SaveTransfer.inspect_file(path)
	if not pending_transfer.get("ok", false):
		_transfer_message("Backup not restored", str(pending_transfer.get("error", "Could not read this backup.")))
		return
	var imported: Dictionary = pending_transfer.data
	var description: String = "Backup date: %s (UTC)\n" % pending_transfer.created
	for mode in GameModes.IDS:
		var mode_stats: Dictionary = imported.stats.get(mode, {})
		description += "\n%s · best %d · %s" % [mode.capitalize(), ScoreManager.counter(mode_stats, "best"), "saved board" if imported.runs.has(mode) else "no saved board"]
	description += "\n\nRestore replaces progress on this installation. Your current progress will be kept for Undo restore. Nothing changes until you confirm."
	var box: VBoxContainer = _sheet("Restore this backup?", description)
	_button("Restore backup", _apply_transfer, true, box).name = "ConfirmImport"
	_button("Keep current progress", func() -> void:
		pending_transfer.clear()
		_close_modal(), false, box).name = "CancelImport"

func _apply_transfer() -> void:
	if not pending_transfer.get("ok", false): return
	if not _checkpoint() or not _save_result(store.import_progress(pending_transfer.data)):
		pending_transfer.clear()
		_transfer_message("Backup not restored", store.last_error + " Your current progress is kept.")
		return
	pending_transfer.clear()
	session = null
	tutorial_active = false
	qa_active = false
	armed = false
	result_wait = 0
	preferences.restore(store.data.settings)
	audio.apply_settings()
	_apply_theme()
	settings_return = "home"
	show_save_transfer()
	_transfer_message("Progress restored", "Your saved modes, scores and preferences are ready. Undo last restore is available here if you chose the wrong backup.")

func _slider(title: String, key: String) -> void:
	var box: VBoxContainer = _card()
	var is_height: bool = key == "drag_lift"
	var value_label: Label = _label(_setting_value_text(title, key), 17, ThemeManager.INK, HORIZONTAL_ALIGNMENT_LEFT, box)
	var slider: HSlider = HSlider.new()
	slider.name = key + "Slider"
	slider.min_value = SettingsManager.DRAG_MIN if is_height else 0
	slider.max_value = SettingsManager.DRAG_MAX if is_height else 1
	slider.step = 4 if is_height else 0.05
	slider.value = float(preferences.values[key])
	slider.custom_minimum_size.y = 48
	slider.tooltip_text = title
	box.add_child(slider)
	slider.value_changed.connect(func(value: float) -> void:
		preferences.values[key] = value
		value_label.text = _setting_value_text(title, key)
		if not is_height: audio.apply_settings())
	slider.drag_ended.connect(func(_changed: bool) -> void: _checkpoint())

func _setting_value_text(title: String, key: String) -> String:
	if key == "drag_lift":
		var height: float = float(preferences.values[key])
		return "%s · %s" % [title, "Low" if height <= 72 else ("High" if height >= 104 else "Medium")]
	return "%s · %d%%" % [title, roundi(float(preferences.values[key]) * 100)]

func _toggle(title: String, key: String, scale_toggle: bool = false, parent: Node = null) -> void:
	var toggle: CheckButton = CheckButton.new()
	toggle.text = title
	toggle.name = key
	toggle.custom_minimum_size.y = 54
	toggle.button_pressed = float(preferences.values[key]) > 1 if scale_toggle else bool(preferences.values[key])
	(parent if parent != null else page).add_child(toggle)
	toggle.toggled.connect(func(enabled: bool) -> void:
		preferences.values[key] = (1.2 if enabled else 1.0) if scale_toggle else enabled
		_checkpoint()
		if scale_toggle:
			_apply_theme()
			show_settings(settings_return))

func show_pro(return_to: String = "home") -> void:
	# Legacy navigation cannot expose an unfinished checkout in the internal release.
	if return_to == "settings": show_settings(settings_return, "about")
	else: show_home()

func show_privacy() -> void:
	screen = "privacy"
	_new_page()
	_header("Privacy, simply", func() -> void: show_settings(settings_return))
	_label("Your board stays with you.", 30)
	_label(ReleaseInfo.privacy_text(), 16, ThemeManager.MUTED).name = "PrivacySummary"
	var info: Dictionary = ReleaseInfo.details()
	var policy_url: String = str(info.get("privacy_policy_url", ""))
	if policy_url.begins_with("https://"):
		_button("Read full privacy policy", func() -> void: OS.shell_open(policy_url)).name = "PrivacyPolicyLink"
	var support_email: String = str(info.get("support_email", ""))
	if not support_email.is_empty():
		_button("Contact support", func() -> void: OS.shell_open("mailto:" + support_email)).name = "ContactSupport"
	_button("Reset local progress", func() -> void:
		var box: VBoxContainer = _sheet("Reset this device's progress?", "Scores, daily history and all saved mode boards will be removed from this installation. This cannot be undone.")
		_button("Reset progress", _reset_progress, false, box).name = "ConfirmResetProgress"
		_button("Keep my progress", _close_modal, true, box))

func _reset_progress() -> void:
	if not _save_result(store.reset_progress(preferences.values)):
		var box: VBoxContainer = _sheet("Progress was not reset", store.last_error + " Your board and progress are kept.")
		_button("Try reset again", _reset_progress, false, box).disabled = store.read_only
		_button("Keep my progress", _close_modal, true, box)
		return
	session = null
	tutorial_active = false
	qa_active = false
	show_home()

func show_licenses() -> void:
	screen = "licenses"
	_new_page()
	_header("Made with care", func() -> void: show_settings(settings_return))
	_label("Made by Kreativ", 28)
	_label("Design, code and original placeholder tones created for BLOCKSHIFT. Replaceable audio by the future Kreativ Sound library.", 16, ThemeManager.MUTED)
	_label("GODOT ENGINE · MIT", 12, Color("55ded2"))
	_label(FileAccess.get_file_as_string("res://assets/licenses/GODOT_LICENSE.txt"), 13, ThemeManager.MUTED)
	_label("MANROPE · SIL OPEN FONT LICENSE", 12, Color("55ded2"))
	_label(FileAccess.get_file_as_string("res://assets/fonts/OFL.txt"), 13, ThemeManager.MUTED)
	_button("Godot third-party notices", func() -> void:
		screen = "licenses"
		_new_page()
		_header("Engine third-party notices", show_licenses)
		_label(FileAccess.get_file_as_string("res://assets/licenses/GODOT_COPYRIGHT.txt"), 12, ThemeManager.MUTED))

func show_debug() -> void:
	if OS.is_debug_build() and ResourceLoader.exists("res://scripts/debug/debug_menu.gd"):
		var debug_menu: GDScript = load("res://scripts/debug/debug_menu.gd")
		debug_menu.show_menu(self)

func _process(delta: float) -> void:
	if result_wait > 0 and screen == "game" and modal == null and session != null and session.over:
		result_wait -= delta
		if result_wait <= 0: show_game_over()
	if tutorial_active and tutorial_step == 1 and screen == "game" and modal == null and tutorial_stage_wait > 0:
		tutorial_stage_wait -= delta
		if tutorial_stage_wait <= 0: _prepare_shift_lesson()
	if tutorial_active and tutorial_step == 3 and screen == "game" and modal == null and tutorial_finish_wait > 0:
		tutorial_finish_wait -= delta
		if tutorial_finish_wait <= 0: _show_tutorial_complete()
	if session != null and screen == "game" and modal == null and not session.over:
		session.elapsed += minf(delta, 1.0)
		autosave += delta
		if autosave >= 15:
			autosave = 0
			_checkpoint()
	if store != null and not store.last_error.is_empty() and not store.read_only:
		save_retry_elapsed += delta
		if save_retry_elapsed >= 5:
			save_retry_elapsed = 0
			_checkpoint()
	else:
		save_retry_elapsed = 0
	if toast_timer > 0:
		toast_timer -= delta
		if toast_timer <= 0: _refresh_game()

func _unhandled_key_input(event: InputEvent) -> void:
	if event.is_action_pressed("ui_cancel"):
		_back()
		get_viewport().set_input_as_handled()
	if OS.is_debug_build() and event is InputEventKey and event.pressed and event.keycode == KEY_F8 and screen == "game":
		show_debug()

func _back() -> void:
	if transfer_dialog_open: return
	var now: int = Time.get_ticks_msec()
	# Some Android devices deliver both a window request and a cancel key.
	if now - last_back_at < 180:
		return
	last_back_at = now
	if modal != null:
		if modal_back.is_valid(): modal_back.call()
		else: _close_modal()
	elif screen == "game":
		pause_game()
	elif screen == "settings":
		_leave_settings()
	elif page_back.is_valid():
		page_back.call()
	else:
		show_home()

func _notification(what: int) -> void:
	if what == NOTIFICATION_WM_CLOSE_REQUEST:
		_checkpoint()
		get_tree().quit()
	elif what == NOTIFICATION_APPLICATION_PAUSED or what == NOTIFICATION_APPLICATION_FOCUS_OUT:
		if store != null:
			_checkpoint()
			var controlled_test: bool = OS.is_debug_build() and has_meta("qa_save_path") and bool(get_meta("qa_controlled_focus", false))
			if not controlled_test and screen == "game" and modal == null and session != null and not session.over:
				pause_game.call_deferred()
