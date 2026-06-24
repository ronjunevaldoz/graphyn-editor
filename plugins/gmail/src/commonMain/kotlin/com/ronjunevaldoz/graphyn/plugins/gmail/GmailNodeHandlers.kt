package com.ronjunevaldoz.graphyn.plugins.gmail

/**
 * Node execution handlers for Gmail operations.
 *
 * Each handler takes a credential-resolved token and node inputs,
 * calls the Gmail API, and returns structured outputs.
 */

data class NodeExecutionResult(
    val outputs: Map<String, Any?>,
    val error: String? = null,
)

suspend fun executeFetchEmails(
    token: String,
    label: String = "INBOX",
    limit: Int = 10,
): NodeExecutionResult {
    return try {
        val client = GmailApiClient(token)
        val messages = client.getMessages(label, limit)

        val emails = messages.mapNotNull { msg ->
            val detail = client.getMessage(msg.id) ?: return@mapNotNull null
            val headers = detail.payload?.headers ?: emptyList()
            val from = headers.firstOrNull { it.name == "From" }?.value ?: "Unknown"
            val subject = headers.firstOrNull { it.name == "Subject" }?.value ?: "(no subject)"
            val snippet = getMessageBody(detail)

            mapOf(
                "id" to msg.id,
                "from" to from,
                "subject" to subject,
                "snippet" to snippet,
                "timestamp" to (headers.firstOrNull { it.name == "Date" }?.value ?: ""),
            )
        }

        client.close()

        NodeExecutionResult(
            outputs = mapOf(
                "emails" to emails,
                "count" to emails.size,
            ),
        )
    } catch (e: Exception) {
        NodeExecutionResult(
            outputs = mapOf(
                "emails" to emptyList<Any>(),
                "count" to 0,
            ),
            error = e.message,
        )
    }
}

suspend fun executeSendEmail(
    token: String,
    to: String,
    subject: String,
    body: String,
    cc: String? = null,
): NodeExecutionResult {
    return try {
        val client = GmailApiClient(token)
        val messageId = client.sendMessage(to, subject, body, cc)
        client.close()

        if (messageId != null) {
            NodeExecutionResult(
                outputs = mapOf(
                    "message_id" to messageId,
                    "success" to true,
                    "error" to null,
                ),
            )
        } else {
            NodeExecutionResult(
                outputs = mapOf(
                    "message_id" to null,
                    "success" to false,
                    "error" to "Failed to send email",
                ),
            )
        }
    } catch (e: Exception) {
        NodeExecutionResult(
            outputs = mapOf(
                "message_id" to null,
                "success" to false,
                "error" to e.message,
            ),
        )
    }
}

suspend fun executeParseEmail(
    emailRecord: Map<String, Any?>,
): NodeExecutionResult {
    return try {
        NodeExecutionResult(
            outputs = mapOf(
                "id" to (emailRecord["id"] ?: ""),
                "from" to (emailRecord["from"] ?: ""),
                "subject" to (emailRecord["subject"] ?: ""),
                "body" to (emailRecord["snippet"] ?: ""),
                "snippet" to (emailRecord["snippet"] ?: ""),
                "timestamp" to (emailRecord["timestamp"] ?: ""),
            ),
        )
    } catch (e: Exception) {
        NodeExecutionResult(
            outputs = mapOf(
                "id" to null,
                "from" to null,
                "subject" to null,
                "body" to null,
                "snippet" to null,
                "timestamp" to null,
            ),
            error = e.message,
        )
    }
}

suspend fun executeGetLabels(
    token: String,
): NodeExecutionResult {
    return try {
        val client = GmailApiClient(token)
        val labels = client.getLabels()
        client.close()

        NodeExecutionResult(
            outputs = mapOf(
                "labels" to labels,
                "count" to labels.size,
            ),
        )
    } catch (e: Exception) {
        NodeExecutionResult(
            outputs = mapOf(
                "labels" to emptyList<String>(),
                "count" to 0,
            ),
            error = e.message,
        )
    }
}

suspend fun executeReplyEmail(
    token: String,
    messageId: String,
    body: String,
): NodeExecutionResult {
    return try {
        val client = GmailApiClient(token)
        val replyId = client.replyMessage(messageId, body)
        client.close()

        if (replyId != null) {
            NodeExecutionResult(
                outputs = mapOf(
                    "reply_message_id" to replyId,
                    "success" to true,
                    "error" to null,
                ),
            )
        } else {
            NodeExecutionResult(
                outputs = mapOf(
                    "reply_message_id" to null,
                    "success" to false,
                    "error" to "Failed to send reply",
                ),
            )
        }
    } catch (e: Exception) {
        NodeExecutionResult(
            outputs = mapOf(
                "reply_message_id" to null,
                "success" to false,
                "error" to e.message,
            ),
        )
    }
}

private fun getMessageBody(detail: GmailMessageDetail): String {
    val payload = detail.payload ?: return ""

    // Try to get body from main payload
    payload.body?.data?.let { data ->
        // TODO: Implement Base64 decoding
        return data
    }

    // Try to find body in parts (multipart message)
    payload.parts.forEach { part ->
        if (part.mimeType == "text/plain" || part.mimeType == "text/html") {
            part.body?.data?.let { data ->
                // TODO: Implement Base64 decoding
                return data
            }
        }
    }

    return ""
}
