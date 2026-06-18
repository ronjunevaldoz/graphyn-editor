package com.ronjunevaldoz.graphyn.core.designsystem.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.designsystem.theme.appTheme
import kotlinx.coroutines.delay

@Composable
fun ToastHost(
    toastHostState: ToastHostState = LocalToastHostState.current,
    modifier: Modifier = Modifier,
) {
    val theme = appTheme
    Box(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            toastHostState.toasts.takeLast(3).forEach { toast ->
                var visible by remember(toast.id) { mutableStateOf(true) }
                LaunchedEffect(toast.id) {
                    delay(toast.durationMs)
                    visible = false
                    delay(300)
                    toastHostState.dismiss(toast.id)
                }
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(200)) + slideInVertically(tween(200)) { it },
                    exit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { it },
                ) {
                    val shape = RoundedCornerShape(theme.shapes.lg)
                    val (bg, border, content) = when (toast.variant) {
                        ToastVariant.Default     -> Triple(theme.colors.surface, theme.colors.border, theme.colors.onSurface)
                        ToastVariant.Destructive -> Triple(theme.colors.destructive, theme.colors.destructive, theme.colors.onDestructive)
                        ToastVariant.Success     -> Triple(theme.colors.success, theme.colors.success, theme.colors.onStatus)
                        ToastVariant.Warning     -> Triple(theme.colors.warning, theme.colors.warning, theme.colors.onStatus)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            .shadow(8.dp, shape)
                            .background(bg, shape)
                            .border(1.dp, border, shape)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            AppText(text = toast.title, style = AppTextStyle.LabelLarge, color = content)
                            if (toast.description != null) {
                                AppText(text = toast.description, style = AppTextStyle.BodySmall, color = content.copy(alpha = 0.8f))
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        AppIconButton(onClick = { toastHostState.dismiss(toast.id) }) {
                            AppText(text = "✕", style = AppTextStyle.LabelSmall, color = content)
                        }
                    }
                }
            }
        }
    }
}
