# Graphyn Media AI

JVM/Desktop plugin providing text-to-speech, caption-style, speech-to-text, and OCR workflow nodes.
Each AI capability is a command-line adapter so the heavy models stay out of the JVM process.

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
