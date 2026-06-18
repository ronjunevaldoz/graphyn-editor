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

private val BgColor      = Color(0xFF2A2A2A)
private val HeaderColor  = Color(0xFF4A6A8A)
private val BorderColor  = Color(0xFF555555)
private val TextColor    = Color(0xFFE8E8E8)
private val LabelColor   = Color(0xFFAAAAAA)
private val ValueColor   = Color(0xFF6DB3F2)
private val SelectBorder = Color(0xFF72A0D8)

@Composable
fun BlenderNodeCard(ctx: NodeCanvasContext) {
    val shape = RoundedCornerShape(4.dp)
    val border = if (ctx.selected) SelectBorder else BorderColor
    Box(
        modifier = Modifier
            .width(220.dp)
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
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                BasicText(ctx.spec.label, style = TextStyle(color = TextColor, fontSize = 11.sp, fontWeight = FontWeight.Bold))
            }
            Column(modifier = Modifier.padding(vertical = 2.dp)) {
                ctx.spec.inputs.forEach { port ->
                    BlenderPortRow(
                        name = port.name,
                        defaultVal = ctx.spec.defaultValues[port.name]?.let { blenderValueLabel(it) },
                        isInput = true,
                    )
                }
                ctx.spec.outputs.forEach { port ->
                    BlenderPortRow(name = port.name, defaultVal = null, isInput = false)
                }
            }
        }
    }
}

@Composable
private fun BlenderPortRow(name: String, defaultVal: String?, isInput: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp, horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isInput) {
            BasicText(name, style = TextStyle(color = LabelColor, fontSize = 10.sp))
            if (defaultVal != null) {
                Spacer(Modifier.weight(1f))
                BasicText(defaultVal, style = TextStyle(color = ValueColor, fontSize = 10.sp))
            } else {
                Spacer(Modifier.weight(1f))
            }
        } else {
            Spacer(Modifier.weight(1f))
            BasicText(name, style = TextStyle(color = LabelColor, fontSize = 10.sp))
        }
    }
}

private fun blenderValueLabel(value: com.ronjunevaldoz.graphyn.core.model.WorkflowValue): String = when (value) {
    is com.ronjunevaldoz.graphyn.core.model.WorkflowValue.IntValue    -> value.value.toString()
    is com.ronjunevaldoz.graphyn.core.model.WorkflowValue.DoubleValue -> "%.3f".format(value.value)
    is com.ronjunevaldoz.graphyn.core.model.WorkflowValue.StringValue -> value.value
    is com.ronjunevaldoz.graphyn.core.model.WorkflowValue.BooleanValue -> if (value.value) "true" else "false"
    else -> ""
}
