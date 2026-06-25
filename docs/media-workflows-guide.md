# Media Workflows Guide

A comprehensive guide to building, executing, and customizing media workflows in Graphyn using video, audio, and text-to-speech nodes.

---

## Table of Contents

1. [Overview](#overview)
2. [Getting Started](#getting-started)
3. [Template Workflows](#template-workflows)
4. [Node Reference](#node-reference)
5. [Configuration Guide](#configuration-guide)
6. [Workflow Patterns](#workflow-patterns)
7. [Troubleshooting](#troubleshooting)
8. [Advanced Topics](#advanced-topics)

---

## Overview

The media workflow system provides **7 core nodes** for video, audio, and text-to-speech operations on the **JVM/Desktop** platform (Phase 1).

Implementation status, unit coverage, and workflow execution coverage are tracked in
[Media Workflow Status](./architecture/media-workflow-status.md).

### Capabilities

- **Video Operations**: Import metadata, stitch clips, encode to MP4
- **Audio Operations**: Extract from video, mix multiple tracks
- **Text-to-Speech**: Synthesize audio with caching and styling metadata
- **File Management**: Integration with existing I/O nodes for input/output

### Architecture

- **Intermediate files** stored in `~/.graphyn/temp/`
- **TTS cache** at `~/.graphyn/cache/tts/` (keyed by text, language, voice, speed)
- **FFmpeg-powered**: Requires FFmpeg >= 4.0 on PATH
- **TTS executable**: Configured via `GRAPHYN_TTS_EXECUTABLE` environment variable

### Media Handles

Video and audio are represented as opaque `RecordValue` handles:

```kotlin
VideoHandle = { kind: "video", path: String, mime_type: String }
AudioHandle = { kind: "audio", path: String, mime_type: String }
```

These prevent mismatched connections (e.g., audio → video port) while maintaining workflow compatibility.

---

## Getting Started

### 1. Launch Demo App

Run the Graphyn desktop app:

```bash
./gradlew :app:desktopApp:run
```

### 2. Select a Template

Open **Demo Workflows** and choose one:
- **Text to Speech** — Simple TTS example
- **Video Narration** — Video + TTS narration
- **Audio Mix** — Combine multiple audio tracks
- **Video Stitch** — Concatenate video clips

### 3. Configure Paths

Update file paths in node configs:
- Video/audio input files
- Output destination paths
- Text files to read

### 4. Execute

Press **Run** to execute the workflow. Monitor progress in the **Execution Panel**.

---

## Template Workflows

### Template 1: Simple Text to Speech

**Purpose**: Convert text to audio with automatic caching.

**Nodes**:
```
File Read (input.txt)
    ↓
Text to Speech
    ↓
(Cached audio output)
```

**Configuration**:

| Node | Field | Value | Notes |
|------|-------|-------|-------|
| File Read | path | `input.txt` | UTF-8 text file |
| TTS | language | `en` | Language code (en, zh, es, ...) |
| TTS | voice_id | `default` | Voice identifier |
| TTS | speed | `1.0` | 0.5–2.0 (slow to fast) |

**Output**: Audio file cached at `~/.graphyn/cache/tts/<hash>.wav`

**Use Cases**:
- Generate narration scripts
- Create audio descriptions
- TTS engine testing
- Batch text-to-audio conversion

---

### Template 2: Video Narration

**Purpose**: Add synthesized narration to an existing video.

**Nodes**:
```
Video Import (input.mp4)
    ├→ Extract Audio
    │       ↓
    │   Audio Mix ← Text to Speech (narration.txt)
    │       ↓
    └──→ Video Encode → output.mp4
```

**Configuration**:

| Node | Field | Value | Notes |
|------|-------|-------|-------|
| Video Import | path | `input.mp4` | Source video |
| File Read | path | `narration.txt` | Narration script |
| TTS | language | `en` | Match narration language |
| TTS | voice_id | `narrator` | Professional voice option |
| Audio Mix | — | — | Auto-mixes all inputs equally |
| Video Encode | output_path | `output.mp4` | Final output |
| Video Encode | bitrate | `high` | Quality level |

**Workflow Execution Flow**:

1. **Video Import** reads metadata (fps, duration, dimensions)
2. **Extract Audio** demuxes the original audio track to WAV
3. **Text to Speech** synthesizes narration (cached on repeat)
4. **Audio Mix** combines background audio + narration
5. **Video Encode** renders final MP4 with mixed audio

**Output**: `output.mp4` with dual audio tracks (original + narration)

**Use Cases**:
- Add voice-over to instructional videos
- Auto-narrate video content
- Generate multi-language versions (re-run with different `voice_id`)
- Overlay narration on existing audio

**Tips**:
- Ensure narration duration roughly matches video length
- Test voice_id options to find best narrator voice
- Use `speed` parameter to sync narration with video pacing

---

### Template 3: Audio Mix

**Purpose**: Combine multiple audio sources and define caption styling.

**Nodes**:
```
(Audio Extract or TTS)
    ├→ Audio Mix → (mixed audio output)
    └→
(Audio Extract or TTS)

Caption Style (metadata for Phase 2)
```

**Configuration**:

| Node | Field | Value | Notes |
|------|-------|-------|-------|
| Audio Extract | video | `{video handle}` | Source video |
| TTS | text | `(from File Read)` | Foreground audio |
| Audio Mix | audio_tracks | (auto-populated) | All connected audio inputs |
| Audio Mix | volumes | (empty) | Defaults to equal mix (1.0 each) |
| Caption Style | color | `#FFFFFF` | Text color (hex) |
| Caption Style | background_color | `#000000` | Background color |
| Caption Style | font_size | `24` | Point size |
| Caption Style | position | `bottom` | Placement: top, center, bottom |

**Output**: Mixed audio + caption style metadata (used in Phase 2)

**Use Cases**:
- Blend background music with dialogue
- Combine voiceover + ambient sound
- Create multi-track compositions
- Prepare styled captions for later overlay

**Tips**:
- Leave `volumes` empty for equal mixing (all tracks at 1.0)
- Manually set volumes if one track should be louder:
  ```
  volumes = [1.0, 0.5]  // First track loud, second quiet
  ```
- Caption Style node is metadata-only; it feeds Phase 2 overlays

---

### Template 4: Video Stitch

**Purpose**: Concatenate multiple video clips with hard cuts.

**Status**: Verified in the desktop demo. See
[Media Workflow Status](./architecture/media-workflow-status.md) for the current coverage
snapshot.

**Nodes**:
```
Video Import (clip1.mp4)
    ├→ Videos List ──→ Video Stitch → Video Encode → output.mp4
    └→
Video Import (clip2.mp4)
```

**Note**: The **Videos List** helper node collects individual videos into a list, which is required by Video Stitch.

**Configuration**:

| Node | Field | Value | Notes |
|------|-------|-------|-------|
| Video Import (1st) | path | `clip1.mp4` | First clip |
| Video Import (2nd) | path | `clip2.mp4` | Second clip (add more as needed) |
| Video Stitch | transition | `cut` | Hard cut only (fade in Phase 2) |
| Video Encode | output_path | `stitched.mp4` | Final output |
| Video Encode | bitrate | `high` | Quality level |
| Video Encode | codec | `h264` | Only option in Phase 1 |

**Workflow Execution Flow**:

1. **Video Import** reads metadata for each clip
2. **Video Stitch** concatenates clips using FFmpeg concat demuxer
3. **Video Encode** renders final MP4

**Output**: `stitched.mp4` with all clips in sequence

**Use Cases**:
- Assemble video compilations
- Create highlight reels
- Merge related clips
- Batch video concatenation

**Tips**:
- All input clips should use the same codec/resolution for best results
- Clips are joined in node connection order
- Phase 2 will add fade transitions and transitions between clips
- Audio is preserved from each clip (auto-synchronized)

---

## Node Reference

### `media.video_import`

**Purpose**: Load video file and extract metadata.

**Inputs**:
- `path` (string) — File path to video (MP4, MOV, etc.)

**Outputs**:
- `video` (VideoHandle) — Opaque handle to video file
- `width` (int) — Frame width in pixels
- `height` (int) — Frame height in pixels
- `duration_ms` (double) — Total duration in milliseconds
- `fps` (double) — Frames per second

**Implementation**: Uses `ffprobe` to read metadata without decoding frames.

**Example**:
```
Input: path = "/home/user/intro.mp4"
Output: video = {kind:"video", path:"/tmp/graphyn/vid_abc.mp4", mime:"video/mp4"}
        width = 1920, height = 1080, duration_ms = 5000.0, fps = 30.0
```

---

### `media.audio_extract`

**Purpose**: Demux audio stream from video.

**Inputs**:
- `video` (VideoHandle) — Video file handle

**Outputs**:
- `audio` (AudioHandle) — Extracted audio file
- `sample_rate` (int) — Audio sample rate (Hz)
- `duration_ms` (double) — Audio duration in milliseconds

**Implementation**: Uses FFmpeg to extract first audio stream to PCM WAV.

**Example**:
```
Input: video = {kind:"video", ...}
Output: audio = {kind:"audio", path:"/tmp/graphyn/audio_xyz.wav", mime:"audio/wav"}
        sample_rate = 48000, duration_ms = 5000.0
```

---

### `media.videos_list`

**Purpose**: Collect individual video handles into a list.

**Inputs**:
- `video1` (VideoHandle) — First clip
- `video2` (VideoHandle, optional) — Second clip
- `video3` (VideoHandle, optional) — Third clip
- `video4` (VideoHandle, optional) — Fourth clip

**Outputs**:
- `videos` (ListType(VideoHandle)) — Collected list of videos

**Behavior**:
- Connects 2–4 individual video inputs into a single list output
- Used before `media.video_stitch` which requires a list input
- Only provide videos you actually want to stitch (leave optional ones unwired)

**Example**:
```
clip1.mp4 → video1 ──┐
clip2.mp4 → video2 ──┴→ videos → [clip1, clip2] (ready for stitch)
```

**Use Cases**:
- Prepare clips for video stitching
- Work around type system limitation requiring lists for multi-input operations

---

### `media.audio_mix`

**Purpose**: Combine multiple audio tracks.

**Inputs**:
- `audio_tracks` (list of AudioHandle) — Audio sources to mix
- `volumes` (list of double, optional) — Volume multipliers (0.0–1.0)

**Outputs**:
- `audio` (AudioHandle) — Mixed audio file
- `duration_ms` (double) — Duration of mixed output

**Behavior**:
- If `volumes` is empty or shorter than track count, defaults to 1.0 for missing values
- Shorter tracks are padded with silence to match longest duration
- Output is PCM WAV at sample rate of first input

**Example**:
```
Input: audio_tracks = [{kind:"audio",...}, {kind:"audio",...}]
       volumes = []
Output: audio = {kind:"audio", path:"/tmp/graphyn/mixed.wav", ...}
        duration_ms = 5000.0
```

---

### `media.video_stitch`

**Purpose**: Concatenate video clips.

**Inputs**:
- `videos` (list of VideoHandle) — Clips to concatenate
- `transition` (enum: "cut" | "fade") — Transition style

**Outputs**:
- `video` (VideoHandle) — Stitched video
- `duration_ms` (double) — Total duration
- `frame_count` (int) — Total frames in output

**Behavior (Phase 1)**:
- Only "cut" (hard cut) is supported
- Fade transitions are Phase 2 work
- Audio from each clip is preserved and auto-synchronized
- Resolution matches first clip (others scaled if needed)

**Example**:
```
Input: videos = [{kind:"video",...}, {kind:"video",...}]
       transition = "cut"
Output: video = {kind:"video", path:"/tmp/graphyn/stitched.mp4", ...}
        duration_ms = 10000.0, frame_count = 300
```

---

### `media.video_encode`

**Purpose**: Render video to MP4 file.

**Inputs**:
- `video` (VideoHandle) — Video to encode
- `audio` (AudioHandle, optional) — Audio to mix in
- `output_path` (string) — Destination file path
- `bitrate` (enum: "low" | "medium" | "high") — Quality level
- `codec` (enum: "h264") — Video codec

**Outputs**:
- `file_path` (string) — Final output path (copy of input)
- `size_bytes` (double) — File size in bytes
- `duration_ms` (double) — Duration of encoded video

**Codec Details (Phase 1)**:
- **Video**: H.264 (AVC)
- **Audio**: AAC (if audio input provided)
- **Container**: MP4

**Bitrate Mapping**:
| Level | Video Bitrate | Audio Bitrate |
|-------|---------------|---------------|
| low | 1000k | 96k |
| medium | 2500k | 128k |
| high | 5000k | 192k |

**Example**:
```
Input: video = {kind:"video",...}
       audio = {kind:"audio",...}
       output_path = "final.mp4"
       bitrate = "high", codec = "h264"
Output: file_path = "final.mp4"
        size_bytes = 125000000.0
        duration_ms = 5000.0
```

---

### `media.text_to_speech`

**Purpose**: Convert text to audio using TTS engine.

**Inputs**:
- `text` (string) — Text to synthesize
- `language` (enum: "en" | "zh" | "es" | ...) — Language code
- `voice_id` (string) — Voice identifier
- `speed` (double, 0.5–2.0) — Playback speed

**Outputs**:
- `audio` (AudioHandle) — Synthesized audio
- `duration_ms` (double) — Duration of generated audio
- `cached` (boolean) — Whether output was cached

**Caching**:
- Cache key: SHA-256(text + language + voice_id + speed)
- Cache directory: `~/.graphyn/cache/tts/`
- Hit avoids redundant API calls or synthesis

**TTS Engine**:
- Default adapter calls `GRAPHYN_TTS_EXECUTABLE` with:
  ```
  --text <text> --language <language> --voice <voice> --speed <speed> --output <path>
  ```
- Credentials managed by executable (via environment, config, etc.)

**Example**:
```
Input: text = "Hello world"
       language = "en"
       voice_id = "default"
       speed = 1.0
Output: audio = {kind:"audio", path:"~/.graphyn/cache/tts/abc123.wav", ...}
        duration_ms = 1200.0
        cached = false (first call)
```

**Second call with same inputs**:
```
Output: audio = {kind:"audio", path:"~/.graphyn/cache/tts/abc123.wav", ...}
        duration_ms = 1200.0
        cached = true (from cache)
```

---

### `media.caption_style`

**Purpose**: Define caption appearance metadata (Phase 2 preview).

**Inputs**:
- `color` (string) — Text color in hex (e.g., "#FFFFFF")
- `background_color` (string) — Background color in hex
- `font_size` (int) — Point size (e.g., 24)
- `position` (enum: "top" | "center" | "bottom") — Vertical placement

**Outputs**:
- `style_config` (RecordValue) — Styled metadata for captions

**Purpose**: This is a metadata node that prepares caption styling for Phase 2 caption_overlay node. Currently outputs the configuration record for downstream use.

**Example**:
```
Input: color = "#FFFFFF"
       background_color = "#000000"
       font_size = 24
       position = "bottom"
Output: style_config = {color: "#FFFFFF", background_color: "#000000", ...}
```

---

## Configuration Guide

### Environment Variables

#### FFmpeg Paths

```bash
# Override ffmpeg executable location
export GRAPHYN_FFMPEG=/usr/local/bin/ffmpeg

# Override ffprobe executable location
export GRAPHYN_FFPROBE=/usr/local/bin/ffprobe
```

Default: Uses FFmpeg on system `PATH`.

#### TTS Engine

```bash
# Specify TTS executable (wraps Qwen3/Qute SDK)
export GRAPHYN_TTS_EXECUTABLE=/path/to/tts-cli

# Example wrapper script:
#!/bin/bash
# tts-cli - Wrapper for Qwen3/Qute TTS
source ~/.config/tts-credentials
qwen_tts \
  --text "$TEXT" \
  --language "$LANGUAGE" \
  --voice "$VOICE" \
  --speed "$SPEED" \
  --output "$OUTPUT"
```

#### Graphyn Home

```bash
# Custom Graphyn data directory (default: ~/.graphyn)
export GRAPHYN_HOME=/var/cache/graphyn
```

This directory contains:
- `temp/` — Intermediate media files (auto-cleanup recommended)
- `cache/tts/` — TTS audio cache

### File Path Configuration

**Absolute Paths** (recommended):
```
/home/user/videos/input.mp4
/tmp/output.mp4
```

**Relative Paths** (relative to working directory):
```
videos/clip1.mp4
./output/final.mp4
../shared/audio.wav
```

**Special Variables** (not yet supported, but planned):
```
${HOME}/videos/input.mp4
${GRAPHYN_HOME}/temp/working.mp4
```

---

## Workflow Patterns

### Pattern 1: Simple Conversion

**Text → Audio**

```
[File Read] → [TTS] → (audio output)
```

Use when: Single narration, voice prompt, TTS demonstration

---

### Pattern 2: Video Enhancement

**Video + New Audio**

```
[Video Import] → [Video Encode] ← [Audio Mix] ← [TTS]
```

Use when: Adding narration, replacing audio, overdubbing

---

### Pattern 3: Audio Composition

**Combine Multiple Audio**

```
[TTS] ──┐
        ├→ [Audio Mix] → (final audio)
[Audio Extract] ──┘
```

Use when: Mixing dialogue + music, layering audio tracks

---

### Pattern 4: Video Assembly

**Concatenate Clips**

```
[Import Clip 1] ──┐
                  ├→ [Video Stitch] → [Video Encode] → (final MP4)
[Import Clip 2] ──┘
```

Use when: Creating compilations, merging sequences

---

### Pattern 5: Full Pipeline

**Video + Narration + Export**

```
[Video Import]
    ├→ [Extract Audio] ──┐
    │                    ├→ [Mix] ─┐
[Narration TTS] ─────────┘         │
                                   ├→ [Encode] → (final.mp4)
                                   │
                        [Video] ───┘
```

Use when: Complete post-production workflow

---

## Troubleshooting

### FFmpeg Not Found

**Error**: `FFmpeg executable not found on PATH`

**Solutions**:

1. Install FFmpeg:
   ```bash
   # macOS
   brew install ffmpeg
   
   # Linux
   sudo apt-get install ffmpeg
   
   # Windows
   choco install ffmpeg
   ```

2. Override path:
   ```bash
   export GRAPHYN_FFMPEG=/path/to/ffmpeg
   export GRAPHYN_FFPROBE=/path/to/ffprobe
   ```

3. Verify installation:
   ```bash
   ffmpeg -version
   ffprobe -version
   ```

---

### TTS Executable Not Found

**Error**: `GRAPHYN_TTS_EXECUTABLE not configured`

**Solutions**:

1. Set environment variable:
   ```bash
   export GRAPHYN_TTS_EXECUTABLE=/path/to/tts-wrapper
   ```

2. Create wrapper script:
   ```bash
   #!/bin/bash
   # ~/.local/bin/graphyn-tts
   exec /opt/qwen-tts/qwen-tts-cli "$@"
   ```

3. Make executable:
   ```bash
   chmod +x ~/.local/bin/graphyn-tts
   export GRAPHYN_TTS_EXECUTABLE=~/.local/bin/graphyn-tts
   ```

---

### File Not Found

**Error**: `Input file path does not exist`

**Check**:

1. Verify file exists:
   ```bash
   ls -la /path/to/video.mp4
   ```

2. Check permissions:
   ```bash
   chmod 644 /path/to/video.mp4
   ```

3. Use absolute path:
   ```
   /home/user/videos/input.mp4  (not ./input.mp4)
   ```

---

### Disk Space Issues

**Error**: `No space left on device` during video processing

**Solutions**:

1. Check available space:
   ```bash
   df -h ~/.graphyn/temp
   ```

2. Clear old temp files:
   ```bash
   rm -rf ~/.graphyn/temp/*
   ```

3. Change temp location:
   ```bash
   export GRAPHYN_HOME=/mnt/fast-ssd
   ```

---

### Audio Sync Problems

**Issue**: Audio doesn't align with video after mixing

**Solutions**:

1. Check audio duration:
   - Use `ffprobe` to verify source audio length
   - Ensure narration duration matches video pacing

2. Adjust TTS speed:
   - Use `speed` parameter: 0.5–2.0
   - Higher speed = shorter duration

3. Use Phase 2 timing controller (when available):
   - Synchronize tracks explicitly with sync points

---

### Encoding Takes Too Long

**Performance Tips**:

1. Use lower bitrate:
   ```
   bitrate = "low"  (1000k)
   ```

2. Reduce resolution (manual pre-processing):
   - Downscale video before import
   - Use existing lower-res clips

3. Parallel execution:
   - Run multiple workflows simultaneously
   - One per available CPU core

4. Check system resources:
   ```bash
   top -u $USER | grep ffmpeg
   ```

---

### Cache Issues

**Clear TTS Cache**:

```bash
rm -rf ~/.graphyn/cache/tts/*
```

**Rebuild Cache**:
- Re-run TTS nodes
- Cache automatically regenerates

**Cache Location**:
```bash
ls ~/.graphyn/cache/tts/
# Output: abc123def456.wav, ...
```

---

## Advanced Topics

### Custom TTS Engines

**Implement Your Own Adapter**

1. Create executable that accepts:
   ```bash
   --text "input text"
   --language "en"
   --voice "voice_id"
   --speed "1.5"
   --output "/path/to/output.wav"
   ```

2. Output PCM WAV file to `--output` path

3. Set environment variable:
   ```bash
   export GRAPHYN_TTS_EXECUTABLE=/path/to/custom-tts
   ```

4. Test:
   ```bash
   /path/to/custom-tts \
     --text "Hello" \
     --language en \
     --voice default \
     --speed 1.0 \
     --output /tmp/test.wav
   ```

---

### Batch Processing

**Process Multiple Videos**

While Graphyn doesn't have built-in batch nodes yet, you can:

1. Create a workflow for one video
2. Run it repeatedly with different input paths
3. Or use shell script:

```bash
#!/bin/bash
for video in videos/*.mp4; do
  output="${video%.mp4}_narrated.mp4"
  # Trigger workflow via REST API (if available)
  graphyn-cli run video-narration \
    --input "$video" \
    --output "$output"
done
```

---

### Performance Optimization

**For Large Videos**:

1. **Pre-process externally**:
   - Compress input video
   - Normalize audio
   - Downscale resolution

2. **Use temp SSD**:
   ```bash
   export GRAPHYN_HOME=/mnt/nvme/graphyn
   ```

3. **Monitor resources**:
   ```bash
   watch -n 1 'df -h ~/.graphyn/temp; free -h'
   ```

---

### Integrating with External Services

**Consume API Results**

```
[HTTP Request] → [JSON Parse] → [File Write] ──→ (metadata)
                                                    ↓
                                           [Video Import + Config]
```

---

### Script Node Integration

The **Kotlin Script node** (`script.eval`) is JVM-only but powerful for media workflows:

#### Use Case 1: Calculate Timing Offsets

**Workflow**: Narration timing sync

```kotlin
// Script node code:
// Given video metadata, calculate TTS narration speed to match duration
val videoDurationMs = input as? Double ?: 5000.0
val narrationWords = 50  // Expected word count
val wordsPerSecond = 2.5  // Typical speech rate
val requiredDurationMs = (narrationWords / wordsPerSecond) * 1000

// Calculate speed adjustment: 1.0 = normal, 2.0 = double speed
val speedAdjustment = videoDurationMs / requiredDurationMs
mapOf(
    "speed" to speedAdjustment,
    "message" to "Narration speed: ${String.format("%.2f", speedAdjustment)}x"
)
```

Then wire output `speed` → `media.text_to_speech` speed input.

#### Use Case 2: Build Video List Dynamically

**Workflow**: Conditional clip selection

```kotlin
// Input: list of video paths (as JSON string or comma-separated)
val clipPaths = (input as? String ?: "clip1.mp4,clip2.mp4").split(",")
val selectedClips = clipPaths
    .filter { it.endsWith(".mp4") }
    .take(4)  // Limit to 4 clips max

// Output as instructions for nodes
mapOf(
    "count" to selectedClips.size.toString(),
    "clips" to selectedClips.joinToString("|"),
    "ready" to (selectedClips.isNotEmpty()).toString()
)
```

#### Use Case 3: File Path Management

**Workflow**: Organize outputs by date/time

```kotlin
// Generate output path with timestamp
val timestamp = java.time.LocalDateTime.now()
    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss"))
val basePath = System.getProperty("user.home") + "/Videos/Graphyn"
val outputPath = "$basePath/output_$timestamp.mp4"

mapOf(
    "output_path" to outputPath,
    "timestamp" to timestamp
)
```

Then wire to `media.video_encode` output_path input.

#### Use Case 4: Quality Decision Logic

**Workflow**: Choose encoding bitrate based on duration

```kotlin
// Input: video duration in milliseconds
val durationMs = input as? Double ?: 0.0
val durationMins = durationMs / 60000.0

val bitrate = when {
    durationMins < 1.0 -> "high"      // Short clips: high quality
    durationMins < 10.0 -> "medium"   // Medium: balanced
    else -> "low"                       // Long: space-saving
}

mapOf(
    "bitrate" to bitrate,
    "duration_mins" to String.format("%.1f", durationMins)
)
```

Wire to `media.video_encode` bitrate input.

#### Use Case 5: Metadata Extraction & Transformation

**Workflow**: Parse and restructure video metadata

```kotlin
// Input: structured video metadata
val metadata = mapOf(
    "width" to 1920,
    "height" to 1080,
    "fps" to 30.0,
    "duration_ms" to 5000.0
)

// Compute derived fields
val aspectRatio = metadata["width"] as Int / (metadata["height"] as Int).toDouble()
val totalFrames = (metadata["duration_ms"] as Double / 1000.0) * 
                  (metadata["fps"] as Double)

mapOf(
    "aspect_ratio" to String.format("%.2f", aspectRatio),
    "total_frames" to totalFrames.toInt().toString(),
    "summary" to "Video: ${metadata["width"]}x${metadata["height"]} @ ${metadata["fps"]}fps"
)
```

---

## Script Node Patterns for Media

### Pattern 1: Pre-Processing (Calculate → Set)

```
[Video Import] → [Script: Calc Speed] → [TTS (speed = result)]
```

### Pattern 2: Post-Processing (Inspect → Report)

```
[Video Encode] → [Script: Extract Path] → [File Write]
```

### Pattern 3: Conditional Routing

```
[Video Import] → [Script: Check Duration] 
    ├─ (if short) → [High Quality Encode]
    └─ (if long) → [Low Quality Encode]
```

### Pattern 4: Data Transformation

```
[Query Metadata] → [Script: Format JSON] → [HTTP Request]
```

---

## Limitations & Considerations

| Capability | Available | Notes |
|-----------|-----------|-------|
| Kotlin syntax | ✅ Full | Standard library, no external deps |
| Math operations | ✅ Yes | Numbers, strings, collections |
| Date/time | ✅ Yes | `java.time.*` available |
| File I/O | ✅ Limited | Read metadata, but not decode media |
| FFmpeg calls | ❌ No | Use media nodes instead |
| OpaqueType access | ⚠️ Tricky | Values are serialized; direct inspection limited |
| Multi-line code | ✅ Yes | Paste full Kotlin scripts |
| Imports | ✅ Yes | `import java.time.*` etc. |
| Error handling | ✅ Yes | Try-catch; errors output to `error` port |

---

## Practical Example: Complete Workflow with Script

**Goal**: Auto-encode video at quality matching its length

```
[Video Import] 
    ├→ duration_ms
    │       ↓
    │   [Script: Decide Bitrate]
    │       ↓ (bitrate)
    ├────────┘
    │       ↓
[Video Encode] → output.mp4
```

**Script Code**:

```kotlin
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// Input: duration in milliseconds from video_import.duration_ms
val durationMs = input as? Double ?: 0.0
val durationMins = durationMs / 60000.0

// Decide bitrate
val bitrate = when {
    durationMins < 2.0 -> "high"
    durationMins < 15.0 -> "medium"
    else -> "low"
}

// Generate output path
val timestamp = LocalDateTime.now()
    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss"))
val outputPath = "${System.getProperty("user.home")}/Videos/output_$timestamp.mp4"

// Return decision
mapOf(
    "bitrate" to bitrate,
    "output_path" to outputPath,
    "message" to "Duration: ${String.format("%.1f", durationMins)}m → $bitrate quality"
)
```

**Connections**:
```
video_import.duration_ms → script.input
script.result (parsed as bitrate) → video_encode.bitrate
script.result (parsed as output_path) → video_encode.output_path
```

---

## When to Use vs. When to Create a Node

| Task | Use Script | Create Node |
|------|-----------|-------------|
| Calculate timings | ✅ | ❌ |
| Format paths | ✅ | ❌ |
| Choose parameters | ✅ | ❌ |
| Heavy math | ✅ | ❌ |
| **Call external tools** | ❌ | ✅ |
| **Multi-step I/O** | ❌ | ✅ |
| **Reusable operation** | ❌ | ✅ |

---

## Tips for Media + Script Integration

1. **Serialize handles carefully** — OpaqueType values need special handling
2. **Use meaningful output names** — Makes wiring easier (`bitrate`, `speed`, `output_path`)
3. **Add `message` output** — Helps debug workflows visually
4. **Test script separately** — Paste code in REPL to verify logic
5. **Avoid large data** — Scripts are synchronous; keep operations quick

Would you like me to add a template workflow that uses the Script node for one of these patterns?

---

## FAQ

**Q: Can I edit videos in-place without FFmpeg?**

A: No. All video/audio operations require FFmpeg >= 4.0 on your system. It's the only backend in Phase 1.

**Q: When will fade transitions be available?**

A: Phase 2. Hard cuts ("cut") are the only transition in Phase 1.

**Q: Can I use this on mobile/web?**

A: Not yet. Phase 1 is JVM/Desktop only. Android, iOS, and web support is Phase 2+.

**Q: How long are TTS results cached?**

A: Until you manually clear `~/.graphyn/cache/tts/`. No automatic expiration.

**Q: Can I mix H.264 and H.265 videos?**

A: Not directly in Phase 1. FFmpeg will auto-transcode, but expect slower encoding. Pre-convert to matching codec first.

**Q: How do I get frame-by-frame control?**

A: Phase 2 adds `video_decode` (extract frames) and `video_compose` (layer frames). Use `media.video_stitch` with many clips for now.

---

## Resources

- **Plan Document**: [`docs/architecture/media-workflow-plan.md`](./architecture/media-workflow-plan.md)
- **Node Specs**: [`plugins/media-core/README.md`](../plugins/media-core/README.md)
- **TTS Setup**: [`plugins/media-ai/README.md`](../plugins/media-ai/README.md)
- **FFmpeg Docs**: [ffmpeg.org](https://ffmpeg.org)
- **Issue Tracker**: GitHub Issues (link your repo here)

---

## Feedback

Found an issue or have a suggestion? Please open an issue or contact the Graphyn team.
