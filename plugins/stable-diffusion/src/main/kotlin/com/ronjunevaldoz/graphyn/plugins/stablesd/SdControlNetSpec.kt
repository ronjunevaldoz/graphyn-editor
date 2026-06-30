package com.ronjunevaldoz.graphyn.plugins.stablesd

import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.DoubleType
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.NullableType
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.OpaqueType
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.StringType
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

/**
 * Node spec for `sd.controlnet` — ControlNet conditioning and inpainting mask config.
 *
 * Outputs an opaque token consumed by generation nodes (sd.txt2img, sd.img2img).
 * Only wire this node when using ControlNet (SD1.5 only) or inpainting — it's optional.
 */
object SdControlNetSpec {
    val controlNet = NodeSpec(
        type = "sd.controlnet",
        label = "SD ControlNet",
        description = "ControlNet conditioning image and inpainting mask. Wire controlnet → generation node.",
        category = CATEGORY_SD_CFG,
        inputs = listOf(
            PortSpec("control_image", NullableType(StringType), portColor = COLOR_IMAGE,
                description = "--control-image: Path to the ControlNet conditioning image (SD1.5 only)."),
            PortSpec("control_strength", DoubleType, portColor = COLOR_FLOAT,
                description = "--control-strength: ControlNet influence weight. Default: 0.9."),
            PortSpec("mask_image", NullableType(StringType), portColor = COLOR_IMAGE,
                description = "--mask: Inpainting mask path (white = inpaint, black = preserve)."),
        ),
        outputs = listOf(
            PortSpec("controlnet", OpaqueType, portColor = COLOR_CONTROLNET,
                description = "Opaque ControlNet config token consumed by generation nodes."),
        ),
        defaultValues = mapOf(
            "control_strength" to WorkflowValue.DoubleValue(0.9),
        ),
    )
}
