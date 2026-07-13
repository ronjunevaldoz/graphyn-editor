package com.ronjunevaldoz.graphyn.mcp

import com.ronjunevaldoz.graphyn.GraphynRunRegistry
import com.ronjunevaldoz.graphyn.createGraphynServerRuntime
import com.ronjunevaldoz.graphyn.core.store.FileWorkflowStore
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

    val store = FileWorkflowStore()
    val runtime = createGraphynServerRuntime(
        extraPlugins = listOf(ShortsPlugin, MediaCorePlugin(), MediaAiPlugin(), StableDiffusionPlugin()),
    )
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
