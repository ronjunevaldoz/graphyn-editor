package com.ronjunevaldoz.graphyn.core.sync

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.core.model.WorkflowValueFlattener

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
