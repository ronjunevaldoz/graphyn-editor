#!/bin/bash

# Exit immediately if a command exits with a non-zero status.
set -e

ENV_NAME=".venv_media_agents"
PROJECT_ROOT=$(pwd)
VENV_PATH="$PROJECT_ROOT/env/$ENV_NAME"

echo "🚀 Starting Graphyn Media AI Environment Setup..."

# 1. Create Virtual Environment
if [ ! -d "$VENV_MAX_PATH" ]; then
    echo "📦 Creating virtual environment in $ENV_NAME..."
else
    echo "✅ Virtual environment already exists."
fi
python3 -m venv "$VENV_PATH"

# 2. Activate Environment and Install Dependencies
echo "📥 Installing required Python packages (this may take a few minutes)..."
"$VENV_PATH/bin/pip" install --upgrade pip
"$VENV_PATH/bin/pip" install openai-whisper setuptools-rust

# 3. Verify Installation
echo "🔍 Verifying installation..."
"$VENV_PATH/bin/python" -c "import whisper; print('✅ Whisper loaded successfully!')"

# 4. Final Instructions
echo ""
echo "------------------------------------------------------------"
echo "✨ SETUP COMPLETE ✨"
echo "------------------------------------------------------------"
echo "Your media automation environment is ready at:"
echo "  $VENV_PATH"
echo ""
echo "To use this environment to your STT adapter, add this to your .env:"
echo "  GRAPHYN_STT_EXECUTABLE=$VENV_PATH/bin/python $PROJECT_ROOT/scripts/stt_adapter.py"
echo ""
echo "Note: Remember to replace '/path/to/your/' with your actual absolute path!"
echo "------------------------------------------------------------"
