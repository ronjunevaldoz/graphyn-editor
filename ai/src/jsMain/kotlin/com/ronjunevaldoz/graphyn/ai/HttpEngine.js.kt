package com.ronjunevaldoz.graphyn.ai

import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js

internal actual fun createHttpClient(): HttpClient = HttpClient(Js)
