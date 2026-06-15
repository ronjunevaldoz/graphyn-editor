package com.ronjunevaldoz.graphyn

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.ronjunevaldoz.graphyn.plugins.samplelogger.SampleLoggerPlugin

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Graphyn",
    ) {
        App(plugins = listOf(SampleLoggerPlugin))
    }
}
