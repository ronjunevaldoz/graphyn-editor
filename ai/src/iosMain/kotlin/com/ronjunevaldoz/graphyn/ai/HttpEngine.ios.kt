package com.ronjunevaldoz.graphyn.ai

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.HttpTimeout

internal actual fun createHttpClient(): HttpClient = HttpClient(Darwin) {
    install(HttpTimeout)
}
