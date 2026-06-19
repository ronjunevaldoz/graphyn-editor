package com.ronjunevaldoz.graphyn.core.model

/**
 * Declares a single port on a node.
 *
 * @param description Short hint shown in the inspector below the port name.
 *   Describe what the port expects or produces — e.g. "CLIP-encoded positive prompt".
 * @param portColor Optional ARGB color override for the connection dot on the canvas.
 *   Falls back to the type-based color from `GraphynPortTypeColor` when null.
 */
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
 */
data class NodeRef(
    val id: String,
    val type: String,
    val config: Map<String, WorkflowValue> = emptyMap(),
)

/** Directed edge from one node's output port to another node's input port. */
data class ConnectionRef(
    val fromNodeId: String,
    val fromPort: String,
    val toNodeId: String,
    val toPort: String,
)

/** Complete, serializable description of a workflow — nodes and the edges between them. */
data class WorkflowDefinition(
    val id: String,
    val name: String,
    val nodes: List<NodeRef>,
    val connections: List<ConnectionRef>,
)
