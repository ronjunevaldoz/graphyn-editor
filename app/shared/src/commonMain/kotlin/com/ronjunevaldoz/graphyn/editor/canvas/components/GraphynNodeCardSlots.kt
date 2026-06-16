package com.ronjunevaldoz.graphyn.editor.canvas.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable

/**
 * Named slot bundle for [GraphynNodeCard].
 *
 * This is a public, plugin-friendly surface for custom node layouts.
 */
data class GraphynNodeCardSlots(
    val header: @Composable ColumnScope.() -> Unit = {},
    val body: @Composable ColumnScope.() -> Unit = {},
    val ports: @Composable ColumnScope.() -> Unit = {},
    val footer: @Composable ColumnScope.() -> Unit = {},
)
