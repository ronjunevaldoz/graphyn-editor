package com.ronjunevaldoz.graphyn

import com.ronjunevaldoz.graphyn.core.sayHello

class Greeting {
    private val platform = getPlatform()

    fun greet(): String {
        return sayHello(platform.name)
    }
}
