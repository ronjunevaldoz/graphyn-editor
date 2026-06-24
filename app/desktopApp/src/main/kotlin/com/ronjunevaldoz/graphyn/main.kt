package com.ronjunevaldoz.graphyn

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.ronjunevaldoz.graphyn.bootstrap.GraphynBootstrap
import com.ronjunevaldoz.graphyn.bootstrap.GraphynBootstrapJvm
import com.ronjunevaldoz.graphyn.core.store.FileWorkflowStore
import com.ronjunevaldoz.graphyn.plugins.script.ScriptEditorPlugin
import com.ronjunevaldoz.graphyn.plugins.script.ScriptPlugin
import com.ronjunevaldoz.graphyn.plugins.gmail.GmailPlugin
import com.ronjunevaldoz.graphyn.plugins.linkedin.LinkedInPlugin

fun main() = application {
    val store = FileWorkflowStore()
    Window(
        onCloseRequest = ::exitApplication,
        title = "Graphyn",
    ) {
        DemoApp(
            runtimePlugins = GraphynBootstrap.runtimePlugins(extraPlugins = listOf(ScriptPlugin, GmailPlugin, LinkedInPlugin)),
            editorPlugins  = GraphynBootstrap.editorPlugins(extraPlugins = listOf(ScriptEditorPlugin) + GraphynBootstrapJvm.serviceIntegrationEditorPlugins),
            store = store,
        )
    }
}
