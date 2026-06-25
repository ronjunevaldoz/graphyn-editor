package com.ronjunevaldoz.graphyn.plugins.gmail

import com.ronjunevaldoz.graphyn.core.execution.NodeExecutor
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.pluginapi.GRAPHYN_PLUGIN_API_VERSION
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPlugin
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPluginMetadata
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPluginRegistrar

/**
 * Gmail runtime plugin. Resolves `credential` inputs through the platform
 * [StoredCredentialProvider] by default — a `scope:key` reference is read from the platform
 * credential store, a literal bearer token is used as-is. Hosts wanting custom (e.g. OAuth-backed)
 * resolution can build their own [GmailExecutors] and register those instead.
 */
object GmailPlugin : GraphynPlugin {
    private val executors = GmailExecutors()

    override val metadata = GraphynPluginMetadata(
        id = "graphyn.gmail",
        displayName = "Gmail Integration",
        version = "0.1.0",
        apiVersion = GRAPHYN_PLUGIN_API_VERSION,
    )

    override fun register(registrar: GraphynPluginRegistrar) {
        registrar.registerNodeSpec(GmailNodeSpecs.gmailFetchEmails)
        registrar.registerNodeSpec(GmailNodeSpecs.gmailSendEmail)
        registrar.registerNodeSpec(GmailNodeSpecs.gmailParseEmail)
        registrar.registerNodeSpec(GmailNodeSpecs.gmailGetLabels)
        registrar.registerNodeSpec(GmailNodeSpecs.gmailReplyEmail)

        registrar.registerExecutor(GmailNodeSpecs.gmailFetchEmails.type, executors.fetchEmails)
        registrar.registerExecutor(GmailNodeSpecs.gmailSendEmail.type, executors.sendEmail)
        registrar.registerExecutor(GmailNodeSpecs.gmailParseEmail.type, executors.parseEmail)
        registrar.registerExecutor(GmailNodeSpecs.gmailGetLabels.type, executors.getLabels)
        registrar.registerExecutor(GmailNodeSpecs.gmailReplyEmail.type, executors.replyEmail)
    }
}

/**
 * Executors for the Gmail nodes. The `credential` input is resolved through [credentialProvider]:
 * a `scope:key` reference (e.g. `gmail:main`) is looked up in the platform credential store, while
 * a literal bearer token is used as-is. Defaults to the platform [StoredCredentialProvider]; a host
 * can pass its own (OAuth-backed) provider.
 */
class GmailExecutors(private val credentialProvider: CredentialProvider? = StoredCredentialProvider()) {
    private suspend fun Map<String, WorkflowValue>.token(): String =
        resolveCredentialToken(str("credential"), credentialProvider)

    val fetchEmails = NodeExecutor { input ->
        executeFetchEmails(
            token = input.token(),
            label = input.str("label").ifEmpty { "INBOX" },
            limit = (input["limit"] as? WorkflowValue.IntValue)?.value ?: 10,
        ).outputs.toWorkflowOutputs()
    }

    val sendEmail = NodeExecutor { input ->
        executeSendEmail(
            token = input.token(),
            to = input.str("to"),
            subject = input.str("subject"),
            body = input.str("body"),
            cc = (input["cc"] as? WorkflowValue.StringValue)?.value,
        ).outputs.toWorkflowOutputs()
    }

    val parseEmail = NodeExecutor { input ->
        val emailRecord = (input["email"] as? WorkflowValue.RecordValue)
            ?.fields?.mapValues { (_, v) -> fromWorkflowValue(v) } ?: emptyMap()
        executeParseEmail(emailRecord).outputs.toWorkflowOutputs()
    }

    val getLabels = NodeExecutor { input ->
        executeGetLabels(input.token()).outputs.toWorkflowOutputs()
    }

    val replyEmail = NodeExecutor { input ->
        executeReplyEmail(
            token = input.token(),
            messageId = input.str("message_id"),
            body = input.str("body"),
        ).outputs.toWorkflowOutputs()
    }
}

private fun Map<String, WorkflowValue>.str(key: String): String =
    (this[key] as? WorkflowValue.StringValue)?.value ?: ""

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

private fun fromWorkflowValue(v: WorkflowValue): Any? = when (v) {
    is WorkflowValue.StringValue -> v.value
    is WorkflowValue.IntValue -> v.value
    is WorkflowValue.DoubleValue -> v.value
    is WorkflowValue.BooleanValue -> v.value
    is WorkflowValue.ListValue -> v.items.map { fromWorkflowValue(it) }
    is WorkflowValue.RecordValue -> v.fields.mapValues { (_, value) -> fromWorkflowValue(value) }
    WorkflowValue.NullValue -> null
    WorkflowValue.OpaqueValue -> null
}
