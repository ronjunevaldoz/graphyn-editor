package com.ronjunevaldoz.graphyn.plugins.stablesd

import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.BooleanType
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.DoubleType
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.EnumType
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.IntType
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.ListType
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.NullableType
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.OpaqueType
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.StringType
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

/**
 * Node spec for `sd.hires` — encapsulates `sd_hires_params_t`.
 *
 * Wire to the optional `hires` port on generation nodes to enable hires-fix (upscale + re-denoise).
 * Defaults match `sd_hires_params_init()` in `stable-diffusion.cpp`.
 */
object SdHiresSpec {
    val hires = NodeSpec(
        type = "sd.hires",
        label = "SD HiRes Fix",
        description = "Hires-fix: upscale the low-res generation then re-denoise at target resolution (sd_hires_params_t).",
        category = CATEGORY_SD_CFG,
        inputs = listOf(
            PortSpec("enabled", BooleanType, portColor = COLOR_BOOL,
                description = "--hires: Enable hires fix."),
            PortSpec("upscaler", EnumType(SD_HIRES_UPSCALERS), portColor = COLOR_STRING,
                description = "--hires-upscaler: Upscaling algorithm used before the second denoise pass."),
            PortSpec("model_path", NullableType(StringType), portColor = COLOR_STRING,
                description = "Path to upscaler model weights (required when upscaler is 'Model')."),
            PortSpec("scale", DoubleType, portColor = COLOR_FLOAT,
                description = "--hires-scale: Multiplier for width and height. Default: 2.0."),
            PortSpec("target_width", IntType, portColor = COLOR_INT,
                description = "--hires-width: Explicit target width in px. 0 = derive from scale."),
            PortSpec("target_height", IntType, portColor = COLOR_INT,
                description = "--hires-height: Explicit target height in px. 0 = derive from scale."),
            PortSpec("steps", IntType, portColor = COLOR_INT,
                description = "--hires-steps: Denoising steps for the hires pass. 0 = reuse generation steps."),
            PortSpec("denoising_strength", DoubleType, portColor = COLOR_FLOAT,
                description = "--hires-denoising-strength: Strength of denoising in the hires pass. Default: 0.7."),
            PortSpec("upscale_tile_size", IntType, portColor = COLOR_INT,
                description = "--hires-upscale-tile-size: Tile size for tiled upscaling. Default: 128."),
            PortSpec("custom_sigmas", NullableType(ListType(DoubleType)), portColor = COLOR_FLOAT,
                description = "--hires-sigmas: Custom sigma schedule for the hires denoising pass."),
        ),
        outputs = listOf(
            PortSpec("hires", OpaqueType, portColor = COLOR_SAMPLER,
                description = "Opaque hires-fix config passed to generation nodes."),
        ),
        defaultValues = mapOf(
            "enabled"             to WorkflowValue.BooleanValue(true),
            "upscaler"            to WorkflowValue.StringValue("Latent"),
            "scale"               to WorkflowValue.DoubleValue(2.0),
            "target_width"        to WorkflowValue.IntValue(0),
            "target_height"       to WorkflowValue.IntValue(0),
            "steps"               to WorkflowValue.IntValue(0),
            "denoising_strength"  to WorkflowValue.DoubleValue(0.7),
            "upscale_tile_size"   to WorkflowValue.IntValue(128),
        ),
    )
}
