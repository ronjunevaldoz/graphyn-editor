package com.ronjunevaldoz.graphyn.plugins.stablesd

import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.BooleanType
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.DoubleType
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.IntType
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.NullableType
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.OpaqueType
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.StringType
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

/**
 * Node spec for `sd.vae_tiling` — encapsulates `sd_tiling_params_t`.
 *
 * Splits the VAE encode/decode into tiles to reduce peak VRAM for large images/videos.
 * Wire to the optional `vae_tiling` port on generation nodes.
 */
object SdTilingSpec {
    val vaeTiling = NodeSpec(
        type = "sd.vae_tiling",
        label = "SD VAE Tiling",
        description = "Tiled VAE encode/decode to reduce VRAM for large images or video frames (sd_tiling_params_t).",
        category = CATEGORY_SD_CFG,
        inputs = listOf(
            PortSpec("enabled", BooleanType, portColor = COLOR_BOOL,
                description = "--vae-tiling: Enable VAE tiling."),
            PortSpec("temporal_tiling", BooleanType, portColor = COLOR_BOOL,
                description = "--temporal-tiling: Also tile along the temporal dimension (video frames)."),
            PortSpec("tile_size_x", IntType, portColor = COLOR_INT,
                description = "--vae-tile-size: Tile width in pixels. 0 = auto."),
            PortSpec("tile_size_y", IntType, portColor = COLOR_INT,
                description = "Tile height in pixels. 0 = auto (matches tile_size_x if set)."),
            PortSpec("target_overlap", DoubleType, portColor = COLOR_FLOAT,
                description = "--vae-tile-overlap: Target overlap fraction between adjacent tiles. Default: 0.5."),
            PortSpec("rel_size_x", DoubleType, portColor = COLOR_FLOAT,
                description = "--vae-relative-tile-size: Tile width as fraction of image width. 0.0 = use tile_size_x."),
            PortSpec("rel_size_y", DoubleType, portColor = COLOR_FLOAT,
                description = "Tile height as fraction of image height. 0.0 = use tile_size_y."),
            PortSpec("extra_tiling_args", NullableType(StringType), portColor = COLOR_STRING,
                description = "--extra-tiling-args: Extra tiling arguments passed verbatim to sd-cli."),
        ),
        outputs = listOf(
            PortSpec("vae_tiling", OpaqueType, portColor = COLOR_SAMPLER,
                description = "Opaque VAE tiling config passed to generation nodes."),
        ),
        defaultValues = mapOf(
            "enabled"        to WorkflowValue.BooleanValue(true),
            "temporal_tiling" to WorkflowValue.BooleanValue(false),
            "tile_size_x"    to WorkflowValue.IntValue(0),
            "tile_size_y"    to WorkflowValue.IntValue(0),
            "target_overlap" to WorkflowValue.DoubleValue(0.5),
            "rel_size_x"     to WorkflowValue.DoubleValue(0.0),
            "rel_size_y"     to WorkflowValue.DoubleValue(0.0),
        ),
    )
}
