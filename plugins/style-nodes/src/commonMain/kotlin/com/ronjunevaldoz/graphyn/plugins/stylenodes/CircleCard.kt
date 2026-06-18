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
import com.ronjunevaldoz.graphyn.editor.canvas.NodeCanvasContext
import com.ronjunevaldoz.graphyn.editor.canvas.NodeStatusBadge
import kotlin.math.roundToInt

@Composable
fun CircleCard(ctx: NodeCanvasContext) {
    Box {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(CIRCLE_BG)
                    .then(if (ctx.selected) Modifier.border(2.dp, NODE_SELECT, CircleShape) else Modifier)
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
                BasicText(
                    ctx.spec.label.take(1).uppercase(),
                    style = TextStyle(color = NODE_TEXT, fontSize = 22.sp, fontWeight = FontWeight.Bold),
                )
            }
            BasicText(
                ctx.spec.label,
                style = TextStyle(color = NODE_MUTED, fontSize = 10.sp, fontWeight = FontWeight.Medium),
            )
        }
        NodeStatusBadge(status = ctx.executionStatus, surfaceColor = CIRCLE_BG,
            modifier = Modifier.align(Alignment.TopEnd))
    }
}
