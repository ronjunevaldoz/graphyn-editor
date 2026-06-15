package com.ronjunevaldoz.graphyn.editor.shell

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.ronjunevaldoz.graphyn.core.registry.DefaultNodeSpecRegistry
import com.ronjunevaldoz.graphyn.editor.state.GraphynEditorState
import com.ronjunevaldoz.graphyn.editor.theme.rememberGraphynAppearanceState
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class GraphynEditorShellUiTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun zoomButtonsUpdateViewportState() {
        val state = GraphynEditorState()
        val nodeSpecs = DefaultNodeSpecRegistry()

        rule.setContent {
            GraphynEditorShell(
                dependencies = GraphynEditorShellDependencies(
                    nodeSpecs = nodeSpecs,
                ),
                state = state,
                appearanceState = rememberGraphynAppearanceState(),
            )
        }

        rule.onNodeWithTag("zoom-in-button").performClick()
        rule.waitUntil(timeoutMillis = 5_000) {
            state.viewport.scale > 1.1f
        }
        assertEquals(1.15f, state.viewport.scale, 0.001f)
    }
}
