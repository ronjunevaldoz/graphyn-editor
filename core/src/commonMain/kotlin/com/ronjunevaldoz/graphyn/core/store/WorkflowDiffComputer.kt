package com.ronjunevaldoz.graphyn.core.store

import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition

/** Computes a [WorkflowDiff] between two workflow snapshots. */
object WorkflowDiffComputer {

    /**
     * Returns a diff that, when applied to [before], describes [after].
     *
     * Node identity is based on [com.ronjunevaldoz.graphyn.core.model.NodeRef.id].
     * A node present in both versions with differing fields is reported as modified.
     */
    fun compute(before: WorkflowDefinition, after: WorkflowDefinition): WorkflowDiff {
        val beforeNodes = before.nodes.associateBy { it.id }
        val afterNodes = after.nodes.associateBy { it.id }

        val nodesAdded = after.nodes.filter { it.id !in beforeNodes }
        val nodesRemoved = before.nodes.filter { it.id !in afterNodes }
        val nodesModified = after.nodes
            .filter { it.id in beforeNodes && it != beforeNodes[it.id] }
            .map { NodeDiff(it.id, beforeNodes.getValue(it.id), it) }

        val beforeConnections = before.connections.toSet()
        val afterConnections = after.connections.toSet()

        return WorkflowDiff(
            nodesAdded = nodesAdded,
            nodesRemoved = nodesRemoved,
            nodesModified = nodesModified,
            connectionsAdded = (afterConnections - beforeConnections).toList(),
            connectionsRemoved = (beforeConnections - afterConnections).toList(),
        )
    }
}
