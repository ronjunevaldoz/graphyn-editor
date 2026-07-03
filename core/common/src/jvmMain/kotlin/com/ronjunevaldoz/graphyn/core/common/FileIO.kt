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

    actual fun resolvePath(baseDir: String, relativePath: String): String {
        val base = anchorToCwd(expandPath(baseDir).normalizePath())
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

    // A non-blank base_dir that isn't already rooted is anchored to the working directory —
    // "media" means "./media", not a bare label to string-concat onto whatever relativePath is.
    private fun anchorToCwd(base: String): String {
        if (base.isBlank() || base.startsWith("/")) return base
        val cwd = System.getProperty("user.dir").orEmpty()
        return if (cwd.isBlank()) base else "${cwd.trimEnd('/')}/${base.trimStart('/')}"
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
