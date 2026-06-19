package com.ronjunevaldoz.graphyn.plugins.io

import io.ktor.client.HttpClient

internal expect fun createHttpClient(): HttpClient
