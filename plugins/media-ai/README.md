# Graphyn Media AI

JVM/Desktop plugin providing text-to-speech and caption-style workflow nodes.

## TTS Adapter

Set `GRAPHYN_TTS_EXECUTABLE` to a command-line adapter for your TTS provider. Graphyn invokes:

```text
<executable> \
  --text <text> \
  --language <language> \
  --voice <voice-id> \
  --speed <0.5-2.0> \
  --output <wav-path>
```

The adapter must write a non-empty WAV file and exit with code 0. Provider credentials should be
read by the adapter from its environment. This contract can wrap the internal Qwen/Qute SDK
without adding an unpublished dependency to Graphyn.

Generated files are cached under `${GRAPHYN_HOME:-~/.graphyn}/cache/tts`. The key includes text,
language, voice, and speed.

## Nodes

- `media.text_to_speech`
- `media.caption_style`
