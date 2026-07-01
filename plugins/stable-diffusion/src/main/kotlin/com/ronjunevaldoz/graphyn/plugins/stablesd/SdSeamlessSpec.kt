package com.ronjunevaldoz.graphyn.plugins.stablesd

import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.BooleanType
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.OpaqueType
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

/**
 * Node spec for `sd.seamless` — circular padding for tileable (seamless) textures.
 *
 * Pulled out of the 37-field `sd.context` node: seamless tiling is a rarely-used, self-contained
 * feature, so it lives as an optional sub-node wired to `sd.context`'s `seamless` port (the same
 * pattern as sd.lora/sd.hires). Its three flags are merged back into the context at execution time.
 */
object SdSeamlessSpec {
    val seamless = NodeSpec(
        type = "sd.seamless",
        label = "SD Seamless",
        description = "Circular padding for seamless/tileable textures. Wire to sd.context's seamless port.",
        category = CATEGORY_SD_CFG,
        inputs = listOf(
            PortSpec("circular", BooleanType, portColor = COLOR_BOOL,
                description = "--circular: circular padding in BOTH X and Y (seamless tiling on both axes)."),
            PortSpec("circular_x", BooleanType, portColor = COLOR_BOOL,
                description = "--circularx: circular padding in X only (horizontal tiling)."),
            PortSpec("circular_y", BooleanType, portColor = COLOR_BOOL,
                description = "--circulary: circular padding in Y only (vertical tiling)."),
        ),
        outputs = listOf(
            PortSpec("seamless", OpaqueType, portColor = COLOR_CONTEXT,
                description = "Opaque seamless config; connect to sd.context's seamless input."),
        ),
        defaultValues = mapOf(
            "circular"   to WorkflowValue.BooleanValue(false),
            "circular_x" to WorkflowValue.BooleanValue(false),
            "circular_y" to WorkflowValue.BooleanValue(false),
        ),
    )
}
