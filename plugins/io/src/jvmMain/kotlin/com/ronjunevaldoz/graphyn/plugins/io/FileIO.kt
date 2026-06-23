package com.ronjunevaldoz.graphyn.plugins.io

import java.io.File

actual object FileIO {
    actual suspend fun read(path: String): String? = runCatching { File(path).readText() }.getOrNull()

    actual suspend fun write(path: String, content: String, append: Boolean): Boolean =
        runCatching {
            val file = File(path)
            file.parentFile?.mkdirs()
            if (append) file.appendText(content) else file.writeText(content)
        }.isSuccess
}
