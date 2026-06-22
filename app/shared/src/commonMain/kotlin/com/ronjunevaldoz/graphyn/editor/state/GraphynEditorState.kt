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
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.core.registry.NodeSpecRegistry
import com.ronjunevaldoz.graphyn.core.sync.WorkflowDataStore
import com.ronjunevaldoz.graphyn.editor.canvas.GraphynCanvasBounds
import com.ronjunevaldoz.graphyn.editor.canvas.GraphynCanvasLayout
import com.ronjunevaldoz.graphyn.editor.canvas.GraphynCanvasMetrics
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
) : GraphynEditorStateView {
    internal val viewportState = GraphynViewportState(canvasBounds)
    internal val layout = GraphynNodeLayoutState(canvasBounds, viewportScale = { viewportState.viewport.scale })
    internal val log = GraphynDebugLogState()
    internal val telemetry = GraphynDebugLogState()
    internal val dataStore = WorkflowDataStore(initialWorkflow)

    // When true, a canvas resize re-runs fitToContent so the graph stays centered/contained.
    // Cleared once the user manually pans or zooms, so we never fight their navigation.
    private var autoFitOnResize = false
    internal val history = GraphynHistoryState()
    internal val clipboard = GraphynClipboardState()

    private val workflowState = mutableStateOf(initialWorkflow)
    private val _workflowFlow = MutableStateFlow(initialWorkflow)
    override val workflowFlow: StateFlow<WorkflowDefinition?> = _workflowFlow.asStateFlow()

    override var workflow: WorkflowDefinition?
        get() = workflowState.value
        set(value) {
            workflowState.value = value
            _workflowFlow.value = value
            dataStore.updateWorkflow(value)
            viewportState.refresh(value?.nodes?.mapTo(mutableSetOf()) { it.id }.orEmpty())
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
        nodes.forEachIndexed { index, node ->
            layout.setNodePosition(node.id, GraphynCanvasLayout.fallbackPosition(index))
        }
        viewportState.refresh(nodes.mapTo(mutableSetOf()) { it.id })
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
    fun nodeSize(nodeId: String): IntSize = layout.nodeSize()
    fun nodeBounds(nodeId: String, index: Int): Rect = layout.nodeBounds(nodeId, index)
    fun isWorldPositionOverNode(position: Offset): Boolean {
        val nodes = workflow?.nodes ?: return false
        return layout.isOverNode(position, nodes.mapIndexed { i, n -> n.id to i })
    }

    // Viewport delegation
    fun updateCanvasSize(size: IntSize) {
        val changed = size != viewportState.canvasSize
        if (changed) telemetry.push("canvasSize -> ${size.width}x${size.height}")
        viewportState.updateCanvasSize(size)
        if (changed && autoFitOnResize && size.width > 0 && size.height > 0) fitToContent()
    }

    /** Manual pan/zoom: stop auto-refitting so we don't override the user's navigation. */
    fun updateViewportTransform(pan: Offset, zoom: Float, focus: Offset) {
        autoFitOnResize = false
        viewportState.updateTransform(pan, zoom, focus)
    }
    fun resetViewport() { viewportState.reset(); log.push("Viewport reset") }
    fun fitToContent(
        positions: Map<String, IntOffset>? = null,
        sizes: Map<String, IntSize> = emptyMap(),
    ) {
        val resolvedPositions = positions ?: layout.nodePositionsByNodeId
        val resolvedSizes = sizes.ifEmpty {
            workflow?.nodes?.associate { node ->
                node.id to (canvasCards?.resolve(node.type)
                    ?.let { IntSize(it.nodeWidth, it.nodeHeight) }
                    ?: GraphynCanvasMetrics.NodeSize)
            } ?: emptyMap()
        }
        viewportState.fitToPositions(resolvedPositions, resolvedSizes, maxScale = 1.0f)
        autoFitOnResize = resolvedPositions.isNotEmpty()
        if (resolvedPositions.isNotEmpty()) {
            val vp = viewportState.viewport
            val cs = viewportState.canvasSize
            val default = GraphynCanvasMetrics.NodeSize
            val minX = resolvedPositions.values.minOf { it.x.toFloat() }
            val maxX = resolvedPositions.entries.maxOf { (id, p) -> p.x + (resolvedSizes[id]?.width ?: default.width).toFloat() }
            val minY = resolvedPositions.values.minOf { it.y.toFloat() }
            val maxY = resolvedPositions.entries.maxOf { (id, p) -> p.y + (resolvedSizes[id]?.height ?: default.height).toFloat() }
            val lGap = (minX * vp.scale + vp.offset.x).toInt()
            val rGap = (cs.width - (maxX * vp.scale + vp.offset.x)).toInt()
            val tGap = (minY * vp.scale + vp.offset.y).toInt()
            val bGap = (cs.height - (maxY * vp.scale + vp.offset.y)).toInt()
            val s = (vp.scale * 1000).toInt() / 1000f
            telemetry.push("fit: canvas=${cs.width}x${cs.height} scale=$s off=(${vp.offset.x.toInt()},${vp.offset.y.toInt()}) L=$lGap R=$rGap T=$tGap B=$bGap")
        }
    }
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
