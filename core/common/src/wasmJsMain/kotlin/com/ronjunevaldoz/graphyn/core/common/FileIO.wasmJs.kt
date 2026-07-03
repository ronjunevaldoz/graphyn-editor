package com.ronjunevaldoz.graphyn.core.common

// No filesystem access in a browser sandbox — matches this type's own contract: "Returns
// null/false on platforms without filesystem access."
actual object FileIO {
    actual suspend fun read(path: String): String? = null
    actual suspend fun write(path: String, content: String, append: Boolean): Boolean = false
    actual fun resolvePath(baseDir: String, relativePath: String): String = joinPath(baseDir, relativePath)
}
