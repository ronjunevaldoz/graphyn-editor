package com.ronjunevaldoz.graphyn.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.designsystem.theme.appTheme

@Composable
fun AppSeparator(
    modifier: Modifier = Modifier,
    vertical: Boolean = false,
    thickness: Dp = 1.dp,
    color: Color = appTheme.colors.border,
) {
    Box(
        modifier = modifier
            .clearAndSetSemantics {}
            .then(
                if (vertical) Modifier.width(thickness).fillMaxHeight()
                else Modifier.height(thickness).fillMaxWidth(),
            )
            .background(color),
    )
}
