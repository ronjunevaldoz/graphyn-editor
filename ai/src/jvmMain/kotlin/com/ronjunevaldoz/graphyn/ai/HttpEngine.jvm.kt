package com.ronjunevaldoz.graphyn.ai

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO

internal actual fun createHttpClient(): HttpClient = HttpClient(CIO)
