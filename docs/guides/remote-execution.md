# Remote Execution

By default, `WorkflowExecutionEngine` runs node executors in the same process as the editor. For a SaaS product, you typically want execution to happen on a server — for security, performance, or because executors need server-side resources (credentials, filesystem, outbound HTTP).

This guide shows how to delegate execution to a backend.

---

## How the engine works

`WorkflowExecutionEngine` accepts a map of `NodeExecutor` functions keyed by node type. Each executor is a `suspend` function:

```kotlin
typealias NodeExecutor = suspend (inputs: Map<String, WorkflowValue>) -> Map<String, WorkflowValue>
```

You can replace any or all executors with remote calls.

---

## Pattern — HTTP delegation

Create a `RemoteNodeExecutor` that POSTs the node type and input values to your backend and returns the output values.

```kotlin
class RemoteWorkflowExecutor(
    private val apiUrl: String,
    private val httpClient: HttpClient, // Ktor or your HTTP client of choice
) {
    fun executorFor(nodeType: String): NodeExecutor = { inputs ->
        val response = httpClient.post("$apiUrl/execute/$nodeType") {
            contentType(ContentType.Application.Json)
            setBody(ExecutionRequest(nodeType = nodeType, inputs = inputs.mapValues { it.value.toJson() }))
        }
        val result: ExecutionResponse = response.body()
        result.outputs.mapValues { workflowValueFromJson(it.value) }
    }
}
```

Wire it into the engine at startup:

```kotlin
val remoteExecutor = RemoteWorkflowExecutor(apiUrl = "https://api.yourapp.com", httpClient = client)

val engine = WorkflowExecutionEngine(
    executors = myNodeSpecs.all().associate { spec ->
        spec.type to remoteExecutor.executorFor(spec.type)
    },
    nodeSpecs = myNodeSpecs,
)
```

Pass the engine to the shell:

```kotlin
GraphynEditorShell(
    dependencies = GraphynEditorShellDependencies(
        nodeSpecs = mySpecs,
        executionEngine = engine,
    ),
    state = state,
)
```

---

## Pattern — WebSocket streaming (per-node progress)

For long-running workflows, poll the backend over a WebSocket so that each node's status updates in real time. Drive `state.dispatch(GraphynEditorIntent.SetNodeStatus(...))` as results arrive:

```kotlin
LaunchedEffect(executionId) {
    webSocket.incoming.collect { frame ->
        val event = parseExecutionEvent(frame)
        when (event) {
            is NodeStarted -> state.dispatch(GraphynEditorIntent.SetNodeStatus(event.nodeId, NodeExecutionStatus.Running))
            is NodeFinished -> state.dispatch(GraphynEditorIntent.SetNodeStatus(event.nodeId, NodeExecutionStatus.Success))
            is NodeFailed -> state.dispatch(GraphynEditorIntent.SetNodeStatus(event.nodeId, NodeExecutionStatus.Error))
        }
    }
}
```

The editor will show Running/Success/Error badges on each node as events arrive.

---

## Security note

Never expose credentials (API keys, tokens) to the client-side executor. If `io.http_request` runs in the browser, the user can intercept its headers. Move sensitive executors entirely to the server and have the client-side executor make an authenticated call to your backend instead.

---

## Server module

The `server/` module in this repo provides a Ktor server skeleton ready to host node executors. See `server/src/main/kotlin` for the entry point.
