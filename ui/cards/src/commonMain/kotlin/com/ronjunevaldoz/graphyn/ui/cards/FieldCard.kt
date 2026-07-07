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
import androidx.compose.foundation.shape.RoundedCornerShape
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
internal fun FieldCard(ctx: NodeCanvasContext, hasEnterHint: Boolean = false) {
    val theme = LocalFieldNodeTheme.current
    val bg = theme.background()
    val borderColor = if (ctx.selected) theme.selectedBorder() else theme.border()
    val shape = RoundedCornerShape(appTheme.shapes.md)
    Box(
        modifier = Modifier
            .width(CARD_WIDTH_DP.dp)
            .clip(shape)
            .background(bg)
            .border(1.dp, borderColor, shape)
            // Double-tap enters the node's subgraph when it has one (e.g. a collapsed subgraph
            // node) — a plain node with no subgraph has ctx.onEnterSubgraph == null, so this is a
            // no-op double-tap for every other card.
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
                nodeId = ctx.node.id
            )
            FieldBody(
                inputs = ctx.spec.inputs,
                values = ctx.spec.defaultValues + ctx.node.config,
                onValueChange = { key, value -> ctx.onConfigChange(key, value) },
                theme = theme,
                modifier = Modifier.padding(horizontal = 10.dp)
            )

            if(ctx.spec.outputs.isNotEmpty()) {
                Spacer(
                    Modifier.fillMaxWidth()
                        .height(FOOTER_DIVIDER_DP.dp)
                        .background(theme.divider())
                )
            }

            FieldFooter(
                outputs = ctx.spec.outputs,
                theme = theme,
                modifier = Modifier.padding(horizontal = 10.dp)
            )

            // A real row, not an overlay — otherwise it paints over the last port row's text.
            // Only rendered when the factory reserved height for it (see FieldCardFactory.hasEnterHint).
            if (hasEnterHint && ctx.onEnterSubgraph != null) {
                FieldCardEnterHint(theme)
            }
        }
        NodeStatusBadge(ctx.executionStatus, Modifier.align(Alignment.TopEnd).padding(appTheme.spacing.xs), bg)
    }
}

/**
 * "↳ Enter" hint row surfaced whenever the node has a subgraph to drill into
 * ([NodeCanvasContext.onEnterSubgraph] is non-null) — the double-tap gesture on [FieldCardFactory]
 * itself is otherwise undiscoverable.
 */
@Composable
private fun FieldCardEnterHint(theme: FieldNodeTheme) {
    Box(Modifier.fillMaxWidth().height(ENTER_HINT_DP.dp), contentAlignment = Alignment.CenterEnd) {
        BasicText(
            "↳ Enter",
            modifier = Modifier.padding(horizontal = 10.dp),
            style = TextStyle(color = theme.labelColor(), fontSize = 10.sp),
        )
    }
}
