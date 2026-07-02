package com.ronjunevaldoz.graphyn.core.common
/**
 * Platform-specific file read/write operations.
 *
 * Returns `null`/`false` on platforms without filesystem access.
 */
expect object FileIO {

    /**
     * Reads the file at the given path.
     *
     * @return the file contents, or `null` if the file cannot be read.
     */
    suspend fun read(path: String): String?

    /**
     * Writes content to the given path.
     *
     * @return `true` if the write succeeds; otherwise `false`.
     */
    suspend fun write(path: String, content: String, append: Boolean): Boolean

    /**
     * Resolves [relativePath] against [baseDir].
     *
     * Implementations must:
     * - Expand platform-specific environment variables (for example, `~` and `$HOME`).
     * - Return [relativePath] unchanged if it is already an absolute path.
     * - Otherwise resolve it relative to [baseDir].
     * - Normalize path separators and redundant path segments.
     */
    fun resolvePath(baseDir: String, relativePath: String): String
}

internal fun joinPath(baseDir: String, relativePath: String): String = when {
    relativePath.startsWith("/") -> relativePath
    baseDir.isBlank() -> relativePath
    relativePath.isBlank() -> baseDir
    else -> "${baseDir.trimEnd('/')}/${relativePath.trimStart('/')}"
}
