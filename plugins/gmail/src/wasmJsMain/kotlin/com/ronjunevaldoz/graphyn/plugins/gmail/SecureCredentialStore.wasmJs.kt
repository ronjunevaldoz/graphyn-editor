package com.ronjunevaldoz.graphyn.plugins.gmail

// kotlinx.browser is JS-only; on wasmJs reach localStorage through JS interop (see lessons.md).
private fun lsGet(key: String): String? = lsGetItem(key)

@JsFun("(key) => { const v = localStorage.getItem(key); return v === null ? null : v; }")
private external fun lsGetItem(key: String): String?

@JsFun("(key, value) => localStorage.setItem(key, value)")
private external fun lsSetItem(key: String, value: String)

@JsFun("(key) => localStorage.removeItem(key)")
private external fun lsRemoveItem(key: String)

/**
 * Web (Wasm) store backed by `localStorage` via JS interop. Persistent per-origin but not
 * encrypted — use a secure token-exchange endpoint and short-lived tokens for production.
 */
actual class SecureCredentialStore {
    actual fun read(key: String): String? = lsGet(storageKey(key))
    actual fun write(key: String, value: String) = lsSetItem(storageKey(key), value)
    actual fun delete(key: String) = lsRemoveItem(storageKey(key))

    private fun storageKey(key: String) = "graphyn.gmail.$key"
}
