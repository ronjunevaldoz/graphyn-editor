package com.ronjunevaldoz.graphyn.editor.canvas.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.execution.NodeExecutionStatus
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.editor.canvas.GraphynCanvasMetrics
import com.ronjunevaldoz.graphyn.editor.design.GraphynDs
import kotlin.math.roundToInt

@Composable
fun GraphynNodeCard(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    executionStatus: NodeExecutionStatus = NodeExecutionStatus.Idle,
    onClick: () -> Unit = {},
    onMove: (IntOffset) -> Unit,
    slots: GraphynNodeCardSlots = GraphynNodeCardSlots(),
) {
    val colors = GraphynDs.colors
    val borderColor = if (selected) colors.selectionRing else colors.border
    val interactionSource = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(8.dp)

    Box(
        modifier = modifier
            .size(GraphynCanvasMetrics.NodeSize.width.dp, GraphynCanvasMetrics.NodeSize.height.dp)
            .clip(shape)
            .background(colors.surfaceCard)
            .border(1.dp, borderColor, shape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    awaitTouchSlopOrCancellation(down.id) { change, _ -> change.consume() }
                        ?: return@awaitEachGesture
                    drag(down.id) { change ->
                        change.consume()
                        val d = change.position - change.previousPosition
                        onMove(IntOffset(d.x.roundToInt(), d.y.roundToInt()))
                    }
                }
            },
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(colors.accent))
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                with(slots) { header(); body(); ports(); footer() }
            }
        }
        GraphynNodeStatusBadge(
            status = executionStatus,
            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
        )
    }
}

@Composable
fun GraphynNodeCardHeader(node: NodeRef, spec: NodeSpec?) {
    val colors = GraphynDs.colors
    val type = GraphynDs.type
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        BasicText(spec?.label ?: node.type, style = type.nodeTitle.copy(color = colors.textPrimary))
        BasicText(node.id, style = type.nodeSubtitle.copy(color = colors.textSecondary))
    }
}

