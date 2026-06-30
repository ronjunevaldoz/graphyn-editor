package com.ronjunevaldoz.graphyn

import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.core.serialization.DefaultWorkflowJsonCodec
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WorkflowRunRouteTest {

    @Test
    fun overridesMergeOntoMatchingNodeAndPreserveOthers() {
        val wf = WorkflowDefinition(
            id = "wf", name = "W",
            nodes = listOf(
                NodeRef("a", "t", config = mapOf("x" to WorkflowValue.IntValue(1), "y" to WorkflowValue.IntValue(2))),
                NodeRef("b", "t", config = mapOf("x" to WorkflowValue.IntValue(9))),
            ),
            connections = emptyList(),
        )
        val merged = applyOverrides(wf, mapOf("a" to mapOf("x" to WorkflowValue.IntValue(42))))

        val a = merged.nodes.first { it.id == "a" }
        assertEquals(WorkflowValue.IntValue(42), a.config["x"], "override replaces matched port")
        assertEquals(WorkflowValue.IntValue(2), a.config["y"], "other ports on the node are untouched")
        assertEquals(WorkflowValue.IntValue(9), merged.nodes.first { it.id == "b" }.config["x"], "other nodes untouched")
    }

    @Test
    fun emptyOverridesReturnSameGraph() {
        val wf = WorkflowDefinition("wf", "W", listOf(NodeRef("a", "t")), emptyList())
        assertTrue(applyOverrides(wf, emptyMap()) === wf)
    }

    @Test
    fun runUnknownWorkflowReturns404() = testApplication {
        application { module() }
        val response = client.post("/workflows/does-not-exist-xyz/run") {
            contentType(ContentType.Application.Json); setBody("""{"overrides":{}}""")
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun runStoredWorkflowSyncReturnsResult() = testApplication {
        application { module() }
        val wf = WorkflowDefinition(
            id = "wf-run-sync", name = "Run Sync",
            nodes = listOf(NodeRef("n1", "json.parse",
                config = mapOf("json" to WorkflowValue.StringValue("{\"k\":1}")))),
            connections = emptyList(),
        )
        client.post("/workflows") {
            contentType(ContentType.Application.Json); setBody(DefaultWorkflowJsonCodec.encodeToString(wf))
        }

        val response = client.post("/workflows/${wf.id}/run") {
            contentType(ContentType.Application.Json); setBody("{}")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("nodeOutputsByNodeId"), "returns an execution result")
    }
}
