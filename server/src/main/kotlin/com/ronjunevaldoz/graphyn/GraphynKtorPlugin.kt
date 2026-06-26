package com.ronjunevaldoz.graphyn

import com.ronjunevaldoz.graphyn.core.store.FileWorkflowStore
import com.ronjunevaldoz.graphyn.core.store.WorkflowStore
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPlugin
import com.ronjunevaldoz.graphyn.runtime.GraphynRuntime
import io.ktor.server.application.Application
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.install
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.sse.SSE
import kotlinx.serialization.json.Json

/**
 * Configuration for the [Graphyn] Ktor plugin.
 *
 * ```kotlin
 * install(Graphyn) {
 *     plugins(MediaCorePlugin, MyCustomPlugin)
 *     store = MyDatabaseStore()
 *     routePrefix = "/graphyn"
 *     requireApiKey = true   // honours GRAPHYN_API_KEY env var when true (default)
 * }
 * ```
 *
 * [GraphynRuntime.runtimePlugins] are always included. [plugins] adds extra specs and executors
 * on top — e.g. `MediaCorePlugin` for video/audio nodes, or your own domain plugin.
 */
class GraphynKtorConfig {
    /** Additional node plugins to install on top of [GraphynRuntime.runtimePlugins]. */
    var extraPlugins: List<GraphynPlugin> = emptyList()

    /** Where workflows are persisted. Defaults to [FileWorkflowStore] in the working directory. */
    var store: WorkflowStore = FileWorkflowStore()

    /**
     * URL prefix for all Graphyn routes.
     * e.g. `"/graphyn"` mounts execution at `/graphyn/execute` and CRUD at `/graphyn/workflows`.
     * Empty string (default) mounts at the root.
     */
    var routePrefix: String = ""

    /**
     * When `true` (default), [GraphynAuthPlugin] is installed to enforce Bearer-token auth.
     * Set `false` when the host app handles auth upstream (e.g. an API gateway).
     */
    var requireApiKey: Boolean = true

    /**
     * Explicit API key used when [requireApiKey] is `true`.
     * `null` (default) falls back to the `GRAPHYN_API_KEY` environment variable.
     */
    var apiKey: String? = null

    /** Convenience setter — replaces [extraPlugins]. */
    fun plugins(vararg p: GraphynPlugin) { extraPlugins = p.toList() }
}

/**
 * Ktor application plugin that mounts the full Graphyn workflow API onto an existing server.
 *
 * Installs SSE support, the execution engine, and CRUD workflow storage.  The host app retains
 * full control over auth, TLS, and routing — Graphyn adds its routes under [GraphynKtorConfig.routePrefix].
 *
 * Usage:
 * ```kotlin
 * fun Application.module() {
 *     install(Graphyn) {
 *         plugins(MediaCorePlugin)
 *         store = MyDatabaseStore()
 *         routePrefix = "/api/graphyn"
 *     }
 * }
 * ```
 */
val Graphyn = createApplicationPlugin("Graphyn", ::GraphynKtorConfig) {
    val cfg = pluginConfig
    val runtime = createGraphynServerRuntime(cfg.extraPlugins)
    val registry = GraphynRunRegistry(runtime.executionEngine)
    val json = Json { encodeDefaults = false; ignoreUnknownKeys = true }

    application.install(SSE)
    if (cfg.requireApiKey) application.install(GraphynAuthPlugin) { apiKey = cfg.apiKey }

    application.routing {
        route(cfg.routePrefix) {
            executionRoutes(runtime, registry, json)
            workflowRoutes(cfg.store, json)
        }
    }
}
