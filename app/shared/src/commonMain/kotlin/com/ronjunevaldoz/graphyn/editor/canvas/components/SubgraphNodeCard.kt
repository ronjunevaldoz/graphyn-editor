package com.ronjunevaldoz.graphyn.editor.canvas.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.editor.canvas.NodeCanvasContext
import com.ronjunevaldoz.graphyn.editor.canvas.NodeCanvasFactory
import com.ronjunevaldoz.graphyn.editor.design.GraphynDs
import kotlin.math.roundToInt

private const val WIDTH = 220
private const val HEADER = 42
private const val ROW = 22
private const val DIVIDER = 9

/**
 * Built-in card for editor-created subgraph nodes (`GRAPHYN_SUBGRAPH_TYPE`). Renders the derived
 * boundary ports as labelled rows, shows the nested node count, and opens the subgraph on
 * double-click via [NodeCanvasContext.onEnterSubgraph]. Used as a fallback when the host has not
 * registered a card for the type, so collapsed subgraphs render consistently in any editor.
 *
 * Constructed per node with the boundary port counts so [nodeHeight] matches the rendered card —
 * the canvas relies on it for hit-testing, minimap, and auto-layout sizing.
 */
class SubgraphNodeCardFactory(
    private val inputRows: Int,
    private val outputRows: Int,
) : NodeCanvasFactory {
    override val nodeWidth = WIDTH
    override val nodeHeight = HEADER + inputRows * ROW + DIVIDER + outputRows * ROW

    override fun portAnchorY(portIndex: Int, isInput: Boolean, spec: NodeSpec): Int =
        if (isInput) HEADER + portIndex * ROW + ROW / 2
        else HEADER + spec.inputs.size * ROW + DIVIDER + portIndex * ROW + ROW / 2

    @Composable
    override fun NodeCanvas(context: NodeCanvasContext) = SubgraphNodeCard(context)
}

@Composable
private fun SubgraphNodeCard(ctx: NodeCanvasContext) {
    val colors = GraphynDs.colors
    val type = GraphynDs.type
    val shape = RoundedCornerShape(8.dp)
    val border = if (ctx.selected) colors.selectionRing else colors.accent
    val nested = ctx.node.subgraph?.nodes?.size ?: 0

    Box(
        modifier = Modifier
            .testTag("subgraph-node-card-${ctx.node.id}")
            .width(WIDTH.dp)
            .clip(shape)
            .background(colors.surfaceCard)
            .border(if (ctx.selected) 2.dp else 1.dp, border, shape)
            .pointerInput(ctx.onEnterSubgraph) {
                detectTapGestures(
                    onTap = { ctx.onSelect() },
                    onDoubleTap = { ctx.onEnterSubgraph?.invoke() },
                )
            },
    ) {
        Column(Modifier.fillMaxWidth()) {
            SubgraphHeader(ctx.spec.label, nested, ctx.onMove)
            ctx.spec.inputs.forEach { PortLabelRow(it.name, isInput = true) }
            Box(Modifier.fillMaxWidth().height(DIVIDER.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(colors.border))
            }
            ctx.spec.outputs.forEach { PortLabelRow(it.name, isInput = false) }
        }
        GraphynNodeStatusBadge(ctx.executionStatus, Modifier.align(Alignment.TopEnd).padding(4.dp))
    }
}

@Composable
private fun SubgraphHeader(label: String, nestedCount: Int, onMove: (IntOffset) -> Unit) {
    val colors = GraphynDs.colors
    val type = GraphynDs.type
    Column(
        modifier = Modifier.fillMaxWidth().height(HEADER.dp)
            .background(colors.accent.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    awaitTouchSlopOrCancellation(down.id) { c, _ -> c.consume() } ?: return@awaitEachGesture
                    drag(down.id) { c ->
                        c.consume()
                        val d = c.position - c.previousPosition
                        onMove(IntOffset(d.x.roundToInt(), d.y.roundToInt()))
                    }
                }
            },
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        BasicText("◰ $label", style = type.nodeTitle.copy(color = colors.textPrimary))
        BasicText("$nestedCount nodes · double-click to open", style = type.mono.copy(color = colors.textSecondary))
    }
}

@Composable
private fun PortLabelRow(name: String, isInput: Boolean) {
    val colors = GraphynDs.colors
    val type = GraphynDs.type
    BasicText(
        name,
        modifier = Modifier.fillMaxWidth().height(ROW.dp).padding(horizontal = 12.dp, vertical = 3.dp),
        style = type.bodySmall.copy(color = colors.textSecondary, textAlign = if (isInput) TextAlign.Start else TextAlign.End),
    )
}
