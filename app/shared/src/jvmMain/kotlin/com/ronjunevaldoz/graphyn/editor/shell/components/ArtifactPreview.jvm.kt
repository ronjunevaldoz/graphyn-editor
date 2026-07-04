package com.ronjunevaldoz.graphyn.editor.shell.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import java.awt.Desktop
import java.io.BufferedInputStream
import java.io.File
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
        if (item.type == ArtifactType.Video || item.type == ArtifactType.Audio) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.clickable { revealInFileBrowser(item.filePath) },
            ) {
                BasicText(text = item.fileName, style = type.mono.copy(color = colors.textPrimary))
                BasicText(text = "Click to show in file browser", style = type.mono.copy(color = colors.textSecondary))
            }
            return@Box
        }
        BasicText(
            text = "Could not load: ${item.filePath}",
            style = type.mono.copy(color = colors.textSecondary),
        )
    }
}

private fun revealInFileBrowser(filePath: String) {
    val file = File(filePath)
    val desktop = runCatching { Desktop.getDesktop() }.getOrNull() ?: return
    when {
        desktop.isSupported(Desktop.Action.BROWSE_FILE_DIR) ->
            runCatching { desktop.browseFileDirectory(file) }
        desktop.isSupported(Desktop.Action.OPEN) ->
            runCatching { desktop.open(file.parentFile ?: file) }
    }
}
