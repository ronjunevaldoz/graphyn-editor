package com.ronjunevaldoz.graphyn.editor.shell.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.editor.design.GraphynDs

@Composable
internal actual fun ArtifactPreview(item: ArtifactItem, modifier: Modifier) {
    Box(modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        BasicText(item.filePath, style = GraphynDs.type.mono.copy(color = GraphynDs.colors.textSecondary))
    }
}
