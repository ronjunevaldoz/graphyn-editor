package com.ronjunevaldoz.graphyn.core.designsystem.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.designsystem.theme.appTheme

@Composable
fun AppProgress(
    progress: Float?,
    modifier: Modifier = Modifier,
    height: Dp = 4.dp,
    color: Color = appTheme.colors.primary,
    trackColor: Color = appTheme.colors.secondary,
) {
    if (progress == null) {
        val infiniteTransition = rememberInfiniteTransition(label = "progress")
        val offsetFraction by infiniteTransition.animateFloat(
            initialValue = -0.5f,
            targetValue = 1.5f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1200),
                repeatMode = RepeatMode.Restart,
            ),
            label = "progressOffset",
        )
        var containerWidth by remember { mutableStateOf(0) }
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(height / 2))
                .background(trackColor)
                .onSizeChanged { containerWidth = it.width },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(height)
                    .clip(RoundedCornerShape(height / 2))
                    .background(color)
                    .graphicsLayer { translationX = containerWidth * offsetFraction },
            )
        }
    } else {
        val animatedProgress by animateFloatAsState(
            targetValue = progress.coerceIn(0f, 1f),
            animationSpec = tween(durationMillis = 300),
            label = "progressValue",
        )
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(height / 2))
                .background(trackColor),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .height(height)
                    .clip(RoundedCornerShape(height / 2))
                    .background(color),
            )
        }
    }
}
