package com.ronjunevaldoz.graphyn.core.designsystem.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.designsystem.theme.appTheme

sealed interface IconSize {
    val dp: Dp
    data object Xs : IconSize { override val dp = 12.dp }
    data object Sm : IconSize { override val dp = 16.dp }
    data object Md : IconSize { override val dp = 20.dp }
    data object Lg : IconSize { override val dp = 24.dp }
    data object Xl : IconSize { override val dp = 32.dp }
}

@Composable
fun AppIcon(
    imageVector: ImageVector,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    size: IconSize = IconSize.Md,
    tint: Color = Color.Unspecified,
) {
    AppIcon(
        painter = rememberVectorPainter(imageVector),
        contentDescription = contentDescription,
        modifier = modifier,
        size = size,
        tint = tint,
    )
}

@Composable
fun AppIcon(
    painter: Painter,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    size: IconSize = IconSize.Md,
    tint: Color = Color.Unspecified,
) {
    val resolvedTint = if (tint == Color.Unspecified) appTheme.colors.onSurface else tint
    Image(
        painter = painter,
        contentDescription = contentDescription,
        modifier = modifier.size(size.dp),
        colorFilter = ColorFilter.tint(resolvedTint),
    )
}
