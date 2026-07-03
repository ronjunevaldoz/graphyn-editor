package com.ronjunevaldoz.graphyn.plugins.mediacore.renderer

import com.ronjunevaldoz.graphyn.plugins.mediacore.model.Caption
import com.ronjunevaldoz.graphyn.plugins.mediacore.model.CaptionStyle

interface CaptionRenderer<T> {
    fun render(
        captions: List<Caption>,
        style: CaptionStyle,
        width: Int,
        height: Int,
    ): T
}

