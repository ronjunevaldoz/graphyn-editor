package com.ronjunevaldoz.graphyn.mcp

import com.ronjunevaldoz.graphyn.GraphynRunRegistry
import com.ronjunevaldoz.graphyn.createGraphynServerRuntime
import com.ronjunevaldoz.graphyn.core.store.InMemoryWorkflowStore
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private val testJson = Json { encodeDefaults = false; ignoreUnknownKeys = true }

private fun CallToolResult.text(): String = (content.single() as TextContent).text

/** {"greet","text.format"} with a literal "values" config — same shape verified manually against the real MCP stdio server. */
private fun greetWorkflowJson(id: String) = """
    {"version":1,"workflow":{"id":"$id","name":"Greet","nodes":[
        {"id":"greet","type":"text.format","config":{
            "template":{"kind":"string","value":"Hello, {name}!"},
            "values":{"kind":"record","fields":{"name":{"kind":"string","value":"World"}}}
        }}
    ],"connections":[]}}
""".trimIndent()

/** A node with an unmet required input ("values" needs a value/connection) — must fail validation. */
private fun invalidWorkflowJson(id: String) = """
    {"version":1,"workflow":{"id":"$id","name":"Invalid","nodes":[
        {"id":"greet","type":"text.format","config":{}}
    ],"connections":[]}}
""".trimIndent()

class WorkflowToolsTest {

    @Test
    fun listWorkflowsReflectsStore() = runTest {
        val store = InMemoryWorkflowStore()
        assertEquals("[]", listWorkflows(store, testJson).text())

        publishWorkflow(store, createGraphynServerRuntime(), testJson, buildJsonObject { put("workflow", greetWorkflowJson("wf-1")) })
        assertTrue(listWorkflows(store, testJson).text().contains("wf-1"))
    }

    @Test
    fun getWorkflowMissingIdErrors() = runTest {
        assertTrue(getWorkflow(InMemoryWorkflowStore(), null).isError == true)
    }

    @Test
    fun getWorkflowUnknownIdErrors() = runTest {
        assertTrue(getWorkflow(InMemoryWorkflowStore(), buildJsonObject { put("id", "nope") }).isError == true)
    }

    @Test
    fun getWorkflowRoundTripsPublished() = runTest {
        val store = InMemoryWorkflowStore()
        publishWorkflow(store, createGraphynServerRuntime(), testJson, buildJsonObject { put("workflow", greetWorkflowJson("wf-2")) })
        val result = getWorkflow(store, buildJsonObject { put("id", "wf-2") })
        assertFalse(result.isError == true)
        assertTrue(result.text().contains("\"id\": \"wf-2\""))
    }

    @Test
    fun publishInvalidJsonErrors() = runTest {
        val result = publishWorkflow(InMemoryWorkflowStore(), createGraphynServerRuntime(), testJson, buildJsonObject { put("workflow", "not json") })
        assertTrue(result.isError == true)
    }

    @Test
    fun publishFailsValidationOnUnmetRequiredInput() = runTest {
        val result = publishWorkflow(
            InMemoryWorkflowStore(), createGraphynServerRuntime(), testJson,
            buildJsonObject { put("workflow", invalidWorkflowJson("wf-3")) },
        )
        assertTrue(result.isError == true)
        assertTrue(result.text().contains("missing_required_input"))
    }

    @Test
    fun publishValidWorkflowSucceeds() = runTest {
        val store = InMemoryWorkflowStore()
        val result = publishWorkflow(store, createGraphynServerRuntime(), testJson, buildJsonObject { put("workflow", greetWorkflowJson("wf-4")) })
        assertFalse(result.isError == true)
        assertTrue(store.list().any { it.id == "wf-4" })
    }

    @Test
    fun deleteWorkflowMissingIdErrors() = runTest {
        assertTrue(deleteWorkflow(InMemoryWorkflowStore(), null).isError == true)
    }

    @Test
    fun deleteWorkflowRemovesFromStore() = runTest {
        val store = InMemoryWorkflowStore()
        publishWorkflow(store, createGraphynServerRuntime(), testJson, buildJsonObject { put("workflow", greetWorkflowJson("wf-5")) })
        deleteWorkflow(store, buildJsonObject { put("id", "wf-5") })
        assertFalse(store.list().any { it.id == "wf-5" })
    }

    @Test
    fun executeUnknownIdErrors() = runTest {
        val runtime = createGraphynServerRuntime()
        val result = executeWorkflow(InMemoryWorkflowStore(), runtime, GraphynRunRegistry(runtime.executionEngine), testJson, buildJsonObject { put("id", "nope") })
        assertTrue(result.isError == true)
    }

    @Test
    fun executeSyncRunsWorkflow() = runTest {
        val store = InMemoryWorkflowStore()
        val runtime = createGraphynServerRuntime()
        publishWorkflow(store, runtime, testJson, buildJsonObject { put("workflow", greetWorkflowJson("wf-6")) })
        val result = executeWorkflow(store, runtime, GraphynRunRegistry(runtime.executionEngine), testJson, buildJsonObject { put("id", "wf-6") })
        assertFalse(result.isError == true)
        assertTrue(result.text().contains("Hello, World!"))
    }

    // Real runBlocking (not runTest's virtual clock): the registry runs the workflow on a real
    // Dispatchers.Default coroutine outside this test's scope, so virtual-time delay() would not
    // reliably wait for it — poll real wall-clock time for a bounded number of attempts instead.
    @Test
    fun executeAsyncReturnsRunIdThenStatusReportsCompletion() = runBlocking {
        val store = InMemoryWorkflowStore()
        val runtime = createGraphynServerRuntime()
        val registry = GraphynRunRegistry(runtime.executionEngine)
        publishWorkflow(store, runtime, testJson, buildJsonObject { put("workflow", greetWorkflowJson("wf-7")) })

        val started = executeWorkflow(store, runtime, registry, testJson, buildJsonObject { put("id", "wf-7"); put("async", true) })
        assertFalse(started.isError == true)
        val runId = testJson.parseToJsonElement(started.text()).jsonObject.getValue("runId").jsonPrimitive.content
        assertTrue(runId.isNotBlank())

        var statusText = ""
        var attempts = 0
        while (attempts < 20 && !statusText.contains("Completed")) {
            val status = executionStatus(registry, testJson, buildJsonObject { put("runId", runId) })
            assertFalse(status.isError == true)
            statusText = status.text()
            if (!statusText.contains("Completed")) Thread.sleep(50)
            attempts++
        }
        assertTrue(statusText.contains("Completed"))
    }

    @Test
    fun executionStatusUnknownRunIdErrors() = runTest {
        val runtime = createGraphynServerRuntime()
        val result = executionStatus(GraphynRunRegistry(runtime.executionEngine), testJson, buildJsonObject { put("runId", "nope") })
        assertTrue(result.isError == true)
    }

    @Test
    fun listNodeTypesIncludesTextFormat() = runTest {
        val runtime = createGraphynServerRuntime()
        assertTrue(listNodeTypes(runtime, testJson).text().contains("text.format"))
    }
}
