# graphyn-core

Pure Kotlin workflow model — no Compose, no Android, no UI dependency.

## What's here

| Package | Contents |
|---|---|
| `model` | `WorkflowDefinition`, `NodeRef`, `ConnectionRef`, `PortSpec`, `WorkflowValue`, `WorkflowType` |
| `execution` | `WorkflowExecutionEngine`, `NodeExecutorRegistry`, `NodeExecutionStatus` |
| `validation` | `WorkflowValidator`, `WorkflowValidationResult` |
| `registry` | `NodeSpecRegistry` |
| `serialization` | `WorkflowDocument`, `DefaultWorkflowDocumentCodec` |

## Dependency

```kotlin
implementation("io.github.ronjunevaldoz:graphyn-core:0.1.0")
```

## Usage

```kotlin
// Execute a workflow
val engine = WorkflowExecutionEngine(executorRegistry, nodeSpecRegistry)
val result = engine.execute(workflow)

// Serialize / deserialize
val json   = DefaultWorkflowDocumentCodec().encode(workflow)
val loaded = DefaultWorkflowDocumentCodec().decode(json)
```

No platform or Compose imports — safe for server, CLI, and pure KMP contexts.
