package com.ronjunevaldoz.graphyn.plugins.stablesd

// Port colours — matched to stable-diffusion.cpp semantic domains
internal const val COLOR_MODEL      = 0xFF6B6BF7L // purple — loaded model/context
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
internal val SD_PREDICTIONS = listOf("eps", "v", "edm_v", "sd3_flow", "flux_flow", "sefi_flow")
internal val SD_LORA_MODES  = listOf("auto", "immediately", "at_runtime")
internal val SD_VAE_FORMATS = listOf("auto", "flux", "sd3", "flux2")
internal val SD_HIRES_UPSCALERS = listOf(
    "None", "Latent", "Latent (nearest)", "Latent (nearest-exact)", "Latent (antialiased)",
    "Latent (bicubic)", "Latent (bicubic antialiased)", "Lanczos", "Nearest", "Model",
)
internal val SD_CACHE_MODES = listOf(
    "disabled", "easycache", "ucache", "dbcache", "taylorseer", "cache_dit", "spectrum",
)
