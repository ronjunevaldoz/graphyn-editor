package com.ronjunevaldoz.graphyn.core.execution

import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import kotlinx.serialization.Serializable

/**
 * Outcome of a workflow run.
 *
 * Execution is resilient: a node that throws is recorded as [NodeExecutionStatus.Error] and its
 * transitive dependents are marked [NodeExecutionStatus.Skipped], but independent branches still
 * run. So a result can be partial — inspect [statusByNodeId] rather than assuming every node ran.
 *
 * @param nodeOutputsByNodeId Outputs of nodes that completed successfully, keyed by node id then port name.
 * @param executionOrder Nodes that actually executed (Success or Error), in topological order. Skipped nodes are excluded.
 * @param statusByNodeId Final status of every node in the workflow.
 * @param errorsByNodeId Failure message for each errored node.
 * @param durationsByNodeId Wall-clock milliseconds spent in each executed node.
 * @param subResults Nested results for any nodes whose [com.ronjunevaldoz.graphyn.core.model.NodeRef.subgraph] ran.
 */
@Serializable
data class WorkflowExecutionResult(
    val nodeOutputsByNodeId: Map<String, Map<String, WorkflowValue>>,
    val executionOrder: List<String>,
    val statusByNodeId: Map<String, NodeExecutionStatus> = emptyMap(),
    val errorsByNodeId: Map<String, String> = emptyMap(),
    val durationsByNodeId: Map<String, Long> = emptyMap(),
    val subResults: Map<String, WorkflowExecutionResult> = emptyMap(),
) {
    /** True if every node reached [NodeExecutionStatus.Success]. */
    val isFullSuccess: Boolean get() = statusByNodeId.values.all { it == NodeExecutionStatus.Success }

    val successCount: Int get() = statusByNodeId.values.count { it == NodeExecutionStatus.Success }
    val errorCount: Int get() = statusByNodeId.values.count { it == NodeExecutionStatus.Error }
    val skippedCount: Int get() = statusByNodeId.values.count { it == NodeExecutionStatus.Skipped }
}
