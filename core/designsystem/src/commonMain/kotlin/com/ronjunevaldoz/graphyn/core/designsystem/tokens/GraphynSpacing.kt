package com.ronjunevaldoz.graphyn.core.designsystem.tokens

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Compact spacing tokens for the Graphyn editor UI components.
 * Used for fine-grained layout control in node cards and fields.
 */
@Immutable
data class GraphynSpacing(
    val xs: Dp = 1.dp,
    val sm: Dp = 2.dp,
    val md: Dp = 3.dp,
    val lg: Dp = 4.dp,
    val xl: Dp = 5.dp,
    val xxl: Dp = 6.dp,
    val xxxl: Dp = 8.dp,
    val huge: Dp = 10.dp,
    val massive: Dp = 12.dp,
    val cardRow: Dp = 24.dp,
    val fieldWidth: Dp = 80.dp,
    val fieldMaxWidth: Dp = 160.dp,
)

val LocalGraphynSpacing = compositionLocalOf { GraphynSpacing() }

object GraphynSpacingValues {
    val spacing: GraphynSpacing
        @androidx.compose.runtime.Composable
        @androidx.compose.runtime.ReadOnlyComposable
        get() = LocalGraphynSpacing.current
}
