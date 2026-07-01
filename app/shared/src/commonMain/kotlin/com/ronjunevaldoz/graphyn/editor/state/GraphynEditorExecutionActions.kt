package com.ronjunevaldoz.graphyn.editor.state

import com.ronjunevaldoz.graphyn.core.execution.ExecutionEvent
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
    val allNodeIds = workflow?.nodes?.map { it.id }.orEmpty()
    executionStatusByNodeId = allNodeIds.associateWith { id ->
        result.statusByNodeId[id] ?: NodeExecutionStatus.Idle
    }
    lastExecutionResult = result
    val summary = buildString {
        append(if (result.isFullSuccess) "✓ Run complete — " else "✗ Run complete — ")
        append("${result.successCount} ok")
        if (result.errorCount > 0) append(", ${result.errorCount} failed")
        if (result.skippedCount > 0) append(", ${result.skippedCount} skipped")
    }
    log.push(summary)
}

fun GraphynEditorState.execute(engine: WorkflowExecutionEngine): Job {
    val w = workflow ?: return Job()
    executionStatusByNodeId = w.nodes.associate { it.id to NodeExecutionStatus.Idle }
    return scope.launch {
        SdArtifactContext.workflowId = w.id
        SdArtifactContext.workflowName = w.name
        try {
            val result = engine.execute(w, onEvent = { event ->
                // Live per-node status as the engine progresses.
                val status = when (event) {
                    is ExecutionEvent.Started   -> NodeExecutionStatus.Running
                    is ExecutionEvent.Succeeded -> NodeExecutionStatus.Success
                    is ExecutionEvent.Failed    -> NodeExecutionStatus.Error
                    is ExecutionEvent.Skipped   -> NodeExecutionStatus.Skipped
                }
                executionStatusByNodeId = executionStatusByNodeId + (event.nodeId to status)
                if (event is ExecutionEvent.Failed) log.push("✗ ${event.nodeId}: ${event.message}")
            })
            applyExecutionResult(result)
        } catch (e: Exception) {
            // Structural failure (cycle, duplicate ids) — not a per-node error.
            executionStatusByNodeId = w.nodes.associate { it.id to NodeExecutionStatus.Error }
            log.push("Execution failed: ${e.message}")
        } finally {
            SdArtifactContext.workflowId = null
            SdArtifactContext.workflowName = null
        }
    }
}
