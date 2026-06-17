package com.ronjunevaldoz.graphyn.core.validation

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.ValidationError
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValidator
import com.ronjunevaldoz.graphyn.core.registry.NodeSpecRegistry

class WorkflowGraphValidator(
    private val nodeSpecRegistry: NodeSpecRegistry,
) : WorkflowValidator {
    override fun validate(workflow: WorkflowDefinition): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        val nodesById = workflow.nodes.associateBy { it.id }

        if (nodesById.size != workflow.nodes.size) {
            errors += ValidationError(code = "duplicate_node_id", message = "Workflow contains duplicate node ids.")
        }

        val specsByNodeId = workflow.nodes.associateWith { nodeSpecRegistry.resolve(it.type) }

        specsByNodeId.forEach { (node, spec) ->
            if (spec == null) {
                errors += ValidationError(
                    code = "unknown_node_type",
                    message = "No node spec registered for type '${node.type}'.",
                    nodeId = node.id,
                )
            }
        }

        val incomingByTarget = workflow.connections.groupBy { "${it.toNodeId}:${it.toPort}" }
        validateConnections(workflow.connections, nodesById, specsByNodeId, errors)

        workflow.nodes.forEach { node ->
            val spec = specsByNodeId[node] ?: return@forEach
            validateRequiredInputs(node = node, spec = spec, incomingByTarget = incomingByTarget, errors = errors)
        }

        if (hasCycle(workflow.nodes, workflow.connections)) {
            errors += ValidationError(code = "cycle_detected", message = "Workflow contains a cycle, which is not supported yet.")
        }

        return errors
    }

    private fun hasCycle(nodes: List<NodeRef>, connections: List<ConnectionRef>): Boolean {
        val adjacency = connections.groupBy { it.fromNodeId }.mapValues { (_, refs) -> refs.map { it.toNodeId } }
        val visiting = mutableSetOf<String>()
        val visited = mutableSetOf<String>()

        fun dfs(nodeId: String): Boolean {
            if (nodeId in visiting) return true
            if (nodeId in visited) return false
            visiting += nodeId
            for (next in adjacency[nodeId].orEmpty()) { if (dfs(next)) return true }
            visiting -= nodeId
            visited += nodeId
            return false
        }

        return nodes.any { dfs(it.id) }
    }
}
