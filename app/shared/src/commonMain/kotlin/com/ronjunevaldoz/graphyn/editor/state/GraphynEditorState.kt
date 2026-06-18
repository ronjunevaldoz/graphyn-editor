package com.ronjunevaldoz.graphyn.editor.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.ronjunevaldoz.graphyn.core.execution.NodeExecutionStatus
import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.core.registry.NodeSpecRegistry
import com.ronjunevaldoz.graphyn.core.sync.WorkflowDataStore
import com.ronjunevaldoz.graphyn.editor.canvas.GraphynCanvasBounds
import com.ronjunevaldoz.graphyn.editor.canvas.GraphynCanvasLayout
import com.ronjunevaldoz.graphyn.editor.interaction.GraphynConnectionDraft
import com.ronjunevaldoz.graphyn.editor.interaction.GraphynEditorIntent
import com.ronjunevaldoz.graphyn.editor.interaction.GraphynNodePickerState

class GraphynEditorState(
    initialWorkflow: WorkflowDefinition? = null,
    private val canvasBounds: GraphynCanvasBounds = GraphynCanvasBounds(),
    internal val nodeSpecs: NodeSpecRegistry? = null,
) {
    internal val viewportState = GraphynViewportState(canvasBounds)
    internal val layout = GraphynNodeLayoutState(canvasBounds, viewportScale = { viewportState.viewport.scale })
    internal val log = GraphynDebugLogState()
    internal val dataStore = WorkflowDataStore(initialWorkflow)
    internal val history = GraphynHistoryState()
    internal val clipboard = GraphynClipboardState()

    private val workflowState = mutableStateOf(initialWorkflow)
    var workflow: WorkflowDefinition?
        get() = workflowState.value
        set(value) {
            workflowState.value = value
            dataStore.updateWorkflow(value)
            viewportState.refresh(value?.nodes?.mapTo(mutableSetOf()) { it.id }.orEmpty())
        }

    var selectedNodeId by mutableStateOf<String?>(null)
    var selectedNodeIds by mutableStateOf<Set<String>>(emptySet())
    var selectedConnection by mutableStateOf<ConnectionRef?>(null)
    var connectionDraft by mutableStateOf<GraphynConnectionDraft?>(null)
    var connectionDraftPosition by mutableStateOf<Offset?>(null)
    var nodePickerState by mutableStateOf<GraphynNodePickerState?>(null)
    var nodeOutputsByNodeId by mutableStateOf<Map<String, Map<String, WorkflowValue>>>(emptyMap())
    var executionStatusByNodeId by mutableStateOf<Map<String, NodeExecutionStatus>>(emptyMap())
    var rejectedConnectionPort by mutableStateOf<Pair<String, String>?>(null)

    val effectiveSelectedNodeIds: Set<String>
        get() = if (selectedNodeIds.isNotEmpty()) selectedNodeIds
                else selectedNodeId?.let { setOf(it) } ?: emptySet()

    var viewport: GraphynViewport
        get() = viewportState.viewport
        set(value) { viewportState.viewport = value }
    val canvasSize get() = viewportState.canvasSize
    val graphWorldBounds get() = viewportState.graphWorldBounds
    val debugLogEntries get() = log.entries
    val nodePositionsByNodeId get() = layout.nodePositionsByNodeId

    init {
        val nodes = initialWorkflow?.nodes.orEmpty()
        nodes.forEachIndexed { index, node ->
            layout.setNodePosition(node.id, GraphynCanvasLayout.fallbackPosition(index))
        }
        viewportState.refresh(nodes.mapTo(mutableSetOf()) { it.id })
    }

    fun dispatch(intent: GraphynEditorIntent) {
        when (intent) {
            is GraphynEditorIntent.SelectNode -> selectNode(intent.nodeId)
            is GraphynEditorIntent.ToggleNodeSelection -> toggleNodeSelection(intent.nodeId)
            GraphynEditorIntent.SelectAll -> selectAllNodes()
            GraphynEditorIntent.DeleteSelectedNode -> withHistory { deleteSelectedNode() }
            is GraphynEditorIntent.SelectConnection -> selectConnection(intent.connection)
            GraphynEditorIntent.DeleteSelectedConnection -> withHistory { deleteSelectedConnection() }
            is GraphynEditorIntent.MoveNode -> layout.moveNode(intent.nodeId, intent.delta)
            is GraphynEditorIntent.MoveSelectedNodes -> moveSelectedNodes(intent.delta)
            GraphynEditorIntent.Undo -> restoreSnapshot(history.undo(snapshot()))
            GraphynEditorIntent.Redo -> restoreSnapshot(history.redo(snapshot()))
            GraphynEditorIntent.CopySelection -> copySelection()
            GraphynEditorIntent.Paste -> withHistory { pasteNodes() }
            GraphynEditorIntent.DuplicateSelection -> withHistory { duplicateSelection() }
            is GraphynEditorIntent.BeginConnection -> {
                connectionDraft = GraphynConnectionDraft(intent.fromNodeId, intent.fromPort, intent.isFromInput)
                connectionDraftPosition = null
            }
            is GraphynEditorIntent.CompleteConnection -> withHistory { completeConnection(intent.toNodeId, intent.toPort) }
            is GraphynEditorIntent.AddNode -> withHistory { addNode(intent.spec) }
            is GraphynEditorIntent.AddNodeAndConnect -> withHistory { addNodeAndConnect(intent.spec, intent.toPort, intent.worldPosition) }
            is GraphynEditorIntent.UpdateConnectionDraftPosition -> connectionDraftPosition = intent.position
            is GraphynEditorIntent.UpdateViewportTransform -> viewportState.updateTransform(intent.pan, intent.zoom, intent.focus)
            GraphynEditorIntent.CancelConnection -> { connectionDraft = null; connectionDraftPosition = null; nodePickerState = null }
            is GraphynEditorIntent.ReconnectSelectedConnection -> withHistory { reconnectSelectedConnection(intent.toNodeId, intent.toPort) }
            is GraphynEditorIntent.ShowNodePicker -> {
                val draft = connectionDraft ?: return
                nodePickerState = GraphynNodePickerState(intent.screenPosition, intent.worldPosition, draft)
            }
            GraphynEditorIntent.DismissNodePicker -> { nodePickerState = null; connectionDraft = null; connectionDraftPosition = null }
            is GraphynEditorIntent.UpdateNodeExecutionStatus -> executionStatusByNodeId = executionStatusByNodeId + (intent.nodeId to intent.status)
        }
    }

    internal fun snapshot() = GraphynEditorSnapshot(workflow, layout.nodePositionsByNodeId.toMap())
    internal fun withHistory(block: () -> Unit) { history.push(snapshot()); block() }
    internal fun restoreSnapshot(s: GraphynEditorSnapshot?) {
        s ?: return
        workflow = s.workflow
        s.positions.forEach { (id, pos) -> layout.setNodePosition(id, pos) }
    }

    // Layout delegation
    fun setNodePosition(nodeId: String, position: IntOffset, clearDragRemainder: Boolean = true) =
        layout.setNodePosition(nodeId, position, clearDragRemainder)
    fun nodePosition(nodeId: String, index: Int): IntOffset = layout.nodePosition(nodeId, index)
    fun nodeSize(nodeId: String): IntSize = layout.nodeSize()
    fun nodeBounds(nodeId: String, index: Int): Rect = layout.nodeBounds(nodeId, index)
    fun isWorldPositionOverNode(position: Offset): Boolean {
        val nodes = workflow?.nodes ?: return false
        return layout.isOverNode(position, nodes.mapIndexed { i, n -> n.id to i })
    }

    // Viewport delegation
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
