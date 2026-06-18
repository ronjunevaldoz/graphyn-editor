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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ronjunevaldoz.graphyn.editor.canvas.NodeCanvasContext
import kotlin.math.roundToInt

private val CircleBg     = Color(0xFF1A82E2)
private val SelectBorder = Color(0xFF5BA0EA)
private val LabelColor   = Color(0xFFE8E8E8)

@Composable
fun N8nNodeCard(ctx: NodeCanvasContext) {
    val borderColor = if (ctx.selected) SelectBorder else Color.Transparent
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(CircleBg)
                .border(2.dp, borderColor, CircleShape)
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
                nodeIcon(ctx.spec.type),
                style = TextStyle(color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold),
            )
        }
        BasicText(
            ctx.spec.label,
            style = TextStyle(color = LabelColor, fontSize = 10.sp, fontWeight = FontWeight.Medium),
        )
    }
}

private fun nodeIcon(type: String): String = when {
    type.contains("webhook") -> "⚡"
    type.contains("http")    -> "🌐"
    type.contains("trigger") -> "▶"
    else -> type.take(1).uppercase()
}
