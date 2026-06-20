package com.ronjunevaldoz.graphyn.plugins.io

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.editor.canvas.NodeCanvasContext
import com.ronjunevaldoz.graphyn.editor.canvas.NodeStatusBadge
import kotlin.math.roundToInt

private val SHAPE = RoundedCornerShape(8.dp)
private val BG = Color(0xFF0A2522)
private val HEADER_COLOR = Color(0xFF34D399)
private val BORDER = Color(0xFF059669)
private val BORDER_SEL = Color(0xFF6EE7B7)
private val TEXT_HEADER = Color(0xFF064E3B)
private val TEXT_LIGHT = Color(0xFFECFDF5)
private val TEXT_MUTED = Color(0xFF6EE7B7)
private val BTN_BG = Color(0xFF065F46)

internal const val FILE_BROWSE_WIDTH = 280
internal const val FILE_BROWSE_HEIGHT = 120

@Composable
internal fun FileBrowseCard(ctx: NodeCanvasContext) = BrowseCard(ctx, "📄") {
    FilePicker.pickFile { path ->
        if (path != null) ctx.onConfigChange("path", WorkflowValue.StringValue(path))
    }
}

@Composable
internal fun FolderBrowseCard(ctx: NodeCanvasContext) = BrowseCard(ctx, "📁") {
    FilePicker.pickFolder { path ->
        if (path != null) ctx.onConfigChange("path", WorkflowValue.StringValue(path))
    }
}

@Composable
private fun BrowseCard(ctx: NodeCanvasContext, icon: String, onBrowse: () -> Unit) {
    val path = (ctx.node.config["path"] as? WorkflowValue.StringValue)?.value?.ifBlank { null }
    val border = if (ctx.selected) BORDER_SEL else BORDER
    Box(Modifier.size(FILE_BROWSE_WIDTH.dp, FILE_BROWSE_HEIGHT.dp)) {
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
                    .background(HEADER_COLOR)
                    .clickable { ctx.onSelect() }
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicText(icon, style = TextStyle(fontSize = 14.sp))
                Spacer(Modifier.width(6.dp))
                BasicText(ctx.spec.label, style = TextStyle(color = TEXT_HEADER, fontSize = 12.sp, fontWeight = FontWeight.SemiBold))
            }
            Row(
                Modifier.weight(1f).fillMaxWidth().padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val display = path?.split('/', '\\')?.filter { it.isNotBlank() }?.lastOrNull() ?: "None selected"
                BasicText(
                    display,
                    style = TextStyle(color = if (path != null) TEXT_LIGHT else TEXT_MUTED, fontSize = 10.sp),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Box(
                    Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(BTN_BG)
                        .clickable(onClick = onBrowse)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    BasicText("Browse", style = TextStyle(color = HEADER_COLOR, fontSize = 9.sp, fontWeight = FontWeight.Medium))
                }
            }
        }
        NodeStatusBadge(ctx.executionStatus, Modifier.align(Alignment.TopEnd).padding(4.dp), BG)
    }
}
