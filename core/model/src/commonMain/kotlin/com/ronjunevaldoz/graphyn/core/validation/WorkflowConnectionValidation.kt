package com.ronjunevaldoz.graphyn.core.validation

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.ValidationError
import com.ronjunevaldoz.graphyn.core.model.WorkflowTypeCompatibility
import com.ronjunevaldoz.graphyn.core.registry.NodeSpecRegistry

internal fun validateConnections(
    connections: List<ConnectionRef>,
    nodesById: Map<String, NodeRef>,
    specsByNodeId: Map<NodeRef, NodeSpec?>,
    errors: MutableList<ValidationError>,
) {
    connections.forEach { connection ->
        val fromNode = nodesById[connection.fromNodeId]
        val toNode = nodesById[connection.toNodeId]

        if (fromNode == null) {
            errors += ValidationError(
                code = "missing_source_node",
                message = "Connection source node '${connection.fromNodeId}' does not exist.",
                nodeId = connection.fromNodeId, port = connection.fromPort,
            )
            return@forEach
        }
        if (toNode == null) {
            errors += ValidationError(
                code = "missing_target_node",
                message = "Connection target node '${connection.toNodeId}' does not exist.",
                nodeId = connection.toNodeId, port = connection.toPort,
            )
            return@forEach
        }

        val fromSpec = specsByNodeId[fromNode]
        val toSpec = specsByNodeId[toNode]
        if (fromSpec == null || toSpec == null) return@forEach

        val fromPort = fromSpec.outputs.firstOrNull { it.name == connection.fromPort }
        val toPort = toSpec.inputs.firstOrNull { it.name == connection.toPort }

        if (fromPort == null) {
            errors += ValidationError(
                code = "missing_output_port",
                message = "Node '${fromNode.id}' has no output port named '${connection.fromPort}'.",
                nodeId = fromNode.id, port = connection.fromPort,
            )
            return@forEach
        }
        if (toPort == null) {
            errors += ValidationError(
                code = "missing_input_port",
                message = "Node '${toNode.id}' has no input port named '${connection.toPort}'.",
                nodeId = toNode.id, port = connection.toPort,
            )
            return@forEach
        }
        if (!WorkflowTypeCompatibility.isCompatible(toPort.type, fromPort.type)) {
            errors += ValidationError(
                code = "type_mismatch",
                message = "Cannot connect '${fromPort.type}' to '${toPort.type}'.",
                nodeId = toNode.id, port = connection.toPort,
            )
        }
    }

    connections
        .groupBy { "${it.toNodeId}:${it.toPort}" }
        .filterValues { it.size > 1 }
        .keys.forEach { targetKey ->
            val (nodeId, port) = targetKey.split(":", limit = 2)
            errors += ValidationError(
                code = "duplicate_input_connection",
                message = "Input port '$port' on node '$nodeId' has more than one connection.",
                nodeId = nodeId, port = port,
            )
        }
}

internal fun validateRequiredInputs(
    node: NodeRef,
    spec: NodeSpec,
    incomingByTarget: Map<String, List<ConnectionRef>>,
    errors: MutableList<ValidationError>,
) {
    spec.inputs.forEach { input ->
        val targetKey = "${node.id}:${input.name}"
        val hasConnection = incomingByTarget[targetKey].isNullOrEmpty().not()
        val hasFallback = node.config.containsKey(input.name) || spec.defaultValues.containsKey(input.name)
        if (input.required && !hasConnection && !hasFallback) {
            errors += ValidationError(
                code = "missing_required_input",
                message = "Required input '${input.name}' is not satisfied.",
                nodeId = node.id, port = input.name,
            )
        }
    }
}
