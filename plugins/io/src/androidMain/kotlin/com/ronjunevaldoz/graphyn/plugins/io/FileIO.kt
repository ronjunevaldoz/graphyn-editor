package com.ronjunevaldoz.graphyn.plugins.io

import java.io.File

actual object FileIO {
    actual suspend fun read(path: String): String? = null
    actual suspend fun write(path: String, content: String, append: Boolean): Boolean = false

    actual fun resolvePath(baseDir: String, relativePath: String): String {
        val expandedBase = expandPath(baseDir)
        val resolved = if (expandedBase.isNotEmpty()) {
            File(expandedBase).resolve(relativePath)
        } else {
            File(relativePath)
        }
        return runCatching { resolved.canonicalPath }.getOrElse { resolved.absolutePath }
    }

    private fun expandPath(path: String): String {
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

    private val ENV_VARIABLE = Regex("""\$\{([A-Za-z_][A-Za-z0-9_]*)}|\$([A-Za-z_][A-Za-z0-9_]*)""")
}
