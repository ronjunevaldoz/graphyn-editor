package com.ronjunevaldoz.graphyn.editor.canvas

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import com.ronjunevaldoz.graphyn.editor.canvas.NodeCanvasRegistry
import com.ronjunevaldoz.graphyn.editor.interaction.GraphynEditorIntent
import com.ronjunevaldoz.graphyn.editor.shortcuts.EditorShortcutAction
import com.ronjunevaldoz.graphyn.editor.shortcuts.GraphynShortcutState
import com.ronjunevaldoz.graphyn.editor.state.GraphynEditorState

internal fun Modifier.graphynDraftTrackingGesture(state: GraphynEditorState): Modifier =
    pointerInput(state.connectionDraft?.fromNodeId, state.viewport) {
        if (state.connectionDraft == null) return@pointerInput
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                val position = event.changes.firstOrNull()?.position ?: continue
                state.dispatch(
                    GraphynEditorIntent.UpdateConnectionDraftPosition(state.screenToWorld(position)),
                )
            }
        }
    }

internal fun Modifier.graphynScrollZoomGesture(state: GraphynEditorState): Modifier =
    pointerInput(state.viewport) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                val scrollDelta = event.changes.firstOrNull()?.scrollDelta ?: Offset.Zero
                if (scrollDelta == Offset.Zero) continue
                if (state.connectionDraft != null) {
                    state.dispatch(GraphynEditorIntent.CancelConnection)
                    continue
                }
                val focus = event.changes.firstOrNull()?.position ?: Offset.Zero
                val zoom = (1f - scrollDelta.y * 0.0045f).coerceIn(0.7f, 1.35f)
                state.dispatch(GraphynEditorIntent.UpdateViewportTransform(Offset.Zero, zoom, focus))
            }
        }
    }

internal fun Modifier.graphynPanGesture(
    state: GraphynEditorState,
    canvasCards: NodeCanvasRegistry? = null,
): Modifier =
    pointerInput(state.workflow) {
        awaitEachGesture {
            val firstDown = awaitFirstDown(requireUnconsumed = false)

            if (state.connectionDraft != null) {
                var releasePos = firstDown.position
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull() ?: break
                    releasePos = change.position
                    if (!change.pressed) break
                }
                val worldPos = state.screenToWorld(releasePos)
                state.dispatch(GraphynEditorIntent.ShowNodePicker(releasePos, worldPos))
                return@awaitEachGesture
            }

            val startWorld = state.screenToWorld(firstDown.position)
            val overNode = state.workflow?.nodes?.withIndex()?.any { (index, node) ->
                val pos = state.nodePosition(node.id, index)
                val factory = resolveNodeFactory(node, canvasCards, state.nodeSpecs, state.workflow)
                val w = (factory?.nodeWidth ?: GraphynCanvasMetrics.NodeSize.width).toFloat()
                val h = (factory?.nodeHeight ?: GraphynCanvasMetrics.NodeSize.height).toFloat()
                startWorld.x >= pos.x && startWorld.x <= pos.x + w &&
                    startWorld.y >= pos.y && startWorld.y <= pos.y + h
            } ?: false
            if (overNode) return@awaitEachGesture

            var accumulatedDrag = Offset.Zero
            var dragging = false
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull() ?: continue
                if (!change.pressed) break
                val delta = change.position - change.previousPosition
                if (delta == Offset.Zero) continue
                accumulatedDrag += delta
                if (!dragging && accumulatedDrag.getDistance() <= viewConfiguration.touchSlop) continue
                dragging = true
                change.consume()
                state.dispatch(GraphynEditorIntent.UpdateViewportTransform(delta, 1f, change.position))
            }
        }
    }

internal fun Modifier.graphynKeyboardShortcuts(
    state: GraphynEditorState,
    shortcuts: GraphynShortcutState,
): Modifier =
    onKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
        val action = shortcuts.resolveAction(event)
        if (action != null) {
            dispatchShortcutAction(state, action)
            return@onKeyEvent true
        }
        when {
            event.key == Key.Escape -> {
                if (state.connectionDraft != null) state.dispatch(GraphynEditorIntent.CancelConnection)
                else { state.selectedNodeId = null; state.selectedNodeIds = emptySet(); state.selectedConnection = null }
                true
            }
            event.key == Key.Backspace || event.key == Key.Delete -> when {
                state.connectionDraft != null -> { state.dispatch(GraphynEditorIntent.CancelConnection); true }
                state.effectiveSelectedNodeIds.isNotEmpty() -> { state.dispatch(GraphynEditorIntent.DeleteSelectedNode); true }
                state.selectedConnection != null -> { state.dispatch(GraphynEditorIntent.DeleteSelectedConnection); true }
                else -> false
            }
            else -> false
        }
    }

private fun dispatchShortcutAction(state: GraphynEditorState, action: EditorShortcutAction) {
    val intent = when (action) {
        EditorShortcutAction.Undo -> GraphynEditorIntent.Undo
        EditorShortcutAction.Redo -> GraphynEditorIntent.Redo
        EditorShortcutAction.Copy -> GraphynEditorIntent.CopySelection
        EditorShortcutAction.Paste -> GraphynEditorIntent.Paste
        EditorShortcutAction.Duplicate -> GraphynEditorIntent.DuplicateSelection
        EditorShortcutAction.SelectAll -> GraphynEditorIntent.SelectAll
        EditorShortcutAction.AutoLayout -> GraphynEditorIntent.AutoLayout
        EditorShortcutAction.Group -> GraphynEditorIntent.CreateGroupFromSelection
        EditorShortcutAction.CollapseToSubgraph -> GraphynEditorIntent.CollapseSelectionToSubgraph
    }
    state.dispatch(intent)
}
