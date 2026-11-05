class_name ThemeManager
extends RefCounted
const INK: Color = Color("eef3ff")
const MUTED: Color = Color("8b9ab7")
const BACKGROUND: Color = Color("080e1c")
const SURFACE: Color = Color("111d31")
const THEMES: Dictionary = {
	"orbit": [Color("55ded2"), Color("a68aee"), Color("efba70"), Color("78aaf2")],
	"dusk": [Color("e0a1b5"), Color("b29aee"), Color("edc890"), Color("94b6dd")],
	"lagoon": [Color("74cbb9"), Color("8bbbd4"), Color("d2c68c"), Color("b4c7de")]}

static func palette(name: String, zen: bool = false) -> Array:
	return THEMES.lagoon if zen else THEMES.get(name, THEMES.orbit)

static func panel(color: Color = SURFACE, radius: int = 18, border: Color = Color.TRANSPARENT) -> StyleBoxFlat:
	var style: StyleBoxFlat = StyleBoxFlat.new()
	style.bg_color = color
	style.set_corner_radius_all(radius)
	style.set_border_width_all(0 if border.a == 0 else 1)
	style.border_color = border
	style.content_margin_left = 20
	style.content_margin_right = 20
	style.content_margin_top = 14
	style.content_margin_bottom = 14
	return style

static func primary_surface() -> StyleBoxTexture:
	var style: StyleBoxTexture = StyleBoxTexture.new()
	style.texture = preload("res://assets/graphics/button.svg")
	style.texture_margin_left = 18
	style.texture_margin_right = 18
	style.texture_margin_top = 18
	style.texture_margin_bottom = 18
	style.content_margin_left = 20
	style.content_margin_right = 20
	style.content_margin_top = 14
	style.content_margin_bottom = 14
	return style
