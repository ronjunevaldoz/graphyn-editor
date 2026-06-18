package com.ronjunevaldoz.graphyn.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ronjunevaldoz.graphyn.core.designsystem.theme.appTheme

@Composable
fun AppSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable () -> Unit,
) {
    val theme = appTheme
    val sheetShape = RoundedCornerShape(topStart = theme.shapes.xxl, topEnd = theme.shapes.xxl)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss,
                    ),
            )
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .background(theme.colors.surface, sheetShape)
                    .padding(top = 12.dp, bottom = 24.dp)
                    .align(Alignment.BottomCenter),
            ) {
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .height(4.dp)
                        .background(theme.colors.border, RoundedCornerShape(2.dp))
                        .align(Alignment.CenterHorizontally),
                )
                Spacer(Modifier.height(16.dp))
                if (title != null) {
                    AppText(text = title, style = AppTextStyle.TitleSmall, modifier = Modifier.padding(horizontal = 24.dp))
                    Spacer(Modifier.height(16.dp))
                }
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    content()
                }
            }
        }
    }
}
