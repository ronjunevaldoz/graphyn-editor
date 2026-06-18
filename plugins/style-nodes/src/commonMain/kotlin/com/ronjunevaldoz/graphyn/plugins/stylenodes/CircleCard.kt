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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
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

class CircleCardFactory(
    val theme: CircleNodeTheme = CircleNodeTheme(),
    val avatar: (@Composable (node: NodeRef, spec: NodeSpec) -> Unit)? = null,
) : NodeCanvasFactory {
    override val nodeWidth = 80
    override val nodeHeight = 100
    override val nodeShape = NodeShape.Circle

    @Composable
    override fun NodeCanvas(context: NodeCanvasContext) {
        CircleCard(context, theme, avatar)
    }
}

@Composable
private fun CircleCard(
    ctx: NodeCanvasContext,
    theme: CircleNodeTheme,
    avatar: (@Composable (NodeRef, NodeSpec) -> Unit)?,
) {
    val bg = theme.background()
    val border = if (ctx.selected) theme.selectedBorder() else bg
    Box {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(bg)
                    .then(if (ctx.selected) Modifier.border(2.dp, border, CircleShape) else Modifier)
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
