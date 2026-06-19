package com.ronjunevaldoz.graphyn.editor.canvas.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ronjunevaldoz.graphyn.editor.state.GraphynEditorState
import com.ronjunevaldoz.graphyn.editor.state.NodeGroup

private val GROUP_COLORS = listOf(
    Color(0x336C63F7), Color(0x33F9A825), Color(0x334ADE80),
    Color(0x33F87171), Color(0x3338BDF8),
)
private val GROUP_BORDER_COLORS = listOf(
    Color(0xFF6C63F7), Color(0xFFF9A825), Color(0xFF4ADE80),
    Color(0xFFF87171), Color(0xFF38BDF8),
)
private const val PADDING = 20

@Composable
internal fun GraphynGroupLayer(state: GraphynEditorState) {
    state.groups.forEachIndexed { index, group ->
        GroupFrame(state, group, index % GROUP_COLORS.size)
    }
}

@Composable
private fun GroupFrame(state: GraphynEditorState, group: NodeGroup, colorIndex: Int) {
    val positions = group.nodeIds.mapNotNull { nodeId ->
        val idx = state.workflow?.nodes?.indexOfFirst { it.id == nodeId }?.takeIf { it >= 0 } ?: return@mapNotNull null
        state.nodePosition(nodeId, idx)
    }
    if (positions.isEmpty()) return

    val nodeSize = IntSize(220, 180)
    val minX = positions.minOf { it.x } - PADDING
    val minY = positions.minOf { it.y } - PADDING
    val maxX = positions.maxOf { it.x } + nodeSize.width + PADDING
    val maxY = positions.maxOf { it.y } + nodeSize.height + PADDING
    val w = (maxX - minX).dp
    val h = (maxY - minY).dp
    val fillColor = GROUP_COLORS[colorIndex]
    val borderColor = GROUP_BORDER_COLORS[colorIndex]
    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier = Modifier
            .offset { IntOffset(minX, minY) }
            .size(w, h)
            .drawBehind { drawRect(fillColor) }
            .border(1.5.dp, borderColor.copy(alpha = 0.6f), shape),
        contentAlignment = Alignment.TopStart,
    ) {
        BasicText(
            group.label,
            modifier = Modifier.offset(x = 8.dp, y = 4.dp),
            style = TextStyle(color = borderColor, fontSize = 10.sp),
        )
    }
}
