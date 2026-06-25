# graphyn-core

Pure Kotlin workflow model — no Compose, no Android, no UI dependency.

## What's here

`core` is a folder of focused, layered modules — there is no umbrella `:core` module.
Depend on the specific submodule(s) you need; everything builds up from `core:model`.
Package names are unchanged (`com.ronjunevaldoz.graphyn.core.*`).

| Module | Packages | Contents |
|---|---|---|
| `core:model` | `model`, `validation`, `registry`, `sync` | `WorkflowDefinition`, `NodeRef`, `ConnectionRef`, `PortSpec`, `WorkflowValue`, `WorkflowType`, `WorkflowGraphValidator`, `NodeSpecRegistry`, `NodeGroups` |
| `core:execution` | `execution` | `WorkflowExecutionEngine`, `NodeExecutorRegistry`, `NodeExecutionStatus` |
| `core:serialization` | `serialization` | `WorkflowDocument`, `DefaultWorkflowDocumentCodec` |
| `core:data` | `store` | `WorkflowStore`, `InMemoryWorkflowStore`, file/local-storage persistence |

## Dependency

Consumed by Gradle project path:

```kotlin
implementation(projects.core.model)
implementation(projects.core.execution)     // if you execute workflows
implementation(projects.core.serialization) // if you (de)serialize
implementation(projects.core.data)          // if you persist
```

> Maven Central publishing is per-submodule and is being reworked after the split
> (the previous aggregate `graphyn-core` artifact was removed).

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
