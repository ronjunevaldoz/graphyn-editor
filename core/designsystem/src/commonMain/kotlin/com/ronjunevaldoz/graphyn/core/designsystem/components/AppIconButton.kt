package com.ronjunevaldoz.graphyn.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.designsystem.theme.appTheme

@Composable
fun AppIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: AppButtonVariant = AppButtonVariant.Ghost,
    content: @Composable () -> Unit,
) {
    val theme = appTheme
    val colors = theme.colors
    val bgColor = when (variant) {
        AppButtonVariant.Default     -> colors.primary
        AppButtonVariant.Secondary   -> colors.secondary
        AppButtonVariant.Destructive -> colors.destructive
        AppButtonVariant.Ghost,
        AppButtonVariant.Outline     -> androidx.compose.ui.graphics.Color.Transparent
    }
    val alpha = if (enabled) 1f else 0.38f
    val interactionSource = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(theme.shapes.md)

    Box(
        modifier = modifier
            .size(40.dp)
            .clip(shape)
            .background(bgColor.copy(alpha = bgColor.alpha * alpha))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
