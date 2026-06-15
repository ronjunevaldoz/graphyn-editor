package com.ronjunevaldoz.graphyn.editor.shell

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.ronjunevaldoz.graphyn.core.registry.DefaultNodeSpecRegistry
import com.ronjunevaldoz.graphyn.editor.state.GraphynEditorState
import com.ronjunevaldoz.graphyn.editor.theme.rememberGraphynAppearanceState
import kotlin.test.Test
import kotlin.test.assertEquals

class GraphynEditorShellUiTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun zoomButtonsUpdateViewportState() = runComposeUiTest {
        val state = GraphynEditorState()
        val nodeSpecs = DefaultNodeSpecRegistry()

        setContent {
            GraphynEditorShell(
                dependencies = GraphynEditorShellDependencies(
                    nodeSpecs = nodeSpecs,
                ),
                state = state,
                appearanceState = rememberGraphynAppearanceState(),
            )
        }

        onNodeWithTag("zoom-in-button").performClick()
        waitUntil(timeoutMillis = 5_000) {
            state.viewport.scale > 1.1f
        }
        assertEquals(1.15f, state.viewport.scale, 0.001f)
    }
}
