package com.ronjunevaldoz.graphyn.plugins.io

actual object EnvReader {
    actual fun get(name: String): String? = System.getenv(name)
}
