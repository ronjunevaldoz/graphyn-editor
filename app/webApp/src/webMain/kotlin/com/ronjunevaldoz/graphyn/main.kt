package com.ronjunevaldoz.graphyn

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport {
        DemoApp(store = createWebStore())
    }
}
