package com.ronjunevaldoz.graphyn.ui.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider

private val TOOLTIP_BG     = Color(0xFF1A1A2E)
private val TOOLTIP_BORDER = Color(0xFF6B6BF7)
private val TOOLTIP_TEXT   = Color(0xFFCCCCDD)

/** Popup tooltip anchored ABOVE the node header — entirely outside the card bounds. */
@Composable
internal fun FieldNodeTooltip(description: String) {
    Popup(
        popupPositionProvider = AboveAnchorPositionProvider,
        onDismissRequest = {},
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 220.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(TOOLTIP_BG)
                .border(1.dp, TOOLTIP_BORDER.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 5.dp),
        ) {
            BasicText(
                description,
                style = TextStyle(color = TOOLTIP_TEXT, fontSize = 9.sp, lineHeight = 13.sp),
            )
        }
    }
}

private object AboveAnchorPositionProvider : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val x = (anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2)
            .coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))
        val y = anchorBounds.top - popupContentSize.height - 4
        return IntOffset(x, y)
    }
}
