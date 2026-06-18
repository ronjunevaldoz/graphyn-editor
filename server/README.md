# server

JVM Ktor server that exposes workflow execution over HTTP.

## Running

```bash
./gradlew :server:run
```

## Endpoints

| Method | Path | Description |
|---|---|---|
| `POST` | `/execute` | Execute a workflow; body is `WorkflowDocument` JSON, response is `WorkflowExecutionResult` JSON |
| `GET` | `/health` | Health check |

## Wiring plugins

```kotlin
// GraphynServerRuntime.kt
val runtime = GraphynServerRuntime().apply {
    install(MathPlugin)
    install(SampleLoggerPlugin)
}
```

Plugins registered here provide the executors used by `POST /execute`. The server module is not published to Maven Central — it is a standalone JVM application.
