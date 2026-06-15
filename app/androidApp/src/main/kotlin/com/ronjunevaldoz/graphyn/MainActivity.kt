package com.ronjunevaldoz.graphyn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.ronjunevaldoz.graphyn.plugins.samplelogger.SampleLoggerPlugin
import com.ronjunevaldoz.graphyn.plugins.sampleloggerui.SampleLoggerEditorPlugin
import com.ronjunevaldoz.graphyn.editor.plugins.DefaultGraphynEditorPluginRegistry

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
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
}

@Preview
@Composable
fun AppAndroidPreview() {
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
