package com.ronjunevaldoz.graphyn.plugins.preview

import androidx.compose.runtime.Composable
import com.ronjunevaldoz.graphyn.editor.canvas.NodeCanvasContext

@Composable
internal actual fun MediaOutputCardPlatform(ctx: NodeCanvasContext) = MediaOutputPlaceholderCard(ctx)
