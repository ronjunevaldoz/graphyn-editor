package com.ronjunevaldoz.graphyn.editor.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.IntOffset
import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.core.sync.WorkflowDataStore
import com.ronjunevaldoz.graphyn.editor.canvas.GraphynCanvasLayout
import com.ronjunevaldoz.graphyn.editor.interaction.GraphynConnectionDraft
import com.ronjunevaldoz.graphyn.editor.interaction.GraphynEditorIntent

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
    var nodePositionsByNodeId by mutableStateOf<Map<String, IntOffset>>(emptyMap())
    var connectionDraft by mutableStateOf<GraphynConnectionDraft?>(null)
    private val dataStore = WorkflowDataStore(initialWorkflow)

    fun selectNode(nodeId: String?) {
        selectedNodeId = nodeId
    }

    fun dispatch(intent: GraphynEditorIntent) {
        when (intent) {
            is GraphynEditorIntent.SelectNode -> selectNode(intent.nodeId)
            is GraphynEditorIntent.MoveNode -> moveNode(intent.nodeId, intent.delta)
            is GraphynEditorIntent.BeginConnection -> {
                connectionDraft = GraphynConnectionDraft(
                    fromNodeId = intent.fromNodeId,
                    fromPort = intent.fromPort,
                )
            }
            is GraphynEditorIntent.CompleteConnection -> {
                val draft = connectionDraft ?: return
                val currentWorkflow = workflow ?: return
                workflow = currentWorkflow.copy(
                    connections = currentWorkflow.connections + ConnectionRef(
                        fromNodeId = draft.fromNodeId,
                        fromPort = draft.fromPort,
                        toNodeId = intent.toNodeId,
                        toPort = intent.toPort,
                    ),
                )
                connectionDraft = null
            }
            GraphynEditorIntent.CancelConnection -> {
                connectionDraft = null
            }
        }
    }

    fun updateNodeOutputs(nodeId: String, outputs: Map<String, WorkflowValue>) {
        nodeOutputsByNodeId = nodeOutputsByNodeId + (nodeId to outputs)
        dataStore.updateNodeOutputs(nodeId, outputs)
    }

    fun setNodePosition(nodeId: String, position: IntOffset) {
        nodePositionsByNodeId = nodePositionsByNodeId + (nodeId to position)
    }

    fun moveNode(nodeId: String, delta: IntOffset) {
        val currentPosition = nodePositionsByNodeId[nodeId] ?: IntOffset.Zero
        setNodePosition(
            nodeId = nodeId,
            position = IntOffset(
                x = currentPosition.x + delta.x,
                y = currentPosition.y + delta.y,
            ),
        )
    }

    fun nodePosition(nodeId: String, index: Int): IntOffset =
        nodePositionsByNodeId[nodeId] ?: GraphynCanvasLayout.fallbackPosition(index)

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
