package com.ronjunevaldoz.graphyn.bootstrap

private val SCHEDULERS = mapOf(
    "discrete" to 0, "karras" to 1, "exponential" to 2, "ays" to 3,
    "gits" to 4, "sgm_uniform" to 5, "simple" to 6, "smoothstep" to 7,
    "kl_optimal" to 8, "lcm" to 9,
)

private val VALUE_FLAGS = setOf(
    "--prompt", "--negative-prompt", "--width", "--height", "--steps", "--seed",
    "--cfg-scale", "--sampling-method", "--scheduler", "--flow-shift", "--guidance",
    "--img-cfg-scale", "--eta", "--timestep-shift", "--strength", "--init-img",
    "--batch-count", "--clip-skip", "--control-image", "--control-strength", "--mask",
    "--hires-scale", "--hires-steps", "--hires-denoising-strength",
)

private val LORA_PATTERN = Regex("<lora:([^:>]+):([\\d.]+)>")

private fun String.jsonEscape() = replace("\\", "\\\\").replace("\"", "\\\"")

internal fun argsToJson(args: List<String>): String {
    val kv = mutableMapOf<String, String>()
    val flags = mutableSetOf<String>()
    val prompts = mutableListOf<String>()
    var i = 0
    while (i < args.size) {
        val flag = args[i++]
        if (flag in VALUE_FLAGS) {
            val v = args.getOrNull(i++) ?: continue
            if (flag == "--prompt") prompts += v else kv[flag] = v
        } else {
            flags += flag
        }
    }
    val loraPaths = prompts.mapNotNull { LORA_PATTERN.find(it)?.groupValues?.get(1) }
    val loraMultipliers = prompts.mapNotNull { LORA_PATTERN.find(it)?.groupValues?.get(2)?.toFloatOrNull() }
    val prompt = prompts.lastOrNull { !LORA_PATTERN.containsMatchIn(it) } ?: ""

    fun str(k: String, d: String = "") = (kv[k] ?: d).jsonEscape()
    fun int(k: String, d: Int = 0) = kv[k]?.toIntOrNull() ?: d
    fun float(k: String, d: Float = 0f) = kv[k]?.toFloatOrNull() ?: d
    fun long(k: String, d: Long = 0L) = kv[k]?.toLongOrNull() ?: d

    val loraPathJson = loraPaths.joinToString(",") { "\"${it.jsonEscape()}\"" }
    val loraMultJson = loraMultipliers.joinToString(",")

    return buildString {
        append("""{"prompt":"${prompt.jsonEscape()}","negativePrompt":"${str("--negative-prompt")}",""")
        append(""""width":${int("--width", 512)},"height":${int("--height", 512)},""")
        append(""""steps":${int("--steps", 20)},"seed":${long("--seed", -1L)},""")
        append(""""cfgScale":${float("--cfg-scale", 7.5f)},"samplingMethod":"${str("--sampling-method", "euler")}",""")
        append(""""scheduler":${SCHEDULERS[kv["--scheduler"]] ?: 0},""")
        append(""""flowShift":${float("--flow-shift", 3.0f)},"distilledGuidance":${float("--guidance")},""")
        append(""""imgCfg":${float("--img-cfg-scale")},"eta":${float("--eta")},""")
        append(""""shiftedTimestep":${int("--timestep-shift")},"strength":${float("--strength", 0.6f)},""")
        append(""""initImagePath":"${str("--init-img")}","batchCount":${int("--batch-count", 1)},""")
        append(""""clipSkip":${int("--clip-skip")},"controlImagePath":"${str("--control-image")}",""")
        append(""""controlStrength":${float("--control-strength", 0.9f)},"maskImagePath":"${str("--mask")}",""")
        append(""""vaeTilingEnabled":${"--vae-tiling" in flags},"hiresEnabled":${"--hires" in flags},""")
        append(""""hiresScale":${float("--hires-scale", 2.0f)},"hiresSteps":${int("--hires-steps")},""")
        append(""""hiresDenoisingStrength":${float("--hires-denoising-strength", 0.7f)},""")
        append(""""loraPaths":[$loraPathJson],"loraMultipliers":[$loraMultJson]}""")
    }
}
