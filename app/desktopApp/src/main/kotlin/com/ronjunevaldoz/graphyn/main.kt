package com.ronjunevaldoz.graphyn

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.ronjunevaldoz.graphyn.bootstrap.GraphynBootstrap
import com.ronjunevaldoz.graphyn.core.store.FileWorkflowStore
import com.ronjunevaldoz.graphyn.plugins.script.ScriptEditorPlugin
import com.ronjunevaldoz.graphyn.plugins.script.ScriptPlugin

fun main() = application {
    val store = FileWorkflowStore()
    Window(
        onCloseRequest = ::exitApplication,
        title = "Graphyn",
    ) {
        DemoApp(
            runtimePlugins = GraphynBootstrap.runtimePlugins(extraPlugins = listOf(ScriptPlugin)),
            editorPlugins  = GraphynBootstrap.editorPlugins(extraPlugins = listOf(ScriptEditorPlugin)),
            store = store,
        )
    }
}
