package com.ronjunevaldoz.graphyn.bootstrap

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val SCHEDULERS = mapOf(
    "discrete" to 0, "karras" to 1, "exponential" to 2, "ays" to 3,
    "gits" to 4, "sgm_uniform" to 5, "simple" to 6, "smoothstep" to 7,
    "kl_optimal" to 8, "lcm" to 9,
)

/**
 * Every value-less CLI toggle emitted anywhere across the `build*Args` helpers in
 * `plugins/stable-diffusion` (`SdCliContextArgs.kt`, `SdCliMediaArgs.kt`, the image/video
 * executors). [parseArgs] must know these so it doesn't consume the *next* token as this
 * flag's value — the previous version assumed the opposite default (anything not in a small
 * "known value flags" allowlist was treated as boolean), which silently misparsed any newly
 * added value-taking flag that hadn't also been added to that allowlist.
 */
private val BOOLEAN_FLAGS = setOf(
    "--offload-to-cpu", "--mmap", "--fa", "--diffusion-fa", "--diffusion-conv-direct",
    "--vae-conv-direct", "--clip-on-cpu", "--vae-on-cpu", "--control-net-cpu", "--stream-layers",
    "--eager-load", "--circular", "--circularx", "--circulary", "--force-sdxl-vae-conv-scale",
    "--chroma-disable-dit-mask", "--chroma-enable-t5-mask", "--qwen-image-zero-cond-t", "--hires",
    "--disable-auto-resize-ref-image", "--increase-ref-index", "--vae-tiling", "--temporal-tiling",
    "--disable-image-metadata",
)

private val LORA_PATTERN = Regex("<lora:([^:>]+):([\\d.]+)>")

private val requestJson = Json { encodeDefaults = true }

private class ParsedArgs(
    val kv: Map<String, String>,
    val flags: Set<String>,
    val prompts: List<String>,
)

/**
 * Every token starting with `--` consumes the next token as its value *unless* it's in
 * [BOOLEAN_FLAGS] — the safe default, since almost every sd-cli flag takes a value and a
 * value-less flag mistakenly treated as value-taking would just consume the *next* flag as a
 * literal string (loud, easy to spot), whereas the old inverted default silently swallowed the
 * wrong token with no error at all.
 */
private fun parseArgs(args: List<String>): ParsedArgs {
    val kv = mutableMapOf<String, String>()
    val flags = mutableSetOf<String>()
    val prompts = mutableListOf<String>()
    var i = 0
    while (i < args.size) {
        val flag = args[i++]
        if (flag == "--prompt") {
            prompts += args.getOrNull(i++) ?: continue
        } else if (flag in BOOLEAN_FLAGS) {
            flags += flag
        } else if (flag.startsWith("--")) {
            kv[flag] = args.getOrNull(i++) ?: continue
        } else {
            flags += flag
        }
    }
    return ParsedArgs(kv, flags, prompts)
}

@Serializable
internal data class SdGenerateImageRequest(
    val prompt: String,
    val negativePrompt: String = "",
    val width: Int = 512,
    val height: Int = 512,
    val steps: Int = 20,
    val seed: Long = -1L,
    val cfgScale: Float = 7.5f,
    val samplingMethod: String = "euler",
    val scheduler: Int = 0,
    val flowShift: Float = 3.0f,
    val distilledGuidance: Float = 0f,
    val imgCfg: Float = 0f,
    val eta: Float = 0f,
    val shiftedTimestep: Int = 0,
    val strength: Float = 0.6f,
    val initImagePath: String = "",
    val batchCount: Int = 1,
    val clipSkip: Int = 0,
    val controlImagePath: String = "",
    val controlStrength: Float = 0.9f,
    val maskImagePath: String = "",
    val vaeTilingEnabled: Boolean = false,
    val hiresEnabled: Boolean = false,
    val hiresScale: Float = 2.0f,
    val hiresSteps: Int = 0,
    val hiresDenoisingStrength: Float = 0.7f,
    val diffusionModelPath: String = "",
    val clipLPath: String = "",
    val clipGPath: String = "",
    val t5xxlPath: String = "",
    val vaePath: String = "",
    val llmPath: String = "",
    val llmVisionPath: String = "",
    val qwenImageZeroCondT: Boolean = false,
    val backend: String = "",
    val nThreads: Int = -1,
    val maxVram: String = "",
    val streamLayers: Boolean = false,
    val diffusionFa: Boolean = false,
    val loraPaths: List<String> = emptyList(),
    val loraMultipliers: List<Float> = emptyList(),
)

@Serializable
internal data class SdGenerateVideoRequest(
    val prompt: String,
    val negativePrompt: String = "",
    val initImagePath: String = "",
    val width: Int = 832,
    val height: Int = 480,
    val videoFrames: Int = 81,
    val fps: Int = 16,
    val steps: Int = 4,
    val moeBoundary: Float = 0.5f,
    val seed: Long = -1L,
    val cfgScale: Float = 1.0f,
    val flowShift: Float = 5.0f,
    val samplingMethod: String = "euler",
    val lowNoiseModelPath: String = "",
    val highNoiseModelPath: String = "",
    val clipVisionPath: String = "",
    val textEncoderPath: String = "",
    val vaePath: String = "",
    val loraPaths: List<String> = emptyList(),
    val loraMultipliers: List<Float> = emptyList(),
    val loraHighNoise: List<Boolean> = emptyList(),
)

/**
 * Translates the video executor's `--flag value` list into a `/api/sd/generate-video` JSON body.
 * Inline `<lora:name:mult>` tags are lifted into structured lora arrays; `is_high_noise` is
 * inferred from the lora name (Wan MoE LoRAs are named `*_high_noise_*` / `*_low_noise_*`).
 */
internal fun videoArgsToJson(args: List<String>): String {
    val parsed = parseArgs(args)
    val kv = parsed.kv
    val prompts = parsed.prompts
    val loraPaths = prompts.mapNotNull { LORA_PATTERN.find(it)?.groupValues?.get(1) }
    val loraMults = prompts.mapNotNull { LORA_PATTERN.find(it)?.groupValues?.get(2)?.toFloatOrNull() }
    val prompt = prompts.lastOrNull { !LORA_PATTERN.containsMatchIn(it) } ?: ""

    fun str(k: String, d: String = "") = kv[k] ?: d
    fun int(k: String, d: Int = 0) = kv[k]?.toIntOrNull() ?: d
    fun float(k: String, d: Float = 0f) = kv[k]?.toFloatOrNull() ?: d
    fun long(k: String, d: Long = 0L) = kv[k]?.toLongOrNull() ?: d

    val request = SdGenerateVideoRequest(
        prompt = prompt,
        negativePrompt = str("--negative-prompt"),
        initImagePath = str("--init-img"),
        width = int("--width", 832),
        height = int("--height", 480),
        videoFrames = int("--video-frames", 81),
        fps = int("--fps", 16),
        steps = int("--steps", 4),
        moeBoundary = float("--moe-boundary", 0.5f),
        seed = long("--seed", -1L),
        cfgScale = float("--cfg-scale", 1.0f),
        flowShift = float("--flow-shift", 5.0f),
        samplingMethod = str("--sampling-method", "euler"),
        lowNoiseModelPath = str("--diffusion-model"),
        highNoiseModelPath = str("--high-noise-diffusion-model"),
        clipVisionPath = str("--clip_vision"),
        textEncoderPath = str("--t5xxl"),
        vaePath = str("--vae"),
        loraPaths = loraPaths,
        loraMultipliers = loraMults,
        loraHighNoise = loraPaths.map { it.contains("high_noise") },
    )
    return requestJson.encodeToString(SdGenerateVideoRequest.serializer(), request)
}

internal fun argsToJson(args: List<String>): String {
    val parsed = parseArgs(args)
    val kv = parsed.kv
    val flags = parsed.flags
    val prompts = parsed.prompts
    val loraPaths = prompts.mapNotNull { LORA_PATTERN.find(it)?.groupValues?.get(1) }
    val loraMultipliers = prompts.mapNotNull { LORA_PATTERN.find(it)?.groupValues?.get(2)?.toFloatOrNull() }
    val prompt = prompts.lastOrNull { !LORA_PATTERN.containsMatchIn(it) } ?: ""

    fun str(k: String, d: String = "") = kv[k] ?: d
    fun int(k: String, d: Int = 0) = kv[k]?.toIntOrNull() ?: d
    fun float(k: String, d: Float = 0f) = kv[k]?.toFloatOrNull() ?: d
    fun long(k: String, d: Long = 0L) = kv[k]?.toLongOrNull() ?: d

    val request = SdGenerateImageRequest(
        prompt = prompt,
        negativePrompt = str("--negative-prompt"),
        width = int("--width", 512),
        height = int("--height", 512),
        steps = int("--steps", 20),
        seed = long("--seed", -1L),
        cfgScale = float("--cfg-scale", 7.5f),
        samplingMethod = str("--sampling-method", "euler"),
        scheduler = SCHEDULERS[kv["--scheduler"]] ?: 0,
        flowShift = float("--flow-shift", 3.0f),
        distilledGuidance = float("--guidance"),
        imgCfg = float("--img-cfg-scale"),
        eta = float("--eta"),
        shiftedTimestep = int("--timestep-shift"),
        strength = float("--strength", 0.6f),
        initImagePath = str("--init-img"),
        batchCount = int("--batch-count", 1),
        clipSkip = int("--clip-skip"),
        controlImagePath = str("--control-image"),
        controlStrength = float("--control-strength", 0.9f),
        maskImagePath = str("--mask"),
        vaeTilingEnabled = "--vae-tiling" in flags,
        hiresEnabled = "--hires" in flags,
        hiresScale = float("--hires-scale", 2.0f),
        hiresSteps = int("--hires-steps"),
        hiresDenoisingStrength = float("--hires-denoising-strength", 0.7f),
        diffusionModelPath = str("--diffusion-model"),
        clipLPath = str("--clip_l"),
        clipGPath = str("--clip_g"),
        t5xxlPath = str("--t5xxl"),
        vaePath = str("--vae"),
        llmPath = str("--llm"),
        llmVisionPath = str("--llm_vision"),
        qwenImageZeroCondT = "--qwen-image-zero-cond-t" in flags,
        backend = str("--backend"),
        nThreads = int("--threads", -1),
        maxVram = str("--max-vram"),
        streamLayers = "--stream-layers" in flags,
        diffusionFa = "--diffusion-fa" in flags || "--fa" in flags,
        loraPaths = loraPaths,
        loraMultipliers = loraMultipliers,
    )
    return requestJson.encodeToString(SdGenerateImageRequest.serializer(), request)
}
