# Plugin API Draft

The plugin model should support external node packs without forcing the core to know about UI details.

## Design Goal

- Core stays library-first
- Plugins can register specs, executors, and optional editor panels
- JVM targets can optionally support auto-discovery later
- Non-JVM targets should use explicit registration

## Suggested Shapes

```kotlin
interface GraphynPlugin {
    val id: String
    val displayName: String

    fun register(context: GraphynPluginContext)
}

interface GraphynPluginContext {
    fun registerNodeSpec(spec: NodeSpec)
    fun registerExecutor(type: String, executor: NodeExecutor)
    fun registerPanel(type: String, factory: NodePanelFactory)
}

fun interface NodePanelFactory {
    fun create(): Any
}
```

## Recommended Registry Split

- `NodeSpecRegistry`
- `NodeExecutorRegistry`
- `NodePanelRegistry`

This keeps the responsibilities separate:
- spec defines the contract
- executor defines runtime behavior
- panel defines editor UI

## Loading Strategy

Preferred order:
1. Host app creates the base registries
2. Built-in modules register their own nodes
3. External plugins register through `GraphynPlugin.register(...)`
4. JVM/Desktop/Server may later add discovery via `ServiceLoader`

## Why This Works

- It avoids a hardcoded switch statement.
- It keeps plugins testable.
- It works even if a plugin only provides a spec and executor.
- It allows editor-only panels without making panels part of the core runtime contract.
