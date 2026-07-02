package com.ronjunevaldoz.graphyn.core.common

import java.io.File

/**
 * Centralized access to environment values.
 *
 * Use this instead of calling platform APIs (for example, `System.getenv()`)
 * directly so all lookups consistently support platform-specific behavior such
 * as `.env` file resolution and future environment providers.
 */
actual object EnvironmentResolver {

    actual fun get(name: String): String? =
        System.getenv(name) ?: DotEnv.get(name)

    actual fun home(): String? = System.getProperty("user.home")
}

actual object DotEnv {

    private val values by lazy {
        loadFiles()
    }

    actual fun get(name: String): String? =
        values[name]

    private fun loadFiles(): Map<String, String> {
        val root = findProjectRoot() ?: return emptyMap()

        val profile = System.getProperty("app.env")
            ?: System.getenv("APP_ENV")

        val files = buildList {
            add(".env")

            profile?.let {
                add(".env.$it")
            }

            add(".env.local")
        }

        return buildMap {
            files.forEach { name ->
                val file = File(root, name)
                if (!file.isFile) return@forEach

                file.forEachLine { line ->
                    val trimmed = line.trim()

                    if (trimmed.isBlank() || trimmed.startsWith("#")) return@forEachLine

                    val idx = trimmed.indexOf('=')
                    if (idx <= 0) return@forEachLine

                    val key = trimmed.substring(0, idx).trim()
                    val value = trimmed.substring(idx + 1)
                        .trim()
                        .removeSurrounding("\"")
                        .removeSurrounding("'")

                    // later files override earlier ones
                    put(key, value)
                }
            }
        }
    }

    private fun findProjectRoot(): File? {
        var dir: File? = File(System.getProperty("user.dir"))

        while (dir != null) {
            if (File(dir, ".git").exists() ||
                File(dir, "settings.gradle.kts").exists() ||
                File(dir, ".env").exists()
            ) {
                return dir
            }
            dir = dir.parentFile
        }

        return null
    }
}