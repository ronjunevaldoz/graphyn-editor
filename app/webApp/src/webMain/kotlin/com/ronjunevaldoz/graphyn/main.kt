package com.ronjunevaldoz.graphyn

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeViewport
import com.ronjunevaldoz.graphyn.editor.panels.DefaultEditorPanelRegistry
import com.ronjunevaldoz.graphyn.plugins.samplelogger.SampleLoggerPlugin
import com.ronjunevaldoz.graphyn.plugins.sampleloggerui.SampleLoggerEditorPanels

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport {
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
