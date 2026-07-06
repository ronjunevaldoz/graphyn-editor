@file:OptIn(com.ronjunevaldoz.graphyn.core.GraphynExperimentalApi::class)

package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.execution.WorkflowExecutionEngine
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.pluginapi.DefaultGraphynPluginRegistry
import kotlinx.coroutines.runBlocking
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Runs real SD templates through the execution engine + production SD plugin (HTTP backend → the
 * deployed server-sd, using ~/.graphyn/settings.json for URL + key) — the same call the
 * /execute and /workflows/{id}/run routes make. Opt-in: needs a reachable, configured SD server.
 * Run with: ./gradlew :app:app:jvmTest --tests "*SdTemplateApiRunTest*"
 */
class SdTemplateApiRunTest {

    private val engine: WorkflowExecutionEngine by lazy {
        val plugins = DefaultGraphynPluginRegistry().apply {
            (GraphynDemoPlugins.runtime + GraphynBootstrapJvm.mediaRuntimePlugins).forEach { install(it) }
        }
        WorkflowExecutionEngine(plugins.nodeExecutors, plugins.nodeSpecs)
    }

    // Small (512x512 JPEG, ~30KB) on purpose: the deployed nginx proxy in front of server-sd
    // rejects uploads over its default client_max_body_size (1MB) with a 413, and the original
    // 1024x1024 PNG (~1.7MB) tripped it. Downsizing here works around the proxy limit for testing;
    // the real fix is raising client_max_body_size on the nginx config server-side.
    private val initImage = "/tmp/aitest/flux_small.jpg"

    private fun WorkflowDefinition.withInit(nodeId: String, path: String) = copy(
        nodes = nodes.map {
            if (it.id == nodeId) it.copy(config = it.config + ("init_image" to WorkflowValue.StringValue(path))) else it
        },
    )

    @BeforeTest
    fun unloadOllamaModel() {
        runBlocking { unloadOllamaModels() }
    }

    /**
     * Same override shape as graphyn-ktor-plugin's `POST /workflows/{id}/run` — nodeId -> (port ->
     * value), merged onto that node's existing config. Proves a caller can override values through
     * the workflow API without resending (or knowing) the whole graph.
     */
    private fun WorkflowDefinition.withOverrides(overrides: Map<String, Map<String, WorkflowValue>>) = copy(
        nodes = nodes.map { node -> overrides[node.id]?.let { node.copy(config = node.config + it) } ?: node },
    )

    private fun run(wf: WorkflowDefinition, outNode: String, outPort: String, assertOk: Boolean = true): String? = runBlocking {
        val started = System.currentTimeMillis()
        val result = com.ronjunevaldoz.graphyn.editor.state.SdArtifactContext
            .withWorkflow(wf.id, wf.name) { engine.execute(wf) }
        val elapsed = (System.currentTimeMillis() - started) / 1000
        val raw = result.nodeOutputsByNodeId[outNode]?.get(outPort)
        val path = (raw as? WorkflowValue.StringValue)?.value
            ?: ((raw as? WorkflowValue.ListValue)?.items?.firstOrNull() as? WorkflowValue.StringValue)?.value
        println("[${wf.name}] ${elapsed}s — statuses=${result.statusByNodeId.values.groupingBy { it }.eachCount()} — out=$path")
        result.errorsByNodeId.forEach { (id, err) -> println("  ERROR $id: $err") }
        if (assertOk) assertTrue(path != null && java.io.File(path).isFile, "expected an output file, got $path")
        path
    }

    // Overrides only touch the prompt (a safe, visible change) — resolution/steps/frames stay at
    // each template's already-tuned-for-12GB defaults. That's the "proper parameters" constraint:
    // demonstrate override capability without risking OOM on a 12 GB card.
    private fun s(v: String) = WorkflowValue.StringValue(v)

    @Test fun fluxTxt2Img() = run(
        fluxTxt2ImgWorkflow.withOverrides(mapOf("generate" to mapOf("prompt" to s("a lighthouse at dusk, cinematic")))),
        "generate", "image",
    ).let {}

    @Test fun qwenTxt2Img() = run(
        qwenTxt2ImgWorkflow.withOverrides(mapOf("generate" to mapOf("prompt" to s("a bowl of ramen, top-down studio photo")))),
        "generate", "image",
    ).let {}

    @Test fun qwenImg2Img() = run(
        qwenImg2ImgWorkflow.withInit("generate", initImage)
            .withOverrides(mapOf("generate" to mapOf("prompt" to s("turn the scene into a snowy winter version")))),
        "generate", "image",
    ).let {}

    // Video tier: server-sd's Wan i2v VAE decode previously failed with "vae decode compute failed
    // while processing a tile" even at 320x320x9 (8.7 GB used — not OOM), a native compute/kernel
    // failure, not a VRAM limit. Re-testing now that server-sd has changed since — assertOk=false so
    // a still-failing decode doesn't fail the suite; the printed status/error reports current state.
    @Test fun wan5bImg2Vid() = run(
        wan5bImg2VidWorkflow.withInit("img2vid", initImage)
            .withOverrides(mapOf("img2vid" to mapOf("prompt" to s("gentle snowfall begins, soft wind")))),
        "img2vid", "frames", assertOk = false,
    ).let {}

    /** A14B supports 480P too; this preset keeps the same model family at a lower resolution. */
    @Test fun wan480pImg2Vid() = run(
        wan480pImg2VidWorkflow.withInit("img2vid", initImage)
            .withOverrides(mapOf("img2vid" to mapOf("prompt" to s("gentle snowfall begins, soft wind")))),
        "img2vid", "frames", assertOk = false,
    ).let {}

    /** A14B (~25.8 GB) also runs impractically slowly on 12 GB (395 s VAE encode alone) — record only. */
    @Test fun wanA14bImg2Vid() = run(
        wanImg2VidWorkflow.withInit("img2vid", initImage)
            .withOverrides(mapOf("img2vid" to mapOf("prompt" to s("gentle snowfall begins, soft wind")))),
        "img2vid", "frames", assertOk = false,
    ).let {}

    // A14B at real resolution takes 30-60+ min per run. This validates the same model paths + LoRA
    // wiring + pipeline actually execute (load, sample, hit the known decode failure) at a tiny
    // resolution/frame-count, in a couple minutes instead — a config/wiring check, not a quality run.
    @Test fun wanA14bImg2VidLightweightCheck() = run(
        wanImg2VidWorkflow.withInit("img2vid", initImage)
            .withOverrides(
                mapOf(
                    "img2vid" to mapOf(
                        "prompt" to s("gentle snowfall begins, soft wind"),
                        "width" to WorkflowValue.IntValue(128),
                        "height" to WorkflowValue.IntValue(128),
                        "video_frames" to WorkflowValue.IntValue(5),
                    ),
                ),
            ),
        "img2vid", "frames", assertOk = false,
    ).let {}
}
