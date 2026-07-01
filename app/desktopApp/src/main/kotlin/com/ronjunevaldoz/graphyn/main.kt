package com.ronjunevaldoz.graphyn

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.ronjunevaldoz.graphyn.ai.OllamaConfig
import com.ronjunevaldoz.graphyn.ai.OllamaWorkflowGenerator
import com.ronjunevaldoz.graphyn.bootstrap.GraphynBootstrap
import com.ronjunevaldoz.graphyn.bootstrap.GraphynBootstrapJvm
import com.ronjunevaldoz.graphyn.core.store.FileArtifactHistory
import com.ronjunevaldoz.graphyn.core.store.FileSettingsStore
import com.ronjunevaldoz.graphyn.core.store.FileWorkflowStore
import com.ronjunevaldoz.graphyn.core.store.GraphynSettings
import com.ronjunevaldoz.graphyn.plugins.script.ScriptEditorPlugin
import com.ronjunevaldoz.graphyn.plugins.script.ScriptPlugin
import com.ronjunevaldoz.graphyn.plugins.gmail.GmailPlugin
import com.ronjunevaldoz.graphyn.plugins.linkedin.LinkedInPlugin

fun main() = application {
    val store = FileWorkflowStore()
    val settingsStore = FileSettingsStore()
    // AI assistant URL: active-environment setting, then env var, then default. (Applies on launch.)
    val ollamaHost = settingsStore.read().value(GraphynSettings.KEY_AI_URL)
        ?: System.getenv("GRAPHYN_OLLAMA_HOST") ?: OllamaConfig.DEFAULT_BASE_URL
    val generator = OllamaWorkflowGenerator(OllamaConfig(baseUrl = ollamaHost))

    Window(
        onCloseRequest = ::exitApplication,
        title = "Graphyn",
    ) {
        GraphynApp(
            runtimePlugins = GraphynBootstrap.runtimePlugins(
                extraPlugins = listOf(ScriptPlugin, GmailPlugin, LinkedInPlugin) +
                    GraphynBootstrapJvm.mediaRuntimePlugins,
            ),
            editorPlugins = GraphynBootstrap.editorPlugins(
                extraPlugins = listOf(ScriptEditorPlugin) + GraphynBootstrapJvm.serviceIntegrationEditorPlugins,
            ),
            store = store,
            settingsStore = settingsStore,
            artifactHistory = FileArtifactHistory(),
            workflowGenerator = generator,
        )
    }
}
