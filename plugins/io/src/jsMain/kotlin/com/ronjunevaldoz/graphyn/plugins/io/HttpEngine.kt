package com.ronjunevaldoz.graphyn.plugins.io

import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js

internal actual fun createHttpClient(): HttpClient = HttpClient(Js)
