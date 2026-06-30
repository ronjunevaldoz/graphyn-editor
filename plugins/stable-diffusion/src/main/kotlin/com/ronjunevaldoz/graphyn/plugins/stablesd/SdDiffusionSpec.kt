package com.ronjunevaldoz.graphyn.plugins.stablesd

import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.NullableType
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.OpaqueType
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.StringType

/**
 * Node spec for `sd.diffusion` — the diffusion model (UNet/DiT/MMDiT) path group.
 *
 * Wire diffusion → [SdModelSpec.model]. Separate from encoders and VAE so you can swap
 * the diffusion checkpoint independently (e.g. switch between Schnell and Dev without
 * touching the shared T5-XXL encoder).
 *
 * Use `model_path` for all-in-one checkpoints; use `diffusion_model_path` for split models
 * where the diffusion weights are separate from the encoders.
 */
object SdDiffusionSpec {
    val diffusion = NodeSpec(
        type = "sd.diffusion",
        label = "SD Diffusion",
        description = "Diffusion model paths (UNet/DiT/MMDiT). Wire diffusion → sd.model.",
        category = CATEGORY_SD_CFG,
        inputs = listOf(
            PortSpec("model_path", NullableType(StringType), portColor = COLOR_STRING,
                description = "--model: All-in-one checkpoint (.ckpt/.safetensors/.gguf). Use this OR diffusion_model_path."),
            PortSpec("diffusion_model_path", NullableType(StringType), portColor = COLOR_STRING,
                description = "--diffusion-model: Diffusion model weights separate from the full checkpoint (FLUX/SD3 split format)."),
            PortSpec("high_noise_diffusion_model_path", NullableType(StringType), portColor = COLOR_STRING,
                description = "--high-noise-diffusion-model: High-noise diffusion model for MoE video models (e.g. Wan)."),
            PortSpec("uncond_diffusion_model_path", NullableType(StringType), portColor = COLOR_STRING,
                description = "--uncond-diffusion-model: Unconditional diffusion model path for models that require it."),
            PortSpec("embeddings_connectors_path", NullableType(StringType), portColor = COLOR_STRING,
                description = "--embeddings-connectors: Embedding connector weights (used in some multi-modal architectures)."),
        ),
        outputs = listOf(
            PortSpec("diffusion", OpaqueType, portColor = COLOR_DIFFUSION,
                description = "Opaque diffusion-model-paths token consumed by sd.model."),
        ),
    )
}
