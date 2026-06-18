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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
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
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.editor.canvas.NodeCanvasContext
import com.ronjunevaldoz.graphyn.editor.canvas.NodeStatusBadge
import kotlin.math.roundToInt

private fun PortSpec.color() = portColor?.let { Color(it) } ?: NODE_MUTED

@Composable
fun FieldCard(ctx: NodeCanvasContext) {
    val shape = RoundedCornerShape(CORNER_RADIUS.dp)
    val border = if (ctx.selected) NODE_SELECT else NODE_BORDER
    Box(
        modifier = Modifier
            .width(220.dp)
            .clip(shape)
            .background(NODE_BG)
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
                modifier = Modifier.fillMaxWidth().background(FIELD_HEADER_BG)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                BasicText(ctx.spec.label, style = TextStyle(color = NODE_TEXT, fontSize = 12.sp, fontWeight = FontWeight.SemiBold))
            }
            Column(modifier = Modifier.padding(vertical = 2.dp)) {
                ctx.spec.inputs.forEach { port ->
                    FieldInputRow(port, ctx.spec.defaultValues[port.name]?.let { fieldValueLabel(it) })
                }
                ctx.spec.outputs.forEach { FieldOutputRow(it) }
            }
        }
        NodeStatusBadge(status = ctx.executionStatus, surfaceColor = NODE_BG,
            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp))
    }
}

@Composable
private fun FieldInputRow(port: PortSpec, defaultVal: String?) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(port.color()))
        Spacer(Modifier.width(5.dp))
        BasicText(port.name, style = TextStyle(color = NODE_MUTED, fontSize = 10.sp))
        Spacer(Modifier.weight(1f))
        if (defaultVal != null) {
            Box(
                modifier = Modifier
                    .widthIn(min = 44.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(FIELD_VALUE_BG)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                BasicText(defaultVal, style = TextStyle(color = NODE_TEXT, fontSize = 10.sp))
            }
        }
    }
}

@Composable
private fun FieldOutputRow(port: PortSpec) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.weight(1f))
        BasicText(port.name, style = TextStyle(color = NODE_MUTED, fontSize = 10.sp))
        Spacer(Modifier.width(5.dp))
        Box(Modifier.size(6.dp).clip(CircleShape).background(port.color()))
    }
}

private fun fieldValueLabel(v: WorkflowValue): String = when (v) {
    is WorkflowValue.IntValue     -> v.value.toString()
    is WorkflowValue.DoubleValue  -> (kotlin.math.round(v.value * 1000) / 1000.0).toString()
    is WorkflowValue.StringValue  -> v.value
    is WorkflowValue.BooleanValue -> if (v.value) "true" else "false"
    else -> ""
}
