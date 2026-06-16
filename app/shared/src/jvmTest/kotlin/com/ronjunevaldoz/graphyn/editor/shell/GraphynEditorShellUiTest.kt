package com.ronjunevaldoz.graphyn.editor.shell

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.ronjunevaldoz.graphyn.core.registry.DefaultNodeSpecRegistry
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.editor.panels.DefaultEditorPanelRegistry
import com.ronjunevaldoz.graphyn.editor.panels.EditorPanelFactory
import com.ronjunevaldoz.graphyn.editor.panels.EditorPanelContext
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

    @Test
    fun registeredEditorPanelIsRenderedForTheSelectedNode() {
        val state = GraphynEditorState(
            WorkflowDefinition(
                id = "workflow-panels",
                name = "Panels",
                nodes = listOf(
                    NodeRef(id = "logger-1", type = "logger"),
                ),
                connections = emptyList(),
            ),
        ).apply {
            selectedNodeId = "logger-1"
        }
        val nodeSpecs = DefaultNodeSpecRegistry()
        val panels = DefaultEditorPanelRegistry().apply {
            register(
                "logger",
                EditorPanelFactory { _: EditorPanelContext ->
                    androidx.compose.material3.Text("Custom logger panel")
                },
            )
        }

        rule.setContent {
            GraphynEditorShell(
                dependencies = GraphynEditorShellDependencies(
                    nodeSpecs = nodeSpecs,
                    panels = panels,
                ),
                state = state,
                appearanceState = rememberGraphynAppearanceState(),
            )
        }

        rule.onNodeWithText("Custom logger panel").assertExists()
    }
}
