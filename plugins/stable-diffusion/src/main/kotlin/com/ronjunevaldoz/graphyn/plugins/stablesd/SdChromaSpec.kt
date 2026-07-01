package com.ronjunevaldoz.graphyn.plugins.stablesd

import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.BooleanType
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.IntType
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.OpaqueType
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

/**
 * Node spec for `sd.chroma` — Chroma-model masking options, pulled out of `sd.context`.
 *
 * Only relevant to Chroma checkpoints; wire to `sd.context`'s `chroma` port when using one. Absent
 * = native defaults (DiT mask on, T5 mask off, pad 1). Flags merge back into the context at runtime.
 */
object SdChromaSpec {
    val chroma = NodeSpec(
        type = "sd.chroma",
        label = "SD Chroma",
        description = "Chroma-model DiT/T5 masking. Wire to sd.context's chroma port (Chroma checkpoints only).",
        category = CATEGORY_SD_CFG,
        inputs = listOf(
            PortSpec("chroma_use_dit_mask", BooleanType, portColor = COLOR_BOOL,
                description = "false → --chroma-disable-dit-mask. Default true (enabled)."),
            PortSpec("chroma_use_t5_mask", BooleanType, portColor = COLOR_BOOL,
                description = "true → --chroma-enable-t5-mask. Default false."),
            PortSpec("chroma_t5_mask_pad", IntType, portColor = COLOR_INT,
                description = "--chroma-t5-mask-pad: T5 mask padding. Default 1."),
        ),
        outputs = listOf(
            PortSpec("chroma", OpaqueType, portColor = COLOR_CONTEXT,
                description = "Opaque Chroma config; connect to sd.context's chroma input."),
        ),
        defaultValues = mapOf(
            "chroma_use_dit_mask" to WorkflowValue.BooleanValue(true),
            "chroma_use_t5_mask"  to WorkflowValue.BooleanValue(false),
            "chroma_t5_mask_pad"  to WorkflowValue.IntValue(1),
        ),
    )
}
