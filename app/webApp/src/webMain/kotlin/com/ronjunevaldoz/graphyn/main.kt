package com.ronjunevaldoz.graphyn

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeViewport
import com.ronjunevaldoz.graphyn.editor.plugins.DefaultGraphynEditorPluginRegistry
import com.ronjunevaldoz.graphyn.plugins.samplelogger.SampleLoggerPlugin
import com.ronjunevaldoz.graphyn.plugins.sampleloggerui.SampleLoggerEditorPlugin

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport {
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
