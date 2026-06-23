package com.ronjunevaldoz.graphyn.ai

import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import kotlinx.coroutines.runBlocking
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Live integration test against a real Ollama host. @Ignore by default (network + model latency);
 * remove the annotation and run manually to validate the full generate → parse pipeline.
 */
class OllamaLiveIntegrationTest {

    private val catalog = listOf(
        NodeSpec("http_request", "HTTP Request",
            inputs = listOf(PortSpec("url", WorkflowType.StringType), PortSpec("method", WorkflowType.StringType)),
            outputs = listOf(PortSpec("body", WorkflowType.StringType), PortSpec("statusCode", WorkflowType.IntType), PortSpec("ok", WorkflowType.BooleanType))),
        NodeSpec("file_write", "File Write",
            inputs = listOf(PortSpec("path", WorkflowType.StringType), PortSpec("content", WorkflowType.StringType)),
            outputs = listOf(PortSpec("success", WorkflowType.BooleanType))),
    )

    @Ignore
    @Test
    fun generatesWorkflowFromLiveHost() = runBlocking {
        val generator = OllamaWorkflowGenerator(
            OllamaConfig(baseUrl = "https://ron-local-home.duckdns.org/ollama/", model = "qwen2.5-coder:14b"),
        )
        val result = generator.generate("Fetch a URL and write the response body to a file", catalog)
        println("RESULT: $result")
        assertTrue(result is WorkflowGenerationResult.Success, "expected success, got $result")
    }
}
