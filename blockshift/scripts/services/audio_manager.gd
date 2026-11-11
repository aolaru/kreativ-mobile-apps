class_name AudioManager
extends Node
## Swap manifest paths for Kreativ Sound WAV/OGG without changing game code.
var settings: SettingsManager
var streams: Dictionary = {}
var voices: Array[AudioStreamPlayer] = []
var ambience: AudioStreamPlayer
var fading_ambience: AudioStreamPlayer
var ambience_tween: Tween
const AMBIENCE_LABELS: Dictionary = {"rain": "Rain", "forest": "Forest", "ocean": "Ocean", "off": "Off"}
const AMBIENCE_DESCRIPTIONS: Dictionary = {"rain": "Gentle rain on leaves.", "forest": "Soft wind and distant birds.", "ocean": "Slow waves on the shore.", "off": "A quiet space. Sound effects stay separate."}
var cursor: int = 0
var suspended: bool = false

func setup(preferences: SettingsManager) -> void:
	settings = preferences
	var manifest: Dictionary = JSON.parse_string(FileAccess.get_file_as_string("res://assets/audio/manifest.json"))
	for key in manifest:
		if ResourceLoader.exists(manifest[key]):
			streams[key] = load(manifest[key])
	for i in 6:
		var voice: AudioStreamPlayer = AudioStreamPlayer.new()
		voice.bus = "SFX"
		add_child(voice)
		voices.append(voice)
	ambience = AudioStreamPlayer.new()
	ambience.bus = "Ambience"
	add_child(ambience)
	fading_ambience = AudioStreamPlayer.new()
	fading_ambience.bus = "Ambience"
	add_child(fading_ambience)
	apply_settings()

func apply_settings() -> void:
	for pair in [["SFX", "sfx"], ["Ambience", "ambience"]]:
		var index: int = AudioServer.get_bus_index(pair[0])
		var volume: float = float(settings.values[pair[1]])
		AudioServer.set_bus_volume_db(index, linear_to_db(maxf(0.0001, volume)))
		AudioServer.set_bus_mute(index, volume <= 0)
	_sync_ambience()

func _sync_ambience() -> void:
	if ambience == null:
		return
	var selected: AudioStream = streams.get("ambience_" + str(settings.values.ambience_sound))
	if ambience.stream != selected:
		# Two bounded voices; rapid changes cancel the previous transition safely.
		if ambience_tween != null: ambience_tween.kill()
		fading_ambience.stop()
		var old: AudioStreamPlayer = ambience
		ambience = fading_ambience
		fading_ambience = old
		ambience.stream = selected
		ambience.volume_db = -60.0
		ambience_tween = create_tween().set_parallel(true)
		ambience_tween.tween_property(fading_ambience, "volume_db", -60.0, 0.45)
		ambience_tween.tween_property(ambience, "volume_db", 0.0, 0.65)
		ambience_tween.chain().tween_callback(fading_ambience.stop)
	var muted: bool = float(settings.values.ambience) <= 0
	if selected != null and not muted and not suspended and not ambience.playing:
		ambience.play()
	ambience.stream_paused = suspended or muted
	fading_ambience.stream_paused = suspended or muted

func preview_ambience() -> bool:
	if ambience == null or ambience.stream == null or suspended or float(settings.values.ambience) <= 0:
		return false
	ambience.play(0.0)
	ambience.stream_paused = false
	return true

func play_event(event: String) -> void:
	if voices.is_empty() or not streams.has(event) or float(settings.values.sfx) <= 0:
		return
	var voice: AudioStreamPlayer = voices[cursor]
	cursor = (cursor + 1) % voices.size()
	voice.stream = streams[event]
	voice.play()

func haptic(duration: int = 12) -> void:
	if bool(settings.values.haptics) and OS.has_feature("android"):
		Input.vibrate_handheld(duration, 0.3)

func _notification(what: int) -> void:
	if ambience == null:
		return
	if OS.is_debug_build() and bool(get_meta("qa_controlled_focus", false)):
		return
	if what == NOTIFICATION_APPLICATION_PAUSED or what == NOTIFICATION_APPLICATION_FOCUS_OUT:
		suspended = true
		_sync_ambience()
	elif what == NOTIFICATION_APPLICATION_RESUMED or what == NOTIFICATION_APPLICATION_FOCUS_IN:
		suspended = false
		_sync_ambience()
