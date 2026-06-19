package com.ronjunevaldoz.graphyn.plugins.script

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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

private const val CARD_WIDTH_DP  = 320
private const val HEADER_DP      = 28
private const val PORT_ROW_DP    = 26
private const val CODE_HEIGHT_DP = 140

private val CODE_BG   = Color(0xFF1E1E2E)
private val CODE_TEXT = Color(0xFFCDD6F4)
private val CODE_HINT = Color(0xFF6C7086)

internal object ScriptCardFactory : NodeCanvasFactory {
    override val nodeWidth  = CARD_WIDTH_DP
    override val nodeHeight = HEADER_DP + PORT_ROW_DP + 1 + CODE_HEIGHT_DP + 1 + PORT_ROW_DP * 2

    override fun portAnchorY(portIndex: Int, isInput: Boolean, spec: NodeSpec): Int =
        if (isInput) {
            HEADER_DP + PORT_ROW_DP / 2
        } else {
            HEADER_DP + PORT_ROW_DP + 1 + CODE_HEIGHT_DP + 1 + portIndex * PORT_ROW_DP + PORT_ROW_DP / 2
        }

    @Composable
    override fun NodeCanvas(context: NodeCanvasContext) = ScriptCard(context)
}

@Composable
private fun ScriptCard(ctx: NodeCanvasContext) {
    val colors = appTheme.colors
    val shape = RoundedCornerShape(6.dp)
    val code = (ctx.node.config["code"] as? WorkflowValue.StringValue)?.value
        ?: (ctx.spec.defaultValues["code"] as? WorkflowValue.StringValue)?.value
        ?: ""

    // Drag lives on the outer box so every non-interactive area (header, port rows, dividers)
    // is a valid drag handle. BasicTextField absorbs events within its own bounds naturally.
    Box(
        modifier = Modifier.width(CARD_WIDTH_DP.dp).clip(shape)
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
            ScriptHeader(ctx.spec.label, colors.surfaceVariant, colors.onSurface)

            Row(
                modifier = Modifier.fillMaxWidth().height(PORT_ROW_DP.dp).padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicText("input", style = TextStyle(color = colors.onSurfaceVariant, fontSize = 10.sp))
            }

            Box(Modifier.fillMaxWidth().height(1.dp).background(colors.border))

            BasicTextField(
                value = code,
                onValueChange = { ctx.onConfigChange("code", WorkflowValue.StringValue(it)) },
                modifier = Modifier.fillMaxWidth().height(CODE_HEIGHT_DP.dp)
                    .background(CODE_BG).padding(horizontal = 10.dp, vertical = 6.dp),
                visualTransformation = KotlinHighlightTransformation,
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = CODE_TEXT,
                    lineHeight = 16.sp,
                ),
                decorationBox = { inner ->
                    if (code.isEmpty()) {
                        BasicText("// Write Kotlin here…",
                            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = CODE_HINT))
                    }
                    inner()
                },
            )

            Box(Modifier.fillMaxWidth().height(1.dp).background(colors.border))

            listOf("result", "error").forEach { port ->
                Row(
                    modifier = Modifier.fillMaxWidth().height(PORT_ROW_DP.dp).padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                ) {
                    BasicText(port, style = TextStyle(color = colors.onSurfaceVariant, fontSize = 10.sp))
                }
            }
        }
        NodeStatusBadge(ctx.executionStatus, Modifier.align(Alignment.TopEnd).padding(4.dp), colors.surface)
    }
}

@Composable
private fun ScriptHeader(label: String, bg: Color, textColor: Color) {
    Box(
        modifier = Modifier.fillMaxWidth().height(HEADER_DP.dp).background(bg).padding(horizontal = 10.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicText(label, style = TextStyle(color = textColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold))
    }
}
