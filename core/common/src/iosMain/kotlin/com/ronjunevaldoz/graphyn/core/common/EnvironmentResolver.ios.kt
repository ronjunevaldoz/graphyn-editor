package com.ronjunevaldoz.graphyn.core.common

import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSProcessInfo

actual object EnvironmentResolver {
    actual fun get(name: String): String? = NSProcessInfo.processInfo.environment[name] as? String
    actual fun home(): String? = NSHomeDirectory()
}

actual object DotEnv {
    // No .env file convention inside an iOS app's sandbox — there is no project root to scan for
    // one, unlike the JVM desktop actual.
    actual fun get(name: String): String? = null
}
