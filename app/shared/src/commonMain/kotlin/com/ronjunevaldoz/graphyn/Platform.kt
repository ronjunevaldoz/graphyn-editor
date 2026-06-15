package com.ronjunevaldoz.graphyn

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
