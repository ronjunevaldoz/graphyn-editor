package com.ronjunevaldoz.graphyn.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.designsystem.theme.appTheme

@Composable
fun AppTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: (@Composable () -> Unit)? = null,
    backgroundColor: Color = appTheme.colors.background,
    contentColor: Color = appTheme.colors.onSurface,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .height(56.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (navigationIcon != null) {
            navigationIcon()
        } else {
            Spacer(Modifier.padding(start = 12.dp))
        }
        AppText(
            text = title,
            style = AppTextStyle.TitleSmall,
            color = contentColor,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
        )
        actions?.invoke()
    }
}
