package com.ronjunevaldoz.graphyn.editor.canvas

import androidx.compose.ui.unit.IntSize

object GraphynCanvasMetrics {
    val NodeSize = IntSize(280, 180)
    const val PortDotDiameter = 12
    const val PortDotRadius = PortDotDiameter / 2

    // Estimated dp offsets from card top matching GraphynNodeCard layout:
    // 16dp top padding + ~44dp header + 10dp gap = 70dp to port section
    // labelMedium 16dp + 6dp gap + half bubble (16dp) = 38dp to first port center
    private const val PortSectionTopDp = 70
    private const val PortLabelHeightDp = 16
    private const val PortLabelGapDp = 6
    private const val PortBubbleHeightDp = 32
    private const val PortBubbleGapDp = 8

    fun portAnchorY(portIndex: Int): Int =
        PortSectionTopDp + PortLabelHeightDp + PortLabelGapDp + PortBubbleHeightDp / 2 +
            portIndex * (PortBubbleHeightDp + PortBubbleGapDp)
}
