package com.ronjunevaldoz.graphyn.plugins.mediacore.renderer

import androidx.compose.ui.graphics.Color
import com.ronjunevaldoz.graphyn.plugins.mediacore.model.Caption
import com.ronjunevaldoz.graphyn.plugins.mediacore.model.CaptionAlignment
import com.ronjunevaldoz.graphyn.plugins.mediacore.model.CaptionStyle

class AssCaptionRenderer : CaptionRenderer<String> {

    override fun render(
        captions: List<Caption>,
        style: CaptionStyle,
        width: Int,
        height: Int,
    ): String {
        val events = captions.joinToString("\n") { caption ->
            require(caption.endMs >= caption.startMs) {
                "Caption end_ms must be >= start_ms."
            }

            "Dialogue: 0,${caption.startMs.toAssTime()},${caption.endMs.toAssTime()},Default,,0,0,0,,${caption.text.escapeAssText()}"
        }

        return """
            [Script Info]
            ScriptType: v4.00+
            PlayResX: $width
            PlayResY: $height

            [V4+ Styles]
            Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding
            Style: Default,${style.fontFamily},${style.fontSize},${style.textColor.toAssColor()},&H000000FF,${style.outlineColor.toAssColor()},${style.backgroundColor?.toAssColor() ?: "&H80000000"},${style.bold.toAssFlag()},${style.italic.toAssFlag()},0,0,100,100,0,0,3,${style.outlineWidth},${style.shadow},${style.alignment.toAssAlignment()},${style.marginHorizontal},${style.marginHorizontal},${style.marginVertical},1

            [Events]
            Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
            $events
        """.trimIndent() + "\n"
    }
}




private fun CaptionAlignment.toAssAlignment(): Int = when (this) {
    CaptionAlignment.BottomLeft -> 1
    CaptionAlignment.BottomCenter -> 2
    CaptionAlignment.BottomRight -> 3
    CaptionAlignment.MiddleLeft -> 4
    CaptionAlignment.MiddleCenter -> 5
    CaptionAlignment.MiddleRight -> 6
    CaptionAlignment.TopLeft -> 7
    CaptionAlignment.TopCenter -> 8
    CaptionAlignment.TopRight -> 9
}

private fun Boolean.toAssFlag(): Int =
    if (this) -1 else 0

private fun Color.toAssColor(): String {
    val alpha = (255 - (alpha * 255).toInt()).toHex()
    val blue = (blue * 255).toInt().toHex()
    val green = (green * 255).toInt().toHex()
    val red = (red * 255).toInt().toHex()

    return "&H$alpha$blue$green$red"
}

private fun Int.toHex(): String =
    toString(16).padStart(2, '0').uppercase()

internal fun Double.toAssTime(): String {
    val totalCentis = (this / 10.0).toLong().coerceAtLeast(0)

    val centis = totalCentis % 100
    val totalSeconds = totalCentis / 100
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600

    return buildString {
        append(hours)
        append(':')
        append(minutes.pad2())
        append(':')
        append(seconds.pad2())
        append('.')
        append(centis.pad2())
    }
}

private fun Long.pad2(): String =
    toString().padStart(2, '0')

private fun String.escapeAssText(): String =
    replace("\n", "\\N")
        .replace("{", "(")
        .replace("}", ")")