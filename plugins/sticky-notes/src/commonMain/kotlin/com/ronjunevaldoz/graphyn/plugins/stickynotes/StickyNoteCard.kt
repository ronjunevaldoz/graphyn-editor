package com.ronjunevaldoz.graphyn.plugins.stickynotes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
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

private val SHAPE = RoundedCornerShape(6.dp)
private val BG = Color(0xFFFFFDE7)
private val BORDER_DEFAULT = Color(0xFFFFEE58)
private val BORDER_SELECTED = Color(0xFFF57F17)
private val HEADER_BG = Color(0xFFFFF176)
private val TEXT_COLOR = Color(0xFF4E342E)
private val HINT_COLOR = Color(0xFF9E8A7A)

@Composable
internal fun StickyNoteCard(ctx: NodeCanvasContext) {
    val noteW = (ctx.node.config[STICKY_NOTE_W_KEY] as? WorkflowValue.IntValue)?.value ?: STICKY_NOTE_DEFAULT_W
    val noteH = (ctx.node.config[STICKY_NOTE_H_KEY] as? WorkflowValue.IntValue)?.value ?: STICKY_NOTE_DEFAULT_H

    val currentText = (ctx.node.config[STICKY_NOTE_TEXT_KEY]
        ?: ctx.spec.defaultValues[STICKY_NOTE_TEXT_KEY])
        .let { (it as? WorkflowValue.StringValue)?.value ?: "" }

    var editing by remember { mutableStateOf<String?>(null) }
    var focusGranted by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    fun commit() {
        val raw = editing ?: return
        editing = null
        ctx.onConfigChange(STICKY_NOTE_TEXT_KEY, WorkflowValue.StringValue(raw))
    }
    LaunchedEffect(editing) { if (editing != null) focusRequester.requestFocus() }

    val borderColor = if (ctx.selected) BORDER_SELECTED else BORDER_DEFAULT
    Box(Modifier.size(noteW.dp, noteH.dp)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(SHAPE)
                .background(BG)
                .border(1.5.dp, borderColor, SHAPE),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
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
                    .padding(horizontal = 8.dp),
            ) {
                BasicText(
                    "Note",
                    style = TextStyle(color = TEXT_COLOR, fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                )
            }
            Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                if (editing != null) {
                    BasicTextField(
                        value = editing!!,
                        onValueChange = { editing = it },
                        modifier = Modifier
                            .fillMaxSize()
                            .focusRequester(focusRequester)
                            .onFocusChanged { s ->
                                if (s.isFocused) focusGranted = true else if (focusGranted) commit()
                            },
                        textStyle = TextStyle(color = TEXT_COLOR, fontSize = 11.sp, lineHeight = 16.sp),
                    )
                } else {
                    BasicText(
                        text = currentText.ifEmpty { "Click to add note…" },
                        style = TextStyle(
                            color = if (currentText.isEmpty()) HINT_COLOR else TEXT_COLOR,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                        ),
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { focusGranted = false; editing = currentText },
                    )
                }
            }
        }
        StickyNoteResizeHandle(
            modifier = Modifier.align(Alignment.BottomEnd),
            onResize = { dw, dh ->
                val newW = (noteW + dw).coerceAtLeast(120)
                val newH = (noteH + dh).coerceAtLeast(80)
                if (newW != noteW) ctx.onConfigChange(STICKY_NOTE_W_KEY, WorkflowValue.IntValue(newW))
                if (newH != noteH) ctx.onConfigChange(STICKY_NOTE_H_KEY, WorkflowValue.IntValue(newH))
            },
        )
    }
}
