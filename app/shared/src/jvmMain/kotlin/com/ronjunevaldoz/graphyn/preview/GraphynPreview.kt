package com.ronjunevaldoz.graphyn.preview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.editor.theme.GraphynBranding
import com.ronjunevaldoz.graphyn.editor.theme.GraphynTheme
import androidx.compose.ui.tooling.preview.Preview

/**
 * Wraps preview content with the standard Graphyn theme.
 * Use this in all *Preview.kt files to get consistent theming.
 */
@Composable
fun GraphynPreview(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    GraphynTheme(branding = GraphynBranding(), darkTheme = darkTheme) {
        Box(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

/** Convenience annotation grouping light + dark previews. */
@Preview
annotation class GraphynPreviews
