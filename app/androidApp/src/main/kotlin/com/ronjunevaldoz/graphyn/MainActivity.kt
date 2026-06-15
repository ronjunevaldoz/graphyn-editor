package com.ronjunevaldoz.graphyn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.ronjunevaldoz.graphyn.plugins.samplelogger.SampleLoggerPlugin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            App(plugins = listOf(SampleLoggerPlugin))
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App(plugins = listOf(SampleLoggerPlugin))
}
