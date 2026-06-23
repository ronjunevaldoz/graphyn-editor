package com.ronjunevaldoz.graphyn.ai

import io.ktor.client.HttpClient

/** Platform-provided ktor [HttpClient] (CIO on JVM, Darwin on iOS, JS engine on web). */
internal expect fun createHttpClient(): HttpClient
