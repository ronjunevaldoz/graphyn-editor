package com.ronjunevaldoz.graphyn.plugins.io

/** Platform-specific file read/write. Returns null/false on platforms without filesystem access. */
expect object FileIO {
    suspend fun read(path: String): String?
    suspend fun write(path: String, content: String, append: Boolean): Boolean
}
