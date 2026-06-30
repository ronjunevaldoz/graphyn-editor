package com.ronjunevaldoz.graphyn.plugins.preview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ronjunevaldoz.graphyn.core.designsystem.theme.appTheme
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

@Composable
internal actual fun PreviewContentArea(value: WorkflowValue?) {
    val colors = appTheme.colors
    Box(modifier = Modifier.fillMaxSize().padding(8.dp), contentAlignment = Alignment.TopStart) {
        BasicText(
            text = value.display(),
            style = TextStyle(
                fontFamily = if (value != null && value !is WorkflowValue.NullValue) FontFamily.Monospace else FontFamily.Default,
                fontSize = 10.sp,
                color = if (value == null || value is WorkflowValue.NullValue) colors.onMuted else colors.onSurface,
                lineHeight = 14.sp,
            ),
        )
    }
}
