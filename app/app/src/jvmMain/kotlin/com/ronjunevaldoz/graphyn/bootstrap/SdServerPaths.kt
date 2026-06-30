package com.ronjunevaldoz.graphyn.bootstrap

// Flags whose value is a server-side file path, plus inline <lora:PATH:mult> tags.
private val MODEL_FLAGS = setOf(
    "--diffusion-model", "--high-noise-diffusion-model", "--clip_l", "--clip_g",
    "--clip_vision", "--t5xxl", "--llm", "--vae", "--init-img", "--control-image", "--mask",
)
private val LORA_RE = Regex("<lora:([^:>]+):")

/** The distinct server-side file paths a generation depends on: model/encoder/vae/input flags + LoRAs. */
internal fun collectServerPaths(args: List<String>): List<String> {
    val paths = mutableListOf<String>()
    args.forEachIndexed { i, a -> if (a in MODEL_FLAGS) args.getOrNull(i + 1)?.let { paths += it } }
    args.forEach { a -> LORA_RE.findAll(a).forEach { paths += it.groupValues[1] } }
    return paths.filter { it.isNotBlank() }.distinct()
}
