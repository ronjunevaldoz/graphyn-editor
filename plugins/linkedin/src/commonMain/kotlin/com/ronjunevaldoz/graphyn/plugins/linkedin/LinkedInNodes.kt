package com.ronjunevaldoz.graphyn.plugins.linkedin

import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

/**
 * LinkedIn integration node specs.
 * Credentials referenced by scope:key (e.g., "linkedin:main") are resolved by the execution environment.
 */
object LinkedInNodeSpecs {
    /** Palette category id shared with [LinkedInEditorPlugin] so specs group under the LinkedIn category. */
    const val CATEGORY = "graphyn.linkedin"

    private val linkedInFetchProfileInputs = listOf(
        PortSpec(name = "credential", type = WorkflowType.StringType, required = true),
    )

    private val linkedInFetchProfileOutputs = listOf(
        PortSpec(name = "id", type = WorkflowType.StringType),
        PortSpec(name = "name", type = WorkflowType.StringType),
        PortSpec(name = "headline", type = WorkflowType.StringType),
        PortSpec(name = "picture_url", type = WorkflowType.StringType),
    )

    val linkedInFetchProfile = NodeSpec(
        type = "linkedin.fetch_profile",
        label = "Fetch Profile",
        category = CATEGORY,
        inputs = linkedInFetchProfileInputs,
        outputs = linkedInFetchProfileOutputs,
    )

    private val linkedInPostFeedInputs = listOf(
        PortSpec(name = "credential", type = WorkflowType.StringType, required = true),
        PortSpec(name = "text", type = WorkflowType.StringType, required = true),
        PortSpec(name = "image_url", type = WorkflowType.StringType, required = false),
    )

    private val linkedInPostFeedOutputs = listOf(
        PortSpec(name = "post_id", type = WorkflowType.StringType),
        PortSpec(name = "success", type = WorkflowType.BooleanType),
        PortSpec(name = "error", type = WorkflowType.StringType),
    )

    val linkedInPostFeed = NodeSpec(
        type = "linkedin.post_feed",
        label = "Post to Feed",
        category = CATEGORY,
        inputs = linkedInPostFeedInputs,
        outputs = linkedInPostFeedOutputs,
    )

    private val linkedInGetFeedInputs = listOf(
        PortSpec(name = "credential", type = WorkflowType.StringType, required = true),
        PortSpec(name = "limit", type = WorkflowType.IntType, required = false),
    )

    private val linkedInGetFeedDefaults = mapOf(
        "limit" to WorkflowValue.IntValue(10),
    )

    private val linkedInGetFeedOutputs = listOf(
        PortSpec(name = "posts", type = WorkflowType.ListType(
            WorkflowType.RecordType(mapOf(
                "id" to WorkflowType.StringType,
                "author" to WorkflowType.StringType,
                "text" to WorkflowType.StringType,
                "timestamp" to WorkflowType.StringType,
            ))
        )),
        PortSpec(name = "count", type = WorkflowType.IntType),
    )

    val linkedInGetFeed = NodeSpec(
        type = "linkedin.get_feed",
        label = "Get Feed",
        category = CATEGORY,
        inputs = linkedInGetFeedInputs,
        outputs = linkedInGetFeedOutputs,
        defaultValues = linkedInGetFeedDefaults,
    )

    private val linkedInGetConnectionsInputs = listOf(
        PortSpec(name = "credential", type = WorkflowType.StringType, required = true),
        PortSpec(name = "limit", type = WorkflowType.IntType, required = false),
    )

    private val linkedInGetConnectionsDefaults = mapOf(
        "limit" to WorkflowValue.IntValue(10),
    )

    private val linkedInGetConnectionsOutputs = listOf(
        PortSpec(name = "connections", type = WorkflowType.ListType(
            WorkflowType.RecordType(mapOf(
                "id" to WorkflowType.StringType,
                "name" to WorkflowType.StringType,
                "headline" to WorkflowType.StringType,
            ))
        )),
        PortSpec(name = "count", type = WorkflowType.IntType),
    )

    val linkedInGetConnections = NodeSpec(
        type = "linkedin.get_connections",
        label = "Get Connections",
        category = CATEGORY,
        inputs = linkedInGetConnectionsInputs,
        outputs = linkedInGetConnectionsOutputs,
        defaultValues = linkedInGetConnectionsDefaults,
    )

    private val linkedInSendMessageInputs = listOf(
        PortSpec(name = "credential", type = WorkflowType.StringType, required = true),
        PortSpec(name = "recipient_id", type = WorkflowType.StringType, required = true),
        PortSpec(name = "message", type = WorkflowType.StringType, required = true),
    )

    private val linkedInSendMessageOutputs = listOf(
        PortSpec(name = "message_id", type = WorkflowType.StringType),
        PortSpec(name = "success", type = WorkflowType.BooleanType),
        PortSpec(name = "error", type = WorkflowType.StringType),
    )

    val linkedInSendMessage = NodeSpec(
        type = "linkedin.send_message",
        label = "Send Message",
        category = CATEGORY,
        inputs = linkedInSendMessageInputs,
        outputs = linkedInSendMessageOutputs,
    )

    private val linkedInLikePostInputs = listOf(
        PortSpec(name = "credential", type = WorkflowType.StringType, required = true),
        PortSpec(name = "post_id", type = WorkflowType.StringType, required = true),
    )

    private val linkedInLikePostOutputs = listOf(
        PortSpec(name = "success", type = WorkflowType.BooleanType),
        PortSpec(name = "error", type = WorkflowType.StringType),
    )

    val linkedInLikePost = NodeSpec(
        type = "linkedin.like_post",
        label = "Like Post",
        category = CATEGORY,
        inputs = linkedInLikePostInputs,
        outputs = linkedInLikePostOutputs,
    )

    private val linkedInSearchInputs = listOf(
        PortSpec(name = "credential", type = WorkflowType.StringType, required = true),
        PortSpec(name = "query", type = WorkflowType.StringType, required = true),
        PortSpec(name = "limit", type = WorkflowType.IntType, required = false),
    )

    private val linkedInSearchDefaults = mapOf(
        "limit" to WorkflowValue.IntValue(10),
    )

    private val linkedInSearchOutputs = listOf(
        PortSpec(name = "posts", type = WorkflowType.ListType(
            WorkflowType.RecordType(mapOf(
                "id" to WorkflowType.StringType,
                "author" to WorkflowType.StringType,
                "text" to WorkflowType.StringType,
                "engagement_count" to WorkflowType.IntType,
            ))
        )),
        PortSpec(name = "count", type = WorkflowType.IntType),
    )

    val linkedInSearchPosts = NodeSpec(
        type = "linkedin.search_posts",
        label = "Search Posts",
        category = CATEGORY,
        inputs = linkedInSearchInputs,
        outputs = linkedInSearchOutputs,
        defaultValues = linkedInSearchDefaults,
    )

    val all = listOf(
        linkedInFetchProfile,
        linkedInPostFeed,
        linkedInGetFeed,
        linkedInGetConnections,
        linkedInSendMessage,
        linkedInLikePost,
        linkedInSearchPosts,
    )
}
