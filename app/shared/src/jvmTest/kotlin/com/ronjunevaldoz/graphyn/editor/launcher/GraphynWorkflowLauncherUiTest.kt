@file:OptIn(com.ronjunevaldoz.graphyn.core.GraphynExperimentalApi::class)

package com.ronjunevaldoz.graphyn.editor.launcher

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.editor.theme.GraphynTheme
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class GraphynWorkflowLauncherUiTest {

    @get:Rule
    val rule = createComposeRule()

    private val templates = listOf(
        WorkflowTemplate(
            name = "Starter",
            description = "A simple starting point",
            workflow = WorkflowDefinition("wf-1", "Starter", emptyList(), emptyList()),
        ),
        WorkflowTemplate(
            name = "Data Pipeline",
            description = null,
            workflow = WorkflowDefinition(
                id = "wf-2", name = "Data Pipeline",
                nodes = listOf(NodeRef("n1", "source"), NodeRef("n2", "sink")),
                connections = emptyList(),
            ),
        ),
    )

    @Test
    fun templateCardsRender() {
        rule.setContent {
            GraphynTheme { GraphynWorkflowLauncher(templates = templates, onOpen = {}) }
        }
        rule.onNodeWithText("Starter").assertExists()
        rule.onNodeWithText("Data Pipeline").assertExists()
    }

    @Test
    fun templatesAreGroupedByCategorySection() {
        val categorized = listOf(
            WorkflowTemplate("Captioned", "burn captions", WorkflowDefinition("wf-m", "Captioned", emptyList(), emptyList()), WorkflowCategory.Media),
            WorkflowTemplate("List Ops", "list demo", WorkflowDefinition("wf-e", "List Ops", emptyList(), emptyList()), WorkflowCategory.Examples),
        )
        rule.setContent {
            GraphynTheme { GraphynWorkflowLauncher(templates = categorized, onOpen = {}) }
        }
        rule.onNodeWithText("MEDIA").assertExists()
        rule.onNodeWithText("EXAMPLES").assertExists()
        rule.onNodeWithText("Captioned").assertExists()
    }

    @Test
    fun templateDescriptionRendersOnCard() {
        rule.setContent {
            GraphynTheme { GraphynWorkflowLauncher(templates = templates, onOpen = {}) }
        }
        rule.onNodeWithText("A simple starting point").assertExists()
    }

    @Test
    fun nodeCountRendersOnCard() {
        rule.setContent {
            GraphynTheme { GraphynWorkflowLauncher(templates = templates, onOpen = {}) }
        }
        rule.onNodeWithText("2 nodes").assertExists()
    }

    @Test
    fun clickingTemplateCardFiresOnOpenWithCorrectTemplate() {
        var opened: WorkflowTemplate? = null
        rule.setContent {
            GraphynTheme { GraphynWorkflowLauncher(templates = templates, onOpen = { opened = it }) }
        }
        rule.onNodeWithText("Starter").performClick()
        assertEquals("wf-1", opened?.workflow?.id)
    }

    @Test
    fun recentSectionIsHiddenWhenEmpty() {
        rule.setContent {
            GraphynTheme { GraphynWorkflowLauncher(templates = templates, recentWorkflows = emptyList(), onOpen = {}) }
        }
        rule.onNodeWithText("RECENT").assertDoesNotExist()
    }

    @Test
    fun recentSectionRendersWhenNonEmpty() {
        val recent = listOf(
            WorkflowTemplate("Past Run", null, WorkflowDefinition("wf-99", "Past Run", emptyList(), emptyList())),
        )
        rule.setContent {
            GraphynTheme { GraphynWorkflowLauncher(templates = templates, recentWorkflows = recent, onOpen = {}) }
        }
        rule.onNodeWithText("RECENT").assertExists()
        rule.onNodeWithText("Past Run").assertExists()
        rule.onNodeWithText("Starter").assertDoesNotExist()
    }

    @Test
    fun clickingRecentCardFiresOnOpen() {
        var opened: WorkflowTemplate? = null
        val recent = listOf(
            WorkflowTemplate("Past Run", null, WorkflowDefinition("wf-99", "Past Run", emptyList(), emptyList())),
        )
        rule.setContent {
            GraphynTheme { GraphynWorkflowLauncher(templates = templates, recentWorkflows = recent, onOpen = { opened = it }) }
        }
        rule.onNodeWithText("Past Run").performClick()
        assertEquals("wf-99", opened?.workflow?.id)
    }

    @Test
    fun templatesAreSeparatedFromSavedAndRecentWorkflows() {
        val recent = listOf(
            WorkflowTemplate("Past Run", null, WorkflowDefinition("wf-99", "Past Run", emptyList(), emptyList())),
        )
        rule.setContent {
            GraphynTheme { GraphynWorkflowLauncher(templates = templates, recentWorkflows = recent, onOpen = {}) }
        }

        rule.onNodeWithText("Starter").assertDoesNotExist()
        rule.onNodeWithText("Templates  2").performClick()
        rule.onNodeWithText("Starter").assertExists()
        rule.onNodeWithText("Past Run").assertDoesNotExist()
    }

    @Test
    fun schemaTabShowsInstalledSpecs() {
        val specs = listOf(
            NodeSpec(
                type = "demo.schema",
                label = "Demo Schema",
                inputs = listOf(PortSpec("source", WorkflowType.StringType)),
                outputs = listOf(PortSpec("result", WorkflowType.StringType)),
                defaultValues = mapOf("source" to WorkflowValue.StringValue("input.txt")),
                category = "Custom",
                description = "Example schema card",
            ),
        )
        rule.setContent {
            GraphynTheme { GraphynWorkflowLauncher(templates = templates, nodeSpecs = specs, onOpen = {}) }
        }
        rule.onNodeWithText("Schema  1").performClick()
        rule.onNodeWithText("Demo Schema").assertExists()
        rule.onNodeWithText("source: String").assertExists()
    }
}
