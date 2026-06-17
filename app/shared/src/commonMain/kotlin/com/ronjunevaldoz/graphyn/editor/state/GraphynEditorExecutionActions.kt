package com.ronjunevaldoz.graphyn.editor.state

import com.ronjunevaldoz.graphyn.core.execution.WorkflowExecutionEngine
import com.ronjunevaldoz.graphyn.core.execution.WorkflowExecutionResult
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

fun GraphynEditorState.updateNodeOutputs(nodeId: String, outputs: Map<String, WorkflowValue>) {
    nodeOutputsByNodeId = nodeOutputsByNodeId + (nodeId to outputs)
    dataStore.updateNodeOutputs(nodeId, outputs)
}

fun GraphynEditorState.applyExecutionResult(result: WorkflowExecutionResult) {
    nodeOutputsByNodeId = result.nodeOutputsByNodeId
    result.nodeOutputsByNodeId.forEach { (id, outputs) -> dataStore.updateNodeOutputs(id, outputs) }
    log.push("Execution completed: ${result.nodeOutputsByNodeId.size} node outputs updated")
}

fun GraphynEditorState.execute(engine: WorkflowExecutionEngine) {
    val w = workflow ?: return
    applyExecutionResult(engine.execute(w))
}
