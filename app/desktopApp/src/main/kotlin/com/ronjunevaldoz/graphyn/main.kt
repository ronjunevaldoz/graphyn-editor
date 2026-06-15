package com.ronjunevaldoz.graphyn

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.runtime.remember
import com.ronjunevaldoz.graphyn.editor.plugins.DefaultGraphynEditorPluginRegistry
import com.ronjunevaldoz.graphyn.plugins.samplelogger.SampleLoggerPlugin
import com.ronjunevaldoz.graphyn.plugins.sampleloggerui.SampleLoggerEditorPlugin

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Graphyn",
    ) {
        val editorPlugins = remember {
            DefaultGraphynEditorPluginRegistry().apply {
                install(SampleLoggerEditorPlugin)
            }
        }
        App(
            plugins = listOf(SampleLoggerPlugin),
            panels = editorPlugins.panels,
        )
    }
}
