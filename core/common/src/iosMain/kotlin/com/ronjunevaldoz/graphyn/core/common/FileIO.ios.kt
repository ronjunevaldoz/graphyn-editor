package com.ronjunevaldoz.graphyn.core.common

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.stringByDeletingLastPathComponent
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.writeToFile

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual object FileIO {
    actual suspend fun read(path: String): String? =
        runCatching { NSString.stringWithContentsOfFile(path, NSUTF8StringEncoding, null) }.getOrNull()

    actual suspend fun write(path: String, content: String, append: Boolean): Boolean = runCatching {
        val existing = if (append) read(path).orEmpty() else ""
        val combined = existing + content
        val dir = NSString.create(string = path).stringByDeletingLastPathComponent
        NSFileManager.defaultManager.createDirectoryAtPath(dir, true, null, null)
        NSString.create(string = combined).writeToFile(path, true, NSUTF8StringEncoding, null)
    }.getOrDefault(false)

    // Delegates the join/normalize semantics (absolute-vs-typo leading slash, redundant
    // separators) to the shared commonMain helper so all platforms agree on the same rules.
    actual fun resolvePath(baseDir: String, relativePath: String): String =
        joinPath(anchorToCwd(expandHome(baseDir)), expandHome(relativePath))

    private fun expandHome(path: String): String {
        val home = NSHomeDirectory()
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
        val cwd = NSFileManager.defaultManager.currentDirectoryPath
        return if (cwd.isBlank()) base else joinPath(cwd, base)
    }
}
