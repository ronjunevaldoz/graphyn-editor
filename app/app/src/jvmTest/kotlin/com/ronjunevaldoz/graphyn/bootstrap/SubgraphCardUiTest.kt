@file:OptIn(com.ronjunevaldoz.graphyn.core.GraphynExperimentalApi::class)

package com.ronjunevaldoz.graphyn.bootstrap

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.ronjunevaldoz.graphyn.core.execution.NodeExecutionStatus
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.editor.canvas.NodeCanvasContext
import com.ronjunevaldoz.graphyn.editor.theme.GraphynTheme
import org.junit.Rule
import org.junit.Test

class SubgraphCardUiTest {

    @get:Rule
    val rule = createComposeRule()

    private val spec = NodeSpec(
        type = "demo.subgraph",
        label = "Pipeline",
        inputs = emptyList(),
        outputs = emptyList(),
    )
    private val innerWorkflow = WorkflowDefinition(
        id = "inner", name = "Transform Pipeline",
        nodes = emptyList(), connections = emptyList(),
    )
    private val node = NodeRef(id = "sg1", type = "demo.subgraph", subgraph = innerWorkflow)

    private fun ctx(status: NodeExecutionStatus) = NodeCanvasContext(
        node = node,
        spec = spec,
        selected = false,
        executionStatus = status,
        onSelect = {},
        onMove = {},
    )

    @Test
    fun noBadgeWhenIdle() {
        rule.setContent { GraphynTheme { SubgraphCard(ctx(NodeExecutionStatus.Idle)) } }
        rule.onNodeWithText("+").assertDoesNotExist()
        rule.onNodeWithText("v").assertDoesNotExist()
        rule.onNodeWithText("x").assertDoesNotExist()
    }

    @Test
    fun runningBadgeAppearsWhenRunning() {
        rule.setContent { GraphynTheme { SubgraphCard(ctx(NodeExecutionStatus.Running)) } }
        rule.onNodeWithText("+").assertExists()
    }

    @Test
    fun successBadgeAppearsWhenSuccess() {
        rule.setContent { GraphynTheme { SubgraphCard(ctx(NodeExecutionStatus.Success)) } }
        rule.onNodeWithText("v").assertExists()
    }

    @Test
    fun errorBadgeAppearsWhenError() {
        rule.setContent { GraphynTheme { SubgraphCard(ctx(NodeExecutionStatus.Error)) } }
        rule.onNodeWithText("x").assertExists()
    }

    @Test
    fun subgraphNameRendersInHeader() {
        rule.setContent { GraphynTheme { SubgraphCard(ctx(NodeExecutionStatus.Idle)) } }
        rule.onNodeWithText("Transform Pipeline").assertExists()
    }

    @Test
    fun enterFooterAbsentWithoutCallback() {
        rule.setContent { GraphynTheme { SubgraphCard(ctx(NodeExecutionStatus.Idle)) } }
        rule.onNodeWithText("↳ Enter").assertDoesNotExist()
    }

    @Test
    fun enterFooterPresentWithCallback() {
        val ctxWithEnter = ctx(NodeExecutionStatus.Idle).copy(onEnterSubgraph = {})
        rule.setContent { GraphynTheme { SubgraphCard(ctxWithEnter) } }
        rule.onNodeWithText("↳ Enter").assertExists()
    }
}
