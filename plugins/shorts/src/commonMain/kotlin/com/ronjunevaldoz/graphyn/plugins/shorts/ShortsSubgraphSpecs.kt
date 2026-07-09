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

/**
 * Optional diagnostic input ports shared by [storyboardValidateSpec] and [comparisonValidateSpec].
 * Each pair mirrors one stage of the Ollama request -> parse -> path -> parse chain built by
 * [storyboardGeneratorSubgraph]/[comparisonGeneratorSubgraph] (`request`/`outer`/`response`/the
 * final `json.parse`). None are required — a caller that doesn't wire them still validates
 * normally, it just loses the chain breakdown in the failure message. See [ollamaChainDiagnostics].
 */
private val ollamaChainDiagnosticInputs = listOf(
    PortSpec("httpOk", WorkflowType.NullableType(WorkflowType.BooleanType), required = false, description = "io.http_request 'ok'"),
    PortSpec("httpError", WorkflowType.NullableType(WorkflowType.StringType), required = false, description = "io.http_request 'error'"),
    PortSpec("outerParseOk", WorkflowType.NullableType(WorkflowType.BooleanType), required = false, description = "Envelope json.parse 'ok'"),
    PortSpec("outerParseError", WorkflowType.NullableType(WorkflowType.StringType), required = false, description = "Envelope json.parse 'error'"),
    PortSpec("responseFound", WorkflowType.NullableType(WorkflowType.BooleanType), required = false, description = "json.path 'found' for the 'response' field"),
    PortSpec("innerParseOk", WorkflowType.NullableType(WorkflowType.BooleanType), required = false, description = "Model-JSON json.parse 'ok'"),
    PortSpec("innerParseError", WorkflowType.NullableType(WorkflowType.StringType), required = false, description = "Model-JSON json.parse 'error'"),
)

public val storyboardValidateSpec: NodeSpec = NodeSpec(
    type = ShortsNodeTypes.STORYBOARD_VALIDATE,
    label = "Storyboard Validate",
    description = "Validates the Ollama storyboard JSON and unloads the Ollama model (compiled, not scripted)",
    category = ShortsConstants.CATEGORY,
    inputs = listOf(PortSpec("input", WorkflowType.OpaqueType, required = false)) + ollamaChainDiagnosticInputs,
    outputs = listOf(PortSpec("value", WorkflowType.OpaqueType, description = "Validated storyboard record")),
)

public val comparisonValidateSpec: NodeSpec = NodeSpec(
    type = ShortsNodeTypes.COMPARISON_VALIDATE,
    label = "Comparison Validate",
    description = "Validates the Ollama comparison-arc JSON and unloads the Ollama model (compiled, not scripted)",
    category = ShortsConstants.CATEGORY,
    inputs = listOf(PortSpec("input", WorkflowType.OpaqueType, required = false)) + ollamaChainDiagnosticInputs,
    outputs = listOf(PortSpec("value", WorkflowType.OpaqueType, description = "Validated comparison-arc record")),
)

public val ollamaUnloadSpec: NodeSpec = NodeSpec(
    type = ShortsNodeTypes.OLLAMA_UNLOAD,
    label = "Ollama Unload",
    description = "Force-unloads the Ollama model; wire its 'gate' output into a scene subgraph to order it before generation",
    category = ShortsConstants.CATEGORY,
    inputs = emptyList(),
    outputs = listOf(PortSpec("gate", WorkflowType.OpaqueType, description = "Dependency token — connect to a scene subgraph's 'gate' input")),
)
