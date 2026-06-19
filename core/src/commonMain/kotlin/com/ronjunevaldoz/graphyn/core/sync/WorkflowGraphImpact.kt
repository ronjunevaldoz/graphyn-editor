package com.ronjunevaldoz.graphyn.core.sync

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.core.model.WorkflowValueFlattener

/** BFS traversal helpers for computing which nodes are downstream of a changed node. */
object WorkflowGraphImpact {
    fun affectedNodeIds(workflow: WorkflowDefinition, sourceNodeId: String): Set<String> {
        val adjacency = workflow.connections.groupBy { it.fromNodeId }.mapValues { (_, refs) ->
            refs.map(ConnectionRef::toNodeId)
        }
        val visited = mutableSetOf<String>()
        val queue = ArrayDeque<String>()
        queue.add(sourceNodeId)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (!visited.add(current)) continue
            adjacency[current].orEmpty().forEach(queue::add)
        }

        return visited
    }
}

/**
 * In-memory cache of per-node execution outputs paired with the current workflow graph.
 *
 * Call [updateNodeOutputs] after each node run; it returns the set of downstream node ids that may
 * need to re-execute due to the changed outputs.
 */
class WorkflowDataStore(
    private var workflow: WorkflowDefinition? = null,
) {
    private val nodeOutputsByNodeId = mutableMapOf<String, Map<String, WorkflowValue>>()

    fun updateWorkflow(workflow: WorkflowDefinition?) {
        this.workflow = workflow
    }

    fun updateNodeOutputs(nodeId: String, outputs: Map<String, WorkflowValue>): Set<String> {
        nodeOutputsByNodeId[nodeId] = outputs
        return affectedNodeIds(nodeId)
    }

    fun outputsFor(nodeId: String): Map<String, WorkflowValue> = nodeOutputsByNodeId[nodeId].orEmpty()

    fun flattenedOutputsFor(nodeId: String): Map<String, WorkflowValue> =
        WorkflowValueFlattener.flattenMap(outputsFor(nodeId))

    fun affectedNodeIds(nodeId: String): Set<String> =
        workflow?.let { WorkflowGraphImpact.affectedNodeIds(it, nodeId) }.orEmpty()
}
