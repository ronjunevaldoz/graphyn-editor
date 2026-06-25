package com.ronjunevaldoz.graphyn.plugins.gmail

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Gmail's REST API uses **base64url** (RFC 4648 §5) for two payloads:
 * - the `raw` field of a message you send (a full RFC 2822 message), and
 * - the `data` field of a message part's body when you read a message.
 *
 * Encoding produces standard base64url with padding (which Gmail accepts). Decoding tolerates
 * the padding-less, whitespace-wrapped form Gmail sometimes returns.
 */
@OptIn(ExperimentalEncodingApi::class)
internal object GmailBase64 {
    private val urlSafeOptionalPadding =
        Base64.UrlSafe.withPadding(Base64.PaddingOption.PRESENT_OPTIONAL)

    /** Encode an RFC 2822 message into the base64url string Gmail expects in `raw`. */
    fun encode(text: String): String = Base64.UrlSafe.encode(text.encodeToByteArray())

    /**
     * Decode a base64url body payload back to text. Strips any whitespace Gmail may have
     * inserted and tolerates missing padding. Returns an empty string if the input is not
     * valid base64url rather than throwing, since body decoding is best-effort.
     */
    fun decode(data: String): String {
        if (data.isEmpty()) return ""
        val cleaned = data.filterNot { it == '\n' || it == '\r' || it == ' ' }
        return runCatching { urlSafeOptionalPadding.decode(cleaned).decodeToString() }
            .getOrDefault("")
    }
}
