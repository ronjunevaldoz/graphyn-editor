package com.ronjunevaldoz.graphyn.core.designsystem.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.designsystem.theme.appTheme

sealed interface SpinnerSize {
    val dp: Dp
    val stroke: Float
    data object Sm : SpinnerSize { override val dp = 16.dp; override val stroke = 2f }
    data object Md : SpinnerSize { override val dp = 24.dp; override val stroke = 2.5f }
    data object Lg : SpinnerSize { override val dp = 32.dp; override val stroke = 3f }
}

@Composable
fun AppSpinner(
    modifier: Modifier = Modifier,
    size: SpinnerSize = SpinnerSize.Md,
    color: Color = appTheme.colors.primary,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "spinner")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "spinnerRotation",
    )

    Canvas(modifier = modifier.size(size.dp)) {
        drawArc(
            color = color.copy(alpha = 0.2f),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = size.stroke, cap = StrokeCap.Round),
        )
        drawArc(
            color = color,
            startAngle = rotation,
            sweepAngle = 270f,
            useCenter = false,
            style = Stroke(width = size.stroke, cap = StrokeCap.Round),
        )
    }
}
