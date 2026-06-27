package com.ronjunevaldoz.graphyn.core.model

import kotlinx.serialization.Serializable

/**
 * Declares a single port on a node.
 *
 * @param description Short hint shown in the inspector below the port name.
 *   Describe what the port expects or produces — e.g. "CLIP-encoded positive prompt".
 * @param portColor Optional ARGB color override for the connection dot on the canvas.
 *   Falls back to the type-based color from `GraphynPortTypeColor` when null.
 */
@Serializable
data class PortSpec(
    val name: String,
    val type: WorkflowType,
    val required: Boolean = true,
    val portColor: Long? = null,
    val description: String? = null,
)

/**
 * Immutable schema for a node type. Registered once via [GraphynPluginRegistrar] and
 * shared across all [NodeRef] instances of this type.
 *
 * [defaultValues] seed the input map when no [NodeRef.config] entry and no connected
 * output exist for a given port. Priority order in execution:
 *   defaultValues < node.config < connected upstream output
 *
 * [description] is a short human-readable explanation of what the node does.
 * Shown in the inspector panel and as a subtitle in the palette.
 */
@Serializable
data class NodeSpec(
    val type: String,
    val label: String,
    val inputs: List<PortSpec>,
    val outputs: List<PortSpec>,
    val defaultValues: Map<String, WorkflowValue> = emptyMap(),
    val category: String? = null,
    val description: String? = null,
)

/**
 * A live node instance in the workflow graph.
 *
 * [config] holds user-edited overrides for input ports. It takes precedence over
 * [NodeSpec.defaultValues] but is overridden by any connected upstream output.
 *
 * [subgraph] embeds a nested [WorkflowDefinition] directly on the node. When non-null,
 * the execution engine runs the inner workflow recursively and the editor offers an
 * "Enter Subgraph" action in the inspector. Setting this field does not require a
 * registered executor — the engine handles subgraph nodes automatically.
 */
@Serializable
data class NodeRef(
    val id: String,
    val type: String,
    val config: Map<String, WorkflowValue> = emptyMap(),
    val subgraph: WorkflowDefinition? = null,
    /** Milliseconds before the node is cancelled and recorded as an error. Null = no limit. */
    val timeoutMs: Long? = null,
    /** How many times to retry on failure before propagating the error. 0 = no retry. */
    val maxRetries: Int = 0,
)

/** Directed edge from one node's output port to another node's input port. */
@Serializable
data class ConnectionRef(
    val fromNodeId: String,
    val fromPort: String,
    val toNodeId: String,
    val toPort: String,
)

/** Serializable canvas position for a node in a workflow. */
@Serializable
data class WorkflowNodePosition(
    val x: Int,
    val y: Int,
)

/** Complete, serializable description of a workflow — nodes and the edges between them. */
@Serializable
data class WorkflowDefinition(
    val id: String,
    val name: String,
    val nodes: List<NodeRef>,
    val connections: List<ConnectionRef>,
    val nodePositions: Map<String, WorkflowNodePosition> = emptyMap(),
)
