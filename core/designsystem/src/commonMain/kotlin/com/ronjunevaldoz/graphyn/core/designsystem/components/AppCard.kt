package com.ronjunevaldoz.graphyn.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.designsystem.theme.appTheme

enum class AppCardVariant { Default, Filled }

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    variant: AppCardVariant = AppCardVariant.Default,
    header: (@Composable () -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val theme = appTheme
    val colors = theme.colors
    val spacing = theme.spacing
    val shape = RoundedCornerShape(theme.shapes.xxl)

    val bgColor = when (variant) {
        AppCardVariant.Default -> colors.surface
        AppCardVariant.Filled  -> colors.surfaceVariant
    }

    Column(
        modifier = modifier
            .clip(shape)
            .background(bgColor)
            .border(1.dp, colors.border, shape)
            .padding(spacing.lg),
    ) {
        if (header != null) {
            header()
            Spacer(Modifier.height(spacing.sm))
        }
        content()
        if (footer != null) {
            Spacer(Modifier.height(spacing.sm))
            footer()
        }
    }
}

@Composable
fun AppCardHeader(
    title: String,
    description: String? = null,
    action: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            AppText(title, style = AppTextStyle.TitleSmall)
            if (description != null) {
                Spacer(Modifier.height(4.dp))
                AppText(description, style = AppTextStyle.BodySmall, muted = true)
            }
        }
        action?.invoke()
    }
}
