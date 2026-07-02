package com.ronjunevaldoz.graphyn.plugins.mediacore.model

import androidx.compose.ui.graphics.Color

/** Resolved caption appearance passed to the backend when burning subtitles in. */
data class CaptionStyle(
    val fontFamily: String = "Arial",
    val fontSize: Int = 42,
    val textColor: Color = Color.White,
    val backgroundColor: Color? = null,
    val outlineColor: Color = Color.Black,
    val outlineWidth: Int = 2,
    val shadow: Int = 0,
    val bold: Boolean = true,
    val italic: Boolean = false,
    val alignment: CaptionAlignment = CaptionAlignment.BottomCenter,
    val marginHorizontal: Int = 40,
    val marginVertical: Int = 60,
)