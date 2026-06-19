@file:OptIn(com.ronjunevaldoz.graphyn.core.GraphynExperimentalApi::class)

package com.ronjunevaldoz.graphyn.editor.shell

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.registry.DefaultNodeSpecRegistry
import com.ronjunevaldoz.graphyn.editor.state.GraphynEditorState
import com.ronjunevaldoz.graphyn.editor.theme.rememberGraphynAppearanceState
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GraphynSubgraphNavigatorUiTest {

    @get:Rule
    val rule = createComposeRule()

    private fun stateWith(name: String) = GraphynEditorState(
        WorkflowDefinition(id = "wf", name = name, nodes = emptyList(), connections = emptyList()),
    )

    @Test
    fun homeButtonAppearsWhenOnHomeIsProvided() {
        rule.setContent {
            GraphynSubgraphNavigator(
                dependencies = GraphynEditorShellDependencies(nodeSpecs = DefaultNodeSpecRegistry()),
                state = stateWith("My Flow"),
                appearanceState = rememberGraphynAppearanceState(),
                onHome = {},
            )
        }
        rule.onNodeWithText("⌂").assertExists()
    }

    @Test
    fun homeButtonIsAbsentWithoutOnHome() {
        rule.setContent {
            GraphynSubgraphNavigator(
                dependencies = GraphynEditorShellDependencies(nodeSpecs = DefaultNodeSpecRegistry()),
                state = stateWith("My Flow"),
                appearanceState = rememberGraphynAppearanceState(),
            )
        }
        rule.onNodeWithText("⌂").assertDoesNotExist()
    }

    @Test
    fun workflowNameAppearsInNavBarWhenOnHomeIsSet() {
        rule.setContent {
            GraphynSubgraphNavigator(
                dependencies = GraphynEditorShellDependencies(nodeSpecs = DefaultNodeSpecRegistry()),
                state = stateWith("Pipeline Editor"),
                appearanceState = rememberGraphynAppearanceState(),
                onHome = {},
            )
        }
        rule.onNodeWithText("Pipeline Editor").assertExists()
    }

    @Test
    fun clickingHomeButtonFiresCallback() {
        var fired = false
        rule.setContent {
            GraphynSubgraphNavigator(
                dependencies = GraphynEditorShellDependencies(nodeSpecs = DefaultNodeSpecRegistry()),
                state = stateWith("My Flow"),
                appearanceState = rememberGraphynAppearanceState(),
                onHome = { fired = true },
            )
        }
        rule.onNodeWithText("⌂").performClick()
        assertTrue(fired)
    }

    @Test
    fun navBarIsAbsentWithNoOnHomeAndNoSubgraphStack() {
        rule.setContent {
            GraphynSubgraphNavigator(
                dependencies = GraphynEditorShellDependencies(nodeSpecs = DefaultNodeSpecRegistry()),
                state = stateWith("My Flow"),
                appearanceState = rememberGraphynAppearanceState(),
            )
        }
        // Nav bar should not appear at root with no onHome
        rule.onNodeWithText("My Flow").assertDoesNotExist()
    }
}
