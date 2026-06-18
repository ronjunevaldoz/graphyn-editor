package com.ronjunevaldoz.graphyn.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.designsystem.theme.appTheme

enum class AppBadgeVariant { Default, Secondary, Destructive, Outline, Ghost }

@Composable
fun AppBadge(
    modifier: Modifier = Modifier,
    variant: AppBadgeVariant = AppBadgeVariant.Default,
    content: @Composable () -> Unit,
) {
    val theme = appTheme
    val colors = theme.colors
    val spacing = theme.spacing
    val shapes = theme.shapes
    val shape = RoundedCornerShape(shapes.full)

    val (bgColor, borderColor) = when (variant) {
        AppBadgeVariant.Default     -> Pair(colors.primary, Color.Transparent)
        AppBadgeVariant.Secondary   -> Pair(colors.secondary, Color.Transparent)
        AppBadgeVariant.Destructive -> Pair(colors.destructive, Color.Transparent)
        AppBadgeVariant.Outline     -> Pair(Color.Transparent, colors.border)
        AppBadgeVariant.Ghost       -> Pair(colors.muted, Color.Transparent)
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(bgColor)
            .border(1.dp, borderColor, shape)
            .padding(horizontal = spacing.sm, vertical = spacing.xxs),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
