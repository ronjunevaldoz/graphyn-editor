package com.ronjunevaldoz.graphyn.editor.launcher

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.ronjunevaldoz.graphyn.ai.PlaceholderWorkflowGenerator
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import com.ronjunevaldoz.graphyn.editor.theme.GraphynTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NewWorkflowDialogUiTest {

    private val catalog = listOf(
        NodeSpec("a", "A", inputs = emptyList(), outputs = listOf(PortSpec("out", WorkflowType.StringType))),
        NodeSpec("b", "B", inputs = listOf(PortSpec("in", WorkflowType.StringType)), outputs = emptyList()),
    )

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun startBlankCreatesEmptyWorkflow() = runDesktopComposeUiTest {
        var created: WorkflowDefinition? = null
        setContent {
            GraphynTheme {
                NewWorkflowDialog(
                    generator = PlaceholderWorkflowGenerator(delayMs = 0),
                    catalog = catalog,
                    onCreate = { created = it },
                    onDismiss = {},
                )
            }
        }
        onNodeWithText("New Workflow").assertIsDisplayed()
        onNodeWithText("Start blank").performClick()
        assertEquals(0, created?.nodes?.size)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun generateProducesWorkflowFromCatalog() = runDesktopComposeUiTest {
        var created: WorkflowDefinition? = null
        setContent {
            GraphynTheme {
                NewWorkflowDialog(
                    generator = PlaceholderWorkflowGenerator(delayMs = 0),
                    catalog = catalog,
                    onCreate = { created = it },
                    onDismiss = {},
                )
            }
        }
        onNodeWithTag("new-workflow-prompt").performTextInput("build me a pipe")
        onNodeWithText("Generate").performClick()
        waitForIdle()
        assertTrue((created?.nodes?.size ?: 0) >= 2, "generated workflow should have nodes from catalog")
    }
}
