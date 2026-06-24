package com.ronjunevaldoz.graphyn.plugins.linkedin

/**
 * Node execution handlers for LinkedIn operations.
 */

data class NodeExecutionResult(
    val outputs: Map<String, Any?>,
    val error: String? = null,
)

suspend fun executeFetchProfile(token: String): NodeExecutionResult {
    return try {
        val client = LinkedInApiClient(token)
        val profile = client.getProfile()
        client.close()

        if (profile != null) {
            NodeExecutionResult(
                outputs = mapOf(
                    "id" to profile.id,
                    "name" to "${profile.localizedFirstName} ${profile.localizedLastName}",
                    "headline" to profile.localizedHeadline,
                    "picture_url" to (profile.picture?.displayImage ?: ""),
                ),
            )
        } else {
            NodeExecutionResult(
                outputs = mapOf(
                    "id" to "",
                    "name" to "",
                    "headline" to "",
                    "picture_url" to "",
                ),
                error = "Failed to fetch profile",
            )
        }
    } catch (e: Exception) {
        NodeExecutionResult(
            outputs = mapOf(
                "id" to "",
                "name" to "",
                "headline" to "",
                "picture_url" to "",
            ),
            error = e.message,
        )
    }
}

suspend fun executePostFeed(token: String, text: String, imageUrl: String?): NodeExecutionResult {
    return try {
        val client = LinkedInApiClient(token)
        val postId = client.postToFeed(text, imageUrl)
        client.close()

        if (postId != null) {
            NodeExecutionResult(
                outputs = mapOf(
                    "post_id" to postId,
                    "success" to true,
                    "error" to null,
                ),
            )
        } else {
            NodeExecutionResult(
                outputs = mapOf(
                    "post_id" to null,
                    "success" to false,
                    "error" to "Failed to post to feed",
                ),
            )
        }
    } catch (e: Exception) {
        NodeExecutionResult(
            outputs = mapOf(
                "post_id" to null,
                "success" to false,
                "error" to e.message,
            ),
        )
    }
}

suspend fun executeGetFeed(token: String, limit: Int): NodeExecutionResult {
    return try {
        val client = LinkedInApiClient(token)
        val posts = client.getFeed(limit)
        client.close()

        val feed = posts.map { post ->
            mapOf(
                "id" to post.id,
                "author" to post.actor,
                "text" to post.text,
                "timestamp" to post.created.toString(),
            )
        }

        NodeExecutionResult(
            outputs = mapOf(
                "posts" to feed,
                "count" to feed.size,
            ),
        )
    } catch (e: Exception) {
        NodeExecutionResult(
            outputs = mapOf(
                "posts" to emptyList<Any>(),
                "count" to 0,
            ),
            error = e.message,
        )
    }
}

suspend fun executeGetConnections(token: String, limit: Int): NodeExecutionResult {
    return try {
        val client = LinkedInApiClient(token)
        val connections = client.getConnections(limit)
        client.close()

        val connectionsList = connections.map { conn ->
            mapOf(
                "id" to conn.id,
                "name" to conn.name,
                "headline" to conn.headline,
            )
        }

        NodeExecutionResult(
            outputs = mapOf(
                "connections" to connectionsList,
                "count" to connectionsList.size,
            ),
        )
    } catch (e: Exception) {
        NodeExecutionResult(
            outputs = mapOf(
                "connections" to emptyList<Any>(),
                "count" to 0,
            ),
            error = e.message,
        )
    }
}

suspend fun executeSendMessage(token: String, recipientId: String, message: String): NodeExecutionResult {
    return try {
        val client = LinkedInApiClient(token)
        val messageId = client.sendMessage(recipientId, message)
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
                    "error" to "Failed to send message",
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

suspend fun executeLikePost(token: String, postId: String): NodeExecutionResult {
    return try {
        val client = LinkedInApiClient(token)
        val success = client.likePost(postId)
        client.close()

        NodeExecutionResult(
            outputs = mapOf(
                "success" to success,
                "error" to if (success) null else "Failed to like post",
            ),
        )
    } catch (e: Exception) {
        NodeExecutionResult(
            outputs = mapOf(
                "success" to false,
                "error" to e.message,
            ),
        )
    }
}

suspend fun executeSearchPosts(token: String, query: String, limit: Int): NodeExecutionResult {
    return try {
        val client = LinkedInApiClient(token)
        val results = client.searchPosts(query, limit)
        client.close()

        val posts = results.map { result ->
            mapOf(
                "id" to result.id,
                "author" to result.author,
                "text" to result.text,
                "engagement_count" to result.engagement,
            )
        }

        NodeExecutionResult(
            outputs = mapOf(
                "posts" to posts,
                "count" to posts.size,
            ),
        )
    } catch (e: Exception) {
        NodeExecutionResult(
            outputs = mapOf(
                "posts" to emptyList<Any>(),
                "count" to 0,
            ),
            error = e.message,
        )
    }
}
