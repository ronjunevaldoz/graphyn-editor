package com.ronjunevaldoz.graphyn.plugins.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ronjunevaldoz.graphyn.core.designsystem.theme.appTheme
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.editor.canvas.NodeCanvasContext
import com.ronjunevaldoz.graphyn.editor.canvas.NodeCanvasFactory
import com.ronjunevaldoz.graphyn.editor.canvas.NodeStatusBadge
import kotlin.math.roundToInt

private const val CARD_WIDTH  = 180
private const val HEADER_DP   = 28
private const val PORT_ROW_DP = 22
private const val CONTENT_DP  = 72

internal object PreviewCardFactory : NodeCanvasFactory {
    override val nodeWidth  = CARD_WIDTH
    override val nodeHeight = HEADER_DP + PORT_ROW_DP + CONTENT_DP + PORT_ROW_DP

    override fun portAnchorY(portIndex: Int, isInput: Boolean, spec: NodeSpec): Int =
        if (isInput) HEADER_DP + PORT_ROW_DP / 2
        else HEADER_DP + PORT_ROW_DP + CONTENT_DP + PORT_ROW_DP / 2

    @Composable
    override fun NodeCanvas(context: NodeCanvasContext) = PreviewCard(context)
}

@Composable
private fun PreviewCard(ctx: NodeCanvasContext) {
    val colors = appTheme.colors
    val shape = RoundedCornerShape(6.dp)
    val value = ctx.executionOutputs["value"]

    Box(
        modifier = Modifier.width(CARD_WIDTH.dp).clip(shape)
            .background(colors.surface)
            .border(1.dp, if (ctx.selected) colors.borderFocus else colors.border, shape)
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
                modifier = Modifier.fillMaxWidth().height(HEADER_DP.dp)
                    .background(colors.surfaceVariant).padding(horizontal = 10.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                BasicText("Preview", style = TextStyle(color = colors.onSurface, fontSize = 12.sp, fontWeight = FontWeight.SemiBold))
            }

            Row(Modifier.fillMaxWidth().height(PORT_ROW_DP.dp).padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically) {
                BasicText("value", style = TextStyle(color = colors.onSurfaceVariant, fontSize = 10.sp))
            }

            Box(
                modifier = Modifier.fillMaxWidth().height(CONTENT_DP.dp)
                    .background(colors.muted).padding(8.dp),
                contentAlignment = Alignment.TopStart,
            ) {
                BasicText(
                    text = value.display(),
                    style = TextStyle(
                        fontFamily = if (value != null && value !is WorkflowValue.NullValue) FontFamily.Monospace else FontFamily.Default,
                        fontSize = 10.sp,
                        color = if (value == null || value is WorkflowValue.NullValue) colors.onMuted else colors.onSurface,
                        lineHeight = 14.sp,
                    ),
                )
            }

            Row(Modifier.fillMaxWidth().height(PORT_ROW_DP.dp).padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End) {
                BasicText("value", style = TextStyle(color = colors.onSurfaceVariant, fontSize = 10.sp))
            }
        }
        NodeStatusBadge(ctx.executionStatus, Modifier.align(Alignment.TopEnd).padding(4.dp), colors.surface)
    }
}

private fun WorkflowValue?.display(): String = when (this) {
    null, is WorkflowValue.NullValue    -> "No output yet"
    is WorkflowValue.StringValue        -> value.take(120)
    is WorkflowValue.IntValue           -> value.toString()
    is WorkflowValue.DoubleValue        -> value.toString()
    is WorkflowValue.BooleanValue       -> if (value) "true" else "false"
    is WorkflowValue.ListValue          -> "[ ${items.size} item${if (items.size == 1) "" else "s"} ]"
    is WorkflowValue.RecordValue        -> "{ ${fields.keys.take(3).joinToString(", ")}${if (fields.size > 3) "…" else ""} }"
    else                                -> toString()
}
