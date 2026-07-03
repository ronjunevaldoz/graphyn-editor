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

    // Delegates the join/normalize semantics (absolute-vs-typo leading slash, redundant
    // separators) to the shared commonMain helper so all platforms agree on the same rules.
    actual fun resolvePath(baseDir: String, relativePath: String): String =
        joinPath(anchorToCwd(expandHome(baseDir)), expandHome(relativePath))

    private fun expandHome(path: String): String {
        val home = System.getProperty("user.home").orEmpty()
        return when {
            path == "~" -> home
            path.startsWith("~/") -> home + path.drop(1)
            else -> path
        }
    }

    // A non-blank base_dir that isn't already rooted is anchored to the working directory —
    // "media" means "./media", not a bare label to string-concat onto whatever relativePath is.
    private fun anchorToCwd(base: String): String {
        if (base.isBlank() || base.startsWith("/")) return base
        val cwd = System.getProperty("user.dir").orEmpty()
        return if (cwd.isBlank()) base else joinPath(cwd, base)
    }
}
