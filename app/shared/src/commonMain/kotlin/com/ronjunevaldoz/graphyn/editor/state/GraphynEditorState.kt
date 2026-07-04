package com.ronjunevaldoz.graphyn.editor.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.ronjunevaldoz.graphyn.core.execution.NodeExecutionStatus
import com.ronjunevaldoz.graphyn.core.execution.WorkflowExecutionResult
import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowNodePosition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.core.registry.NodeSpecRegistry
import com.ronjunevaldoz.graphyn.core.store.WorkflowStore
import com.ronjunevaldoz.graphyn.core.sync.WorkflowDataStore
import com.ronjunevaldoz.graphyn.editor.canvas.GraphynCanvasBounds
import com.ronjunevaldoz.graphyn.editor.canvas.NodeCanvasRegistry
import com.ronjunevaldoz.graphyn.editor.interaction.GraphynConnectionDraft
import com.ronjunevaldoz.graphyn.editor.interaction.GraphynEditorIntent
import com.ronjunevaldoz.graphyn.editor.interaction.GraphynNodePickerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GraphynEditorState(
    initialWorkflow: WorkflowDefinition? = null,
    private val canvasBounds: GraphynCanvasBounds = GraphynCanvasBounds(),
    internal val nodeSpecs: NodeSpecRegistry? = null,
    store: WorkflowStore? = null,
) : GraphynEditorStateView {
    private val workflowState = mutableStateOf(initialWorkflow)
    private val _workflowFlow = MutableStateFlow(initialWorkflow)

    internal val viewportState = GraphynViewportState(canvasBounds)
    internal val layout = GraphynNodeLayoutState(
        canvasBounds = canvasBounds,
        viewportScale = { viewportState.viewport.scale },
        onPositionsChanged = ::persistNodePositions,
        nodeSizeResolver = { nodeId -> resolveNodeSizeById(nodeId) },
    )
    internal val log = GraphynDebugLogState()
    internal val telemetry = GraphynDebugLogState()
    internal val dataStore = WorkflowDataStore(initialWorkflow)

    // When true, a canvas resize re-runs fitToContent so the graph stays centered/contained.
    // Cleared once the user manually pans or zooms, so we never fight their navigation.
    internal var autoFitOnResize = false
    internal val history = GraphynHistoryState()
    internal val clipboard = GraphynClipboardState()

    override val workflowFlow: StateFlow<WorkflowDefinition?> = _workflowFlow.asStateFlow()

    override var workflow: WorkflowDefinition?
        get() = workflowState.value
        set(value) {
            workflowState.value = value
            _workflowFlow.value = value
            dataStore.updateWorkflow(value)
            viewportState.refresh(value?.nodes?.mapTo(mutableSetOf()) { it.id }.orEmpty())
            val restored = value?.nodePositions.orEmpty().mapValues { (_, position) ->
                IntOffset(position.x, position.y)
            }
            if (restored != layout.nodePositionsByNodeId) {
                layout.restorePositions(restored)
            }
        }

    override var selectedNodeId by mutableStateOf<String?>(null)
    var selectedNodeIds by mutableStateOf<Set<String>>(emptySet())
    var selectedConnection by mutableStateOf<ConnectionRef?>(null)
    var connectionDraft by mutableStateOf<GraphynConnectionDraft?>(null)
    var connectionDraftPosition by mutableStateOf<Offset?>(null)
    var nodePickerState by mutableStateOf<GraphynNodePickerState?>(null)
    var nodeOutputsByNodeId by mutableStateOf<Map<String, Map<String, WorkflowValue>>>(emptyMap())
    var executionStatusByNodeId by mutableStateOf<Map<String, NodeExecutionStatus>>(emptyMap())
    var lastExecutionResult by mutableStateOf<WorkflowExecutionResult?>(null)
    internal var canvasCards: NodeCanvasRegistry? by mutableStateOf(null)
    /** True once the editor shell has registered its canvas card factories. */
    val hasCanvasCards: Boolean get() = canvasCards != null
    internal val scope = CoroutineScope(SupervisorJob())
    private var _rejectionSerial = 0
    var rejectedConnectionPort by mutableStateOf<Triple<String, String, Int>?>(null)

    fun rejectConnectionPort(nodeId: String, portName: String) {
        rejectedConnectionPort = Triple(nodeId, portName, ++_rejectionSerial)
    }
    var groups by mutableStateOf<List<NodeGroup>>(emptyList())

    val effectiveSelectedNodeIds: Set<String>
        get() = if (selectedNodeIds.isNotEmpty()) selectedNodeIds
                else selectedNodeId?.let { setOf(it) } ?: emptySet()

    var viewport: GraphynViewport
        get() = viewportState.viewport
        set(value) { viewportState.viewport = value }
    val canvasSize get() = viewportState.canvasSize
    val graphWorldBounds get() = viewportState.graphWorldBounds
    val debugLogEntries get() = log.entries
    val telemetryEntries get() = telemetry.entries
    val nodePositionsByNodeId get() = layout.nodePositionsByNodeId

    init {
        val nodes = initialWorkflow?.nodes.orEmpty()
        layout.restorePositions(initialWorkflow?.nodePositions.orEmpty().mapValues { (_, position) ->
            IntOffset(position.x, position.y)
        })
        viewportState.refresh(nodes.mapTo(mutableSetOf()) { it.id })
        if (store != null) initAutoSave(store)
    }

    private fun persistNodePositions(positions: Map<String, IntOffset>) {
        val current = workflowState.value ?: return
        val persisted = positions.mapValues { (_, position) ->
            WorkflowNodePosition(position.x, position.y)
        }
        if (current.nodePositions == persisted) return
        val updated = current.copy(nodePositions = persisted)
        workflowState.value = updated
        _workflowFlow.value = updated
        dataStore.updateWorkflow(updated)
    }

    fun dispatch(intent: GraphynEditorIntent) = handleDispatch(intent)

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
    fun nodeSize(nodeId: String): IntSize = layout.nodeSize(nodeId)
    fun nodeBounds(nodeId: String, index: Int): Rect = layout.nodeBounds(nodeId, index)
    fun isWorldPositionOverNode(position: Offset): Boolean {
        val nodes = workflow?.nodes ?: return false
        return layout.isOverNode(position, nodes.mapIndexed { i, n -> n.id to i })
    }

    // Viewport delegation — fit/resize/pan actions live in GraphynEditorViewportActions.kt
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
