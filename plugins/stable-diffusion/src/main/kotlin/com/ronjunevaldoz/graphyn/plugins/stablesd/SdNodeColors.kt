package com.ronjunevaldoz.graphyn.plugins.stablesd

/**
 * Port colours for the stable-diffusion.cpp plugin.
 *
 * Every OpaqueType port in this plugin carries a unique [portColor] that acts as a semantic
 * channel identifier. The editor's [PortCompatibility] check requires both ends of a connection
 * to share the same portColor, so cross-channel wiring is rejected at every validation layer
 * (picker popup, port-dot highlight, and final connection handler).
 *
 * ## OpaqueType channel map
 *
 * | Constant | Colour | Source node | Destination port |
 * |---|---|---|---|
 * | [COLOR_DIFFUSION] | deep purple | `sd.diffusion` output `diffusion` | `sd.model` input `diffusion` |
 * | [COLOR_ENCODERS]  | indigo      | `sd.encoders` output `encoders`   | `sd.model` input `encoders`  |
 * | [COLOR_VAE_PATH]  | teal        | `sd.vae` output `vae`             | `sd.model` input `vae`       |
 * | [COLOR_MODEL]     | bright purple | `sd.model` output `model`       | `sd.context` input `model`   |
 * | [COLOR_CONTEXT]   | orchid      | `sd.context` output `context`     | generation node input `context` |
 * | [COLOR_CONTROLNET]| deep orange | `sd.controlnet` output `controlnet` | generation node input `controlnet` |
 * | [COLOR_ID_COND]   | pink        | `sd.id_cond` output `id_cond`     | generation node input `id_cond` |
 *
 * Non-OpaqueType ports ([COLOR_SAMPLER], [COLOR_IMAGE], etc.) use colour for visual grouping only;
 * their type matching is handled by [WorkflowTypeCompatibility] without a portColor check.
 */
internal const val COLOR_DIFFUSION  = 0xFF7E57C2L // deep purple — diffusion model paths sub-token
internal const val COLOR_ENCODERS   = 0xFF5C6BC0L // indigo      — text encoder paths sub-token
internal const val COLOR_VAE_PATH   = 0xFF26A69AL // teal        — VAE path sub-token
internal const val COLOR_MODEL      = 0xFF6B6BF7L // bright purple — assembled model paths (sd.model → sd.context)
internal const val COLOR_CONTEXT    = 0xFFAB47BCL // orchid      — initialized context (sd.context → generation)
internal const val COLOR_CONTROLNET = 0xFFFF7043L // deep orange — ControlNet config token
internal const val COLOR_ID_COND    = 0xFFEC407AL // pink        — id-conditioning token
internal const val COLOR_SAMPLER    = 0xFF4CAF50L // green  — sampling config
internal const val COLOR_IMAGE      = 0xFF2196F3L // blue   — image data (path)
internal const val COLOR_VIDEO      = 0xFF00BCD4L // cyan   — video/frame data
internal const val COLOR_LORA       = 0xFFFF9800L // orange — LoRA config
internal const val COLOR_FLOAT      = 0xFFAAAAAAL // grey   — float/double scalar
internal const val COLOR_INT        = 0xFF909090L // dark grey — integer scalar
internal const val COLOR_BOOL       = 0xFF78909CL // blue-grey — boolean flag
internal const val COLOR_STRING     = 0xFF8BC34AL // light green — string / path
internal const val COLOR_LIST       = 0xFFE91E63L // pink — list of items

const val CATEGORY_SD      = "sd.generation"
const val CATEGORY_SD_CFG  = "sd.config"

// Enum string values sourced directly from stable-diffusion.cpp/src/stable-diffusion.cpp
internal val SD_RNG_TYPES = listOf("cuda", "std_default", "cpu")
internal val SD_SAMPLE_METHODS = listOf(
    "euler", "euler_a", "heun", "dpm2", "dpm++2s_a", "dpm++2m", "dpm++2mv2",
    "ipndm", "ipndm_v", "lcm", "ddim_trailing", "tcd",
    "res_multistep", "res_2s", "er_sde", "euler_cfg_pp", "euler_a_cfg_pp", "euler_ge",
)
internal val SD_SCHEDULERS = listOf(
    "discrete", "karras", "exponential", "ays", "gits", "sgm_uniform",
    "simple", "smoothstep", "kl_optimal", "lcm", "bong_tangent",
    "ltx2", "logit_normal", "flux2", "flux",
)
internal val SD_PREDICTIONS   = listOf("eps", "v", "edm_v", "sd3_flow", "flux_flow", "sefi_flow")
internal val SD_LORA_MODES    = listOf("auto", "immediately", "at_runtime")
internal val SD_VAE_FORMATS   = listOf("auto", "flux", "sd3", "flux2")
internal val SD_BACKENDS      = listOf("cuda", "vulkan", "metal", "opencl", "cpu")
internal val SD_WEIGHT_TYPES  = listOf(
    "f32", "f16", "bf16", "q8_0", "q8_1",
    "q4_0", "q4_1", "q4_k", "q5_0", "q5_1", "q5_k", "q6_k",
    "iq1_s", "iq2_xxs", "iq2_xs", "iq3_xxs", "iq4_nl",
)
internal val SD_HIRES_UPSCALERS = listOf(
    "None", "Latent", "Latent (nearest)", "Latent (nearest-exact)", "Latent (antialiased)",
    "Latent (bicubic)", "Latent (bicubic antialiased)", "Lanczos", "Nearest", "Model",
)
internal val SD_CACHE_MODES = listOf(
    "disabled", "easycache", "ucache", "dbcache", "taylorseer", "cache_dit", "spectrum",
)
