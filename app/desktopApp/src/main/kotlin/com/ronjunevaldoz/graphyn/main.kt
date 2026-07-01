package com.ronjunevaldoz.graphyn

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.ronjunevaldoz.graphyn.bootstrap.GraphynBootstrap
import com.ronjunevaldoz.graphyn.bootstrap.GraphynBootstrapJvm
import com.ronjunevaldoz.graphyn.bootstrap.HttpSdServerControl
import com.ronjunevaldoz.graphyn.bootstrap.SettingsBackedWorkflowGenerator
import com.ronjunevaldoz.graphyn.core.store.FileArtifactHistory
import com.ronjunevaldoz.graphyn.core.store.FileSettingsStore
import com.ronjunevaldoz.graphyn.core.store.FileWorkflowStore
import com.ronjunevaldoz.graphyn.plugins.io.GraphynEnvOverrides
import com.ronjunevaldoz.graphyn.plugins.script.ScriptEditorPlugin
import com.ronjunevaldoz.graphyn.plugins.script.ScriptPlugin
import com.ronjunevaldoz.graphyn.plugins.gmail.GmailPlugin
import com.ronjunevaldoz.graphyn.plugins.linkedin.LinkedInPlugin

fun main() = application {
    val store = FileWorkflowStore()
    val settingsStore = FileSettingsStore()
    // The AI generator's URL follows the active-environment setting, resolved per generation.
    val generator = SettingsBackedWorkflowGenerator(settingsStore)
    // Route env-var reads (the workflow `env` node, etc.) through the active environment first, so
    // custom key-values apply live and switching environments swaps credentials across plugins.
    GraphynEnvOverrides.provider = { key -> settingsStore.read().value(key) }

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
            sdServerControl = HttpSdServerControl(settingsStore),
            workflowGenerator = generator,
        )
    }
}
