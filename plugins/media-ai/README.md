# Graphyn Media AI

JVM/Desktop plugin providing text-to-speech, caption-style, speech-to-text, and OCR workflow nodes.
Each AI capability is a command-line adapter so the heavy models stay out of the JVM process.

## Zero-config fallbacks

So the templates run out of the box, the default engines fall back to common system tools when the
matching `GRAPHYN_*_EXECUTABLE` is unset: **macOS `say`** for text-to-speech and **`tesseract`** for
OCR (if installed). A configured executable always takes precedence. Speech-to-text has no standard
CLI fallback, so the captioning template still needs `GRAPHYN_STT_EXECUTABLE`.

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
language, voice, speed, engine, and any engine-specific extras (reference audio path, instruct,
temperature, seed) below.

### Dedicated nodes per engine

Different TTS engines honour different parameters (say has no `instruct`/`temperature`/`seed`;
Qwen3 has no runtime `language`/`speed` but supports voice cloning; OuteTTS has no `speed` but
does honour `language`/`instruct`/`temperature`/`seed`). Rather than one node with dead knobs
for whichever engine is active, there are dedicated node types:

| Node | Engine | Adapter env var | Extra params |
|---|---|---|---|
| `media.text_to_speech` | `GRAPHYN_TTS_EXECUTABLE`, falling back to macOS `say` | `GRAPHYN_TTS_EXECUTABLE` | — |
| `media.text_to_speech.say` | macOS `say` | (built-in, no adapter) | — |
| `media.text_to_speech.qwen3` | Qwen3 (native JNI) | `GRAPHYN_TTS_QWEN3_EXECUTABLE` | `reference_audio_path` (voice cloning, takes precedence over `voice`), `temperature` |
| `media.text_to_speech.oute` | OuteTTS (native JNI) | `GRAPHYN_TTS_OUTE_EXECUTABLE` | `instruct`, `temperature`, `seed` |

`GRAPHYN_TTS_QWEN3_EXECUTABLE` / `GRAPHYN_TTS_OUTE_EXECUTABLE` are separate from
`GRAPHYN_TTS_EXECUTABLE` on purpose — using a dedicated node never changes what
`media.text_to_speech`/`auto` resolves to elsewhere. See
`StudioProjects/Graphyn/scripts/adapters/graphyn-tts-qwen3-jni.sh` and
`graphyn-tts-oute-jni.sh` for native-JNI adapters (no HTTP server required).

## STT Adapter

Set `GRAPHYN_STT_EXECUTABLE` to a transcription adapter. Graphyn invokes
`<executable> --audio <path> --language <lang>` and parses JSON from stdout:

```json
{
  "text": "full transcript",
  "confidence": 0.93,
  "segments": [{ "text": "hello", "start_ms": 0, "end_ms": 500 }]
}
```

`segments` map directly onto the caption-segment record consumed by `media.caption_overlay`.

## OCR Adapter

Set `GRAPHYN_OCR_EXECUTABLE` to an OCR adapter. Graphyn invokes
`<executable> --image <path> --language <lang>` and parses JSON from stdout:

```json
{
  "text": "INVOICE",
  "blocks": [{ "text": "INVOICE", "x": 5, "y": 6, "width": 80, "height": 20, "confidence": 0.95 }]
}
```

## Nodes

- `media.text_to_speech`
- `media.caption_style`
- `media.speech_to_text`
- `media.ocr`
