package com.ronjunevaldoz.graphyn.plugins.shorts

import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType

/**
 * Wrapper specs for the three subgraph nodes the shorts pipeline instantiates. Each wraps a nested
 * [com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition] and exposes a single terminal output;
 * their executors ([ShortsPlugin]) pick out exactly that output port so a bare subgraph node
 * doesn't fall back to merging every unconsumed internal output.
 */
public val shortsSceneSubgraphSpec: NodeSpec = NodeSpec(
    type = ShortsNodeTypes.SCENE_SUBGRAPH,
    label = "Scene Subgraph",
    description = "Runs one reusable shorts scene and exposes the rendered video",
    category = ShortsConstants.CATEGORY,
    inputs = listOf(
        PortSpec("input", WorkflowType.OpaqueType, description = "Scene list from the outline"),
        PortSpec("gate", WorkflowType.OpaqueType, description = "Dependency token used to serialize scene generation"),
    ),
    outputs = listOf(PortSpec("video", WorkflowType.OpaqueType, description = "Rendered scene video")),
)

public val shortsBatchSubgraphSpec: NodeSpec = NodeSpec(
    type = ShortsNodeTypes.BATCH_SUBGRAPH,
    label = "Batch Stitch Subgraph",
    description = "Stitches a small batch of clips into one clip",
    category = ShortsConstants.CATEGORY,
    inputs = listOf(
        PortSpec("video1", WorkflowType.OpaqueType),
        PortSpec("video2", WorkflowType.OpaqueType),
        PortSpec("video3", WorkflowType.OpaqueType),
        PortSpec("video4", WorkflowType.OpaqueType),
    ),
    outputs = listOf(PortSpec("video", WorkflowType.OpaqueType, description = "Stitched video")),
)

public val storyboardSubgraphSpec: NodeSpec = NodeSpec(
    type = ShortsNodeTypes.STORYBOARD_SUBGRAPH,
    label = "Storyboard Subgraph",
    description = "Runs the Ollama storyboard generator and exposes the validated result",
    category = ShortsConstants.CATEGORY,
    inputs = emptyList(),
    outputs = listOf(PortSpec("value", WorkflowType.OpaqueType, description = "Validated storyboard record")),
)

public val storyboardValidateSpec: NodeSpec = NodeSpec(
    type = ShortsNodeTypes.STORYBOARD_VALIDATE,
    label = "Storyboard Validate",
    description = "Validates the Ollama storyboard JSON and unloads the Ollama model (compiled, not scripted)",
    category = ShortsConstants.CATEGORY,
    inputs = listOf(PortSpec("input", WorkflowType.OpaqueType, required = false)),
    outputs = listOf(PortSpec("value", WorkflowType.OpaqueType, description = "Validated storyboard record")),
)

public val ollamaUnloadSpec: NodeSpec = NodeSpec(
    type = ShortsNodeTypes.OLLAMA_UNLOAD,
    label = "Ollama Unload",
    description = "Force-unloads the Ollama model; wire its 'gate' output into a scene subgraph to order it before generation",
    category = ShortsConstants.CATEGORY,
    inputs = emptyList(),
    outputs = listOf(PortSpec("gate", WorkflowType.OpaqueType, description = "Dependency token — connect to a scene subgraph's 'gate' input")),
)
