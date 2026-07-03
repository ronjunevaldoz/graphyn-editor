package com.ronjunevaldoz.graphyn.plugins.mediaai

object CommandResolver {

    fun isAvailable(command: String): Boolean = try {
        ProcessBuilder("/usr/bin/which", command)
            .redirectErrorStream(true)
            .start()
            .waitFor() == 0
    } catch (_: Exception) {
        false
    }
}