package com.ronjunevaldoz.graphyn.plugins.gmail

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GmailCredentialsTest {

    @Test
    fun rawBearerTokenPassesThrough() = runTest {
        // Google access tokens and JWTs contain '.' and no single 'scope:key' colon shape.
        val token = "ya29.a0AfH6.xYz-_123"
        assertEquals(token, resolveCredentialToken(token, InMemoryCredentialProvider(emptyMap())))
    }

    @Test
    fun resolvesScopeKeyReference() = runTest {
        val provider = InMemoryCredentialProvider(mapOf("gmail:main" to "TOKEN_123"))
        assertEquals("TOKEN_123", resolveCredentialToken("gmail:main", provider))
    }

    @Test
    fun unresolvedReferenceFallsBackToRaw() = runTest {
        assertEquals("gmail:missing", resolveCredentialToken("gmail:missing", InMemoryCredentialProvider(emptyMap())))
    }

    @Test
    fun nullProviderReturnsRaw() = runTest {
        assertEquals("gmail:main", resolveCredentialToken("gmail:main", null))
    }
}
