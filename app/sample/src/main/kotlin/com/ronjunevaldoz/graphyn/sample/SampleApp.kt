@file:OptIn(com.ronjunevaldoz.graphyn.core.GraphynExperimentalApi::class)

package com.ronjunevaldoz.graphyn.sample

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.ronjunevaldoz.graphyn.core.execution.WorkflowExecutionEngine
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.serialization.workflowFromJson
import com.ronjunevaldoz.graphyn.core.serialization.toJson
import com.ronjunevaldoz.graphyn.editor.shell.GraphynEditorShell
import com.ronjunevaldoz.graphyn.editor.shell.GraphynEditorShellDependencies
import com.ronjunevaldoz.graphyn.editor.state.rememberGraphynEditorState
import com.ronjunevaldoz.graphyn.plugins.math.MathPlugin
import com.ronjunevaldoz.graphyn.pluginapi.DefaultGraphynPluginRegistry
import kotlinx.coroutines.delay
import java.io.File

private val saveFile = File(System.getProperty("user.home"), ".graphyn/sample-workflow.json")

private fun loadWorkflow(): WorkflowDefinition {
    if (saveFile.exists()) {
        runCatching { workflowFromJson(saveFile.readText()) }.getOrNull()?.let { return it }
    }
    return WorkflowDefinition(
        id = "sample",
        name = "My Math Workflow",
        nodes = listOf(
            NodeRef(id = "add-1", type = "math.add"),
            NodeRef(id = "mul-1", type = "math.multiply"),
        ),
        connections = emptyList(),
    )
}

private fun saveWorkflow(workflow: WorkflowDefinition) {
    saveFile.parentFile.mkdirs()
    saveFile.writeText(workflow.toJson())
}

@Composable
fun SampleApp() {
    val plugins = remember {
        DefaultGraphynPluginRegistry().apply { install(MathPlugin) }
    }
    val engine = remember(plugins) {
        WorkflowExecutionEngine(plugins.nodeExecutors, plugins.nodeSpecs)
    }

    val state = rememberGraphynEditorState(initialWorkflow = remember { loadWorkflow() })

    // Auto-save: debounce 1 s after each edit
    val workflow = state.workflow
    LaunchedEffect(workflow) {
        if (workflow != null) {
            delay(1_000)
            saveWorkflow(workflow)
        }
    }

    GraphynEditorShell(
        dependencies = GraphynEditorShellDependencies(
            nodeSpecs = plugins.nodeSpecs,
            executionEngine = engine,
        ),
        state = state,
    )
}
