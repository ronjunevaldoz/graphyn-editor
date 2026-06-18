package com.ronjunevaldoz.graphyn.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.designsystem.theme.appTheme

enum class AppChipVariant { Default, Selected, Outline }

@Composable
fun AppChip(
    label: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    variant: AppChipVariant = if (selected) AppChipVariant.Selected else AppChipVariant.Default,
) {
    val theme = appTheme
    val colors = theme.colors
    val spacing = theme.spacing
    val shape = RoundedCornerShape(theme.shapes.full)

    val (bgColor, contentColor, borderColor) = when (variant) {
        AppChipVariant.Default  -> Triple(colors.secondary, colors.onSecondary, colors.border)
        AppChipVariant.Selected -> Triple(colors.primary, colors.onPrimary, Color.Transparent)
        AppChipVariant.Outline  -> Triple(Color.Transparent, colors.onSurface, colors.border)
    }

    val interactionSource = remember { MutableInteractionSource() }
    val clickMod = if (onClick != null && enabled) {
        Modifier.clickable(interactionSource = interactionSource, indication = null, role = Role.Button, onClick = onClick)
    } else Modifier

    Row(
        modifier = modifier
            .clip(shape)
            .background(bgColor)
            .border(1.dp, borderColor, shape)
            .then(clickMod)
            .padding(horizontal = spacing.md, vertical = spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppText(text = label, color = contentColor, style = AppTextStyle.LabelSmall)
    }
}
