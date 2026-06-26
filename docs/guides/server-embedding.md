# Embedding the Graphyn Server

The `server` module ships as a standalone Ktor application **and** as an installable Ktor plugin (`install(Graphyn)`), so you can embed workflow execution directly into an existing Ktor server.

---

## Quick start

Add the `:server` module to your project and call `install(Graphyn)` in your application module:

```kotlin
// build.gradle.kts
dependencies {
    implementation(projects.server)
}
```

```kotlin
// Application.kt
fun Application.module() {
    // Your existing plugins …
    install(Authentication) { /* … */ }

    // Mount Graphyn under /graphyn — all workflow routes land here.
    install(Graphyn) {
        routePrefix = "/graphyn"
        requireApiKey = false   // your auth already handles it
    }

    routing {
        get("/health") { call.respondText("ok") }
    }
}
```

---

## Configuration reference

| Property | Type | Default | Description |
|---|---|---|---|
| `routePrefix` | `String` | `""` | URL prefix for all Graphyn routes. `"/graphyn"` mounts execution at `/graphyn/execute`. |
| `requireApiKey` | `Boolean` | `true` | Install [GraphynAuthPlugin] to enforce Bearer-token auth. Set `false` when your gateway handles auth. |
| `apiKey` | `String?` | `null` | Explicit API key. `null` falls back to the `GRAPHYN_API_KEY` env var. |
| `store` | `WorkflowStore` | `FileWorkflowStore()` | Persistence back-end for saved workflows. |
| `extraPlugins` | `List<GraphynPlugin>` | `[]` | Additional node plugins on top of the built-in runtime (see [Custom node plugins](#custom-node-plugins)). |

---

## Routes

After installation, Graphyn mounts the following routes (relative to `routePrefix`):

### Execution

| Method | Path | Description |
|---|---|---|
| `POST` | `/validate` | Validate a workflow. Returns `List<ValidationError>` (empty = valid). |
| `POST` | `/execute` | Run synchronously. Returns `WorkflowExecutionResult`. |
| `POST` | `/executions` | Start async run. Returns `{ runId }` with `202 Accepted`. |
| `GET` | `/executions/{id}/events` | SSE stream of `ExecutionStreamMessage` frames until terminal frame. |

### Workflow CRUD

| Method | Path | Description |
|---|---|---|
| `GET` | `/workflows` | List saved workflows (`List<WorkflowMeta>`). |
| `GET` | `/workflows/{id}` | Load a `WorkflowDefinition` (404 when not found). |
| `POST` | `/workflows` | Save / upsert a workflow. Returns `WorkflowMeta` with `201 Created`. |
| `DELETE` | `/workflows/{id}` | Delete a workflow. Returns `204 No Content`. |

---

## Authentication

By default `requireApiKey = true`. Set `GRAPHYN_API_KEY` in the environment to activate enforcement:

```bash
GRAPHYN_API_KEY=my-secret ./gradlew :server:run
```

Clients pass the key as a Bearer token:

```
Authorization: Bearer my-secret
```

`GET /` (the health-check) is always exempt. All other routes return `401 Unauthorized` without a valid token.

To supply the key programmatically (useful in tests or multi-tenant setups):

```kotlin
install(Graphyn) {
    requireApiKey = true
    apiKey = resolveKeyFromVault()   // your key store
}
```

Set `requireApiKey = false` when your API gateway or reverse proxy already handles auth upstream.

---

## Custom node plugins

`GraphynRuntime.runtimePlugins` (io, text, control, json, list-ops, preview) are always installed.
Add your own domain nodes with `extraPlugins`:

```kotlin
// 1. Define a plugin
object MyPlugin : GraphynPlugin {
    override fun install(registrar: DefaultGraphynPluginRegistry) {
        registrar.registerSpec(NodeSpec(
            type = "my.transform",
            label = "My Transform",
            inputs  = listOf(PortSpec("input", WorkflowType.StringType)),
            outputs = listOf(PortSpec("output", WorkflowType.StringType)),
        ))
        registrar.registerExecutor("my.transform") { inputs ->
            val text = (inputs["input"] as? WorkflowValue.StringValue)?.value ?: ""
            mapOf("output" to WorkflowValue.StringValue(text.uppercase()))
        }
    }
}

// 2. Install it
install(Graphyn) {
    plugins(MyPlugin)           // convenience vararg setter
    // or: extraPlugins = listOf(MyPlugin, AnotherPlugin)
}
```

Any workflow containing `my.transform` nodes can now be executed and validated server-side.

For media workflows, install `MediaCorePlugin` and `MediaAiPlugin` and ensure FFmpeg is on the server's `PATH`:

```kotlin
install(Graphyn) {
    plugins(MediaCorePlugin, MediaAiPlugin)
}
```

---

## Custom persistence

The default `FileWorkflowStore` writes JSON files in the working directory. Replace it with your own implementation of `WorkflowStore`:

```kotlin
class PostgresWorkflowStore(private val db: Database) : WorkflowStore {
    override suspend fun list(): List<WorkflowMeta> = TODO()
    override suspend fun load(id: String): WorkflowDefinition? = TODO()
    override suspend fun save(workflow: WorkflowDefinition): WorkflowMeta = TODO()
    override suspend fun delete(id: String) = TODO()
}

install(Graphyn) {
    store = PostgresWorkflowStore(db)
}
```

---

## Standalone server

If you don't have an existing Ktor app, run the bundled server directly:

```bash
./gradlew :server:run
# or build a fat JAR:
./gradlew :server:buildFatJar
java -jar server/build/libs/server-all.jar
```

The standalone server binds to `0.0.0.0:8080` and uses `install(Graphyn)` with all defaults.

---

## Concurrent run limit

The server caps concurrent async workflow executions at 10 by default. When the limit is reached, `POST /executions` returns `503 Service Unavailable`. The limit is currently not configurable via `GraphynKtorConfig`; for higher throughput deploy multiple instances behind a load balancer.

---

## SSE streaming example (JavaScript)

```javascript
const { runId } = await fetch('/graphyn/executions', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer my-secret' },
    body: JSON.stringify(workflow),
}).then(r => r.json());

const es = new EventSource(`/graphyn/executions/${runId}/events`);
es.addEventListener('event',     e => console.log('node done', JSON.parse(e.data)));
es.addEventListener('completed', e => { console.log('done', JSON.parse(e.data)); es.close(); });
es.addEventListener('failed',    e => { console.error('failed', JSON.parse(e.data)); es.close(); });
```
