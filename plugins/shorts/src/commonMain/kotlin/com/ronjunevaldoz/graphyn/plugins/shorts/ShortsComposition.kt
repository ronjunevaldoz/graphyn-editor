package com.ronjunevaldoz.graphyn.plugins.shorts

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

/**
 * Batch-stitch subgraph: collects its input clips into a `media.videos_list` and stitches them with
 * a hard cut into a single `video`. [index] only disambiguates the definition id/name when several
 * batch nodes appear in one graph; it does not change behavior. Canvas node positions are a
 * desktop-editor concern and are intentionally left unset here — set them at the call site if the
 * subgraph is authored on a canvas.
 */
public fun stitchBatchSubgraph(index: Int): WorkflowDefinition = WorkflowDefinition(
    id = "stitch-batch-$index",
    name = "Batch $index",
    nodes = listOf(
        NodeRef("clips", "media.videos_list"),
        NodeRef("stitch", "media.video_stitch", config = mapOf("transition" to WorkflowValue.StringValue("cut"))),
    ),
    connections = listOf(
        ConnectionRef("clips", "videos", "stitch", "videos"),
    ),
)

/**
 * Builds the shared caption-style node ([ShortsConstants.CATEGORY]) with the pipeline's default
 * styling applied. [overrides] are merged on top of [ShortsConstants.CAPTION_STYLE_DEFAULTS].
 */
public fun shortsCaptionStyleNode(
    overrides: Map<String, WorkflowValue> = emptyMap(),
): NodeRef = NodeRef(
    "captionStyle", ShortsConstants.CAPTION_STYLE_NODE_TYPE,
    config = ShortsConstants.CAPTION_STYLE_DEFAULTS + overrides,
)
