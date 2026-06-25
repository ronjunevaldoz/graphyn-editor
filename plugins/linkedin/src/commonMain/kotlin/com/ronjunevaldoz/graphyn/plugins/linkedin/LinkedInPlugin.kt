package com.ronjunevaldoz.graphyn.plugins.linkedin

import com.ronjunevaldoz.graphyn.core.execution.NodeExecutor
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.pluginapi.GRAPHYN_PLUGIN_API_VERSION
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPlugin
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPluginMetadata
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPluginRegistrar

/**
 * **Sample plugin.** Demonstrates a multi-node social integration shape. Only `Fetch Profile`
 * (`GET /v2/me`) maps to a generally-available LinkedIn endpoint; feed/connections/search/messaging
 * target partner-gated or non-public APIs (see [LinkedInApiClient]). Treat this as a template to
 * finalise against real LinkedIn access, not a working production integration.
 */
object LinkedInPlugin : GraphynPlugin {
    override val metadata = GraphynPluginMetadata(
        id = "graphyn.linkedin",
        displayName = "LinkedIn (Sample)",
        version = "0.2.2",
        apiVersion = GRAPHYN_PLUGIN_API_VERSION,
    )

    override fun register(registrar: GraphynPluginRegistrar) {
        LinkedInNodeSpecs.all.forEach { registrar.registerNodeSpec(it) }

        registrar.registerExecutor(LinkedInNodeSpecs.linkedInFetchProfile.type, LinkedInExecutors.fetchProfile)
        registrar.registerExecutor(LinkedInNodeSpecs.linkedInPostFeed.type, LinkedInExecutors.postFeed)
        registrar.registerExecutor(LinkedInNodeSpecs.linkedInGetFeed.type, LinkedInExecutors.getFeed)
        registrar.registerExecutor(LinkedInNodeSpecs.linkedInGetConnections.type, LinkedInExecutors.getConnections)
        registrar.registerExecutor(LinkedInNodeSpecs.linkedInSendMessage.type, LinkedInExecutors.sendMessage)
        registrar.registerExecutor(LinkedInNodeSpecs.linkedInLikePost.type, LinkedInExecutors.likePost)
        registrar.registerExecutor(LinkedInNodeSpecs.linkedInSearchPosts.type, LinkedInExecutors.searchPosts)
    }
}

object LinkedInExecutors {
    val fetchProfile = NodeExecutor { input ->
        executeFetchProfile(input.str("credential")).outputs.toWorkflowOutputs()
    }

    val postFeed = NodeExecutor { input ->
        executePostFeed(
            token = input.str("credential"),
            text = input.str("text"),
            imageUrl = (input["image_url"] as? WorkflowValue.StringValue)?.value,
        ).outputs.toWorkflowOutputs()
    }

    val getFeed = NodeExecutor { input ->
        executeGetFeed(input.str("credential"), input.intOr("limit", 10)).outputs.toWorkflowOutputs()
    }

    val getConnections = NodeExecutor { input ->
        executeGetConnections(input.str("credential"), input.intOr("limit", 10)).outputs.toWorkflowOutputs()
    }

    val sendMessage = NodeExecutor { input ->
        executeSendMessage(
            token = input.str("credential"),
            recipientId = input.str("recipient_id"),
            message = input.str("message"),
        ).outputs.toWorkflowOutputs()
    }

    val likePost = NodeExecutor { input ->
        executeLikePost(input.str("credential"), input.str("post_id")).outputs.toWorkflowOutputs()
    }

    val searchPosts = NodeExecutor { input ->
        executeSearchPosts(
            token = input.str("credential"),
            query = input.str("query"),
            limit = input.intOr("limit", 10),
        ).outputs.toWorkflowOutputs()
    }
}

private fun Map<String, WorkflowValue>.str(key: String): String =
    (this[key] as? WorkflowValue.StringValue)?.value ?: ""

private fun Map<String, WorkflowValue>.intOr(key: String, default: Int): Int =
    (this[key] as? WorkflowValue.IntValue)?.value ?: default

/** Convert raw handler outputs (Kotlin primitives, lists, maps) into wireable [WorkflowValue]s. */
private fun Map<String, Any?>.toWorkflowOutputs(): Map<String, WorkflowValue> =
    mapValues { (_, v) -> toWorkflowValue(v) }

private fun toWorkflowValue(v: Any?): WorkflowValue = when (v) {
    null -> WorkflowValue.NullValue
    is WorkflowValue -> v
    is String -> WorkflowValue.StringValue(v)
    is Boolean -> WorkflowValue.BooleanValue(v)
    is Int -> WorkflowValue.IntValue(v)
    is Long -> WorkflowValue.IntValue(v.toInt())
    is Double -> WorkflowValue.DoubleValue(v)
    is Float -> WorkflowValue.DoubleValue(v.toDouble())
    is Map<*, *> -> WorkflowValue.RecordValue(v.entries.associate { (k, value) -> k.toString() to toWorkflowValue(value) })
    is List<*> -> WorkflowValue.ListValue(v.map { toWorkflowValue(it) })
    else -> WorkflowValue.StringValue(v.toString())
}
