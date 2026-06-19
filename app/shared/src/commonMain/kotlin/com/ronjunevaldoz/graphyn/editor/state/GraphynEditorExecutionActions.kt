package com.ronjunevaldoz.graphyn.editor.state

import com.ronjunevaldoz.graphyn.core.execution.NodeExecutionStatus
import com.ronjunevaldoz.graphyn.core.execution.WorkflowExecutionEngine
import com.ronjunevaldoz.graphyn.core.execution.WorkflowExecutionResult
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

fun GraphynEditorState.updateNodeOutputs(nodeId: String, outputs: Map<String, WorkflowValue>) {
    nodeOutputsByNodeId = nodeOutputsByNodeId + (nodeId to outputs)
    dataStore.updateNodeOutputs(nodeId, outputs)
}

fun GraphynEditorState.applyExecutionResult(result: WorkflowExecutionResult) {
    nodeOutputsByNodeId = result.nodeOutputsByNodeId
    result.nodeOutputsByNodeId.forEach { (id, outputs) -> dataStore.updateNodeOutputs(id, outputs) }
    val succeeded = result.executionOrder.toSet()
    val allNodeIds = workflow?.nodes?.map { it.id }.orEmpty()
    executionStatusByNodeId = allNodeIds.associateWith { id ->
        if (id in succeeded) NodeExecutionStatus.Success else NodeExecutionStatus.Error
    }
    lastExecutionResult = result
    val failed = allNodeIds.size - succeeded.size
    val summary = if (failed == 0) "✓ Run complete — ${succeeded.size} nodes"
                  else "✗ Run complete — ${succeeded.size} ok, $failed failed"
    log.push(summary)
}

fun GraphynEditorState.execute(engine: WorkflowExecutionEngine): Job {
    val w = workflow ?: return Job()
    executionStatusByNodeId = w.nodes.associate { it.id to NodeExecutionStatus.Running }
    return scope.launch {
        try {
            applyExecutionResult(engine.execute(w))
        } catch (e: Exception) {
            executionStatusByNodeId = w.nodes.associate { it.id to NodeExecutionStatus.Error }
            log.push("Execution failed: ${e.message}")
        }
    }
}
