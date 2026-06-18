package com.ronjunevaldoz.graphyn.ui.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.editor.canvas.NodeCanvasContext
import com.ronjunevaldoz.graphyn.editor.canvas.NodeCanvasFactory
import com.ronjunevaldoz.graphyn.editor.canvas.NodeShape
import com.ronjunevaldoz.graphyn.editor.canvas.NodeStatusBadge
import kotlin.math.roundToInt

private const val LABEL_HEIGHT_DP = 18

class ShapeCardFactory(
    val shape: Shape = CircleShape,
    val size: Dp = 64.dp,
    val theme: ShapeNodeTheme = ShapeNodeTheme(),
    val minimapShape: NodeShape = NodeShape.Circle,
    val avatar: (@Composable (node: NodeRef, spec: NodeSpec) -> Unit)? = null,
) : NodeCanvasFactory {
    override val nodeWidth = size.value.toInt()
    override val nodeHeight = size.value.toInt() + LABEL_HEIGHT_DP
    override val nodeShape = minimapShape

    override fun portAnchorY(portIndex: Int, isInput: Boolean, spec: NodeSpec): Int =
        size.value.toInt() / 2

    @Composable
    override fun NodeCanvas(context: NodeCanvasContext) {
        ShapeCard(context, shape, size, theme, avatar)
    }
}

@Composable
private fun ShapeCard(
    ctx: NodeCanvasContext,
    shape: Shape,
    size: Dp,
    theme: ShapeNodeTheme,
    avatar: (@Composable (NodeRef, NodeSpec) -> Unit)?,
) {
    val bg = theme.background()
    val borderColor = if (ctx.selected) theme.selectedBorder() else bg
    Box {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(shape)
                    .background(bg)
                    .then(if (ctx.selected) Modifier.border(2.dp, borderColor, shape) else Modifier)
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
                contentAlignment = Alignment.Center,
            ) {
                if (avatar != null) {
                    avatar(ctx.node, ctx.spec)
                } else {
                    BasicText(
                        ctx.spec.label.take(1).uppercase(),
                        style = TextStyle(color = theme.iconColor(), fontSize = 22.sp, fontWeight = FontWeight.Bold),
                    )
                }
            }
            BasicText(
                ctx.spec.label,
                style = TextStyle(color = theme.labelColor(), fontSize = 10.sp, fontWeight = FontWeight.Medium),
            )
        }
        NodeStatusBadge(
            status = ctx.executionStatus,
            surfaceColor = bg,
            modifier = Modifier.align(Alignment.TopEnd),
        )
    }
}
