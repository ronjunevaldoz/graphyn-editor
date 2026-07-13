package com.ronjunevaldoz.graphyn.mcp

import com.ronjunevaldoz.graphyn.GraphynRunRegistry
import com.ronjunevaldoz.graphyn.createGraphynServerRuntime
import com.ronjunevaldoz.graphyn.core.store.FileWorkflowStore
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPlugin
import java.io.File
import com.ronjunevaldoz.graphyn.plugins.mediaai.MediaAiPlugin
import com.ronjunevaldoz.graphyn.plugins.mediacore.MediaCorePlugin
import com.ronjunevaldoz.graphyn.plugins.shorts.ShortsPlugin
import com.ronjunevaldoz.graphyn.plugins.stablesd.StableDiffusionPlugin
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.serialization.json.Json

fun main() {
    // stdout is the MCP JSON-RPC channel. Capture the real stream first, then redirect
    // System.out globally so any dependency that logs/prints carelessly (kotlin-logging's
    // own init banner, logback's default console appender) lands on stderr instead of
    // corrupting the protocol stream.
    val protocolOut = System.out
    System.setOut(System.err)

    val store = FileWorkflowStore(root = resolveWorkflowsDir())
    val runtime = createGraphynServerRuntime(extraPlugins = resolveExtraPlugins())
    val registry = GraphynRunRegistry(runtime.executionEngine)
    val json = Json { encodeDefaults = false; ignoreUnknownKeys = true }

    val server = Server(
        Implementation(name = "graphyn-workflows", version = "0.1.0"),
        ServerOptions(capabilities = ServerCapabilities(tools = ServerCapabilities.Tools(listChanged = false))),
    )
    registerWorkflowTools(server, store, runtime, registry, json)

    val transport = StdioServerTransport(
        System.`in`.asSource().buffered(),
        protocolOut.asSink().buffered(),
    ) { }

    runBlocking {
        val session = server.createSession(transport)
        val done = Job()
        session.onClose { done.complete() }
        done.join()
    }
}

/**
 * Defaults to <project-root>/.graphyn/workflows (the process's cwd — reliable here since
 * .mcp.json launches this binary via a relative command path, so the client already cwds to
 * the project root). Keeps different projects' MCP-published workflows from colliding in one
 * global ~/.graphyn/workflows folder — the desktop editor's own default. Override with
 * GRAPHYN_MCP_WORKFLOWS_DIR to point elsewhere, including back at the global default.
 */
private fun resolveWorkflowsDir(): File {
    val override = System.getenv("GRAPHYN_MCP_WORKFLOWS_DIR")?.trim()
    if (!override.isNullOrEmpty()) return File(override)
    return File(System.getProperty("user.dir"), ".graphyn/workflows")
}

private val AVAILABLE_PLUGINS: Map<String, () -> GraphynPlugin> = mapOf(
    "shorts" to { ShortsPlugin },
    "media-core" to { MediaCorePlugin() },
    "media-ai" to { MediaAiPlugin() },
    "stable-diffusion" to { StableDiffusionPlugin() },
)

/**
 * GRAPHYN_MCP_PLUGINS="shorts,media-core" to trim the set. Unset, empty, or "all" installs
 * every plugin in [AVAILABLE_PLUGINS] — "all" is an explicit spelling for configs (like
 * .mcp.json's env block) that always set the key rather than relying on its absence.
 */
private fun resolveExtraPlugins(): List<GraphynPlugin> {
    val raw = System.getenv("GRAPHYN_MCP_PLUGINS")?.trim()
    val names = if (raw.isNullOrEmpty() || raw == "all") {
        AVAILABLE_PLUGINS.keys.toList()
    } else {
        raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }
    return names.map { name ->
        AVAILABLE_PLUGINS[name]?.invoke()
            ?: error("Unknown plugin '$name' in GRAPHYN_MCP_PLUGINS. Available: all, ${AVAILABLE_PLUGINS.keys.joinToString()}")
    }
}
