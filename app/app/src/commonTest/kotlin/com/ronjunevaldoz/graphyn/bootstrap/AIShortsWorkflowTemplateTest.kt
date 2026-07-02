package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AIShortsWorkflowTemplateTest {

    @Test
    fun imageMotionShortsUseSceneSubgraphsAndBatchStitching() {
        val workflow = WorkflowCatalog.ImageShorts.workflow

        assertEquals("ai-shorts-image", workflow.id)
        assertEquals(0, workflow.nodes.count { it.id.startsWith("frame") })
        assertEquals(0, workflow.nodes.count { it.id.startsWith("prompt") })
        assertTrue(workflow.nodes.any { it.id == "scene1" && it.type == SHORTS_SCENE_SUBGRAPH_NODE_TYPE && it.subgraph?.nodes?.any { node -> node.type == "sd.img2vid" } == true })
        assertTrue(workflow.nodes.any { it.id == "scene1" && it.subgraph?.nodes?.any { node -> node.type == "preview.view" } == true })
        assertTrue(workflow.nodes.any { it.id == "batch1" && it.type == SHORTS_BATCH_SUBGRAPH_NODE_TYPE && it.subgraph?.nodes?.any { node -> node.type == "media.video_stitch" } == true })
        assertTrue(ConnectionRef("scenes", "result", "scene1", "prompt") in workflow.connections)
        assertTrue(ConnectionRef("scene1", "video", "batch1", "video1") in workflow.connections)
        assertTrue(ConnectionRef("batch1", "video", "finalBatch", "video1") in workflow.connections)
        assertTrue(ConnectionRef("finalBatch", "video", "captionOverlay", "video") in workflow.connections)
    }

    @Test
    fun videoMotionShortsUseSceneSubgraphsAndBatchStitching() {
        val workflow = WorkflowCatalog.VideoShorts.workflow

        assertEquals("ai-shorts-video", workflow.id)
        assertEquals(0, workflow.nodes.count { it.id.startsWith("frame") })
        assertEquals(0, workflow.nodes.count { it.id.startsWith("prompt") })
        assertTrue(workflow.nodes.any { it.id == "scene1" && it.type == SHORTS_SCENE_SUBGRAPH_NODE_TYPE && it.subgraph?.nodes?.any { node -> node.type == "sd.txt2vid" } == true })
        assertTrue(workflow.nodes.any { it.id == "scene1" && it.subgraph?.nodes?.any { node -> node.type == "preview.view" } == true })
        assertTrue(workflow.nodes.any { it.id == "batch2" && it.type == SHORTS_BATCH_SUBGRAPH_NODE_TYPE && it.subgraph?.nodes?.any { node -> node.type == "media.video_stitch" } == true })
        assertTrue(ConnectionRef("scenes", "result", "scene1", "prompt") in workflow.connections)
        assertTrue(ConnectionRef("scene1", "video", "batch1", "video1") in workflow.connections)
        assertTrue(ConnectionRef("batch2", "video", "finalBatch", "video2") in workflow.connections)
        assertTrue(ConnectionRef("finalBatch", "video", "captionOverlay", "video") in workflow.connections)
    }
}
