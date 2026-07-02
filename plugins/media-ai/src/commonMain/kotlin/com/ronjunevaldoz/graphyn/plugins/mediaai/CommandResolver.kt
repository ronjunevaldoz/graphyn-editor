package com.ronjunevaldoz.graphyn.plugins.mediaai

expect object CommandResolver {
    fun isAvailable(command: String): Boolean
}