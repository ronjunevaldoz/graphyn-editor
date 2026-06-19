package com.ronjunevaldoz.graphyn.plugins.preview

import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType

internal const val CATEGORY_PREVIEW = "preview"

/**
 * A pass-through viewer node. Wire any output port to [value] and the card displays the
 * last execution result inline on the canvas. The value is also forwarded on the [value]
 * output so the node can sit mid-pipeline without breaking the flow.
 */
internal val specPreviewView = NodeSpec(
    type = "preview.view",
    label = "Preview",
    category = CATEGORY_PREVIEW,
    inputs  = listOf(PortSpec("value", WorkflowType.OpaqueType, required = false)),
    outputs = listOf(PortSpec("value", WorkflowType.OpaqueType)),
)
