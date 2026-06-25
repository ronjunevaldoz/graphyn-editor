package com.ronjunevaldoz.graphyn.plugins.io

actual object FileIO {
    actual suspend fun read(path: String): String? = null
    actual suspend fun write(path: String, content: String, append: Boolean): Boolean = false
    actual fun resolvePath(baseDir: String, relativePath: String): String = joinPath(baseDir, relativePath)
}
