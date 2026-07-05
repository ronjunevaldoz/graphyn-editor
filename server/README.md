# server

JVM Ktor server that exposes workflow execution over HTTP — CRUD for stored workflows, a
parameterized "run by id" API, ad-hoc execute/validate, and async job tracking.

## Running

```bash
./gradlew :server:run
```

Starts on `http://localhost:8080`. Workflows persist to `~/.graphyn/workflows` by default
(`FileWorkflowStore`) — the same store the desktop app's editor uses, so workflows saved there
are immediately visible here too.

### Docker

```bash
docker compose -f server/docker-compose.yml up --build
```

Builds the fat jar in a `eclipse-temurin:21-jdk` stage and runs it on `eclipse-temurin:21-jre`.
Published workflows persist across restarts via the `graphyn-server-data` volume (mounted at
`/data`, which `FileWorkflowStore` uses as `$HOME/.graphyn/workflows`).

Auth is off by default (see below). To require a Bearer token, uncomment the `environment:`
block in `server/docker-compose.yml` — do **not** set `GRAPHYN_API_KEY=""`; a present-but-empty
value is treated as a real (empty-string) key to match, not "no auth".

## Endpoints

| Method | Path | Description |
|---|---|---|
| `GET` | `/` | Plain-text health check — always exempt from auth |
| `GET` | `/nodes` | List every registered `NodeSpec` |
| `GET` | `/nodes/{type}` | Fetch one node spec (404 if unregistered) |
| `GET` | `/workflows` | List published workflows (`[WorkflowMeta]`, newest first) |
| `GET` | `/workflows/{id}` | Fetch one published workflow's full JSON |
| `POST` | `/workflows` | Publish/upsert a workflow (body: `WorkflowDocument` JSON) → `WorkflowMeta` (201) |
| `DELETE` | `/workflows/{id}` | Unpublish (deletes all version history too) |
| `POST` | `/workflows/{id}/run` | Run a published workflow by id, with optional per-node/port `overrides`; `async: true` → `{runId}` (202), else synchronous `WorkflowExecutionResult` |
| `POST` | `/validate` | Validate an ad-hoc workflow (body: `WorkflowDocument`) → `[ValidationError]` |
| `POST` | `/execute` | Run an ad-hoc workflow synchronously → `WorkflowExecutionResult` |
| `POST` | `/executions` | Queue an ad-hoc workflow in the background → `{runId}` (202) |
| `GET` | `/executions/{id}/events` (SSE) | Stream `ExecutionStreamMessage` frames for a run until terminal |
| `POST` | `/jobs` | Queue a workflow as a persisted job → `WorkflowJob` (202) |
| `GET` | `/jobs` | List jobs, newest first; `?state=RUNNING` to filter |
| `GET` | `/jobs/{id}` | Fetch one job (404 if unknown) |
| `DELETE` | `/jobs/{id}` | Cancel a queued/running job (204); 409 if already terminal |

`/workflows/{id}/run` is the "publish once, call as an API" path: callers only send the inputs
they want to vary, keyed by node id → port name, without knowing or resending the full graph.

## Auth

Bearer-token auth is on by default but a no-op until a key is configured — set `GRAPHYN_API_KEY`
(env var) or `GraphynKtorConfig.apiKey` to require it. `GET /` is always exempt so load balancers
can probe it freely.

## Default node set

`Application.kt`'s `main()` installs `Graphyn` with no extra plugins, so only the ~26 core node
types are registered (`control.*`, `listops.*`, `types.*`, `text.*`, `io.*`, `json.*`,
`preview.view`, `media.file_output`). Workflows using media/AI/Stable-Diffusion nodes (most
workflows saved from the desktop editor, e.g. shorts pipelines) will fail `/validate` with
`unknown_node_type` until the relevant plugins are installed:

```kotlin
fun Application.module() {
    install(Graphyn) {
        plugins(MediaCorePlugin, MediaAiPlugin /* , ... */)
    }
}
```

## Known gotcha: `WorkflowValue`'s discriminator key differs by route

`/workflows` (publish/fetch) serializes `WorkflowValue` with `"kind"` as the type discriminator
(`GraphynWorkflowJson`, `classDiscriminator = "kind"`). `/workflows/{id}/run`'s `overrides` body
and `/execute`'s/`/executions`'s output use a separate `Json` instance with the **default**
discriminator, `"type"`. Sending an override with `"kind"` throws a decoding exception; use
`"type"` there instead. Not yet unified — worth fixing so the API is consistent everywhere.
