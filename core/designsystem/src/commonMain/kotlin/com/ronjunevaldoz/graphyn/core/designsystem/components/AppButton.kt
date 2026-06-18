package com.ronjunevaldoz.graphyn.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.designsystem.theme.appTheme

enum class AppButtonVariant { Default, Outline, Secondary, Ghost, Destructive }
enum class AppButtonSize { Xs, Sm, Md, Lg }

@Composable
fun AppButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: AppButtonVariant = AppButtonVariant.Default,
    size: AppButtonSize = AppButtonSize.Md,
    content: @Composable () -> Unit,
) {
    val theme = appTheme
    val colors = theme.colors
    val shapes = theme.shapes
    val spacing = theme.spacing

    val (bgColor, contentColor, borderColor) = when (variant) {
        AppButtonVariant.Default     -> Triple(colors.primary, colors.onPrimary, Color.Transparent)
        AppButtonVariant.Outline     -> Triple(Color.Transparent, colors.onSurface, colors.border)
        AppButtonVariant.Secondary   -> Triple(colors.secondary, colors.onSecondary, Color.Transparent)
        AppButtonVariant.Ghost       -> Triple(Color.Transparent, colors.onSurface, Color.Transparent)
        AppButtonVariant.Destructive -> Triple(colors.destructive, colors.onDestructive, Color.Transparent)
    }

    val (hPad, vPad, minHeight) = when (size) {
        AppButtonSize.Xs -> Triple(spacing.sm, spacing.xs, 28.dp)
        AppButtonSize.Sm -> Triple(spacing.md, spacing.xs, 32.dp)
        AppButtonSize.Md -> Triple(spacing.lg, spacing.sm, 40.dp)
        AppButtonSize.Lg -> Triple(spacing.xl, spacing.md, 48.dp)
    }

    val alpha = if (enabled) 1f else 0.38f
    val shape = RoundedCornerShape(shapes.md)
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = minHeight)
            .clip(shape)
            .background(bgColor.copy(alpha = bgColor.alpha * alpha))
            .border(1.dp, if (borderColor == Color.Transparent) Color.Transparent else borderColor, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = hPad, vertical = vPad),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            content()
        }
    }
}
