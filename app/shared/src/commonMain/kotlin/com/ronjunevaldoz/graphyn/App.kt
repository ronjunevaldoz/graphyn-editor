package com.ronjunevaldoz.graphyn

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.ronjunevaldoz.graphyn.core.registry.DefaultNodeSpecRegistry
import com.ronjunevaldoz.graphyn.editor.shell.GraphynEditorShell
import com.ronjunevaldoz.graphyn.editor.shell.GraphynEditorShellDependencies
import com.ronjunevaldoz.graphyn.editor.state.rememberGraphynEditorState
import com.ronjunevaldoz.graphyn.editor.theme.GraphynBranding
import com.ronjunevaldoz.graphyn.editor.theme.GraphynTheme

@Composable
@Preview
fun App(
    branding: GraphynBranding = GraphynBranding(),
) {
    val nodeSpecs = remember { DefaultNodeSpecRegistry() }
    val state = rememberGraphynEditorState()

    GraphynTheme(branding = branding) {
        GraphynEditorShell(
            branding = branding,
            dependencies = GraphynEditorShellDependencies(
                nodeSpecs = nodeSpecs,
            ),
            state = state,
        )
    }
}
