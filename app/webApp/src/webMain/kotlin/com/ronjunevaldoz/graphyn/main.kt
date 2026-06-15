package com.ronjunevaldoz.graphyn

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.ronjunevaldoz.graphyn.plugins.samplelogger.SampleLoggerPlugin

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport {
        App(plugins = listOf(SampleLoggerPlugin))
    }
}
