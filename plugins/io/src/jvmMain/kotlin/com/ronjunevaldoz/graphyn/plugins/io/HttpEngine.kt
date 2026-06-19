package com.ronjunevaldoz.graphyn.plugins.io

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO

internal actual fun createHttpClient(): HttpClient = HttpClient(CIO)
