package com.ronjunevaldoz.graphyn.plugins.stylenodes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.editor.canvas.NodeCanvasContext
import com.ronjunevaldoz.graphyn.editor.canvas.NodeCanvasFactory
import com.ronjunevaldoz.graphyn.editor.canvas.NodeStatusBadge
import kotlin.math.roundToInt

internal const val HEADER_DP = 28
internal const val ROW_DP = 22

class FieldCardFactory(
    val theme: FieldNodeTheme = FieldNodeTheme(),
) : NodeCanvasFactory {
    override val nodeWidth = 220
    override val nodeHeight = 120

    override fun portAnchorY(portIndex: Int, isInput: Boolean, spec: NodeSpec): Int =
        HEADER_DP + portIndex * ROW_DP + ROW_DP / 2

    @Composable
    override fun NodeCanvas(context: NodeCanvasContext) = FieldCard(context, theme)
}

@Composable
private fun FieldCard(ctx: NodeCanvasContext, theme: FieldNodeTheme) {
    val bg = theme.background()
    val borderColor = if (ctx.selected) theme.selectedBorder() else theme.border()
    val shape = RoundedCornerShape(CORNER_RADIUS.dp)
    Box(
        modifier = Modifier
            .width(220.dp).clip(shape).background(bg).border(1.dp, borderColor, shape)
            .clickable { ctx.onSelect() }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    awaitTouchSlopOrCancellation(down.id) { c, _ -> c.consume() }
                        ?: return@awaitEachGesture
                    drag(down.id) { c ->
                        c.consume()
                        val d = c.position - c.previousPosition
                        ctx.onMove(IntOffset(d.x.roundToInt(), d.y.roundToInt()))
                    }
                }
            },
    ) {
        Column {
            FieldHeader(ctx.spec.label, theme)
            FieldBody(ctx.spec, ctx.spec.defaultValues, theme)
            FieldFooter(theme)
        }
        NodeStatusBadge(ctx.executionStatus, Modifier.align(Alignment.TopEnd).padding(4.dp), bg)
    }
}
