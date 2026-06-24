package com.ronjunevaldoz.graphyn.plugins.gmail

import com.ronjunevaldoz.graphyn.core.execution.NodeExecutor
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.pluginapi.GRAPHYN_PLUGIN_API_VERSION
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPlugin
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPluginMetadata
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPluginRegistrar

object GmailPlugin : GraphynPlugin {
    override val metadata = GraphynPluginMetadata(
        id = "graphyn.gmail",
        displayName = "Gmail Integration",
        version = "0.1.0",
        apiVersion = GRAPHYN_PLUGIN_API_VERSION,
    )

    override fun register(registrar: GraphynPluginRegistrar) {
        // Register node specs
        registrar.registerNodeSpec(GmailNodeSpecs.gmailFetchEmails)
        registrar.registerNodeSpec(GmailNodeSpecs.gmailSendEmail)
        registrar.registerNodeSpec(GmailNodeSpecs.gmailParseEmail)
        registrar.registerNodeSpec(GmailNodeSpecs.gmailGetLabels)
        registrar.registerNodeSpec(GmailNodeSpecs.gmailReplyEmail)

        // Register executors
        registrar.registerExecutor(GmailNodeSpecs.gmailFetchEmails.type, GmailExecutors.fetchEmails)
        registrar.registerExecutor(GmailNodeSpecs.gmailSendEmail.type, GmailExecutors.sendEmail)
        registrar.registerExecutor(GmailNodeSpecs.gmailParseEmail.type, GmailExecutors.parseEmail)
        registrar.registerExecutor(GmailNodeSpecs.gmailGetLabels.type, GmailExecutors.getLabels)
        registrar.registerExecutor(GmailNodeSpecs.gmailReplyEmail.type, GmailExecutors.replyEmail)
    }
}

object GmailExecutors {
    val fetchEmails = NodeExecutor { input ->
        val credential = (input["credential"] as? WorkflowValue.StringValue)?.value ?: ""
        val label = (input["label"] as? WorkflowValue.StringValue)?.value ?: "INBOX"
        val limit = (input["limit"] as? WorkflowValue.IntValue)?.value ?: 10

        val result = executeFetchEmails(credential, label, limit)
        result.outputs.mapValues { (_, v) ->
            when (v) {
                is List<*> -> WorkflowValue.StringValue(v.toString()) // TODO: proper list conversion
                is Int -> WorkflowValue.IntValue(v)
                is String -> WorkflowValue.StringValue(v)
                is Boolean -> WorkflowValue.BooleanValue(v)
                else -> WorkflowValue.StringValue(v.toString())
            }
        }
    }

    val sendEmail = NodeExecutor { input ->
        val credential = (input["credential"] as? WorkflowValue.StringValue)?.value ?: ""
        val to = (input["to"] as? WorkflowValue.StringValue)?.value ?: ""
        val subject = (input["subject"] as? WorkflowValue.StringValue)?.value ?: ""
        val body = (input["body"] as? WorkflowValue.StringValue)?.value ?: ""
        val cc = (input["cc"] as? WorkflowValue.StringValue)?.value

        val result = executeSendEmail(credential, to, subject, body, cc)
        result.outputs.mapValues { (_, v) ->
            when (v) {
                is String -> WorkflowValue.StringValue(v)
                is Boolean -> WorkflowValue.BooleanValue(v)
                null -> WorkflowValue.StringValue("")
                else -> WorkflowValue.StringValue(v.toString())
            }
        }
    }

    val parseEmail = NodeExecutor { input ->
        // Extract email record from input
        @Suppress("UNCHECKED_CAST")
        val emailRecord = (input["email"] as? Map<String, Any?>) ?: emptyMap()

        val result = executeParseEmail(emailRecord)
        result.outputs.mapValues { (_, v) ->
            when (v) {
                is String -> WorkflowValue.StringValue(v ?: "")
                null -> WorkflowValue.StringValue("")
                else -> WorkflowValue.StringValue(v.toString())
            }
        }
    }

    val getLabels = NodeExecutor { input ->
        val credential = (input["credential"] as? WorkflowValue.StringValue)?.value ?: ""

        val result = executeGetLabels(credential)
        result.outputs.mapValues { (_, v) ->
            when (v) {
                is List<*> -> WorkflowValue.StringValue(v.toString()) // TODO: proper list conversion
                is Int -> WorkflowValue.IntValue(v)
                else -> WorkflowValue.StringValue(v.toString())
            }
        }
    }

    val replyEmail = NodeExecutor { input ->
        val credential = (input["credential"] as? WorkflowValue.StringValue)?.value ?: ""
        val messageId = (input["message_id"] as? WorkflowValue.StringValue)?.value ?: ""
        val body = (input["body"] as? WorkflowValue.StringValue)?.value ?: ""

        val result = executeReplyEmail(credential, messageId, body)
        result.outputs.mapValues { (_, v) ->
            when (v) {
                is String -> WorkflowValue.StringValue(v ?: "")
                is Boolean -> WorkflowValue.BooleanValue(v)
                null -> WorkflowValue.StringValue("")
                else -> WorkflowValue.StringValue(v.toString())
            }
        }
    }
}
