package com.ronjunevaldoz.graphyn.plugins.shorts

import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ShortsBuildersTest {

    @Test
    fun storyboardGeneratorWiresOllamaCallThroughValidate() {
        val wf = storyboardGeneratorSubgraph("origami cranes")
        assertTrue(wf.nodes.any { it.type == "io.http_request" })
        assertTrue(wf.nodes.any { it.type == ShortsNodeTypes.OLLAMA_URL })
        assertTrue(wf.nodes.any { it.type == ShortsNodeTypes.OLLAMA_BODY })
        assertTrue(wf.nodes.any { it.type == ShortsNodeTypes.STORYBOARD_VALIDATE })
        val body = wf.nodes.first { it.id == "ollama_body" }
        assertEquals(WorkflowValue.StringValue("origami cranes"), body.config["topic"])
    }

    @Test
    fun imageMotionSceneUsesFluxAndKenBurns() {
        val scene = imageMotionSceneSubgraph(prompt = "a red kite", niche = "hobbies")
        assertTrue(scene.nodes.any { it.type == "sd.txt2img" })
        assertTrue(scene.nodes.any { it.type == "media.ken_burns" })
        assertTrue(scene.nodes.any { it.type == ShortsConstants.PROMPT_ENHANCE_NODE_TYPE })
    }

    @Test
    fun dynamicSceneLeavesPromptEnhanceUnconfigured() {
        val scene = imageMotionSceneSubgraphDynamic(id = "scene-0")
        assertEquals(emptyMap(), scene.nodes.first { it.id == "promptEnhance" }.config)
    }

    @Test
    fun stitchBatchStitchesAVideoList() {
        val batch = stitchBatchSubgraph(0)
        assertTrue(batch.nodes.any { it.type == "media.videos_list" })
        assertTrue(batch.nodes.any { it.type == "media.video_stitch" })
    }
}
