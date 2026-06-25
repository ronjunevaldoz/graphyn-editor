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

Each submodule is published independently to Maven Central (the old aggregate
`graphyn-core` artifact was replaced by these):

```kotlin
implementation("io.github.ronjunevaldoz:graphyn-core-model:0.2.1")
implementation("io.github.ronjunevaldoz:graphyn-core-execution:0.2.1")     // if you execute workflows
implementation("io.github.ronjunevaldoz:graphyn-core-serialization:0.2.1") // if you (de)serialize
implementation("io.github.ronjunevaldoz:graphyn-core-data:0.2.1")          // if you persist
```

Within this repo, depend by project path instead: `implementation(projects.core.model)`, etc.

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
