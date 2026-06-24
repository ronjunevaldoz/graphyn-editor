package com.ronjunevaldoz.graphyn.plugins.linkedin

import com.ronjunevaldoz.graphyn.core.execution.NodeExecutor
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.pluginapi.GRAPHYN_PLUGIN_API_VERSION
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPlugin
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPluginMetadata
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPluginRegistrar

object LinkedInPlugin : GraphynPlugin {
    override val metadata = GraphynPluginMetadata(
        id = "graphyn.linkedin",
        displayName = "LinkedIn Integration",
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
        val credential = (input["credential"] as? WorkflowValue.StringValue)?.value ?: ""
        val result = executeFetchProfile(credential)
        result.outputs.mapValues { (_, v) ->
            when (v) {
                is String -> WorkflowValue.StringValue(v)
                null -> WorkflowValue.StringValue("")
                else -> WorkflowValue.StringValue(v.toString())
            }
        }
    }

    val postFeed = NodeExecutor { input ->
        val credential = (input["credential"] as? WorkflowValue.StringValue)?.value ?: ""
        val text = (input["text"] as? WorkflowValue.StringValue)?.value ?: ""
        val imageUrl = (input["image_url"] as? WorkflowValue.StringValue)?.value

        val result = executePostFeed(credential, text, imageUrl)
        result.outputs.mapValues { (_, v) ->
            when (v) {
                is String -> WorkflowValue.StringValue(v ?: "")
                is Boolean -> WorkflowValue.BooleanValue(v)
                null -> WorkflowValue.StringValue("")
                else -> WorkflowValue.StringValue(v.toString())
            }
        }
    }

    val getFeed = NodeExecutor { input ->
        val credential = (input["credential"] as? WorkflowValue.StringValue)?.value ?: ""
        val limit = (input["limit"] as? WorkflowValue.IntValue)?.value ?: 10

        val result = executeGetFeed(credential, limit)
        result.outputs.mapValues { (_, v) ->
            when (v) {
                is List<*> -> WorkflowValue.StringValue(v.toString())
                is Int -> WorkflowValue.IntValue(v)
                else -> WorkflowValue.StringValue(v.toString())
            }
        }
    }

    val getConnections = NodeExecutor { input ->
        val credential = (input["credential"] as? WorkflowValue.StringValue)?.value ?: ""
        val limit = (input["limit"] as? WorkflowValue.IntValue)?.value ?: 10

        val result = executeGetConnections(credential, limit)
        result.outputs.mapValues { (_, v) ->
            when (v) {
                is List<*> -> WorkflowValue.StringValue(v.toString())
                is Int -> WorkflowValue.IntValue(v)
                else -> WorkflowValue.StringValue(v.toString())
            }
        }
    }

    val sendMessage = NodeExecutor { input ->
        val credential = (input["credential"] as? WorkflowValue.StringValue)?.value ?: ""
        val recipientId = (input["recipient_id"] as? WorkflowValue.StringValue)?.value ?: ""
        val message = (input["message"] as? WorkflowValue.StringValue)?.value ?: ""

        val result = executeSendMessage(credential, recipientId, message)
        result.outputs.mapValues { (_, v) ->
            when (v) {
                is String -> WorkflowValue.StringValue(v ?: "")
                is Boolean -> WorkflowValue.BooleanValue(v)
                null -> WorkflowValue.StringValue("")
                else -> WorkflowValue.StringValue(v.toString())
            }
        }
    }

    val likePost = NodeExecutor { input ->
        val credential = (input["credential"] as? WorkflowValue.StringValue)?.value ?: ""
        val postId = (input["post_id"] as? WorkflowValue.StringValue)?.value ?: ""

        val result = executeLikePost(credential, postId)
        result.outputs.mapValues { (_, v) ->
            when (v) {
                is Boolean -> WorkflowValue.BooleanValue(v)
                is String -> WorkflowValue.StringValue(v ?: "")
                null -> WorkflowValue.StringValue("")
                else -> WorkflowValue.StringValue(v.toString())
            }
        }
    }

    val searchPosts = NodeExecutor { input ->
        val credential = (input["credential"] as? WorkflowValue.StringValue)?.value ?: ""
        val query = (input["query"] as? WorkflowValue.StringValue)?.value ?: ""
        val limit = (input["limit"] as? WorkflowValue.IntValue)?.value ?: 10

        val result = executeSearchPosts(credential, query, limit)
        result.outputs.mapValues { (_, v) ->
            when (v) {
                is List<*> -> WorkflowValue.StringValue(v.toString())
                is Int -> WorkflowValue.IntValue(v)
                else -> WorkflowValue.StringValue(v.toString())
            }
        }
    }
}
