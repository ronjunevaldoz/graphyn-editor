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
import java.io.File
import java.text.DecimalFormat
import kotlin.math.roundToInt

private const val CARD_WIDTH  = 220
private const val HEADER_DP   = 28
private const val PORT_ROW_DP = 22
private const val CONTENT_DP  = 100

internal object MediaOutputCardFactory : NodeCanvasFactory {
    override val nodeWidth  = CARD_WIDTH
    override val nodeHeight = HEADER_DP + PORT_ROW_DP + CONTENT_DP + PORT_ROW_DP

    override fun portAnchorY(portIndex: Int, isInput: Boolean, spec: NodeSpec): Int =
        if (isInput) HEADER_DP + PORT_ROW_DP / 2
        else HEADER_DP + PORT_ROW_DP + CONTENT_DP + PORT_ROW_DP / 2

    @Composable
    override fun NodeCanvas(context: NodeCanvasContext) = MediaOutputCard(context)
}

@Composable
internal actual fun MediaOutputCardPlatform(ctx: NodeCanvasContext) = MediaOutputCard(ctx)

@Composable
private fun MediaOutputCard(ctx: NodeCanvasContext) {
    val colors = appTheme.colors
    val shape = RoundedCornerShape(6.dp)
    val filePathValue = ctx.executionOutputs["file_path"] as? WorkflowValue.StringValue
    val filePath = filePathValue?.value ?: ""

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
            // Header
            Box(
                modifier = Modifier.fillMaxWidth().height(HEADER_DP.dp)
                    .background(colors.surfaceVariant).padding(horizontal = 10.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                BasicText(
                    "Media Output",
                    style = TextStyle(color = colors.onSurface, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                )
            }

            // Input port
            Row(
                Modifier.fillMaxWidth().height(PORT_ROW_DP.dp).padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicText("file_path", style = TextStyle(color = colors.onSurfaceVariant, fontSize = 10.sp))
            }

            // Content: File info
            Box(
                modifier = Modifier.fillMaxWidth().height(CONTENT_DP.dp)
                    .background(colors.muted).padding(8.dp),
                contentAlignment = Alignment.TopStart,
            ) {
                if (filePath.isNotEmpty()) {
                    val fileInfo = getFileInfo(filePath)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        BasicText(
                            text = File(filePath).name,
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                color = colors.onSurface,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                        BasicText(
                            text = fileInfo,
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 8.sp,
                                color = colors.onSurfaceVariant,
                                lineHeight = 11.sp,
                            ),
                        )
                        BasicText(
                            text = "📁 Open",
                            style = TextStyle(
                                fontSize = 9.sp,
                                color = colors.primary,
                                fontWeight = FontWeight.SemiBold,
                            ),
                            modifier = Modifier.clickable {
                                openFile(filePath)
                            }.padding(top = 4.dp)
                        )
                    }
                } else {
                    BasicText(
                        "No output yet",
                        style = TextStyle(color = colors.onMuted, fontSize = 10.sp)
                    )
                }
            }

            // Output port
            Row(
                Modifier.fillMaxWidth().height(PORT_ROW_DP.dp).padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                BasicText("file_path", style = TextStyle(color = colors.onSurfaceVariant, fontSize = 10.sp))
            }
        }
        NodeStatusBadge(ctx.executionStatus, Modifier.align(Alignment.TopEnd).padding(4.dp), colors.surface)
    }
}

private fun getFileInfo(filePath: String): String {
    return try {
        val file = File(filePath)
        if (!file.exists()) return "File not found"

        val sizeBytes = file.length()
        val sizeStr = formatBytes(sizeBytes)
        val lastModified = java.text.SimpleDateFormat("HH:mm:ss").format(file.lastModified())

        buildString {
            appendLine("Size: $sizeStr")
            appendLine("Modified: $lastModified")
            if (filePath.endsWith(".mp4")) {
                appendLine("Format: MP4")
            } else if (filePath.endsWith(".wav")) {
                appendLine("Format: WAV")
            }
        }.trim()
    } catch (e: Exception) {
        "Error reading file"
    }
}

private fun formatBytes(bytes: Long): String {
    val units = arrayOf("B", "KB", "MB", "GB")
    var size = bytes.toDouble()
    var unitIndex = 0

    while (size > 1024 && unitIndex < units.size - 1) {
        size /= 1024
        unitIndex++
    }

    val df = DecimalFormat("0.0")
    return "${df.format(size)} ${units[unitIndex]}"
}

private fun openFile(filePath: String) {
    try {
        val file = File(filePath)
        if (file.exists()) {
            // On macOS, use 'open' command
            Runtime.getRuntime().exec(arrayOf("open", filePath))
        }
    } catch (e: Exception) {
        // Silent fail; user can manually navigate
    }
}
