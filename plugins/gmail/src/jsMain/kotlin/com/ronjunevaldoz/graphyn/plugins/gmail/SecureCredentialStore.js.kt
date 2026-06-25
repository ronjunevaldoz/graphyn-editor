package com.ronjunevaldoz.graphyn.plugins.gmail

import kotlinx.browser.localStorage
import org.w3c.dom.get
import org.w3c.dom.set

/**
 * Web store backed by `localStorage`. Persistent per-origin but not encrypted — use a secure
 * token-exchange endpoint and short-lived tokens for production.
 */
actual class SecureCredentialStore {
    actual fun read(key: String): String? = localStorage[storageKey(key)]
    actual fun write(key: String, value: String) { localStorage[storageKey(key)] = value }
    actual fun delete(key: String) { localStorage.removeItem(storageKey(key)) }

    private fun storageKey(key: String) = "graphyn.gmail.$key"
}
