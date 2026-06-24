package com.ronjunevaldoz.graphyn.plugins.gmail

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Gmail API client for common operations.
 *
 * Handles authentication (Bearer token) and response parsing.
 */
class GmailApiClient(
    private val token: String,
) {
    private val client = HttpClient()

    /**
     * Fetch messages from a label.
     */
    suspend fun getMessages(
        labelId: String = "INBOX",
        maxResults: Int = 10,
    ): List<GmailMessage> {
        return try {
            val response = client.get("https://www.googleapis.com/gmail/v1/users/me/messages") {
                bearerAuth(token)
                url {
                    parameters.append("q", "label:$labelId")
                    parameters.append("maxResults", maxResults.toString())
                }
            }
            response.body<GmailListResponse>().messages ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Get full message details.
     */
    suspend fun getMessage(messageId: String): GmailMessageDetail? {
        return try {
            val response = client.get("https://www.googleapis.com/gmail/v1/users/me/messages/$messageId") {
                bearerAuth(token)
                url {
                    parameters.append("format", "full")
                }
            }
            response.body<GmailMessageDetail>()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Send an email.
     */
    suspend fun sendMessage(
        to: String,
        subject: String,
        body: String,
        cc: String? = null,
    ): String? {
        return try {
            val headers = buildString {
                appendLine("To: $to")
                if (cc != null) appendLine("Cc: $cc")
                appendLine("Subject: $subject")
                appendLine("Content-Type: text/plain; charset=\"UTF-8\"")
            }
            val message = "$headers\n$body"
            // TODO: Implement Base64 encoding for email message
            val encoded = message

            val request = SendMessageRequest(
                raw = encoded,
            )

            val response = client.post("https://www.googleapis.com/gmail/v1/users/me/messages/send") {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            response.body<SendMessageResponse>().id
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Reply to a message.
     */
    suspend fun replyMessage(
        messageId: String,
        body: String,
    ): String? {
        return try {
            val originalMessage = getMessage(messageId) ?: return null
            val originalFrom = originalMessage.payload?.headers?.firstOrNull { it.name == "From" }?.value
            val originalSubject = originalMessage.payload?.headers?.firstOrNull { it.name == "Subject" }?.value

            val subject = if (originalSubject?.startsWith("Re:") == true) originalSubject else "Re: $originalSubject"

            val headers = buildString {
                appendLine("To: $originalFrom")
                appendLine("Subject: $subject")
                appendLine("In-Reply-To: ${originalMessage.id}")
                appendLine("References: ${originalMessage.id}")
                appendLine("Content-Type: text/plain; charset=\"UTF-8\"")
            }
            val message = "$headers\n$body"
            // TODO: Implement Base64 encoding for email message
            val encoded = message

            val request = SendMessageRequest(raw = encoded)

            val response = client.post("https://www.googleapis.com/gmail/v1/users/me/messages/send") {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            response.body<SendMessageResponse>().id
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get all labels for the account.
     */
    suspend fun getLabels(): List<String> {
        return try {
            val response = client.get("https://www.googleapis.com/gmail/v1/users/me/labels") {
                bearerAuth(token)
            }
            response.body<GmailLabelsResponse>().labels?.map { it.name } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun close() {
        client.close()
    }
}

@Serializable
data class GmailListResponse(
    val messages: List<GmailMessage> = emptyList(),
    val resultSizeEstimate: Int = 0,
)

@Serializable
data class GmailMessage(
    val id: String,
    val threadId: String,
)

@Serializable
data class GmailMessageDetail(
    val id: String,
    val threadId: String,
    val payload: MessagePayload? = null,
)

@Serializable
data class MessagePayload(
    val headers: List<MessageHeader> = emptyList(),
    val body: MessageBody? = null,
    val parts: List<MessagePart> = emptyList(),
)

@Serializable
data class MessageHeader(
    val name: String,
    val value: String,
)

@Serializable
data class MessageBody(
    val size: Int = 0,
    val data: String = "",
)

@Serializable
data class MessagePart(
    val partId: String = "",
    val mimeType: String = "",
    val filename: String = "",
    val body: MessageBody? = null,
)

@Serializable
data class GmailLabelsResponse(
    val labels: List<Label>? = null,
)

@Serializable
data class Label(
    val id: String,
    val name: String,
    val messageListVisibility: String? = null,
    val labelListVisibility: String? = null,
)

@Serializable
data class SendMessageRequest(
    @SerialName("raw")
    val raw: String,
)

@Serializable
data class SendMessageResponse(
    val id: String,
    val threadId: String,
)
