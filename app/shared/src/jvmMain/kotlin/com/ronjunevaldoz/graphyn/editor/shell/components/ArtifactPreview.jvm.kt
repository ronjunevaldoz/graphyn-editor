package com.ronjunevaldoz.graphyn.editor.shell.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.editor.design.GraphynDs
import java.io.BufferedInputStream
import java.io.FileInputStream

@Composable
internal actual fun ArtifactPreview(item: ArtifactItem, modifier: Modifier) {
    val colors = GraphynDs.colors
    val type = GraphynDs.type
    if (item.type == ArtifactType.Image) {
        val bitmap: ImageBitmap? = remember(item.filePath) {
            runCatching {
                BufferedInputStream(FileInputStream(item.filePath)).use { loadImageBitmap(it) }
            }.getOrNull()
        }
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = item.fileName,
                modifier = modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
            return
        }
    }
    Box(modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        BasicText(
            text = when (item.type) {
                ArtifactType.Image -> "Could not load: ${item.filePath}"
                else -> item.filePath
            },
            style = type.mono.copy(color = colors.textSecondary),
        )
    }
}
