package com.ronjunevaldoz.graphyn.plugins.shorts

import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OllamaGenerateTest {

    @Test
    fun successfulCallReturnsResponseTextAndOk() = runTest {
        var seenUrl = ""
        var seenBody = ""
        val executor = ollamaGenerateExecutor { url, body ->
            seenUrl = url
            seenBody = body
            """{"model":"llama3.1","response":"a witty script","done":true}"""
        }
        val out = executor.execute(mapOf("prompt" to WorkflowValue.StringValue("write a joke")))
        assertEquals(WorkflowValue.StringValue("a witty script"), out["response"])
        assertEquals(WorkflowValue.BooleanValue(true), out["ok"])
        assertEquals("http://localhost:11434/api/generate", seenUrl)
        assertTrue(seenBody.contains("\"prompt\":\"write a joke\""))
        assertTrue(seenBody.contains("\"stream\":false"))
    }

    @Test
    fun customHostAndModelAreUsed() = runTest {
        var seenUrl = ""
        var seenBody = ""
        val executor = ollamaGenerateExecutor { url, body ->
            seenUrl = url; seenBody = body
            """{"response":"ok"}"""
        }
        executor.execute(
            mapOf(
                "prompt" to WorkflowValue.StringValue("hi"),
                "model" to WorkflowValue.StringValue("qwen3:8b"),
                "host" to WorkflowValue.StringValue("http://gpu-box:11434/"),
            ),
        )
        assertEquals("http://gpu-box:11434/api/generate", seenUrl)
        assertTrue(seenBody.contains("\"model\":\"qwen3:8b\""))
    }

    @Test
    fun malformedResponseJsonDegradesToOkFalse() = runTest {
        // Missing the "response" field / not the expected shape — parsed defensively, not thrown.
        val executor = ollamaGenerateExecutor { _, _ -> """{"error":"model not found"}""" }
        val out = executor.execute(mapOf("prompt" to WorkflowValue.StringValue("x")))
        assertEquals(WorkflowValue.StringValue(""), out["response"])
        assertEquals(WorkflowValue.BooleanValue(false), out["ok"])
    }

    @Test
    fun nonJsonResponseDegradesToOkFalse() = runTest {
        val executor = ollamaGenerateExecutor { _, _ -> "connection reset by peer" }
        val out = executor.execute(mapOf("prompt" to WorkflowValue.StringValue("x")))
        assertFalse((out["ok"] as WorkflowValue.BooleanValue).value)
    }

    @Test
    fun transportFailureDegradesToOkFalse() = runTest {
        val executor = ollamaGenerateExecutor { _, _ -> throw RuntimeException("no server") }
        val out = executor.execute(mapOf("prompt" to WorkflowValue.StringValue("x")))
        assertEquals(WorkflowValue.StringValue(""), out["response"])
        assertEquals(WorkflowValue.BooleanValue(false), out["ok"])
    }
}
