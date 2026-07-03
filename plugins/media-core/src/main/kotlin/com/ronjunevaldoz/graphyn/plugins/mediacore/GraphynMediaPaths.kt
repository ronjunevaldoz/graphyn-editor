package com.ronjunevaldoz.graphyn.plugins.mediacore

import com.ronjunevaldoz.graphyn.core.common.EnvironmentResolver
import com.ronjunevaldoz.graphyn.core.common.FileIO
import java.io.File

object GraphynMediaPaths {
    private val rootPath: String by lazy {
        FileIO.resolvePath(
            EnvironmentResolver.get("GRAPHYN_HOME") ?: "~/.graphyn",
            "",
        )
    }

    fun root(): String = rootPath

    fun directory(relativePath: String): String {
        val path = FileIO.resolvePath(rootPath, relativePath)
        File(path).mkdirs()
        return path
    }

    fun temp(): String = directory("temp")

    fun ttsCache(): String = directory("cache/tts")
}