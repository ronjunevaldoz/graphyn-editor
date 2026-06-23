package com.ronjunevaldoz.graphyn.ai

import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
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
    fun fillsAndCoercesNodeConfigByPortType() {
        val typed = listOf(
            NodeSpec("req", "Req", inputs = listOf(
                PortSpec("url", WorkflowType.StringType),
                PortSpec("retries", WorkflowType.IntType),
                PortSpec("ratio", WorkflowType.DoubleType),
                PortSpec("verbose", WorkflowType.BooleanType),
            ), outputs = emptyList()),
        )
        val raw = """{"nodes":[{"id":"n","type":"req","config":{
            "url":"https://x.com","retries":3,"ratio":1.5,"verbose":true,"bogus":"ignored"}}],"connections":[]}"""
        val result = WorkflowJsonParser.parse(raw, typed, "fb") as WorkflowGenerationResult.Success
        val cfg = result.workflow.nodes.single().config
        assertEquals(WorkflowValue.StringValue("https://x.com"), cfg["url"])
        assertEquals(WorkflowValue.IntValue(3), cfg["retries"])
        assertEquals(WorkflowValue.DoubleValue(1.5), cfg["ratio"])
        assertEquals(WorkflowValue.BooleanValue(true), cfg["verbose"])
        assertTrue("bogus" !in cfg, "unknown port should be dropped from config")
    }

    @Test
    fun coercesStringifiedNumbers() {
        val typed = listOf(NodeSpec("n", "N",
            inputs = listOf(PortSpec("count", WorkflowType.IntType)), outputs = emptyList()))
        val raw = """{"nodes":[{"id":"a","type":"n","config":{"count":"7"}}],"connections":[]}"""
        val result = WorkflowJsonParser.parse(raw, typed, "fb") as WorkflowGenerationResult.Success
        assertEquals(WorkflowValue.IntValue(7), result.workflow.nodes.single().config["count"])
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
