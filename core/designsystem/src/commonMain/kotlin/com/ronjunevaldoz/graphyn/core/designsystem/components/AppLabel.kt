package com.ronjunevaldoz.graphyn.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.designsystem.theme.appTheme

@Composable
fun AppLabel(
    text: String,
    modifier: Modifier = Modifier,
    required: Boolean = false,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        AppText(text = text, style = AppTextStyle.LabelLarge, muted = !enabled)
        if (required) {
            AppText(text = "*", style = AppTextStyle.LabelLarge, color = appTheme.colors.destructive)
        }
    }
}
