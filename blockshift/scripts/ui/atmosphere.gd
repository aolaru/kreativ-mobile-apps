class_name Atmosphere
extends Control
## Three small gradient textures; static drawing, no idle animation or shader pass.
var wash: GradientTexture2D
var cyan: GradientTexture2D
var violet: GradientTexture2D

func _ready() -> void:
	mouse_filter = Control.MOUSE_FILTER_IGNORE
	wash = GradientTexture2D.new()
	wash.width = 8
	wash.height = 256
	wash.fill_from = Vector2(0.5, 0)
	wash.fill_to = Vector2(0.5, 1)
	wash.gradient = Gradient.new()
	wash.gradient.colors = PackedColorArray([Color("0c172a"), Color("080e1c"), Color("0d1426")])
	wash.gradient.offsets = PackedFloat32Array([0, 0.52, 1])
	cyan = _glow(Color(0.16, 0.68, 0.70, 0.075))
	violet = _glow(Color(0.44, 0.30, 0.80, 0.065))
	resized.connect(queue_redraw)

func _glow(color: Color) -> GradientTexture2D:
	var texture: GradientTexture2D = GradientTexture2D.new()
	texture.width = 256
	texture.height = 256
	texture.fill = GradientTexture2D.FILL_RADIAL
	texture.fill_from = Vector2(0.5, 0.5)
	texture.fill_to = Vector2(1, 0.5)
	texture.gradient = Gradient.new()
	texture.gradient.colors = PackedColorArray([color, Color(color, 0)])
	return texture

func _draw() -> void:
	if wash == null: return
	draw_texture_rect(wash, Rect2(Vector2.ZERO, size), false)
	draw_texture_rect(cyan, Rect2(Vector2(-size.x * 0.45, -size.x * 0.5), Vector2.ONE * size.x * 1.5), false)
	draw_texture_rect(violet, Rect2(Vector2(size.x * 0.1, size.y - size.x * 1.1), Vector2.ONE * size.x * 1.5), false)
	draw_arc(Vector2(size.x * 0.90, size.y * 0.20), size.x * 0.75, 0.4, 2.5, 48, Color(0.32, 0.65, 0.75, 0.035), 1, true)
