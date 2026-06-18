package com.ronjunevaldoz.graphyn.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.designsystem.theme.appTheme
import kotlin.math.roundToInt

@Composable
fun AppSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    range: ClosedFloatingPointRange<Float> = 0f..1f,
    enabled: Boolean = true,
    trackColor: Color = appTheme.colors.secondary,
    progressColor: Color = appTheme.colors.primary,
) {
    val theme = appTheme
    var trackWidth by remember { mutableStateOf(0) }
    val fraction = ((value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)
    val thumbDp = 20.dp

    Box(
        modifier = modifier.height(thumbDp).padding(horizontal = thumbDp / 2),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { trackWidth = it.width }
                .height(4.dp)
                .background(trackColor, RoundedCornerShape(2.dp))
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectTapGestures { offset ->
                        val newFraction = (offset.x / trackWidth).coerceIn(0f, 1f)
                        onValueChange(range.start + newFraction * (range.endInclusive - range.start))
                    }
                },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(4.dp)
                    .background(if (enabled) progressColor else theme.colors.primaryDisabled, RoundedCornerShape(2.dp)),
            )
        }
        Box(
            modifier = Modifier
                .offset { IntOffset(((fraction * trackWidth) - thumbDp.toPx() / 2).roundToInt(), 0) }
                .size(thumbDp)
                .background(if (enabled) theme.colors.background else theme.colors.primaryDisabled, CircleShape)
                .then(
                    if (enabled) Modifier.pointerInput(Unit) {
                        detectHorizontalDragGestures { _, dragAmount ->
                            val delta = dragAmount / trackWidth
                            val newFraction = (fraction + delta).coerceIn(0f, 1f)
                            onValueChange(range.start + newFraction * (range.endInclusive - range.start))
                        }
                    } else Modifier,
                ),
        ) {
            Box(modifier = Modifier.size(8.dp).align(Alignment.Center).background(if (enabled) progressColor else theme.colors.primaryDisabled, CircleShape))
        }
    }
}
