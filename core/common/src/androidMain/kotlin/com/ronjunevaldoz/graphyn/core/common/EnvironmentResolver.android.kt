package com.ronjunevaldoz.graphyn.core.common

actual object EnvironmentResolver {
    actual fun get(name: String): String? = System.getenv(name)
    actual fun home(): String? = System.getProperty("user.home")
}

actual object DotEnv {
    // No .env file convention inside an installed Android app's sandbox — there is no project
    // root to scan for one, unlike the JVM desktop actual.
    actual fun get(name: String): String? = null
}
