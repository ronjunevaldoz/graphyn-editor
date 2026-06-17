package com.ronjunevaldoz.graphyn.editor.canvas.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.editor.design.GraphynDs

@Composable
fun GraphynEmptyCanvasHint(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = "Add a workflow to start laying out nodes.",
            style = GraphynDs.type.body.copy(color = GraphynDs.colors.textSecondary),
        )
    }
}

@Composable
fun GraphynEmptyNodesHint(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("empty-nodes-hint"),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = "Drag nodes from the palette to get started.",
            style = GraphynDs.type.body.copy(color = GraphynDs.colors.textSecondary),
        )
    }
}
