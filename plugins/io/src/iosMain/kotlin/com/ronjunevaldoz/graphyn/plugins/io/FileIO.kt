package com.ronjunevaldoz.graphyn.plugins.io

import platform.Foundation.NSHomeDirectory

actual object FileIO {
    actual suspend fun read(path: String): String? = null
    actual suspend fun write(path: String, content: String, append: Boolean): Boolean = false

    actual fun resolvePath(baseDir: String, relativePath: String): String {
        val expandedBase = when {
            baseDir == "~" -> NSHomeDirectory()
            baseDir.startsWith("~/") -> NSHomeDirectory() + baseDir.drop(1)
            else -> baseDir
        }
        return joinPath(expandedBase, relativePath)
    }
}
