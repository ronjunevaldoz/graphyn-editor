package com.ronjunevaldoz.graphyn.core.designsystem.components

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import com.ronjunevaldoz.graphyn.core.designsystem.theme.appTheme

enum class AppTextStyle {
    DisplayLarge, DisplayMedium,
    TitleLarge, TitleMedium, TitleSmall,
    BodyLarge, BodyMedium, BodySmall,
    LabelLarge, LabelSmall,
    Mono,
}

@Composable
fun AppText(
    text: String,
    modifier: Modifier = Modifier,
    style: AppTextStyle = AppTextStyle.BodyMedium,
    muted: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    color: Color = Color.Unspecified,
) {
    val theme = appTheme
    val resolvedStyle = when (style) {
        AppTextStyle.DisplayLarge  -> theme.typography.displayLarge
        AppTextStyle.DisplayMedium -> theme.typography.displayMedium
        AppTextStyle.TitleLarge    -> theme.typography.titleLarge
        AppTextStyle.TitleMedium   -> theme.typography.titleMedium
        AppTextStyle.TitleSmall    -> theme.typography.titleSmall
        AppTextStyle.BodyLarge     -> theme.typography.bodyLarge
        AppTextStyle.BodyMedium    -> theme.typography.bodyMedium
        AppTextStyle.BodySmall     -> theme.typography.bodySmall
        AppTextStyle.LabelLarge    -> theme.typography.labelLarge
        AppTextStyle.LabelSmall    -> theme.typography.labelSmall
        AppTextStyle.Mono          -> theme.typography.mono
    }
    val textColor = when {
        color != Color.Unspecified -> color
        muted                      -> theme.colors.onSurfaceVariant
        else                       -> theme.colors.onSurface
    }
    BasicText(
        text = text,
        modifier = modifier,
        style = resolvedStyle.copy(color = textColor),
        maxLines = maxLines,
        overflow = overflow,
    )
}
