class_name BoardEffects
extends RefCounted
## Bounded visual timeline. It never writes to a GameSession or uses its RNG.
var elapsed: float = 0.0
var move_seconds: float = 0.0
var clear_seconds: float = 0.38
var points_seconds: float = 0.86
var ring_seconds: float = 0.40
var landing: Dictionary = {}
var motion: Dictionary = {}
var ghosts: Array[Dictionary] = []
var sweeps: Array[Dictionary] = []
var sparks: Array[Dictionary] = []
var banner: Dictionary = {}
var pickup_left: float = 0.0
var reject_left: float = 0.0
var reject_slot: int = -1
var ring_indices: Array = []
var energy_gain: bool = false
var enabled: bool = true
var serial: int = 0
var banner_start: float = 0.0
var deal: bool = false

func clear() -> void:
	landing.clear()
	motion.clear()
	ghosts.clear()
	sweeps.clear()
	sparks.clear()
	banner.clear()
	ring_indices.clear()
	pickup_left = 0
	reject_left = 0
	reject_slot = -1
	energy_gain = false
	move_seconds = 0
	elapsed = 0
	banner_start = 0
	deal = false

func pickup(animate: bool) -> void:
	clear() # New input always reveals the current authoritative board immediately.
	enabled = animate
	if enabled: pickup_left = float(VisualConfig.value("pickup_seconds"))

func reject(slot: int, animate: bool) -> void:
	if not animate: return
	enabled = true
	reject_slot = slot
	reject_left = 0.22

func begin(result: Dictionary, pending: Dictionary, preferences: SettingsManager, mode: String, streak: int) -> void:
	clear()
	enabled = not bool(preferences.values.reduce_motion)
	if not enabled: return
	serial += 1
	clear_seconds = float(VisualConfig.value("clear_seconds"))
	points_seconds = float(VisualConfig.value("points_seconds"))
	ring_seconds = float(VisualConfig.value("ring_seconds"))
	landing = pending.duplicate(true)
	motion = result.get("motion", {}).duplicate(true)
	move_seconds = float(VisualConfig.value("shift_seconds")) if not motion.is_empty() else (float(VisualConfig.value("landing_seconds")) if not landing.is_empty() else 0.0)
	banner_start = move_seconds
	deal = bool(result.get("dealt_hand", false))
	ring_indices = landing.get("indices", []).duplicate()
	var cleared: Array = result.get("cleared", [])
	var visual: Array = result.get("visual_cells", [])
	var renewed: bool = bool(result.get("zen_reset", false))
	if renewed and visual.size() == 64:
		cleared = []
		for i in 64:
			if int(visual[i]) > 0: cleared.append(i)
	var center: Vector2 = Vector2.ZERO
	for index in cleared:
		center += Vector2(int(index) % 8 + 0.5, int(int(index) / 8) + 0.5)
		if visual.size() == 64 and int(visual[index]) > 0:
			ghosts.append({"index": index, "color": int(visual[index])})
	if not cleared.is_empty():
		center /= cleared.size()
		for axis in ["row", "column"]:
			for line in 8:
				var full: bool = true
				for i in 8:
					if (line * 8 + i if axis == "row" else i * 8 + line) not in cleared: full = false
				if full: sweeps.append({"axis": axis, "index": line})
		banner = {"title": "FRESH SPACE" if renewed else ("COMBO ×%d" % streak if streak > 1 else ("%d LINES" % int(result.lines) if int(result.lines) > 1 else "LINE CLEAR")), "points": "" if renewed else "+%d" % int(result.points), "center": center, "color": Color("c6b1ff") if bool(result.shifted) else Color("7af5dd")}
		energy_gain = not renewed
		if bool(preferences.values.particles) and mode != "zen":
			var rng: RandomNumberGenerator = RandomNumberGenerator.new()
			rng.seed = 700 + serial
			var cap: int = clampi(int(VisualConfig.value("battery_particles_max" if bool(preferences.values.battery_saver) else "particles_max")), 0, 64)
			for index in cleared:
				for i in 2:
					if sparks.size() >= cap: break
					var angle: float = rng.randf_range(-PI, 0)
					var speed: float = rng.randf_range(0.4, 1.5)
					sparks.append({"position": Vector2(int(index) % 8 + 0.5, int(int(index) / 8) + 0.5), "velocity": Vector2(cos(angle), sin(angle)) * speed, "shade": rng.randi_range(0, 3)})

func celebrate(title: String, color: Color) -> void:
	if enabled:
		banner_start = maxf(elapsed, move_seconds)
		banner = {"title": title, "points": "", "center": Vector2(4, 2), "color": color}

func advance(delta: float) -> void:
	elapsed += maxf(0, delta)
	pickup_left = maxf(0, pickup_left - delta)
	reject_left = maxf(0, reject_left - delta)
	if elapsed >= float(VisualConfig.value("deal_seconds")): deal = false
	if reject_left <= 0: reject_slot = -1
	if elapsed >= move_seconds:
		landing.clear()
		motion.clear()
	if elapsed >= move_seconds + clear_seconds:
		ghosts.clear()
		sweeps.clear()
	if elapsed >= banner_start + points_seconds:
		banner.clear()
	if elapsed >= move_seconds + points_seconds:
		sparks.clear()
	if elapsed >= ring_seconds:
		ring_indices.clear()
		energy_gain = false

func active() -> bool:
	return enabled and (deal or pickup_left > 0 or reject_left > 0 or not landing.is_empty() or not motion.is_empty() or not ghosts.is_empty() or not sweeps.is_empty() or not sparks.is_empty() or not banner.is_empty() or not ring_indices.is_empty() or energy_gain)

func deal_progress(slot: int) -> float:
	return clampf((elapsed - slot * 0.04) / maxf(0.001, float(VisualConfig.value("deal_seconds")) - 0.08), 0, 1) if deal else 1.0

func move_progress() -> float:
	return clampf(elapsed / maxf(0.001, move_seconds), 0, 1)

func clear_progress() -> float:
	return clampf((elapsed - move_seconds) / maxf(0.001, clear_seconds), 0, 1)

func points_progress() -> float:
	return clampf((elapsed - banner_start) / maxf(0.001, points_seconds), 0, 1)

func particle_progress() -> float:
	return clampf((elapsed - move_seconds) / maxf(0.001, points_seconds), 0, 1)
