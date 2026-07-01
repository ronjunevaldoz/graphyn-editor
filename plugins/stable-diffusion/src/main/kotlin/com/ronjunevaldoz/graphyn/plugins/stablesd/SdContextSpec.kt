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
            "clip_on_cpu"         to WorkflowValue.BooleanValue(false),
            "vae_on_cpu"          to WorkflowValue.BooleanValue(false),
            "control_net_cpu"     to WorkflowValue.BooleanValue(false),
            "stream_layers"       to WorkflowValue.BooleanValue(false),
            "eager_load"          to WorkflowValue.BooleanValue(false),
            "offload_params_to_cpu" to WorkflowValue.BooleanValue(false),
            "force_sdxl_vae_conv_scale" to WorkflowValue.BooleanValue(false),
            "chroma_use_dit_mask" to WorkflowValue.BooleanValue(true),
            "chroma_use_t5_mask"  to WorkflowValue.BooleanValue(false),
            "chroma_t5_mask_pad"  to WorkflowValue.IntValue(1),
            "qwen_image_zero_cond_t" to WorkflowValue.BooleanValue(false),
        ),
    )
}
