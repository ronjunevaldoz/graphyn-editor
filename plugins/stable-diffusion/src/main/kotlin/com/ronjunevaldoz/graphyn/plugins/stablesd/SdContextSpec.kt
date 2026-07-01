package com.ronjunevaldoz.graphyn.plugins.stablesd

import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.NullableType
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.OpaqueType
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

/**
 * Node spec for `sd.context` — hardware and compute settings for a stable-diffusion.cpp context.
 *
 * Takes an opaque [SdModelSpec.model] token (model file paths) plus compute ports, and outputs
 * a merged context token consumed by generation nodes (sd.txt2img, sd.img2img, sd.txt2vid,
 * sd.img2vid). Wire one context to multiple generation nodes to share configuration.
 *
 * Model paths are separated into [SdModelSpec] so you can swap models without touching compute
 * settings. Ports here map to hardware fields in `sd_ctx_params_t` / `SDContextParams`.
 */
object SdContextSpec {
    val context = NodeSpec(
        type = "sd.context",
        label = "SD Context",
        description = "Hardware/compute settings. Wire sd.model → model, then context → generation nodes.",
        category = CATEGORY_SD_CFG,
        inputs = listOf(
            PortSpec("model", OpaqueType, portColor = COLOR_MODEL,
                description = "Opaque model-paths token from sd.model."),
            PortSpec("seamless", NullableType(OpaqueType), portColor = COLOR_CONTEXT,
                description = "Optional sd.seamless token (circular/tiling flags). Null = no seamless tiling."),
            PortSpec("offload", NullableType(OpaqueType), portColor = COLOR_CONTEXT,
                description = "Optional sd.offload token (max_vram + CPU offload). Null = server auto (max_vram=-1)."),
            PortSpec("chroma", NullableType(OpaqueType), portColor = COLOR_CONTEXT,
                description = "Optional sd.chroma token (Chroma DiT/T5 masking). Null = native defaults."),
        ) + sdContextComputePorts,
        outputs = listOf(
            PortSpec("context", OpaqueType, portColor = COLOR_CONTEXT,
                description = "Opaque model context passed to generation nodes."),
        ),
        defaultValues = mapOf(
            "n_threads"           to WorkflowValue.IntValue(-1),
            "rng_type"            to WorkflowValue.StringValue("cuda"),
            "lora_apply_mode"     to WorkflowValue.StringValue("auto"),
            "enable_mmap"         to WorkflowValue.BooleanValue(false),
            "flash_attn"          to WorkflowValue.BooleanValue(false),
            "diffusion_flash_attn" to WorkflowValue.BooleanValue(false),
            "diffusion_conv_direct" to WorkflowValue.BooleanValue(false),
            "vae_conv_direct"     to WorkflowValue.BooleanValue(false),
            "control_net_cpu"     to WorkflowValue.BooleanValue(false),
            "force_sdxl_vae_conv_scale" to WorkflowValue.BooleanValue(false),
            "qwen_image_zero_cond_t" to WorkflowValue.BooleanValue(false),
        ),
    )
}
