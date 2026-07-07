package com.ronjunevaldoz.graphyn.plugins.stablesd

import com.ronjunevaldoz.graphyn.core.model.WorkflowValue.StringValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Exercises the exact executor the editor's execution engine calls when a graph wires an
 * `sd.server` node's output into `sd.txt2img`'s `server` input — i.e. what "drag a node, connect
 * a wire, run the workflow" resolves to, without needing the full Compose editor UI running.
 */
class SdServerNodeWiringTest {
    private class RecordingBackend : StableDiffusionBackend {
        var lastRequest: SdGenerateImageRequest? = null
        override suspend fun generateImage(request: SdGenerateImageRequest): SdImageResult {
            lastRequest = request
            return SdImageResult(imagePaths = listOf("/tmp/out.png"))
        }
        override suspend fun generateVideo(request: SdGenerateVideoRequest) = error("not used")
    }

    private val baseInputs = mapOf(
        "context" to SdTokens.context(emptyMap()),
        "sampler" to SdTokens.sampler(emptyMap()),
        "prompt" to StringValue("a cat"),
    )

    @Test
    fun wiringAnSdServerNodeIntoTxt2imgDeliversItsConfigToTheBackend() = runTest {
        val backend = RecordingBackend()
        // What StableDiffusionPlugin's sd.server executor produces (SdConfigNodeExecutors.kt),
        // wired into sd.txt2img's `server` port.
        val serverToken = SdTokens.server(
            mapOf(
                "base_url" to StringValue("https://my-app.modal.run"),
                "api_key" to StringValue("modal-key"),
            ),
        )
        txt2imgExecutor(backend).execute(baseInputs + ("server" to serverToken))

        val server = backend.lastRequest?.server
        assertEquals("https://my-app.modal.run", server?.baseUrl)
        assertEquals("modal-key", server?.apiKey)
    }

    @Test
    fun leavingTheServerPortUnwiredKeepsTheRequestServerNull() = runTest {
        val backend = RecordingBackend()
        txt2imgExecutor(backend).execute(baseInputs)

        assertNull(backend.lastRequest?.server)
    }
}
