package com.ronjunevaldoz.graphyn

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.runtime.remember
import com.ronjunevaldoz.graphyn.editor.panels.DefaultEditorPanelRegistry
import com.ronjunevaldoz.graphyn.plugins.samplelogger.SampleLoggerPlugin
import com.ronjunevaldoz.graphyn.plugins.sampleloggerui.SampleLoggerEditorPanels

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Graphyn",
    ) {
        val editorPanels = remember {
            DefaultEditorPanelRegistry().apply {
                SampleLoggerEditorPanels.register(this)
            }
        }
        App(
            plugins = listOf(SampleLoggerPlugin),
            panels = editorPanels,
        )
    }
}
