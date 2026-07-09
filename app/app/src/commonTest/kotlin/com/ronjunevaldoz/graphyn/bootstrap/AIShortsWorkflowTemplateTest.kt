package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowNodePosition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AIShortsWorkflowTemplateTest {

    @Test
    fun imageMotionShortsUseSceneSubgraphsAndBatchStitching() {
        val workflow = WorkflowCatalog.ImageShorts.workflow

        assertEquals("ai-shorts-image", workflow.id)
        assertEquals(WorkflowNodePosition(40, 40), workflow.nodePositions["guide"])
        assertEquals(WorkflowNodePosition(900, 40), workflow.nodePositions["scene1"])
        assertEquals(0, workflow.nodes.count { it.id.startsWith("ollama") })
        assertEquals(0, workflow.nodes.count { it.id.startsWith("frame") })
        assertEquals(0, workflow.nodes.count { it.id.startsWith("prompt") })
        assertTrue(workflow.nodes.any { it.id == "outlineSource" && it.subgraph?.nodes?.any { node -> node.type == "io.http_request" } == true })
        assertEquals(WorkflowNodePosition(380, 40), workflow.nodePositions["outlineSource"])
        assertTrue(workflow.nodes.any { it.id == "scene1" && it.type == SHORTS_SCENE_SUBGRAPH_NODE_TYPE && it.subgraph?.nodes?.any { node -> node.type == "sd.txt2img" } == true })
        assertTrue(workflow.nodes.any { it.id == "scene1" && it.subgraph?.nodes?.any { node -> node.type == "script.eval" } == true })
        assertTrue(workflow.nodes.any { it.id == "scene1" && it.subgraph?.nodes?.any { node -> node.type == "media.prompt_enhance" } == true })
        assertTrue(workflow.nodes.any { it.id == "scene1" && it.subgraph?.nodes?.any { node -> node.type == "media.image_import" } == true })
        assertEquals(WorkflowNodePosition(760, 120), workflow.nodes.first { it.id == "scene1" }.subgraph?.nodePositions?.get("promptEnhance"))
        assertEquals(WorkflowNodePosition(380, 120), workflow.nodes.first { it.id == "scene1" }.subgraph?.nodePositions?.get("scenePrompt"))
        assertTrue(workflow.nodes.any { it.id == "scene1" && it.subgraph?.nodes?.any { node -> node.type == "media.images_list" } == true })
        assertTrue(workflow.nodes.any { it.id == "batch1" && it.type == SHORTS_BATCH_SUBGRAPH_NODE_TYPE && it.subgraph?.nodes?.any { node -> node.type == "media.video_stitch" } == true })
        assertTrue(ConnectionRef("scenes", "result", "scene1", "input") in workflow.connections)
        assertTrue(ConnectionRef("scene1", "video", "scene2", "gate") in workflow.connections)
        assertTrue(ConnectionRef("scene1", "video", "batch1", "video1") in workflow.connections)
        assertTrue(ConnectionRef("batch1", "video", "finalBatch", "video1") in workflow.connections)
        assertTrue(ConnectionRef("finalBatch", "video", "captionOverlay", "video") in workflow.connections)
    }

    @Test
    fun videoMotionShortsUseSceneSubgraphsAndBatchStitching() {
        val workflow = WorkflowCatalog.VideoShorts.workflow

        assertEquals("ai-shorts-video", workflow.id)
        assertEquals(WorkflowNodePosition(1620, 540), workflow.nodePositions["finalBatch"])
        assertEquals(WorkflowNodePosition(900, 1580), workflow.nodePositions["scene8"])
        assertEquals(0, workflow.nodes.count { it.id.startsWith("frame") })
        assertEquals(0, workflow.nodes.count { it.id.startsWith("prompt") })
        assertTrue(workflow.nodes.any { it.id == "outlineSource" && it.subgraph?.nodes?.any { node -> node.type == "io.http_request" } == true })
        assertEquals(WorkflowNodePosition(380, 40), workflow.nodePositions["outlineSource"])
        assertTrue(workflow.nodes.any { it.id == "scene1" && it.type == SHORTS_SCENE_SUBGRAPH_NODE_TYPE && it.subgraph?.nodes?.any { node -> node.type == "sd.txt2vid" } == true })
        assertTrue(workflow.nodes.any { it.id == "scene1" && it.subgraph?.nodes?.any { node -> node.type == "script.eval" } == true })
        assertTrue(workflow.nodes.any { it.id == "scene1" && it.subgraph?.nodes?.any { node -> node.type == "media.prompt_enhance" } == true })
        assertTrue(workflow.nodes.any { it.id == "scene1" && it.subgraph?.nodes?.any { node -> node.type == "preview.view" } == true })
        assertEquals(WorkflowNodePosition(760, 120), workflow.nodes.first { it.id == "scene1" }.subgraph?.nodePositions?.get("promptEnhance"))
        assertEquals(WorkflowNodePosition(380, 120), workflow.nodes.first { it.id == "scene1" }.subgraph?.nodePositions?.get("scenePrompt"))
        assertTrue(workflow.nodes.any { it.id == "batch2" && it.type == SHORTS_BATCH_SUBGRAPH_NODE_TYPE && it.subgraph?.nodes?.any { node -> node.type == "media.video_stitch" } == true })
        assertTrue(ConnectionRef("scenes", "result", "scene1", "input") in workflow.connections)
        assertTrue(ConnectionRef("scene1", "video", "scene2", "gate") in workflow.connections)
        assertTrue(ConnectionRef("scene1", "video", "batch1", "video1") in workflow.connections)
        assertTrue(ConnectionRef("batch2", "video", "finalBatch", "video2") in workflow.connections)
        assertTrue(ConnectionRef("finalBatch", "video", "captionOverlay", "video") in workflow.connections)
    }

    @Test
    fun comparisonShortWritesTimestampedJson() {
        val workflow = comparisonShortWorkflow(topic = "coffee")

        assertTrue(workflow.nodes.any { it.id == "comparisonMetadata" && it.type == "demo.comparison.metadata" })
        assertTrue(ConnectionRef("comparison", "value", "comparisonMetadata", "input") in workflow.connections)
        assertTrue(ConnectionRef("comparisonMetadata", "value", "comparisonJson", "value") in workflow.connections)
        assertTrue(ConnectionRef("comparisonJson", "text", "comparisonJsonWrite", "content") in workflow.connections)
    }
}
