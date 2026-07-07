package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.execution.WorkflowExecutionEngine
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.pluginapi.DefaultGraphynPluginRegistry
import kotlinx.coroutines.runBlocking
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

/** Runs the storyboard-driven image-motion-short template end to end against the real deployment. */
class ImageMotionStoryboardShortRunTest {

    private val engine: WorkflowExecutionEngine by lazy {
        val plugins = DefaultGraphynPluginRegistry().apply {
            (GraphynDemoPlugins.runtime + GraphynBootstrapJvm.mediaRuntimePlugins).forEach { install(it) }
        }
        WorkflowExecutionEngine(plugins.nodeExecutors, plugins.nodeSpecs)
    }

    @BeforeTest
    fun unloadOllamaModel() {
        if (System.getenv("GRAPHYN_LIVE_RUN_TESTS").isNullOrBlank()) return
        runBlocking { unloadOllamaModels() }
    }

    // Requires GRAPHYN_LIVE_RUN_TESTS=1 — hits a real Ollama + Flux/Modal deployment end to end
    // (GPU time, real cost, several minutes). Skipped by default like every other integration
    // test in this codebase (e.g. LlamaTtsEngineTest's GRAPHYN_TTS_MODEL gate).
    @Test fun runsStoryboardShort() = runBlocking {
        if (System.getenv("GRAPHYN_LIVE_RUN_TESTS").isNullOrBlank()) return@runBlocking
        val wf = imageMotionStoryboardShortWorkflow("a quick weeknight pasta dinner")
        val started = System.currentTimeMillis()
        val result = com.ronjunevaldoz.graphyn.editor.state.SdArtifactContext
            .withWorkflow(wf.id, wf.name) { engine.execute(wf) }
        val elapsed = (System.currentTimeMillis() - started) / 1000
        val raw = result.nodeOutputsByNodeId["output"]?.get("file_path")
        val path = (raw as? WorkflowValue.StringValue)?.value
        println("[${wf.name}] ${elapsed}s — statuses=${result.statusByNodeId.values.groupingBy { it }.eachCount()} — out=$path")
        result.errorsByNodeId.forEach { (id, err) -> println("  ERROR $id: $err") }
        assertTrue(path != null && java.io.File(path).isFile, "expected an output video file, got $path")
    }
}
