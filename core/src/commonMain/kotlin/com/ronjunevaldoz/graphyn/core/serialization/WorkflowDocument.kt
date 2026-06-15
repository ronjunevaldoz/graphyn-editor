package com.ronjunevaldoz.graphyn.core.serialization

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

const val GRAPHYN_WORKFLOW_FORMAT_VERSION = 1

@Serializable
data class WorkflowDocument(
    val version: Int = GRAPHYN_WORKFLOW_FORMAT_VERSION,
    val workflow: WorkflowDocumentWorkflow,
)

@Serializable
data class WorkflowDocumentWorkflow(
    val id: String,
    val name: String,
    val nodes: List<WorkflowDocumentNode>,
    val connections: List<WorkflowDocumentConnection>,
)

@Serializable
data class WorkflowDocumentNode(
    val id: String,
    val type: String,
    val config: Map<String, WorkflowValue> = emptyMap(),
)

@Serializable
data class WorkflowDocumentConnection(
    val fromNodeId: String,
    val fromPort: String,
    val toNodeId: String,
    val toPort: String,
)

interface WorkflowDocumentCodec {
    fun encode(workflow: WorkflowDefinition): WorkflowDocument
    fun decode(document: WorkflowDocument): WorkflowDefinition
}

interface WorkflowJsonCodec {
    fun encodeToString(workflow: WorkflowDefinition): String
    fun decodeFromString(value: String): WorkflowDefinition
}

object GraphynWorkflowJson {
    val json: Json = Json {
        prettyPrint = true
        encodeDefaults = false
        ignoreUnknownKeys = true
        classDiscriminator = "kind"
    }
}

object DefaultWorkflowDocumentCodec : WorkflowDocumentCodec {
    override fun encode(workflow: WorkflowDefinition): WorkflowDocument {
        return WorkflowDocument(
            workflow = WorkflowDocumentWorkflow(
                id = workflow.id,
                name = workflow.name,
                nodes = workflow.nodes.map { node ->
                    WorkflowDocumentNode(
                        id = node.id,
                        type = node.type,
                        config = node.config,
                    )
                },
                connections = workflow.connections.map { connection ->
                    WorkflowDocumentConnection(
                        fromNodeId = connection.fromNodeId,
                        fromPort = connection.fromPort,
                        toNodeId = connection.toNodeId,
                        toPort = connection.toPort,
                    )
                },
            ),
        )
    }

    override fun decode(document: WorkflowDocument): WorkflowDefinition {
        return WorkflowDefinition(
            id = document.workflow.id,
            name = document.workflow.name,
            nodes = document.workflow.nodes.map { node ->
                NodeRef(
                    id = node.id,
                    type = node.type,
                    config = node.config,
                )
            },
            connections = document.workflow.connections.map { connection ->
                ConnectionRef(
                    fromNodeId = connection.fromNodeId,
                    fromPort = connection.fromPort,
                    toNodeId = connection.toNodeId,
                    toPort = connection.toPort,
                )
            },
        )
    }
}

object DefaultWorkflowJsonCodec : WorkflowJsonCodec {
    override fun encodeToString(workflow: WorkflowDefinition): String {
        return GraphynWorkflowJson.json.encodeToString(
            WorkflowDocument.serializer(),
            DefaultWorkflowDocumentCodec.encode(workflow),
        )
    }

    override fun decodeFromString(value: String): WorkflowDefinition {
        return DefaultWorkflowDocumentCodec.decode(
            GraphynWorkflowJson.json.decodeFromString(
                WorkflowDocument.serializer(),
                value,
            ),
        )
    }
}
