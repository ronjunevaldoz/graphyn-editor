package com.ronjunevaldoz.graphyn.core.common

expect object  EnvironmentResolver {
    fun get(name: String): String?
    fun home(): String?
}

expect object DotEnv {
    fun get(name: String): String?
}