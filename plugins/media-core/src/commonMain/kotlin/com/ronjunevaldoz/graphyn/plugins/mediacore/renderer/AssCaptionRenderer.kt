package com.ronjunevaldoz.graphyn.plugins.mediacore.renderer

import com.ronjunevaldoz.graphyn.core.common.toAssColor
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
        val styleName = "Graphyn"

        val events = captions.joinToString("\n") { caption ->
            require(caption.endMs >= caption.startMs) {
                "Caption end_ms must be >= start_ms."
            }

            "Dialogue: 0,${caption.startMs.toAssTimeFromMs()},${caption.endMs.toAssTimeFromMs()},$styleName,,0,0,0,,${caption.text.escapeAssText()}"
        }

        val hasBackground = style.backgroundColor != null &&
                style.backgroundColor.alpha > 0.01f

        val borderStyle = if (hasBackground) 3 else 1

        val backColor = style.backgroundColor?.toAssColor()
            ?: "&HFF000000" // fully transparent black

        return """
            [Script Info]
            ScriptType: v4.00+
            PlayResX: $width
            PlayResY: $height
            ScaledBorderAndShadow: yes

            [V4+ Styles]
            Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding
            Style: $styleName,${style.fontFamily.escapeAssField()},${style.fontSize},${style.textColor.toAssColor()},&H000000FF,${style.outlineColor.toAssColor()},$backColor,${style.bold.toAssFlag()},${style.italic.toAssFlag()},0,0,100,100,0,0,$borderStyle,${style.outlineWidth},${style.shadow},${style.alignment.toAssAlignment()},${style.marginHorizontal},${style.marginHorizontal},${style.marginVertical},1

            [Events]
            Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
            $events
        """.trimIndent() + "\n"
    }
}

private fun String.escapeAssField(): String =
    replace(",", "")


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


private fun Int.toHex(): String =
    toString(16).padStart(2, '0').uppercase()

internal fun Double.toAssTimeFromMs(): String {
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