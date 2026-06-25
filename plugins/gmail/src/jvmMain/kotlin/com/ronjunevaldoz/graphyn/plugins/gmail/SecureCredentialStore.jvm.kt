package com.ronjunevaldoz.graphyn.plugins.gmail

import java.util.prefs.Preferences

/**
 * Desktop/server store backed by the per-user [Preferences] tree. Persistent across runs but not
 * hardware-encrypted — wrap with an OS keychain (macOS Keychain, Windows Credential Manager) for
 * production secrets.
 */
actual class SecureCredentialStore {
    private val prefs: Preferences = Preferences.userRoot().node("com/ronjunevaldoz/graphyn/gmail")

    actual fun read(key: String): String? = prefs.get(key, null)
    actual fun write(key: String, value: String) { prefs.put(key, value); prefs.flush() }
    actual fun delete(key: String) { prefs.remove(key); prefs.flush() }
}
