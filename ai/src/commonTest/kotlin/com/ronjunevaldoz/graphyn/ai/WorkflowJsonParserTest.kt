package com.ronjunevaldoz.graphyn.ai

import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WorkflowJsonParserTest {

    private val catalog = listOf(
        NodeSpec("source", "Source", inputs = emptyList(),
            outputs = listOf(PortSpec("out", WorkflowType.StringType))),
        NodeSpec("sink", "Sink",
            inputs = listOf(PortSpec("in", WorkflowType.StringType)), outputs = emptyList()),
    )

    private fun parse(raw: String) = WorkflowJsonParser.parse(raw, catalog, "fallback")

    @Test
    fun parsesValidWorkflow() {
        val raw = """{"id":"w1","name":"Pipe","nodes":[{"id":"a","type":"source"},{"id":"b","type":"sink"}],
            "connections":[{"fromNodeId":"a","fromPort":"out","toNodeId":"b","toPort":"in"}]}"""
        val result = parse(raw) as WorkflowGenerationResult.Success
        assertEquals(2, result.workflow.nodes.size)
        assertEquals(1, result.workflow.connections.size)
        assertEquals("w1", result.workflow.id)
    }

    @Test
    fun stripsMarkdownFencesAndProse() {
        val raw = "Here you go:\n```json\n{\"nodes\":[{\"id\":\"a\",\"type\":\"source\"}],\"connections\":[]}\n```\nDone!"
        val result = parse(raw) as WorkflowGenerationResult.Success
        assertEquals(1, result.workflow.nodes.size)
        assertEquals("fallback", result.workflow.id)
    }

    @Test
    fun dropsUnknownNodeTypes() {
        val raw = """{"nodes":[{"id":"a","type":"source"},{"id":"x","type":"madeup"}],"connections":[]}"""
        val result = parse(raw) as WorkflowGenerationResult.Success
        assertEquals(1, result.workflow.nodes.size)
        assertEquals(listOf("x (madeup)"), result.droppedNodes)
    }

    @Test
    fun dropsDanglingAndBadPortConnections() {
        val raw = """{"nodes":[{"id":"a","type":"source"},{"id":"b","type":"sink"}],
            "connections":[
              {"fromNodeId":"a","fromPort":"out","toNodeId":"b","toPort":"in"},
              {"fromNodeId":"a","fromPort":"wrong","toNodeId":"b","toPort":"in"},
              {"fromNodeId":"a","fromPort":"out","toNodeId":"ghost","toPort":"in"}
            ]}"""
        val result = parse(raw) as WorkflowGenerationResult.Success
        assertEquals(1, result.workflow.connections.size)
        assertEquals(2, result.droppedConnections)
    }

    @Test
    fun failsOnNoJson() {
        assertTrue(parse("sorry, I cannot help") is WorkflowGenerationResult.Failure)
    }

    @Test
    fun failsWhenNoValidNodes() {
        val raw = """{"nodes":[{"id":"x","type":"madeup"}],"connections":[]}"""
        assertTrue(parse(raw) is WorkflowGenerationResult.Failure)
    }
}
