package com.ronjunevaldoz.graphyn.core.designsystem.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.designsystem.theme.appTheme

@Composable
fun AppSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String? = null,
) {
    val theme = appTheme
    val trackColor = if (checked) theme.colors.primary else theme.colors.border
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 22.dp else 2.dp,
        animationSpec = spring(dampingRatio = 0.6f),
        label = "thumbOffset",
    )

    Row(
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            enabled = enabled,
            role = Role.Switch,
            onClick = { onCheckedChange(!checked) },
        ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(width = 44.dp, height = 24.dp)
                .clip(CircleShape)
                .background(if (enabled) trackColor else theme.colors.primaryDisabled),
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .offset(x = thumbOffset, y = 2.dp)
                    .clip(CircleShape)
                    .background(theme.colors.background),
            )
        }
        if (label != null) {
            AppText(text = label, style = AppTextStyle.BodyMedium, muted = !enabled)
        }
    }
}
