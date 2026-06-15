package com.ronjunevaldoz.graphyn.editor.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.core.sync.WorkflowDataStore

class GraphynEditorState(
    initialWorkflow: WorkflowDefinition? = null,
) {
    private val workflowState = mutableStateOf(initialWorkflow)
    var workflow: WorkflowDefinition?
        get() = workflowState.value
        set(value) {
            workflowState.value = value
            dataStore.updateWorkflow(value)
        }

    var selectedNodeId by mutableStateOf<String?>(null)
    var nodeOutputsByNodeId by mutableStateOf<Map<String, Map<String, WorkflowValue>>>(emptyMap())
    private val dataStore = WorkflowDataStore(initialWorkflow)

    fun selectNode(nodeId: String?) {
        selectedNodeId = nodeId
    }

    fun updateNodeOutputs(nodeId: String, outputs: Map<String, WorkflowValue>) {
        nodeOutputsByNodeId = nodeOutputsByNodeId + (nodeId to outputs)
        dataStore.updateNodeOutputs(nodeId, outputs)
    }

    fun outputsFor(nodeId: String): Map<String, WorkflowValue> = nodeOutputsByNodeId[nodeId].orEmpty()

    fun flattenedOutputsFor(nodeId: String): Map<String, WorkflowValue> =
        dataStore.flattenedOutputsFor(nodeId)

    fun affectedNodeIds(nodeId: String): Set<String> =
        workflow?.let {
            dataStore.updateWorkflow(it)
            dataStore.affectedNodeIds(nodeId)
        }.orEmpty()

    fun selectedNode(): NodeRef? {
        val currentWorkflow = workflow ?: return null
        val selectedId = selectedNodeId ?: return null
        return currentWorkflow.nodes.firstOrNull { it.id == selectedId }
    }
}
