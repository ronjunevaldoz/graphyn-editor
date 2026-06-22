package com.ronjunevaldoz.graphyn.core.store

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import kotlinx.serialization.Serializable

/**
 * Describes what changed on a single node between two versions.
 *
 * Both [before] and [after] are full node snapshots — callers can compare any field.
 */
@Serializable
data class NodeDiff(
    val nodeId: String,
    val before: NodeRef,
    val after: NodeRef,
)

/**
 * Structural difference between two [WorkflowDefinition] snapshots.
 *
 * Produced by [WorkflowDiffComputer.compute] and attached to each [WorkflowVersion]
 * so the editor can present a per-save changelog without re-diffing from scratch.
 */
@Serializable
data class WorkflowDiff(
    val nodesAdded: List<NodeRef> = emptyList(),
    val nodesRemoved: List<NodeRef> = emptyList(),
    val nodesModified: List<NodeDiff> = emptyList(),
    val connectionsAdded: List<ConnectionRef> = emptyList(),
    val connectionsRemoved: List<ConnectionRef> = emptyList(),
) {
    /** True when the two snapshots were structurally identical. */
    val isEmpty: Boolean
        get() = nodesAdded.isEmpty() && nodesRemoved.isEmpty() && nodesModified.isEmpty()
            && connectionsAdded.isEmpty() && connectionsRemoved.isEmpty()
}
