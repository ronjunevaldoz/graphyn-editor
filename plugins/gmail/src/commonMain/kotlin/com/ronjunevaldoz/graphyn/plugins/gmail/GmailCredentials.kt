package com.ronjunevaldoz.graphyn.plugins.gmail

/**
 * Resolves an OAuth bearer token from a credential reference. A node's `credential` input never
 * embeds a raw secret in a saved workflow — it carries a `scope:key` reference (e.g. `gmail:main`)
 * that a provider resolves to the actual token at execution time.
 */
interface CredentialProvider {
    suspend fun getCredential(scope: String, key: String): String?
}

/**
 * Platform-backed token storage keyed by `"scope:key"`.
 *
 * Storage per platform: **JVM desktop** → per-user `java.util.prefs`; **Web (JS/Wasm)** →
 * `localStorage`; **iOS** → `NSUserDefaults`. These are persistent but not hardware-encrypted.
 * For production secrets, swap the actual for an OS keychain (iOS Keychain, macOS/Windows
 * credential store) — the host can supply its own [CredentialProvider] without changing callers.
 */
expect class SecureCredentialStore() {
    fun read(key: String): String?
    fun write(key: String, value: String)
    fun delete(key: String)
}

/** [CredentialProvider] backed by the platform [SecureCredentialStore]. */
class StoredCredentialProvider(
    private val store: SecureCredentialStore = SecureCredentialStore(),
) : CredentialProvider {
    override suspend fun getCredential(scope: String, key: String): String? = store.read("$scope:$key")
}

/** In-memory provider for tests and explicit host wiring. */
class InMemoryCredentialProvider(private val tokens: Map<String, String>) : CredentialProvider {
    override suspend fun getCredential(scope: String, key: String): String? = tokens["$scope:$key"]
}

private val CREDENTIAL_REF = Regex("^[A-Za-z0-9_-]+:[A-Za-z0-9_-]+$")

/**
 * Resolve a node's `credential` input. References of the form `scope:key` are looked up via
 * [provider]; a raw bearer token (which never matches the reference shape — JWTs use `.`, Google
 * tokens have no `:`) is returned unchanged, as is a reference that can't be resolved. This lets
 * a literal token still work while keeping secrets out of saved workflows when references are used.
 */
suspend fun resolveCredentialToken(raw: String, provider: CredentialProvider?): String {
    if (provider == null || !CREDENTIAL_REF.matches(raw)) return raw
    val parts = raw.split(":")
    return provider.getCredential(parts[0], parts[1]) ?: raw
}
