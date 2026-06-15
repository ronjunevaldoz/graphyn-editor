package com.ronjunevaldoz.graphyn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.ronjunevaldoz.graphyn.plugins.samplelogger.SampleLoggerPlugin
import com.ronjunevaldoz.graphyn.plugins.sampleloggerui.SampleLoggerEditorPanels
import com.ronjunevaldoz.graphyn.editor.panels.DefaultEditorPanelRegistry

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
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
}

@Preview
@Composable
fun AppAndroidPreview() {
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
