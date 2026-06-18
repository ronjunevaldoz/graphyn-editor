package com.ronjunevaldoz.graphyn.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.designsystem.theme.appTheme

enum class AppAlertVariant { Default, Destructive, Warning, Success }

@Composable
fun AppAlert(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    icon: Painter? = null,
    variant: AppAlertVariant = AppAlertVariant.Default,
) {
    val theme = appTheme
    val shape = RoundedCornerShape(theme.shapes.lg)

    val (bg, border, content) = when (variant) {
        AppAlertVariant.Default     -> Triple(theme.colors.surface, theme.colors.border, theme.colors.onSurface)
        AppAlertVariant.Destructive -> Triple(theme.colors.destructive.copy(alpha = 0.12f), theme.colors.destructive, theme.colors.onSurface)
        AppAlertVariant.Warning     -> Triple(theme.colors.warning.copy(alpha = 0.12f), theme.colors.warning, theme.colors.onSurface)
        AppAlertVariant.Success     -> Triple(theme.colors.success.copy(alpha = 0.12f), theme.colors.success, theme.colors.onSurface)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(bg)
            .border(1.dp, border, shape)
            .padding(theme.spacing.lg),
        verticalAlignment = if (description != null) Alignment.Top else Alignment.CenterVertically,
    ) {
        if (icon != null) {
            AppIcon(painter = icon, contentDescription = null, size = IconSize.Md, tint = content)
            Spacer(Modifier.width(12.dp))
        }
        Column {
            AppText(text = title, style = AppTextStyle.TitleSmall, color = content)
            if (description != null) {
                Spacer(Modifier.height(4.dp))
                AppText(text = description, style = AppTextStyle.BodySmall, color = content.copy(alpha = 0.8f))
            }
        }
    }
}
