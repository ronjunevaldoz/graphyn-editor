package com.ronjunevaldoz.graphyn.plugins.stablesd

import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.NullableType
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.OpaqueType
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.StringType

/**
 * Node spec for `sd.model` — assembles a complete model-paths token for a stable-diffusion.cpp context.
 *
 * Accepts three optional opaque sub-tokens and a set of auxiliary paths. Wire:
 * - [SdDiffusionSpec] → `diffusion` (checkpoint / split-format diffusion weights)
 * - [SdEncodersSpec]  → `encoders`  (CLIP-L, T5-XXL, LLM, …)
 * - [SdVaeSpec]       → `vae`       (VAE path, format, TAESD)
 *
 * All three are optional — omit any sub-node whose paths are bundled in the checkpoint.
 */
object SdModelSpec {
    val model = NodeSpec(
        type = "sd.model",
        label = "SD Model",
        description = "Assembles model paths. Wire sd.diffusion, sd.encoders, sd.vae as needed, then wire model → sd.context.",
        category = CATEGORY_SD_CFG,
        inputs = listOf(
            PortSpec("diffusion", NullableType(OpaqueType), portColor = COLOR_DIFFUSION,
                description = "Optional diffusion-model-paths token from sd.diffusion (checkpoint or split weights)."),
            PortSpec("encoders", NullableType(OpaqueType), portColor = COLOR_ENCODERS,
                description = "Optional encoder-paths token from sd.encoders (CLIP-L, T5-XXL, LLM, etc.)."),
            PortSpec("vae", NullableType(OpaqueType), portColor = COLOR_VAE_PATH,
                description = "Optional VAE config token from sd.vae (vae_path, vae_format, taesd, audio_vae)."),
        ) + sdContextPathPorts,
        outputs = listOf(
            PortSpec("model", OpaqueType, portColor = COLOR_MODEL,
                description = "Opaque model-paths token consumed by sd.context."),
        ),
    )
}
