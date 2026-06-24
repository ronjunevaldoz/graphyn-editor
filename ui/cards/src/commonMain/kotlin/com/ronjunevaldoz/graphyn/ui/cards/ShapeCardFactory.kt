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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
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
import com.ronjunevaldoz.graphyn.core.designsystem.theme.appTheme
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.editor.canvas.NodeCanvasContext
import com.ronjunevaldoz.graphyn.editor.canvas.NodeCanvasFactory
import com.ronjunevaldoz.graphyn.editor.canvas.NodeShape
import com.ronjunevaldoz.graphyn.editor.canvas.NodeStatusBadge
import kotlin.math.max
import kotlin.math.roundToInt

private const val LABEL_HEIGHT_DP = 18

/**
 * Compact shape card — circle by default — for trigger/sink nodes.
 *
 * Set [inlineInputRows] > 0 to show inline config widgets below the shape (ComfyUI/Blender
 * style). Only the first [inlineInputRows] input ports are rendered; ports with no default
 * value show a read-only label. The card width expands to [CARD_WIDTH_DP] to fit widget rows.
 *
 * Port anchors always sit at the vertical centre of the shape, regardless of inline widget rows.
 */
class ShapeCardFactory(
    val shape: Shape = CircleShape,
    val size: Dp = 64.dp,
    val theme: ShapeNodeTheme = ShapeNodeTheme(),
    val minimapShape: NodeShape = NodeShape.Circle,
    val avatar: (@Composable (node: NodeRef, spec: NodeSpec) -> Unit)? = null,
    val inlineInputRows: Int = 0,
    val widgetTheme: FieldNodeTheme = FieldNodeTheme(),
) : NodeCanvasFactory {
    private val sizeInt = size.value.toInt()
    override val nodeWidth = if (inlineInputRows > 0) max(sizeInt, CARD_WIDTH_DP) else sizeInt
    override val nodeHeight = sizeInt + LABEL_HEIGHT_DP + inlineInputRows * ROW_DP
    override val nodeShape = minimapShape

    override fun portAnchorY(portIndex: Int, isInput: Boolean, spec: NodeSpec): Int = sizeInt / 2

    @Composable
    override fun NodeCanvas(context: NodeCanvasContext) {
        ShapeCard(context, shape, size, theme, avatar, inlineInputRows, widgetTheme)
    }
}

@Composable
private fun ShapeCard(
    ctx: NodeCanvasContext,
    shape: Shape,
    size: Dp,
    theme: ShapeNodeTheme,
    avatar: (@Composable (NodeRef, NodeSpec) -> Unit)?,
    inlineInputRows: Int,
    widgetTheme: FieldNodeTheme,
) {
    val bg = theme.background()
    val borderColor = if (ctx.selected) theme.selectedBorder() else bg
    val hasWidgets = inlineInputRows > 0
    // The card's layout width must equal the shape's own width so the port anchors
    // (input at x=0, output at x=nodeWidth) line up with the shape's edges. A label wider
    // than the shape must overflow without widening the card (see label modifier below).
    val cardWidth = if (hasWidgets) max(size.value.toInt(), CARD_WIDTH_DP).dp else size

    Box(modifier = Modifier.width(cardWidth)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
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
                if (avatar != null) avatar(ctx.node, ctx.spec)
                else BasicText(
                    ctx.spec.label.take(1).uppercase(),
                    style = TextStyle(color = theme.iconColor(), fontSize = 22.sp, fontWeight = FontWeight.Bold),
                )
            }
            BasicText(
                ctx.spec.label,
                modifier = if (hasWidgets) Modifier
                else Modifier.wrapContentWidth(align = Alignment.CenterHorizontally, unbounded = true),
                style = appTheme.typography.nodeLabel.copy(color = theme.labelColor(), fontWeight = FontWeight.Medium),
            )
            if (hasWidgets) {
                val inputs = ctx.spec.inputs.take(inlineInputRows)
                val values = ctx.spec.defaultValues + ctx.node.config
                FieldBody(
                    inputs = inputs,
                    values = values,
                    onValueChange = { key, value -> ctx.onConfigChange(key, value) },
                    theme = widgetTheme,
                )
            }
        }
        NodeStatusBadge(status = ctx.executionStatus, surfaceColor = bg, modifier = Modifier.align(Alignment.TopEnd))
    }
}
