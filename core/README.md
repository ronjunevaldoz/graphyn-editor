# graphyn-core

Pure Kotlin workflow model — no Compose, no Android, no UI dependency.

## What's here

`core` is a folder of focused, layered modules. `:core` itself is a thin aggregator
that re-exports the four submodules via `api`. Package names are unchanged
(`com.ronjunevaldoz.graphyn.core.*`), so the split is transparent to source code.

| Module | Packages | Contents |
|---|---|---|
| `core:model` | `model`, `validation`, `registry`, `sync` | `WorkflowDefinition`, `NodeRef`, `ConnectionRef`, `PortSpec`, `WorkflowValue`, `WorkflowType`, `WorkflowGraphValidator`, `NodeSpecRegistry`, `NodeGroups` |
| `core:execution` | `execution` | `WorkflowExecutionEngine`, `NodeExecutorRegistry`, `NodeExecutionStatus` |
| `core:serialization` | `serialization` | `WorkflowDocument`, `DefaultWorkflowDocumentCodec` |
| `core:data` | `store` | `WorkflowStore`, `InMemoryWorkflowStore`, file/local-storage persistence |

## Dependency

```kotlin
// Aggregator — pulls in all four submodules:
implementation("io.github.ronjunevaldoz:graphyn-core:0.2.1")
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
