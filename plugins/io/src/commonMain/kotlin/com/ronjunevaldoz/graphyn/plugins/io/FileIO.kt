package com.ronjunevaldoz.graphyn.plugins.io

/** Platform-specific file read/write. Returns null/false on platforms without filesystem access. */
expect object FileIO {
    suspend fun read(path: String): String?
    suspend fun write(path: String, content: String, append: Boolean): Boolean
    fun resolvePath(baseDir: String, relativePath: String): String
}

internal fun joinPath(baseDir: String, relativePath: String): String = when {
    relativePath.startsWith("/") -> relativePath
    baseDir.isBlank() -> relativePath
    relativePath.isBlank() -> baseDir
    else -> "${baseDir.trimEnd('/')}/${relativePath.trimStart('/')}"
}
