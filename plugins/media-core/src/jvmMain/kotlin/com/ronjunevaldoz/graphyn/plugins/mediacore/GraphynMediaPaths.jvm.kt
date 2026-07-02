package com.ronjunevaldoz.graphyn.plugins.mediacore

import com.ronjunevaldoz.graphyn.core.common.EnvironmentResolver
import com.ronjunevaldoz.graphyn.core.common.FileIO
import java.io.File

actual object GraphynMediaPaths {
    private val rootPath: String by lazy {
        FileIO.resolvePath(
            EnvironmentResolver.get("GRAPHYN_HOME") ?: "~/.graphyn",
            "",
        )
    }

    actual fun root(): String = rootPath

    actual fun directory(relativePath: String): String {
        val path = FileIO.resolvePath(rootPath, relativePath)
        File(path).mkdirs()
        return path
    }

    actual fun temp(): String = directory("temp")

    actual fun ttsCache(): String = directory("cache/tts")
}