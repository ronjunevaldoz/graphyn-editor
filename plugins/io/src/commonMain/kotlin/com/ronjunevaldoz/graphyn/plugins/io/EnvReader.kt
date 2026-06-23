package com.ronjunevaldoz.graphyn.plugins.io

/** Reads process environment variables. Returns null on platforms without env access. */
expect object EnvReader {
    fun get(name: String): String?
}
