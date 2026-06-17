package com.ronjunevaldoz.graphyn.editor.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.core.sync.WorkflowDataStore
import com.ronjunevaldoz.graphyn.editor.canvas.GraphynCanvasBounds
import com.ronjunevaldoz.graphyn.editor.canvas.GraphynCanvasLayout
import com.ronjunevaldoz.graphyn.editor.interaction.GraphynConnectionDraft
import com.ronjunevaldoz.graphyn.editor.interaction.GraphynEditorIntent
import com.ronjunevaldoz.graphyn.editor.interaction.GraphynNodePickerState

class GraphynEditorState(
    initialWorkflow: WorkflowDefinition? = null,
    private val canvasBounds: GraphynCanvasBounds = GraphynCanvasBounds(),
) {
    internal val viewportState = GraphynViewportState(canvasBounds)
    internal val layout = GraphynNodeLayoutState(canvasBounds, viewportScale = { viewportState.viewport.scale })
    internal val log = GraphynDebugLogState()
    internal val dataStore = WorkflowDataStore(initialWorkflow)

    private val workflowState = mutableStateOf(initialWorkflow)
    var workflow: WorkflowDefinition?
        get() = workflowState.value
        set(value) {
            workflowState.value = value
            dataStore.updateWorkflow(value)
            viewportState.refresh(value?.nodes?.mapTo(mutableSetOf()) { it.id }.orEmpty())
        }

    var selectedNodeId by mutableStateOf<String?>(null)
    var selectedConnection by mutableStateOf<ConnectionRef?>(null)
    var connectionDraft by mutableStateOf<GraphynConnectionDraft?>(null)
    var connectionDraftPosition by mutableStateOf<Offset?>(null)
    var nodePickerState by mutableStateOf<GraphynNodePickerState?>(null)
    var nodeOutputsByNodeId by mutableStateOf<Map<String, Map<String, WorkflowValue>>>(emptyMap())

    var viewport: GraphynViewport
        get() = viewportState.viewport
        set(value) { viewportState.viewport = value }
    val canvasSize get() = viewportState.canvasSize
    val graphWorldBounds get() = viewportState.graphWorldBounds
    val debugLogEntries get() = log.entries
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
                connectionDraft = GraphynConnectionDraft(intent.fromNodeId, intent.fromPort, intent.isFromInput)
                connectionDraftPosition = null
            }
            is GraphynEditorIntent.CompleteConnection -> completeConnection(intent.toNodeId, intent.toPort)
            is GraphynEditorIntent.AddNode -> addNode(intent.spec)
            is GraphynEditorIntent.AddNodeAndConnect -> addNodeAndConnect(intent.spec, intent.toPort, intent.worldPosition)
            is GraphynEditorIntent.UpdateConnectionDraftPosition -> connectionDraftPosition = intent.position
            is GraphynEditorIntent.UpdateViewportTransform ->
                viewportState.updateTransform(intent.pan, intent.zoom, intent.focus)
            GraphynEditorIntent.CancelConnection -> {
                connectionDraft = null
                connectionDraftPosition = null
                nodePickerState = null
            }
            is GraphynEditorIntent.ReconnectSelectedConnection ->
                reconnectSelectedConnection(intent.toNodeId, intent.toPort)
            is GraphynEditorIntent.ShowNodePicker -> {
                val draft = connectionDraft ?: return
                nodePickerState = GraphynNodePickerState(intent.screenPosition, intent.worldPosition, draft)
            }
            GraphynEditorIntent.DismissNodePicker -> {
                nodePickerState = null
                connectionDraft = null
                connectionDraftPosition = null
            }
        }
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

    // Queries
    fun selectedNode(): NodeRef? = workflow?.nodes?.firstOrNull { it.id == selectedNodeId }
    fun outputsFor(nodeId: String): Map<String, WorkflowValue> = nodeOutputsByNodeId[nodeId].orEmpty()
    fun flattenedOutputsFor(nodeId: String): Map<String, WorkflowValue> = dataStore.flattenedOutputsFor(nodeId)
    fun affectedNodeIds(nodeId: String): Set<String> = workflow?.let {
        dataStore.updateWorkflow(it); dataStore.affectedNodeIds(nodeId)
    }.orEmpty()
    fun addDebugLog(message: String) = log.push(message)
}
