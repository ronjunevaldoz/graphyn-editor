package com.ronjunevaldoz.graphyn.editor.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import com.ronjunevaldoz.graphyn.core.execution.WorkflowExecutionEngine
import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.core.execution.WorkflowExecutionResult
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
    var connectionDraftPosition by mutableStateOf<Offset?>(null)
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
                connectionDraftPosition = null
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
                connectionDraftPosition = null
            }
            is GraphynEditorIntent.AddNode -> {
                addNode(intent.spec)
            }
            is GraphynEditorIntent.UpdateConnectionDraftPosition -> {
                connectionDraftPosition = intent.position
            }
            GraphynEditorIntent.CancelConnection -> {
                connectionDraft = null
                connectionDraftPosition = null
            }
        }
    }

    fun addNode(spec: NodeSpec) {
        val currentWorkflow = workflow ?: WorkflowDefinition(
            id = "workflow",
            name = "Untitled Workflow",
            nodes = emptyList(),
            connections = emptyList(),
        )
        val nodeId = buildNodeId(spec, currentWorkflow.nodes)
        val node = NodeRef(
            id = nodeId,
            type = spec.type,
            config = spec.defaultValues,
        )
        val nextWorkflow = currentWorkflow.copy(nodes = currentWorkflow.nodes + node)
        workflow = nextWorkflow
        selectedNodeId = nodeId
        connectionDraft = null
        connectionDraftPosition = null
        setNodePosition(nodeId, GraphynCanvasLayout.fallbackPosition(nextWorkflow.nodes.lastIndex))
    }

    fun updateNodeOutputs(nodeId: String, outputs: Map<String, WorkflowValue>) {
        nodeOutputsByNodeId = nodeOutputsByNodeId + (nodeId to outputs)
        dataStore.updateNodeOutputs(nodeId, outputs)
    }

    fun applyExecutionResult(result: WorkflowExecutionResult) {
        nodeOutputsByNodeId = result.nodeOutputsByNodeId
        result.nodeOutputsByNodeId.forEach { (nodeId, outputs) ->
            dataStore.updateNodeOutputs(nodeId, outputs)
        }
    }

    fun execute(engine: WorkflowExecutionEngine) {
        val currentWorkflow = workflow ?: return
        applyExecutionResult(engine.execute(currentWorkflow))
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

    private fun buildNodeId(spec: NodeSpec, nodes: List<NodeRef>): String {
        val prefix = spec.type.substringAfterLast('.').ifBlank { "node" }
        var suffix = 1
        val existingIds = nodes.mapTo(mutableSetOf()) { it.id }
        while (true) {
            val candidate = "$prefix-$suffix"
            if (candidate !in existingIds) {
                return candidate
            }
            suffix += 1
        }
    }
}
