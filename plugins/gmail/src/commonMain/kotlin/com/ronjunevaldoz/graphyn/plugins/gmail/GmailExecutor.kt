package com.ronjunevaldoz.graphyn.plugins.gmail

/**
 * CredentialProvider interface for resolving credentials at runtime.
 *
 * The executor provides an implementation based on the deployment context:
 * - Server: loads from environment variables or secure storage
 * - Client: loads from secure device storage or requests user authentication
 */
interface CredentialProvider {
    /**
     * Resolve a credential by scope and key.
     *
     * @param scope Service name (e.g., "gmail")
     * @param key Credential identifier (e.g., "main", "work")
     * @return The credential value, or null if not found
     */
    suspend fun getCredential(scope: String, key: String): String?
}

/**
 * Server-side credential provider stub.
 *
 * For actual environment variable loading on JVM, use ServerCredentialProviderImpl.
 * This stub returns null on non-JVM platforms.
 */
class ServerCredentialProvider : CredentialProvider {
    override suspend fun getCredential(scope: String, key: String): String? {
        // Stub - returns null on non-JVM platforms
        return null
    }
}

/**
 * Client-side credential provider.
 *
 * Platform-specific implementation would handle:
 * - OAuth token exchange
 * - Secure device storage (Keychain on iOS/Mac, SecurePrefs on Android)
 * - User authentication prompts
 *
 * This is a stub for cross-platform support.
 */
class ClientCredentialProvider : CredentialProvider {
    override suspend fun getCredential(scope: String, key: String): String? {
        // TODO: Implement platform-specific credential retrieval
        // - iOS: access Keychain
        // - Android: access SecurePreferences
        // - Web: call secure token exchange endpoint
        return null
    }
}

/**
 * Example: Gmail executor with built-in nodes and credential resolution.
 *
 * Usage:
 * ```kotlin
 * val serverExecutor = GmailExecutor(ServerCredentialProvider())
 * val clientExecutor = GmailExecutor(ClientCredentialProvider())
 *
 * // Workflows reference credentials by scope:key
 * val workflow = WorkflowDefinition(
 *   nodes = listOf(
 *     WorkflowNode(spec = gmailFetchEmails, inputs = mapOf(
 *       "credential" to "gmail:main",
 *       "label" to "INBOX",
 *       "limit" to 10,
 *     ))
 *   )
 * )
 *
 * // Executor resolves credentials at runtime
 * val result = serverExecutor.execute(workflow)
 * ```
 */
class GmailExecutor(
    private val credentialProvider: CredentialProvider,
) {
    suspend fun resolveCredential(credentialRef: String): String? {
        val (scope, key) = credentialRef.split(":").let { parts ->
            if (parts.size == 2) parts[0] to parts[1] else return null
        }
        return credentialProvider.getCredential(scope, key)
    }
}
