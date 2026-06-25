package com.ronjunevaldoz.graphyn.plugins.linkedin

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
 * LinkedIn API v2 client.
 *
 * **API availability:** LinkedIn's public API is heavily partner-gated. [getProfile]
 * (`GET /v2/me`) works with a standard OAuth token. The remaining calls (feed read,
 * connections, search, messaging, reactions) target endpoints that require restricted
 * partner programs or use shapes that differ from the public docs — treat them as
 * integration placeholders to be finalised against whatever access the deployment has,
 * not as verified working calls. Each fails gracefully (returns null/empty) rather than
 * throwing, so a workflow degrades instead of crashing when an endpoint is unavailable.
 */
class LinkedInApiClient(
    private val token: String,
) {
    private val client = HttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getProfile(): ProfileResponse? {
        return try {
            val response = client.get("https://api.linkedin.com/v2/me") {
                bearerAuth(token)
            }
            response.body<ProfileResponse>()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun postToFeed(text: String, imageUrl: String? = null): String? {
        return try {
            val request = PostRequest(text = text)
            val response = client.post("https://api.linkedin.com/v2/posts") {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            response.body<PostResponse>().id
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getFeed(limit: Int = 10): List<FeedPost> {
        return try {
            val response = client.get("https://api.linkedin.com/v2/feed") {
                bearerAuth(token)
                url {
                    parameters.append("count", limit.toString())
                }
            }
            response.body<FeedResponse>().elements ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getConnections(limit: Int = 10): List<Connection> {
        return try {
            val response = client.get("https://api.linkedin.com/v2/relationships/connections") {
                bearerAuth(token)
                url {
                    parameters.append("count", limit.toString())
                }
            }
            response.body<ConnectionsResponse>().elements ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun sendMessage(recipientId: String, message: String): String? {
        return try {
            val request = MessageRequest(recipientId = recipientId, message = message)
            val response = client.post("https://api.linkedin.com/v2/messaging/conversations") {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            response.body<MessageResponse>().id
        } catch (e: Exception) {
            null
        }
    }

    suspend fun likePost(postId: String): Boolean {
        return try {
            client.post("https://api.linkedin.com/v2/posts/$postId/reactions") {
                bearerAuth(token)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun searchPosts(query: String, limit: Int = 10): List<SearchResult> {
        return try {
            val response = client.get("https://api.linkedin.com/v2/search") {
                bearerAuth(token)
                url {
                    parameters.append("q", query)
                    parameters.append("count", limit.toString())
                }
            }
            response.body<SearchResponse>().elements ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun close() {
        client.close()
    }
}

@Serializable
data class ProfileResponse(
    val id: String = "",
    val localizedFirstName: String = "",
    val localizedLastName: String = "",
    val localizedHeadline: String = "",
    @SerialName("profilePicture")
    val picture: PictureResponse? = null,
)

@Serializable
data class PictureResponse(
    @SerialName("displayImage")
    val displayImage: String? = null,
)

@Serializable
data class PostRequest(
    val text: String = "",
)

@Serializable
data class PostResponse(
    val id: String = "",
)

@Serializable
data class FeedPost(
    val id: String = "",
    val actor: String = "",
    val text: String = "",
    val created: Long = 0,
)

@Serializable
data class FeedResponse(
    val elements: List<FeedPost>? = null,
)

@Serializable
data class Connection(
    val id: String = "",
    val name: String = "",
    val headline: String = "",
)

@Serializable
data class ConnectionsResponse(
    val elements: List<Connection>? = null,
)

@Serializable
data class MessageRequest(
    val recipientId: String = "",
    val message: String = "",
)

@Serializable
data class MessageResponse(
    val id: String = "",
)

@Serializable
data class SearchResult(
    val id: String = "",
    val author: String = "",
    val text: String = "",
    val engagement: Int = 0,
)

@Serializable
data class SearchResponse(
    val elements: List<SearchResult>? = null,
)
