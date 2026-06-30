package com.ronjunevaldoz.graphyn.plugins.stablesd

import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.EnumType
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.NullableType
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.OpaqueType
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.StringType
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

/**
 * Node spec for `sd.vae` — VAE decoder/encoder paths and format config.
 *
 * Wire vae → [SdModelSpec.model]. Separate from the diffusion model so you can override the
 * VAE without re-specifying the whole model path (e.g. swap a baked-in SD1.5 VAE for a
 * colour-corrected variant).
 *
 * Omit this node when the checkpoint bundles an acceptable VAE.
 */
object SdVaeSpec {
    val vae = NodeSpec(
        type = "sd.vae",
        label = "SD VAE",
        description = "VAE model path and format override. Wire vae → sd.model.",
        category = CATEGORY_SD_CFG,
        inputs = listOf(
            PortSpec("vae_path", NullableType(StringType), portColor = COLOR_STRING,
                description = "--vae: Override VAE weights path."),
            PortSpec("vae_format", EnumType(SD_VAE_FORMATS), portColor = COLOR_STRING,
                description = "--vae-format: Force VAE variant (auto/flux/sd3/flux2). Default: auto."),
            PortSpec("audio_vae_path", NullableType(StringType), portColor = COLOR_STRING,
                description = "--audio-vae: Audio VAE path for video models that generate audio."),
            PortSpec("taesd_path", NullableType(StringType), portColor = COLOR_STRING,
                description = "--taesd: TAESD fast latent decoder (enables --preview tae)."),
        ),
        outputs = listOf(
            PortSpec("vae", OpaqueType, portColor = COLOR_VAE_PATH,
                description = "Opaque VAE config token consumed by sd.model."),
        ),
        defaultValues = mapOf(
            "vae_format" to WorkflowValue.StringValue("auto"),
        ),
    )
}
