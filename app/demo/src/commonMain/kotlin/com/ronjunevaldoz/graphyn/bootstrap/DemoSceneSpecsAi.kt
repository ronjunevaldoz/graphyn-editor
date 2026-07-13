package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import com.ronjunevaldoz.graphyn.plugins.stylenodes.CATEGORY_AI

internal const val PORT_MODEL        = 0xFF6B6BF7L
internal const val PORT_CONDITIONING = 0xFFFF9900L
internal const val PORT_LATENT       = 0xFF9C27B0L
internal const val PORT_IMAGE        = 0xFF4CAF50L

val specCheckpointLoader = NodeSpec(
    type = "stylenodes.checkpoint_loader",
    label = "Checkpoint Loader",
    description = "Loads a diffusion model checkpoint, exposing its model, CLIP encoder, and VAE.",
    category = CATEGORY_AI,
    inputs = emptyList(),
    outputs = listOf(
        PortSpec("model", WorkflowType.OpaqueType, portColor = PORT_MODEL,   description = "Loaded diffusion model"),
        PortSpec("clip",  WorkflowType.OpaqueType, portColor = PORT_MODEL,   description = "CLIP text encoder"),
        PortSpec("vae",   WorkflowType.OpaqueType, portColor = PORT_LATENT,  description = "VAE decoder"),
    ),
)

val specClipEncode = NodeSpec(
    type = "stylenodes.clip_encode",
    label = "CLIP Text Encode",
    description = "Encodes a text prompt into CLIP conditioning for guiding image generation.",
    category = CATEGORY_AI,
    inputs = listOf(
        PortSpec("clip", WorkflowType.OpaqueType, portColor = PORT_MODEL,        description = "CLIP encoder"),
        PortSpec("text", WorkflowType.StringType, portColor = PORT_CONDITIONING,  description = "Prompt text to encode"),
    ),
    outputs = listOf(
        PortSpec("conditioning", WorkflowType.OpaqueType, portColor = PORT_CONDITIONING, description = "Encoded conditioning"),
    ),
)

val specVaeDecode = NodeSpec(
    type = "stylenodes.vae_decode",
    label = "VAE Decode",
    description = "Decodes a denoised latent back into pixel space.",
    category = CATEGORY_AI,
    inputs = listOf(
        PortSpec("samples", WorkflowType.OpaqueType, portColor = PORT_LATENT, description = "Denoised latent"),
        PortSpec("vae",     WorkflowType.OpaqueType, portColor = PORT_LATENT, description = "VAE decoder"),
    ),
    outputs = listOf(
        PortSpec("image", WorkflowType.OpaqueType, portColor = PORT_IMAGE, description = "Decoded image"),
    ),
)

val specSaveImage = NodeSpec(
    type = "stylenodes.save_image",
    label = "Save Image",
    description = "Writes the generated image to disk.",
    category = CATEGORY_AI,
    inputs = listOf(
        PortSpec("image", WorkflowType.OpaqueType, portColor = PORT_IMAGE, description = "Image to save"),
    ),
    outputs = emptyList(),
)
