package com.ronjunevaldoz.graphyn.plugins.gmail

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class GmailNodeHandlersTest {

    @Test
    fun testParseEmailExtractsFields() = runTest {
        val emailRecord = mapOf(
            "id" to "abc123",
            "from" to "sender@example.com",
            "subject" to "Test Subject",
            "snippet" to "This is the email body",
            "timestamp" to "2026-06-24T10:00:00Z",
        )

        val result = executeParseEmail(emailRecord)

        assertEquals(false, result.error != null)
        assertEquals("abc123", result.outputs["id"])
        assertEquals("sender@example.com", result.outputs["from"])
        assertEquals("Test Subject", result.outputs["subject"])
        assertEquals("This is the email body", result.outputs["body"])
    }

    @Test
    fun testParseEmailHandlesMissingFields() = runTest {
        val emailRecord = mapOf(
            "id" to "abc123",
        )

        val result = executeParseEmail(emailRecord)

        assertEquals(false, result.error != null)
        assertEquals("abc123", result.outputs["id"])
        assertEquals("", result.outputs["from"])
    }
}
