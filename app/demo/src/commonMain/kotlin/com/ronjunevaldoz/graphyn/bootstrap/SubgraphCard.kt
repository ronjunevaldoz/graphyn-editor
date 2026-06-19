package com.ronjunevaldoz.graphyn.bootstrap

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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.editor.canvas.NodeCanvasContext
import kotlin.math.roundToInt

private val SHAPE = RoundedCornerShape(8.dp)
private val BG = Color(0xFF1E1B3A)
private val HEADER_BG = Color(0xFF7C3AED)
private val BORDER_DEFAULT = Color(0xFF5B21B6)
private val BORDER_SELECTED = Color(0xFFA78BFA)
private val TEXT_LIGHT = Color(0xFFEDE9FE)
private val TEXT_MUTED = Color(0xFFA78BFA)

@Composable
internal fun SubgraphCard(ctx: NodeCanvasContext) {
    val label = (ctx.node.config[SUBGRAPH_LABEL_KEY] as? WorkflowValue.StringValue)?.value
        ?: ctx.spec.label
    val contents = (ctx.node.config[SUBGRAPH_CONTENTS_KEY] as? WorkflowValue.StringValue)?.value
        ?.split(",")?.map { it.trim() }.orEmpty()
    val count = contents.size.takeIf { it > 0 }
        ?: (ctx.node.config[SUBGRAPH_COUNT_KEY] as? WorkflowValue.IntValue)?.value ?: 0
    val border = if (ctx.selected) BORDER_SELECTED else BORDER_DEFAULT

    Column(
        Modifier
            .size(280.dp, 160.dp)
            .clip(SHAPE)
            .background(BG)
            .border(1.5.dp, border, SHAPE),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(36.dp)
                .background(HEADER_BG)
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
                }
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
    }
}
