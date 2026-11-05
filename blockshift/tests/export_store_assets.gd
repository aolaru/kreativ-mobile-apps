extends SceneTree
## Actual game renders in disposable storage plus art from our existing vectors.
const OUT: String = "res://store/assets/"
const QA_PATH: String = "user://qa-store-capture.json"
var app: Control
var output_viewport: SubViewport

func _initialize() -> void:
	_run.call_deferred()

func capture(filename: String) -> void:
	await process_frame
	await process_frame
	await create_timer(0.6).timeout
	await RenderingServer.frame_post_draw
	output_viewport.get_texture().get_image().save_png(OUT + filename)

func text(parent: Node, value: String, point: Vector2, font_size: int, color: Color) -> void:
	var label: Label = Label.new()
	label.text = value
	label.position = point
	label.add_theme_font_override("font", load("res://assets/fonts/Manrope.ttf"))
	label.add_theme_font_size_override("font_size", font_size)
	label.add_theme_color_override("font_color", color)
	parent.add_child(label)

func _run() -> void:
	DirAccess.make_dir_recursive_absolute(OUT)
	var icon: Image = Image.new()
	var svg: String = FileAccess.get_file_as_string("res://assets/icons/icon.svg").replace('rx="96"', 'rx="0"')
	assert(icon.load_svg_from_string(svg, 512.0 / 432.0) == OK)
	icon.convert(Image.FORMAT_RGB8)
	icon.save_png(OUT + "icon-512.png")
	# Native macOS windows are clamped to the screen height. Render the real UI
	# offscreen at store resolution, with the same 9:16 logical canvas.
	output_viewport = SubViewport.new()
	output_viewport.size = Vector2i(1080, 1920)
	output_viewport.size_2d_override = Vector2i(540, 960)
	output_viewport.size_2d_override_stretch = true
	output_viewport.render_target_update_mode = SubViewport.UPDATE_ALWAYS
	root.add_child(output_viewport)
	for suffix in ["", ".tmp", ".bak"]: DirAccess.remove_absolute(QA_PATH + suffix)
	app = load("res://scenes/main.tscn").instantiate()
	app.set_meta("qa_save_path", QA_PATH)
	app.set_meta("qa_controlled_focus", true)
	output_viewport.add_child(app)
	await create_timer(0.8).timeout
	app.preferences.values.tutorial_done = true
	app.preferences.values.reduce_motion = true
	app.show_home()
	await capture("01-home.png")
	for mode in ["classic", "shift", "zen", "daily"]:
		app.start_game(mode)
		app._close_modal()
		for turn in 18:
			var placed: bool = false
			for slot in 3:
				if placed or app.session.pieces[slot].is_empty(): continue
				for y in 8:
					for x in 8:
						if not placed and app.session.board.can_place(app.session.pieces[slot].cells, Vector2i(x, y)):
							app.session.place(slot, Vector2i(x, y))
							placed = true
		app._refresh_game()
		await capture("02-" + mode + ".png")
	app.session = null
	app.queue_free()
	await process_frame
	for suffix in ["", ".tmp", ".bak"]: DirAccess.remove_absolute(QA_PATH + suffix)
	output_viewport.size = Vector2i(1024, 500)
	output_viewport.size_2d_override = Vector2i(1024, 500)
	var art: Control = Control.new()
	output_viewport.add_child(art)
	var background: ColorRect = ColorRect.new()
	background.color = ThemeManager.BACKGROUND
	background.size = Vector2(1024, 500)
	art.add_child(background)
	var hero: TextureRect = TextureRect.new()
	hero.texture = load("res://assets/icons/hero.svg")
	hero.position = Vector2(490, 95)
	hero.size = Vector2(500, 300)
	hero.stretch_mode = TextureRect.STRETCH_KEEP_ASPECT_CENTERED
	art.add_child(hero)
	text(art, "M A D E  B Y  K R E A T I V", Vector2(64, 80), 15, ThemeManager.MUTED)
	text(art, "BLOCKSHIFT", Vector2(60, 152), 56, ThemeManager.INK)
	text(art, "Make a little space.", Vector2(64, 237), 26, Color("55ded2"))
	text(art, "Place. Clear. SHIFT.", Vector2(64, 336), 19, ThemeManager.MUTED)
	await capture("feature-1024x500.png")
	print("Store artwork and five real gameplay/menu renders exported.")
	quit()
