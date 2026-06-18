package com.ronjunevaldoz.graphyn.core.model

data class PortSpec(
    val name: String,
    val type: WorkflowType,
    val required: Boolean = true,
    val portColor: Long? = null,
)

data class NodeSpec(
    val type: String,
    val label: String,
    val inputs: List<PortSpec>,
    val outputs: List<PortSpec>,
    val defaultValues: Map<String, WorkflowValue> = emptyMap(),
)

data class NodeRef(
    val id: String,
    val type: String,
    val config: Map<String, WorkflowValue> = emptyMap(),
)

data class ConnectionRef(
    val fromNodeId: String,
    val fromPort: String,
    val toNodeId: String,
    val toPort: String,
)

data class WorkflowDefinition(
    val id: String,
    val name: String,
    val nodes: List<NodeRef>,
    val connections: List<ConnectionRef>,
)
