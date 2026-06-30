package com.ronjunevaldoz.graphyn.editor.shell.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Platform-specific rendering of a job artifact (image, video, audio). */
@Composable
internal expect fun ArtifactPreview(item: ArtifactItem, modifier: Modifier = Modifier)
