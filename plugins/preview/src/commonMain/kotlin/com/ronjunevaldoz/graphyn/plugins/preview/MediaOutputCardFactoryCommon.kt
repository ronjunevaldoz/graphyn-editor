package com.ronjunevaldoz.graphyn.plugins.preview

import androidx.compose.runtime.Composable
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.editor.canvas.NodeCanvasContext
import com.ronjunevaldoz.graphyn.editor.canvas.NodeCanvasFactory

/**
 * Common wrapper for media output card factory.
 * Platform-specific implementations override the renderContent function.
 */
object MediaOutputCardFactoryCommon : NodeCanvasFactory {
    override val nodeWidth = 220
    override val nodeHeight = 150

    override fun portAnchorY(portIndex: Int, isInput: Boolean, spec: NodeSpec): Int =
        if (isInput) 28 + 22 / 2 else 28 + 22 + 100 + 22 / 2

    @Composable
    override fun NodeCanvas(context: NodeCanvasContext) {
        // Delegate to platform-specific implementation
        MediaOutputCardPlatform(context)
    }
}

@Composable
internal expect fun MediaOutputCardPlatform(ctx: NodeCanvasContext)
