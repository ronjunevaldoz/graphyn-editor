package com.ronjunevaldoz.graphyn.plugins.gmail

import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

/**
 * Gmail integration node specs.
 *
 * All nodes require a credential reference (e.g., "gmail:main") which is resolved
 * by the execution environment (CredentialProvider). Nodes do not embed credentials.
 */

object GmailNodeSpecs {
    private val gmailFetchEmailsInputs = listOf(
        PortSpec(name = "credential", type = WorkflowType.StringType, required = true),
        PortSpec(name = "label", type = WorkflowType.StringType, required = false),
        PortSpec(name = "limit", type = WorkflowType.IntType, required = false),
    )

    private val gmailFetchEmailsDefaults = mapOf(
        "label" to WorkflowValue.StringValue("INBOX"),
        "limit" to WorkflowValue.IntValue(10),
    )

    private val gmailFetchEmailsOutputs = listOf(
        PortSpec(name = "emails", type = WorkflowType.ListType(
            WorkflowType.RecordType(mapOf(
                "id" to WorkflowType.StringType,
                "from" to WorkflowType.StringType,
                "subject" to WorkflowType.StringType,
                "snippet" to WorkflowType.StringType,
                "timestamp" to WorkflowType.StringType,
            ))
        )),
        PortSpec(name = "count", type = WorkflowType.IntType),
    )

    /**
     * Fetch emails from a Gmail label.
     */
    val gmailFetchEmails = NodeSpec(
        type = "gmail.fetch_emails",
        label = "Fetch Gmail Emails",
        inputs = gmailFetchEmailsInputs,
        outputs = gmailFetchEmailsOutputs,
        defaultValues = gmailFetchEmailsDefaults,
    )

    private val gmailSendEmailInputs = listOf(
        PortSpec(name = "credential", type = WorkflowType.StringType, required = true),
        PortSpec(name = "to", type = WorkflowType.StringType, required = true),
        PortSpec(name = "cc", type = WorkflowType.StringType, required = false),
        PortSpec(name = "subject", type = WorkflowType.StringType, required = true),
        PortSpec(name = "body", type = WorkflowType.StringType, required = true),
    )

    private val gmailSendEmailOutputs = listOf(
        PortSpec(name = "message_id", type = WorkflowType.StringType),
        PortSpec(name = "success", type = WorkflowType.BooleanType),
        PortSpec(name = "error", type = WorkflowType.StringType),
    )

    val gmailSendEmail = NodeSpec(
        type = "gmail.send_email",
        label = "Send Gmail Email",
        inputs = gmailSendEmailInputs,
        outputs = gmailSendEmailOutputs,
    )

    private val gmailParseEmailInputs = listOf(
        PortSpec(name = "email", type = WorkflowType.RecordType(mapOf(
            "id" to WorkflowType.StringType,
            "from" to WorkflowType.StringType,
            "subject" to WorkflowType.StringType,
            "snippet" to WorkflowType.StringType,
            "timestamp" to WorkflowType.StringType,
        )), required = true),
    )

    private val gmailParseEmailOutputs = listOf(
        PortSpec(name = "id", type = WorkflowType.StringType),
        PortSpec(name = "from", type = WorkflowType.StringType),
        PortSpec(name = "subject", type = WorkflowType.StringType),
        PortSpec(name = "body", type = WorkflowType.StringType),
        PortSpec(name = "snippet", type = WorkflowType.StringType),
        PortSpec(name = "timestamp", type = WorkflowType.StringType),
    )

    val gmailParseEmail = NodeSpec(
        type = "gmail.parse_email",
        label = "Parse Email",
        inputs = gmailParseEmailInputs,
        outputs = gmailParseEmailOutputs,
    )

    private val gmailGetLabelsInputs = listOf(
        PortSpec(name = "credential", type = WorkflowType.StringType, required = true),
    )

    private val gmailGetLabelsOutputs = listOf(
        PortSpec(name = "labels", type = WorkflowType.ListType(WorkflowType.StringType)),
        PortSpec(name = "count", type = WorkflowType.IntType),
    )

    val gmailGetLabels = NodeSpec(
        type = "gmail.get_labels",
        label = "Get Gmail Labels",
        inputs = gmailGetLabelsInputs,
        outputs = gmailGetLabelsOutputs,
    )

    private val gmailReplyEmailInputs = listOf(
        PortSpec(name = "credential", type = WorkflowType.StringType, required = true),
        PortSpec(name = "message_id", type = WorkflowType.StringType, required = true),
        PortSpec(name = "body", type = WorkflowType.StringType, required = true),
    )

    private val gmailReplyEmailOutputs = listOf(
        PortSpec(name = "reply_message_id", type = WorkflowType.StringType),
        PortSpec(name = "success", type = WorkflowType.BooleanType),
        PortSpec(name = "error", type = WorkflowType.StringType),
    )

    val gmailReplyEmail = NodeSpec(
        type = "gmail.reply_email",
        label = "Reply to Email",
        inputs = gmailReplyEmailInputs,
        outputs = gmailReplyEmailOutputs,
    )

    val all = listOf(
        gmailFetchEmails,
        gmailSendEmail,
        gmailParseEmail,
        gmailGetLabels,
        gmailReplyEmail,
    )
}
