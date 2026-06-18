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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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

private val BgColor      = Color(0xFF1A1A1A)
private val HeaderColor  = Color(0xFF3D2A6B)
private val BorderColor  = Color(0xFF444444)
private val TextColor    = Color(0xFFE0E0E0)
private val MutedColor   = Color(0xFF9B9BA5)
private val SelectBorder = Color(0xFF8B5CF6)

@Composable
fun DarkHeaderCard(ctx: NodeCanvasContext) {
    val shape = RoundedCornerShape(6.dp)
    val border = if (ctx.selected) SelectBorder else BorderColor
    Box(
        modifier = Modifier
            .width(200.dp)
            .clip(shape)
            .background(BgColor)
            .border(1.dp, border, shape)
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
            Box(
                modifier = Modifier.fillMaxWidth().background(HeaderColor)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                BasicText(ctx.spec.label, style = TextStyle(color = TextColor, fontSize = 12.sp, fontWeight = FontWeight.Bold))
            }
            Row(modifier = Modifier.padding(vertical = 4.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    ctx.spec.inputs.forEach { DarkHeaderInputRow(it.name) }
                }
                Column(modifier = Modifier.weight(1f)) {
                    ctx.spec.outputs.forEach { DarkHeaderOutputRow(it.name) }
                }
            }
        }
    }
}

@Composable
private fun DarkHeaderInputRow(name: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.width(16.dp))
        BasicText(name, style = TextStyle(color = MutedColor, fontSize = 10.sp))
    }
}

@Composable
private fun DarkHeaderOutputRow(name: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.weight(1f))
        BasicText(name, style = TextStyle(color = MutedColor, fontSize = 10.sp))
        Spacer(Modifier.width(16.dp))
    }
}
