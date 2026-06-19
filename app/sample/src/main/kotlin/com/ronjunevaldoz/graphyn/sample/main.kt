package com.ronjunevaldoz.graphyn.sample

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Graphyn Sample",
    ) {
        SampleApp()
    }
}
