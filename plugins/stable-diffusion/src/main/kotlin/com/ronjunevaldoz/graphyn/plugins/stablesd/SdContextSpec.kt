package com.ronjunevaldoz.graphyn.plugins.stablesd

import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.OpaqueType
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

/**
 * Node spec for `sd.context` — configures and loads a stable-diffusion.cpp model context.
 *
 * Outputs an opaque [SdContextToken] consumed by generation nodes (sd.txt2img, sd.img2img,
 * sd.txt2vid, sd.img2vid). Loading is expensive; wire one context node to multiple generation
 * nodes to share the configuration without repeating it.
 *
 * All input ports map 1-to-1 to fields in `sd_ctx_params_t` / `SDContextParams`.
 * Ports are split across [sdContextPathPorts] (model files) and [sdContextComputePorts] (hardware).
 */
object SdContextSpec {
    val context = NodeSpec(
        type = "sd.context",
        label = "SD Context",
        description = "Loads a stable-diffusion.cpp model. Wire to sd.txt2img / sd.img2img / sd.txt2vid / sd.img2vid.",
        category = CATEGORY_SD_CFG,
        inputs = sdContextPathPorts + sdContextComputePorts,
        outputs = listOf(
            PortSpec("context", OpaqueType, portColor = COLOR_MODEL,
                description = "Opaque model context passed to generation nodes."),
        ),
        defaultValues = mapOf(
            "vae_format"          to WorkflowValue.StringValue("auto"),
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
            "circular"            to WorkflowValue.BooleanValue(false),
            "circular_x"          to WorkflowValue.BooleanValue(false),
            "circular_y"          to WorkflowValue.BooleanValue(false),
            "force_sdxl_vae_conv_scale" to WorkflowValue.BooleanValue(false),
            "chroma_use_dit_mask" to WorkflowValue.BooleanValue(true),
            "chroma_use_t5_mask"  to WorkflowValue.BooleanValue(false),
            "chroma_t5_mask_pad"  to WorkflowValue.IntValue(1),
            "qwen_image_zero_cond_t" to WorkflowValue.BooleanValue(false),
        ),
    )
}
