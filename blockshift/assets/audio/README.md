# BLOCKSHIFT audio

Rain, Forest and Ocean are original, procedurally created nature-style soundscapes authored for this project. No field recordings, downloaded samples, external services or third-party sound libraries were used. They are not represented as recordings of real locations or species.

- Rain: filtered, layered rainfall and leaf-patter textures; no thunder.
- Forest: a very quiet wind bed with clearer, sparse synthesized bird phrases. Version 0.5.3 reduces the continuous noise after phone-listening feedback.
- Ocean: slowly swelling surf and brighter receding foam.

Each is a 48-second, 32 kHz stereo Vorbis loop with periodic source textures and no fade-to-silence at the join. Rain and Ocean retain their original quiet levels. Forest deliberately has a lower overall level and a much quieter continuous bed so birds are easier to hear; it is not normalised back up to the other soundscapes. `tools/generate_nature_audio.py` uses a fixed seed; install NumPy 2.x and SoundFile 0.13.x in a development virtual environment to recreate them. Use `--only forest` to rewrite Forest without touching Rain or Ocean. Encoded container bytes can vary with encoder versions. These packages are not runtime dependencies.

The existing original synthesized SFX are generated independently by `tools/generate_audio.py`. Runtime paths live in `manifest.json`, so later mastered Kreativ Sound assets can replace them without changing gameplay. Set loop metadata on replacement ambience imports and rerun the decoded audio checks (`ffmpeg` and `ffprobe` required).

Hardware listening on phone speakers/headphones, comfortable loudness and long-session listening remain acceptance checks; numerical level and loop tests cannot certify those qualities.
