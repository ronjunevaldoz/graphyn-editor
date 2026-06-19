package com.ronjunevaldoz.graphyn.bootstrap

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.ronjunevaldoz.graphyn.editor.canvas.NodeStatusBadge
import kotlin.math.roundToInt

private val SHAPE = RoundedCornerShape(8.dp)
private val BG = Color(0xFF1E1B3A)
private val HEADER_BG = Color(0xFF7C3AED)
private val FOOTER_BG = Color(0xFF2D2550)
private val BORDER_DEFAULT = Color(0xFF5B21B6)
private val BORDER_SELECTED = Color(0xFFA78BFA)
private val TEXT_LIGHT = Color(0xFFEDE9FE)
private val TEXT_MUTED = Color(0xFFA78BFA)

@Composable
internal fun SubgraphCard(ctx: NodeCanvasContext) {
    val subgraph = ctx.node.subgraph
    val label = subgraph?.name ?: ctx.spec.label
    val innerNodes = subgraph?.nodes.orEmpty()
    val contents = innerNodes.map { it.type.substringAfterLast('.') }
    val count = innerNodes.size
    val border = if (ctx.selected) BORDER_SELECTED else BORDER_DEFAULT

    Box(Modifier.size(280.dp, 160.dp)) {
        Column(
            Modifier
                .fillMaxSize()
                .clip(SHAPE)
                .background(BG)
                .border(1.5.dp, border, SHAPE)
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
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .background(HEADER_BG)
                    .clickable { ctx.onSelect() }
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicText("⊞", style = TextStyle(color = TEXT_LIGHT, fontSize = 14.sp))
                Spacer(Modifier.width(6.dp))
                BasicText(label, style = TextStyle(color = TEXT_LIGHT, fontSize = 12.sp, fontWeight = FontWeight.SemiBold))
            }
            Box(Modifier.weight(1f).fillMaxWidth().padding(10.dp)) {
                if (contents.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        contents.take(3).forEach { name ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(5.dp).clip(CircleShape).background(HEADER_BG))
                                Spacer(Modifier.width(5.dp))
                                BasicText(name, style = TextStyle(color = TEXT_MUTED, fontSize = 10.sp))
                            }
                        }
                        if (contents.size > 3) {
                            BasicText("+${contents.size - 3} more", style = TextStyle(color = TEXT_MUTED, fontSize = 9.sp))
                        }
                    }
                } else {
                    BasicText("Nested workflow", style = TextStyle(color = TEXT_MUTED, fontSize = 10.sp))
                }
                if (count > 0) {
                    Box(
                        Modifier.align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(HEADER_BG)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        BasicText("$count nodes", style = TextStyle(color = TEXT_LIGHT, fontSize = 9.sp, fontWeight = FontWeight.Medium))
                    }
                }
            }
            val enterSubgraph = ctx.onEnterSubgraph
            if (enterSubgraph != null) {
                val interaction = remember { MutableInteractionSource() }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(FOOTER_BG)
                        .clickable(interactionSource = interaction, indication = null, onClick = enterSubgraph)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                ) {
                    BasicText("↳ Enter", style = TextStyle(color = TEXT_MUTED, fontSize = 10.sp, fontWeight = FontWeight.Medium))
                }
            }
        }
        NodeStatusBadge(
            status = ctx.executionStatus,
            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
            surfaceColor = BG,
        )
    }
}
