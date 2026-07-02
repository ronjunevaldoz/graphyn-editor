#!/bin/bash
set -e

MODEL_NAME="${1:-base}"
MODEL_DIR="${GRAPHYN_MODEL_DIR:-$HOME/.graphyn/models}/whisper"
MODEL_FILE="ggml-${MODEL_NAME}.bin"
MODEL_PATH="$MODEL_DIR/$MODEL_FILE"

MODEL_URL="https://huggingface.co/ggerganov/whisper.cpp/resolve/main/$MODEL_FILE"

echo "🚀 Setting up Whisper model..."
echo "Model: $MODEL_NAME"
echo "Target: $MODEL_PATH"

mkdir -p "$MODEL_DIR"

if [ -f "$MODEL_PATH" ]; then
  echo "✅ Model already exists: $MODEL_PATH"
else
  echo "⬇️ Downloading $MODEL_FILE..."
  curl -L "$MODEL_URL" -o "$MODEL_PATH"
  echo "✅ Download complete."
fi

echo ""
echo "Add this to your .env:"
echo "WHISPER_MODEL_PATH=$MODEL_PATH"