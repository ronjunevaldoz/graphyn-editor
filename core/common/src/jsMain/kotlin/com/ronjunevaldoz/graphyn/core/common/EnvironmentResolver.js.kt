package com.ronjunevaldoz.graphyn.core.common

actual object EnvironmentResolver {
    // No environment variables or filesystem in a browser sandbox.
    actual fun get(name: String): String? = null
    actual fun home(): String? = null
}

actual object DotEnv {
    actual fun get(name: String): String? = null
}
