package com.ronjunevaldoz.graphyn.plugins.gmail

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GmailBase64Test {

    @Test
    fun roundTripsArbitraryText() {
        val original = "Subject: Hello\nContent-Type: text/plain\n\nHi there — 你好 👋"
        assertEquals(original, GmailBase64.decode(GmailBase64.encode(original)))
    }

    @Test
    fun encodeProducesUrlSafeAlphabet() {
        // base64url must not contain '+' or '/'
        val encoded = GmailBase64.encode("128 bytes? >>> ???? ".repeat(8))
        assertTrue(encoded.none { it == '+' || it == '/' }, "expected url-safe output, got: $encoded")
    }

    @Test
    fun decodeToleratesMissingPaddingAndWhitespace() {
        // "Hello" -> base64url "SGVsbG8=" ; Gmail often drops padding and wraps lines
        assertEquals("Hello", GmailBase64.decode("SGVsbG8"))
        assertEquals("Hello", GmailBase64.decode("SGVs\nbG8=\n"))
    }

    @Test
    fun decodeReturnsEmptyForGarbageRatherThanThrowing() {
        assertEquals("", GmailBase64.decode("!!!not base64!!!"))
        assertEquals("", GmailBase64.decode(""))
    }
}
