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
import androidx.compose.foundation.layout.widthIn
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
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.editor.canvas.NodeCanvasContext
import com.ronjunevaldoz.graphyn.editor.canvas.NodeStatusBadge
import kotlin.math.roundToInt

private val BgColor      = Color(0xFF2A2A2A)
private val HeaderColor  = Color(0xFF4A6A8A)
private val BorderColor  = Color(0xFF555555)
private val TextColor    = Color(0xFFE8E8E8)
private val LabelColor   = Color(0xFFAAAAAA)
private val ValueColor   = Color(0xFFDDDDDD)
private val FieldBg      = Color(0xFF0F0F0F)
private val SelectBorder = Color(0xFF72A0D8)

@Composable
fun FieldCard(ctx: NodeCanvasContext) {
    val shape = RoundedCornerShape(4.dp)
    val borderCol = if (ctx.selected) SelectBorder else BorderColor
    Box(
        modifier = Modifier
            .width(220.dp)
            .clip(shape)
            .background(BgColor)
            .border(1.dp, borderCol, shape)
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
                    FieldInputRow(
                        name = port.name,
                        defaultVal = ctx.spec.defaultValues[port.name]?.let { fieldValueLabel(it) },
                    )
                }
                ctx.spec.outputs.forEach { port ->
                    FieldOutputRow(port.name)
                }
            }
        }
        NodeStatusBadge(
            status = ctx.executionStatus,
            surfaceColor = BgColor,
            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
        )
    }
}

@Composable
private fun FieldInputRow(name: String, defaultVal: String?) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .padding(start = 12.dp, end = 8.dp, top = 3.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(name, style = TextStyle(color = LabelColor, fontSize = 10.sp))
        Spacer(Modifier.weight(1f))
        if (defaultVal != null) {
            Box(
                modifier = Modifier
                    .widthIn(min = 60.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(FieldBg)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                BasicText(defaultVal, style = TextStyle(color = ValueColor, fontSize = 10.sp))
            }
        }
    }
}

@Composable
private fun FieldOutputRow(name: String) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .padding(start = 8.dp, end = 12.dp, top = 3.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.weight(1f))
        BasicText(name, style = TextStyle(color = LabelColor, fontSize = 10.sp))
    }
}

private fun fieldValueLabel(value: WorkflowValue): String = when (value) {
    is WorkflowValue.IntValue     -> value.value.toString()
    is WorkflowValue.DoubleValue  -> (kotlin.math.round(value.value * 1000) / 1000.0).toString()
    is WorkflowValue.StringValue  -> value.value
    is WorkflowValue.BooleanValue -> if (value.value) "true" else "false"
    else -> ""
}
