package com.ronjunevaldoz.graphyn.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import com.ronjunevaldoz.graphyn.core.designsystem.theme.appTheme

@Composable
fun AppTooltip(
    tooltip: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val theme = appTheme
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Box(modifier = modifier.hoverable(interactionSource)) {
        content()
        if (isHovered) {
            val density = LocalDensity.current
            val positionProvider = remember(density) {
                object : PopupPositionProvider {
                    override fun calculatePosition(
                        anchorBounds: IntRect,
                        windowSize: IntSize,
                        layoutDirection: LayoutDirection,
                        popupContentSize: IntSize,
                    ): IntOffset = IntOffset(
                        x = anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2,
                        y = anchorBounds.top - popupContentSize.height - with(density) { 4.dp.toPx().toInt() },
                    )
                }
            }
            Popup(popupPositionProvider = positionProvider) {
                Box(
                    modifier = Modifier
                        .background(
                            color = theme.colors.onSurface.copy(alpha = 0.9f),
                            shape = RoundedCornerShape(theme.shapes.sm),
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    AppText(text = tooltip, style = AppTextStyle.BodySmall, color = theme.colors.background)
                }
            }
        }
    }
}
