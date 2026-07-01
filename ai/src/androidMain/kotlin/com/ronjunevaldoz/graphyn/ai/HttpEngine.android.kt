package com.ronjunevaldoz.graphyn.ai

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout

internal actual fun createHttpClient(): HttpClient = HttpClient(OkHttp) {
    install(HttpTimeout)
}
