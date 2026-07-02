package com.ronjunevaldoz.graphyn.plugins.mediacore

expect object GraphynMediaPaths {
    fun root(): String
    fun directory(relativePath: String): String
    fun temp(): String
    fun ttsCache(): String
}