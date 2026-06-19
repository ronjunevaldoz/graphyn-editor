package com.ronjunevaldoz.graphyn.plugins.stickynotes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

private val GRIP_COLOR = Color(0xFFF57F17).copy(alpha = 0.55f)

/** SE-corner drag handle that calls [onResize] with pixel deltas for each pointer event. */
@Composable
internal fun StickyNoteResizeHandle(
    modifier: Modifier = Modifier,
    onResize: (dw: Int, dh: Int) -> Unit,
) {
    Canvas(
        modifier = modifier
            .size(14.dp)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    drag(down.id) { change ->
                        change.consume()
                        val d = change.position - change.previousPosition
                        val dw = d.x.roundToInt()
                        val dh = d.y.roundToInt()
                        if (dw != 0 || dh != 0) onResize(dw, dh)
                    }
                }
            },
    ) {
        val s = size.width
        listOf(
            Offset(s * 0.35f, s), Offset(s, s * 0.35f),
            Offset(s * 0.65f, s), Offset(s, s * 0.65f),
            Offset(s, s),
        ).forEach { drawCircle(GRIP_COLOR, radius = 1.5f, center = it) }
    }
}
