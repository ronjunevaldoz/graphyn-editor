# Graphyn Scripts

This directory contains helper scripts for setting up and maintaining the local Graphyn development environment.

## Prerequisites

* macOS or Linux
* Bash
* Python 3
* Homebrew (recommended on macOS)

---

## Setup Media AI Environment

Creates a dedicated Python virtual environment and installs the dependencies required by the Media AI plugins.

```bash
./setup-media-ai.sh
```

This script:

* Creates `.venv_media_agents`
* Installs `openai-whisper`
* Installs `setuptools-rust`
* Verifies the Whisper installation

After completion, configure your `.env`:

```env
GRAPHYN_STT_EXECUTABLE="<project>/.venv_media_agents/bin/python <project>/scripts/stt_adapter.py"
```

Replace `<project>` with your Graphyn project directory.

---

## Setup Whisper Model

Downloads a Whisper GGML model for FFmpeg's `whisper` filter.

Default model:

```bash
./setup-whisper-model.sh
```

Specific model:

```bash
./setup-whisper-model.sh base
./setup-whisper-model.sh small
./setup-whisper-model.sh medium
./setup-whisper-model.sh large-v3
```

Models are stored in:

```text
~/.graphyn/models/whisper/
```

After downloading, add the generated path to your `.env`:

```env
WHISPER_MODEL_PATH=~/.graphyn/models/whisper/ggml-base.bin
```

---

## Environment Variables

| Variable                 | Description                                                                     |
| ------------------------ | ------------------------------------------------------------------------------- |
| `GRAPHYN_STT_EXECUTABLE` | Python executable and STT adapter command.                                      |
| `WHISPER_MODEL_PATH`     | Path to the Whisper GGML model used by FFmpeg.                                  |
| `GRAPHYN_MODEL_DIR`      | Optional base directory for Graphyn AI models. Defaults to `~/.graphyn/models`. |
| `FFMPEG_EXECUTABLE`      | Optional custom FFmpeg executable. Defaults to `ffmpeg`.                        |

---

## Notes

* FFmpeg must be built with `--enable-whisper`.
* Subtitle rendering additionally requires `--enable-libass`.
* `EnvironmentResolver` automatically resolves values from system environment variables and supported `.env` files.
* Relative paths should be resolved using `FileIO.resolvePath()` rather than hardcoded absolute paths.
