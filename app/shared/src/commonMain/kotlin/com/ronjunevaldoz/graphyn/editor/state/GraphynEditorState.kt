package com.ronjunevaldoz.graphyn.editor.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.ronjunevaldoz.graphyn.core.execution.WorkflowExecutionEngine
import com.ronjunevaldoz.graphyn.core.execution.WorkflowExecutionResult
import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.core.sync.WorkflowDataStore
import com.ronjunevaldoz.graphyn.editor.canvas.GraphynCanvasBounds
import com.ronjunevaldoz.graphyn.editor.canvas.GraphynCanvasLayout
import com.ronjunevaldoz.graphyn.editor.interaction.GraphynConnectionDraft
import com.ronjunevaldoz.graphyn.editor.interaction.GraphynEditorIntent

class GraphynEditorState(
    initialWorkflow: WorkflowDefinition? = null,
    private val canvasBounds: GraphynCanvasBounds = GraphynCanvasBounds(),
) {
    // Sub-states
    private val viewportState = GraphynViewportState(canvasBounds)
    private val layout = GraphynNodeLayoutState(canvasBounds, viewportScale = { viewportState.viewport.scale })
    private val log = GraphynDebugLogState()
    private val dataStore = WorkflowDataStore(initialWorkflow)

    // Workflow
    private val workflowState = mutableStateOf(initialWorkflow)
    var workflow: WorkflowDefinition?
        get() = workflowState.value
        set(value) {
            workflowState.value = value
            dataStore.updateWorkflow(value)
            viewportState.refresh(value?.nodes?.mapTo(mutableSetOf()) { it.id }.orEmpty())
        }

    // Selection
    var selectedNodeId by mutableStateOf<String?>(null)
    var selectedConnection by mutableStateOf<ConnectionRef?>(null)

    // Connection draft
    var connectionDraft by mutableStateOf<GraphynConnectionDraft?>(null)
    var connectionDraftPosition by mutableStateOf<Offset?>(null)

    // Node outputs
    var nodeOutputsByNodeId by mutableStateOf<Map<String, Map<String, WorkflowValue>>>(emptyMap())

    // Delegated viewport reads (viewport is settable so minimap can reposition directly)
    var viewport: GraphynViewport
        get() = viewportState.viewport
        set(value) { viewportState.viewport = value }
    val canvasSize get() = viewportState.canvasSize
    val graphWorldBounds get() = viewportState.graphWorldBounds
    val debugLogEntries get() = log.entries

    // Delegated layout reads
    val nodePositionsByNodeId get() = layout.nodePositionsByNodeId

    init {
        viewportState.refresh(initialWorkflow?.nodes?.mapTo(mutableSetOf()) { it.id }.orEmpty())
    }

    fun dispatch(intent: GraphynEditorIntent) {
        when (intent) {
            is GraphynEditorIntent.SelectNode -> selectNode(intent.nodeId)
            GraphynEditorIntent.DeleteSelectedNode -> deleteSelectedNode()
            is GraphynEditorIntent.SelectConnection -> selectConnection(intent.connection)
            GraphynEditorIntent.DeleteSelectedConnection -> deleteSelectedConnection()
            is GraphynEditorIntent.MoveNode -> layout.moveNode(intent.nodeId, intent.delta)
            is GraphynEditorIntent.BeginConnection -> {
                connectionDraft = GraphynConnectionDraft(intent.fromNodeId, intent.fromPort)
                connectionDraftPosition = null
            }
            is GraphynEditorIntent.CompleteConnection -> completeConnection(intent.toNodeId, intent.toPort)
            is GraphynEditorIntent.AddNode -> addNode(intent.spec)
            is GraphynEditorIntent.UpdateConnectionDraftPosition -> connectionDraftPosition = intent.position
            is GraphynEditorIntent.UpdateViewportTransform ->
                viewportState.updateTransform(intent.pan, intent.zoom, intent.focus)
            GraphynEditorIntent.CancelConnection -> {
                connectionDraft = null
                connectionDraftPosition = null
            }
        }
    }

    fun selectNode(nodeId: String?) {
        selectedNodeId = nodeId
        selectedConnection = null
    }

    fun selectConnection(connection: ConnectionRef?) {
        selectedConnection = connection
        selectedNodeId = null
    }

    fun deleteSelectedNode() {
        val nodeId = selectedNodeId ?: return
        workflow = workflow?.copy(
            nodes = workflow?.nodes.orEmpty().filterNot { it.id == nodeId },
            connections = workflow?.connections.orEmpty().filterNot { it.fromNodeId == nodeId || it.toNodeId == nodeId },
        )
        layout.removeNode(nodeId)
        nodeOutputsByNodeId = nodeOutputsByNodeId - nodeId
        selectedNodeId = null
        if (selectedConnection?.fromNodeId == nodeId || selectedConnection?.toNodeId == nodeId) {
            selectedConnection = null
        }
        log.push("Deleted node $nodeId")
    }

    fun deleteSelectedConnection() {
        val conn = selectedConnection ?: return
        workflow = workflow?.copy(connections = workflow?.connections.orEmpty().filterNot { it == conn })
        selectedConnection = null
        log.push("Deleted connection ${conn.fromNodeId}:${conn.fromPort} -> ${conn.toNodeId}:${conn.toPort}")
    }

    fun addNode(spec: NodeSpec) {
        val current = workflow ?: WorkflowDefinition("workflow", "Untitled Workflow", emptyList(), emptyList())
        val nodeId = buildNodeId(spec, current.nodes)
        val next = current.copy(nodes = current.nodes + NodeRef(nodeId, spec.type, spec.defaultValues))
        workflow = next
        selectedNodeId = nodeId
        connectionDraft = null
        connectionDraftPosition = null
        layout.setNodePosition(nodeId, GraphynCanvasLayout.fallbackPosition(next.nodes.lastIndex))
        log.push("Added node $nodeId (${spec.label})")
    }

    fun updateNodeOutputs(nodeId: String, outputs: Map<String, WorkflowValue>) {
        nodeOutputsByNodeId = nodeOutputsByNodeId + (nodeId to outputs)
        dataStore.updateNodeOutputs(nodeId, outputs)
    }

    fun applyExecutionResult(result: WorkflowExecutionResult) {
        nodeOutputsByNodeId = result.nodeOutputsByNodeId
        result.nodeOutputsByNodeId.forEach { (id, outputs) -> dataStore.updateNodeOutputs(id, outputs) }
        log.push("Execution completed: ${result.nodeOutputsByNodeId.size} node outputs updated")
    }

    fun execute(engine: WorkflowExecutionEngine) {
        val w = workflow ?: return
        applyExecutionResult(engine.execute(w))
    }

    // Layout delegation
    fun setNodePosition(nodeId: String, position: IntOffset, clearDragRemainder: Boolean = true) =
        layout.setNodePosition(nodeId, position, clearDragRemainder)

    fun moveNode(nodeId: String, delta: IntOffset) = layout.moveNode(nodeId, delta)
    fun nodePosition(nodeId: String, index: Int): IntOffset = layout.nodePosition(nodeId, index)
    fun nodeSize(nodeId: String): IntSize = layout.nodeSize()
    fun nodeBounds(nodeId: String, index: Int): Rect = layout.nodeBounds(nodeId, index)

    fun isWorldPositionOverNode(position: Offset): Boolean {
        val nodes = workflow?.nodes ?: return false
        return layout.isOverNode(position, nodes.mapIndexed { i, n -> n.id to i })
    }

    // Viewport delegation
    fun updateViewportTransform(pan: Offset, zoom: Float, focus: Offset) =
        viewportState.updateTransform(pan, zoom, focus)

    fun updateCanvasSize(size: IntSize) = viewportState.updateCanvasSize(size)
    fun resetViewport() { viewportState.reset(); log.push("Viewport reset") }
    fun screenToWorld(position: Offset): Offset = viewportState.screenToWorld(position)
    fun worldToScreen(position: Offset): Offset = viewportState.worldToScreen(position)

    // Query helpers
    fun selectedNode(): NodeRef? = workflow?.nodes?.firstOrNull { it.id == selectedNodeId }
    fun outputsFor(nodeId: String): Map<String, WorkflowValue> = nodeOutputsByNodeId[nodeId].orEmpty()
    fun flattenedOutputsFor(nodeId: String): Map<String, WorkflowValue> = dataStore.flattenedOutputsFor(nodeId)
    fun affectedNodeIds(nodeId: String): Set<String> = workflow?.let {
        dataStore.updateWorkflow(it); dataStore.affectedNodeIds(nodeId)
    }.orEmpty()

    fun addDebugLog(message: String) = log.push(message)

    private fun completeConnection(toNodeId: String, toPort: String) {
        val draft = connectionDraft ?: return
        val w = workflow ?: return
        workflow = w.copy(
            connections = w.connections + ConnectionRef(draft.fromNodeId, draft.fromPort, toNodeId, toPort),
        )
        connectionDraft = null
        connectionDraftPosition = null
        log.push("Connected ${draft.fromNodeId}:${draft.fromPort} -> $toNodeId:$toPort")
    }

}
