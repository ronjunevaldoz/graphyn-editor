package com.ronjunevaldoz.graphyn.plugins.mediacore.model

/** A single timed caption line, in milliseconds relative to the start of the video. */
data class Caption(
    val text: String,
    val startMs: Double,
    val endMs: Double,
)