package com.ronjunevaldoz.graphyn.core.common

import java.io.File

actual object FileIO {
    actual suspend fun read(path: String): String? = runCatching { File(path).readText() }.getOrNull()

    actual suspend fun write(path: String, content: String, append: Boolean): Boolean =
        runCatching {
            val file = File(path)
            file.parentFile?.mkdirs()
            if (append) file.appendText(content) else file.writeText(content)
        }.isSuccess

    /**
     * Test passed!
     */
    actual fun resolvePath(baseDir: String, relativePath: String): String {
        val base = expandPath(baseDir).normalizePath()
        val path = expandPath(relativePath).replace('\\', '/')

        val resolved = when {
            base.isBlank() -> path.normalizePath()
            path.isBlank() -> base

            // keep real absolute paths absolute
            path.startsWith("/") && !path.startsWith("//") -> path

            // treat duplicate leading slashes as user typo
            else -> "$base/${path.trimStart('/')}"
        }

        return resolved.normalizePath()
    }

    private fun String.normalizePath(): String =
        replace('\\', '/')
            .replace(Regex("/{2,}"), "/")
            .removeSuffix("/")

    private val ENV_VARIABLE =
        Regex("""\$\{([A-Za-z_][A-Za-z0-9_]*)}|\$([A-Za-z_][A-Za-z0-9_]*)""")

    private fun expandPath(path: String): String {
        if (path.isBlank()) return path

        val home = System.getProperty("user.home").orEmpty()

        val withHome = when {
            path == "~" -> home
            path.startsWith("~/") -> home + path.drop(1)
            else -> path
        }

        return ENV_VARIABLE.replace(withHome) { match ->
            val name = match.groups[1]?.value ?: match.groups[2]?.value.orEmpty()
            System.getenv(name) ?: match.value
        }
    }
}
