package com.ronjunevaldoz.graphyn.core.designsystem.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.designsystem.theme.appTheme

sealed interface AvatarSize {
    val dp: Dp
    data object Sm : AvatarSize { override val dp = 32.dp }
    data object Md : AvatarSize { override val dp = 40.dp }
    data object Lg : AvatarSize { override val dp = 56.dp }
    data object Xl : AvatarSize { override val dp = 72.dp }
}

@Composable
fun AppAvatar(
    modifier: Modifier = Modifier,
    initials: String? = null,
    painter: Painter? = null,
    contentDescription: String? = null,
    size: AvatarSize = AvatarSize.Md,
) {
    val theme = appTheme
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(theme.colors.secondary),
        contentAlignment = Alignment.Center,
    ) {
        if (painter != null) {
            Image(painter = painter, contentDescription = contentDescription, modifier = Modifier.size(size.dp))
        } else if (initials != null) {
            AppText(
                text = initials.take(2).uppercase(),
                style = if (size.dp >= 56.dp) AppTextStyle.TitleSmall else AppTextStyle.LabelLarge,
                color = theme.colors.onSecondary,
            )
        }
    }
}
