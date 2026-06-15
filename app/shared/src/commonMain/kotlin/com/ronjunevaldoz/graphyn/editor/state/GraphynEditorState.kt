package com.ronjunevaldoz.graphyn.editor.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt
import com.ronjunevaldoz.graphyn.core.execution.WorkflowExecutionEngine
import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.core.execution.WorkflowExecutionResult
import com.ronjunevaldoz.graphyn.core.sync.WorkflowDataStore
import com.ronjunevaldoz.graphyn.editor.canvas.GraphynCanvasLayout
import com.ronjunevaldoz.graphyn.editor.canvas.GraphynCanvasMetrics
import com.ronjunevaldoz.graphyn.editor.canvas.GraphynCanvasBounds
import com.ronjunevaldoz.graphyn.editor.interaction.GraphynConnectionDraft
import com.ronjunevaldoz.graphyn.editor.interaction.GraphynEditorIntent

class GraphynEditorState(
    initialWorkflow: WorkflowDefinition? = null,
    private val canvasBounds: GraphynCanvasBounds = GraphynCanvasBounds(),
) {
    private companion object {
        const val MinViewportScale = 0.45f
        const val MaxViewportScale = 5.0f
        const val MaxDebugLogEntries = 12
    }

    private val workflowState = mutableStateOf(initialWorkflow)
    var workflow: WorkflowDefinition?
        get() = workflowState.value
        set(value) {
            workflowState.value = value
            dataStore.updateWorkflow(value)
            val currentNodeIds = value?.nodes?.mapTo(mutableSetOf()) { it.id }.orEmpty()
            if (currentNodeIds != minimapBoundNodeIds) {
                minimapBoundNodeIds = currentNodeIds
                refreshGraphWorldBounds()
            }
        }

    var selectedNodeId by mutableStateOf<String?>(null)
    var nodeOutputsByNodeId by mutableStateOf<Map<String, Map<String, WorkflowValue>>>(emptyMap())
    var nodePositionsByNodeId by mutableStateOf<Map<String, IntOffset>>(emptyMap())
    var graphWorldBounds by mutableStateOf<Rect?>(null)
    var debugLogEntries by mutableStateOf<List<String>>(emptyList())
    var connectionDraft by mutableStateOf<GraphynConnectionDraft?>(null)
    var connectionDraftPosition by mutableStateOf<Offset?>(null)
    var viewport by mutableStateOf(GraphynViewport())
    var canvasSize by mutableStateOf(IntSize.Zero)
    private val dataStore = WorkflowDataStore(initialWorkflow)
    private var minimapBoundNodeIds: Set<String> = initialWorkflow?.nodes?.mapTo(mutableSetOf()) { it.id }.orEmpty()
    private val nodeDragRemaindersByNodeId = mutableMapOf<String, Offset>()

    init {
        refreshGraphWorldBounds()
    }

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
                pushDebugLog("Connected ${draft.fromNodeId}:${draft.fromPort} -> ${intent.toNodeId}:${intent.toPort}")
            }
            is GraphynEditorIntent.AddNode -> {
                addNode(intent.spec)
            }
            is GraphynEditorIntent.UpdateConnectionDraftPosition -> {
                connectionDraftPosition = intent.position
            }
            is GraphynEditorIntent.UpdateViewportTransform -> {
                updateViewportTransform(
                    pan = intent.pan,
                    zoom = intent.zoom,
                    focus = intent.focus,
                )
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
        pushDebugLog("Added node $nodeId (${spec.label})")
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
        pushDebugLog("Execution completed: ${result.nodeOutputsByNodeId.size} node outputs updated")
    }

    fun execute(engine: WorkflowExecutionEngine) {
        val currentWorkflow = workflow ?: return
        applyExecutionResult(engine.execute(currentWorkflow))
    }

    fun setNodePosition(
        nodeId: String,
        position: IntOffset,
        clearDragRemainder: Boolean = true,
    ) {
        nodePositionsByNodeId = nodePositionsByNodeId + (nodeId to clampNodePosition(nodeId, position))
        if (clearDragRemainder) {
            nodeDragRemaindersByNodeId.remove(nodeId)
        }
    }

    fun moveNode(nodeId: String, delta: IntOffset) {
        val currentPosition = nodePositionsByNodeId[nodeId] ?: IntOffset.Zero
        val previousRemainder = nodeDragRemaindersByNodeId[nodeId] ?: Offset.Zero
        val worldDelta = if (viewport.scale != 0f) {
            Offset(
                x = (delta.x / viewport.scale) + previousRemainder.x,
                y = (delta.y / viewport.scale) + previousRemainder.y,
            )
        } else {
            Offset(
                x = delta.x.toFloat() + previousRemainder.x,
                y = delta.y.toFloat() + previousRemainder.y,
            )
        }
        val appliedDelta = IntOffset(
            x = worldDelta.x.roundToInt(),
            y = worldDelta.y.roundToInt(),
        )
        nodeDragRemaindersByNodeId[nodeId] = Offset(
            x = worldDelta.x - appliedDelta.x,
            y = worldDelta.y - appliedDelta.y,
        )
        setNodePosition(
            nodeId = nodeId,
            position = IntOffset(
                x = currentPosition.x + appliedDelta.x,
                y = currentPosition.y + appliedDelta.y,
            ),
            clearDragRemainder = false,
        )
    }

    fun nodePosition(nodeId: String, index: Int): IntOffset =
        nodePositionsByNodeId[nodeId] ?: GraphynCanvasLayout.fallbackPosition(index)

    fun nodeSize(nodeId: String): IntSize =
        GraphynCanvasMetrics.NodeSize

    fun nodeBounds(nodeId: String, index: Int): Rect {
        val position = nodePosition(nodeId, index)
        val size = nodeSize(nodeId)
        return Rect(
            left = position.x.toFloat(),
            top = position.y.toFloat(),
            right = position.x.toFloat() + size.width,
            bottom = position.y.toFloat() + size.height,
        )
    }

    fun screenToWorld(position: Offset): Offset = viewport.screenToWorld(position)

    fun worldToScreen(position: Offset): Offset = viewport.worldToScreen(position)

    fun isWorldPositionOverNode(position: Offset): Boolean {
        val currentWorkflow = workflow ?: return false
        return currentWorkflow.nodes.anyIndexed { index, node ->
            nodeBounds(node.id, index).contains(position)
        }
    }

    fun updateViewportTransform(
        pan: Offset,
        zoom: Float,
        focus: Offset,
    ) {
        viewport = viewport.zoomAt(
            focus = focus,
            factor = zoom,
            minScale = MinViewportScale,
            maxScale = MaxViewportScale,
        )
        if (pan != Offset.Zero) {
            viewport = viewport.panBy(pan)
        }
        viewport = viewport.constrainTo(graphWorldBounds, canvasSize)
    }

    fun resetViewport() {
        viewport = GraphynViewport()
        viewport = viewport.constrainTo(graphWorldBounds, canvasSize)
        pushDebugLog("Viewport reset")
    }

    fun updateCanvasSize(size: IntSize) {
        canvasSize = size
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

    fun addDebugLog(message: String) {
        pushDebugLog(message)
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

    private fun refreshGraphWorldBounds() {
        val currentWorkflow = workflow ?: run {
            graphWorldBounds = null
            minimapBoundNodeIds = emptySet()
            return
        }
        val currentNodeIds = currentWorkflow.nodes.mapTo(mutableSetOf()) { it.id }
        minimapBoundNodeIds = currentNodeIds
        graphWorldBounds = GraphynCanvasLayout.logicalCanvasBounds(canvasBounds)
        viewport = viewport.constrainTo(graphWorldBounds, canvasSize)
    }

    private fun pushDebugLog(message: String) {
        debugLogEntries = (debugLogEntries + message).takeLast(MaxDebugLogEntries)
    }

    private fun clampNodePosition(nodeId: String, position: IntOffset): IntOffset {
        val size = nodeSize(nodeId)
        val maxX = (canvasBounds.width - size.width).coerceAtLeast(0)
        val maxY = (canvasBounds.height - size.height).coerceAtLeast(0)
        return IntOffset(
            x = position.x.coerceIn(0, maxX),
            y = position.y.coerceIn(0, maxY),
        )
    }

}

private inline fun <T> List<T>.anyIndexed(predicate: (index: Int, item: T) -> Boolean): Boolean {
    for (index in indices) {
        if (predicate(index, this[index])) {
            return true
        }
    }
    return false
}
