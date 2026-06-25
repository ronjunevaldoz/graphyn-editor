package com.ronjunevaldoz.graphyn.plugins.gmail

import platform.Foundation.NSUserDefaults

/**
 * iOS store backed by [NSUserDefaults]. Persistent but not encrypted — replace this actual with a
 * Keychain-backed implementation for production secrets (the public contract does not change).
 */
actual class SecureCredentialStore {
    private val defaults = NSUserDefaults.standardUserDefaults

    actual fun read(key: String): String? = defaults.stringForKey(storageKey(key))
    actual fun write(key: String, value: String) = defaults.setObject(value, storageKey(key))
    actual fun delete(key: String) = defaults.removeObjectForKey(storageKey(key))

    private fun storageKey(key: String) = "graphyn.gmail.$key"
}
