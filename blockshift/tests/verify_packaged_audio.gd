extends SceneTree
## Load actual exported resources extracted into ignored build/, not source OGGs.
func _initialize() -> void:
	var paths: PackedStringArray = OS.get_cmdline_user_args()
	if paths.size() != 3:
		printerr("Supply three extracted .oggvorbisstr resource paths after --.")
		quit(2)
		return
	for path in paths:
		var stream: AudioStreamOggVorbis = ResourceLoader.load(path) as AudioStreamOggVorbis
		if stream == null or not stream.loop or stream.loop_offset != 0.0 or absf(stream.get_length() - 48.0) > 0.02:
			printerr("Packaged soundscape is invalid: " + path.get_file())
			quit(1)
			return
	print("PACKAGED AUDIO: 9 assertions; 0 failures (Vorbis, loop, duration × 3)")
	quit(0)
