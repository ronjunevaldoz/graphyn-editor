package com.ronjunevaldoz.graphyn.core.designsystem.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.designsystem.theme.appTheme

@Composable
fun AppCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String? = null,
) {
    val theme = appTheme
    val interactionSource = remember { MutableInteractionSource() }
    val checkAlpha by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(150),
        label = "checkAlpha",
    )

    Row(
        modifier = modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            role = Role.Checkbox,
            onClick = { onCheckedChange(!checked) },
        ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Canvas(modifier = Modifier.size(18.dp)) {
            val r = CornerRadius(3.dp.toPx())
            if (checked) {
                drawRoundRect(
                    color = if (enabled) theme.colors.primary else theme.colors.primaryDisabled,
                    cornerRadius = r,
                )
                val path = Path().apply {
                    moveTo(size.width * 0.2f, size.height * 0.5f)
                    lineTo(size.width * 0.42f, size.height * 0.72f)
                    lineTo(size.width * 0.78f, size.height * 0.28f)
                }
                drawPath(
                    path = path,
                    color = theme.colors.onPrimary.copy(alpha = checkAlpha),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            } else {
                drawRoundRect(
                    color = if (enabled) theme.colors.border else theme.colors.primaryDisabled,
                    cornerRadius = r,
                    style = Stroke(width = 1.5.dp.toPx()),
                )
            }
        }
        if (label != null) {
            AppText(text = label, style = AppTextStyle.BodyMedium, muted = !enabled)
        }
    }
}
