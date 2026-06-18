package com.ronjunevaldoz.graphyn.core.designsystem.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.designsystem.theme.appTheme

@Composable
fun AppRadioButton(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String? = null,
) {
    val theme = appTheme
    val dotScale by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.6f),
        label = "radioDot",
    )

    Row(
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            enabled = enabled,
            role = Role.RadioButton,
            onClick = onClick,
        ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Canvas(modifier = Modifier.size(18.dp)) {
            val r = size.minDimension / 2
            val strokePx = 1.5.dp.toPx()
            val ringColor = if (enabled) theme.colors.border else theme.colors.primaryDisabled
            val fillColor = if (enabled) theme.colors.primary else theme.colors.primaryDisabled
            drawCircle(color = ringColor, radius = r, style = Stroke(width = strokePx))
            if (dotScale > 0f) {
                drawCircle(color = fillColor, radius = r * 0.5f * dotScale)
                drawCircle(color = fillColor, radius = r, style = Stroke(width = strokePx))
            }
        }
        if (label != null) {
            AppText(text = label, style = AppTextStyle.BodyMedium, muted = !enabled)
        }
    }
}
