package com.ronjunevaldoz.graphyn.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ronjunevaldoz.graphyn.core.designsystem.theme.appTheme

@Composable
fun AppDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    description: String? = null,
    confirmButton: (@Composable () -> Unit)? = null,
    dismissButton: (@Composable () -> Unit)? = null,
    properties: DialogProperties = DialogProperties(),
    content: (@Composable () -> Unit)? = null,
) {
    val theme = appTheme
    val shape = RoundedCornerShape(theme.shapes.xxl)
    Dialog(onDismissRequest = onDismiss, properties = properties) {
        Column(
            modifier = modifier
                .widthIn(min = 280.dp, max = 480.dp)
                .shadow(16.dp, shape)
                .background(theme.colors.surface, shape)
                .padding(24.dp),
        ) {
            if (title != null) {
                AppText(text = title, style = AppTextStyle.TitleMedium)
            }
            if (description != null) {
                Spacer(Modifier.height(8.dp))
                AppText(text = description, style = AppTextStyle.BodyMedium, muted = true)
            }
            if (content != null) {
                Spacer(Modifier.height(16.dp))
                content()
            }
            if (confirmButton != null || dismissButton != null) {
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                ) {
                    dismissButton?.invoke()
                    confirmButton?.invoke()
                }
            }
        }
    }
}

@Composable
fun AppAlertDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    title: String,
    description: String,
    confirmText: String = "Continue",
    dismissText: String = "Cancel",
) {
    AppDialog(
        onDismiss = onDismiss,
        title = title,
        description = description,
        confirmButton = {
            AppButton(onClick = onConfirm, variant = AppButtonVariant.Destructive) {
                AppText(confirmText)
            }
        },
        dismissButton = {
            AppButton(onClick = onDismiss, variant = AppButtonVariant.Ghost) {
                AppText(dismissText)
            }
        },
    )
}
