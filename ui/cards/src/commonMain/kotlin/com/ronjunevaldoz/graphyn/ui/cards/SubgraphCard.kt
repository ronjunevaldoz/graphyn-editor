package com.ronjunevaldoz.graphyn.ui.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ronjunevaldoz.graphyn.core.designsystem.theme.appTheme
import com.ronjunevaldoz.graphyn.editor.canvas.NodeCanvasContext
import com.ronjunevaldoz.graphyn.editor.canvas.NodeStatusBadge

@Composable
internal fun SubgraphCard(ctx: NodeCanvasContext) {
    val theme = LocalFieldNodeTheme.current
    val bg = theme.background()
    val borderColor = if (ctx.selected) theme.selectedBorder() else theme.border()
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(appTheme.shapes.md)
    Box(
        modifier = Modifier.width(CARD_WIDTH_DP.dp).clip(shape).background(bg).border(1.dp, borderColor, shape)
            .pointerInput(ctx.onEnterSubgraph) {
                detectTapGestures(
                    onTap = { ctx.onSelect() },
                    onDoubleTap = { ctx.onEnterSubgraph?.invoke() },
                )
            },
    ) {
        Column {
            FieldHeader(
                ctx.spec.label,
                theme,
                description = ctx.spec.description,
                onMove = ctx.onMove,
                nodeId = ctx.node.id,
            )
            SubgraphBoundaryBody(ctx.spec.inputs, ctx.spec.outputs, theme, Modifier.padding(horizontal = 10.dp))
            if (ctx.onEnterSubgraph != null) {
                Spacer(Modifier.fillMaxWidth().height(ENTER_HINT_DP.dp))
                Box(Modifier.fillMaxWidth().padding(horizontal = 10.dp), contentAlignment = Alignment.CenterEnd) {
                    BasicText("↳ Enter", style = TextStyle(color = theme.labelColor(), fontSize = 10.sp))
                }
            }
        }
        NodeStatusBadge(ctx.executionStatus, Modifier.align(Alignment.TopEnd).padding(appTheme.spacing.xs), bg)
    }
}
