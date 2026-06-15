# Plugin API Draft

Graphyn plugins should extend the editor and runtime without touching the core workflow model.

## Goals

- Keep `core` free of plugin and UI concerns.
- Let hosts register nodes and executors from external modules.
- Support both in-repo plugins and published third-party plugins.
- Keep the first version explicit and predictable, not magical.

## Module Shape

Recommended package split:

- `:core`
  - workflow model
  - types
  - validation
  - serialization
  - execution engine
- `:plugin-api`
  - plugin contracts
  - registrar interfaces
  - metadata/version checks
- `:editor`
  - canvas
  - shell
  - panels
  - plugin-driven UI extensions
- `:server`
  - runtime host
  - execution endpoint
- `:plugins:<name>`
  - optional external or in-repo plugin module
- `:app:*`
  - host applications that select and register plugins

## Core Rule

Plugins should never depend on editor internals directly. They should only know:

- workflow types and node specs
- executor contracts
- plugin metadata

Editor panels stay in the editor module and can be registered with the existing editor plugin context.

That keeps plugins portable across different hosts.

## Proposed API

```kotlin
interface GraphynPlugin {
    val id: String
    val version: String
    val apiVersion: Int

    fun register(registrar: GraphynPluginRegistrar)
}
```

```kotlin
interface GraphynPluginRegistrar {
    fun registerNodeSpec(spec: NodeSpec)
    fun registerExecutor(type: String, executor: NodeExecutor)
}
```

```kotlin
data class GraphynPluginMetadata(
    val id: String,
    val version: String,
    val apiVersion: Int,
)
```

## Loading Model

Start with explicit host registration:

```kotlin
val plugins = listOf(
    MyCustomPlugin,
)

plugins.forEach { plugin ->
    plugin.register(registrar)
}
```

This is the right MVP choice because it:

- works on all Kotlin Multiplatform targets
- avoids classpath scanning in web builds
- keeps startup deterministic
- makes plugin order obvious

## Platform Strategy

### JVM / Desktop / Server

- Can later support `ServiceLoader` discovery.
- Can also support classpath-based plugin packs.
- Still should allow explicit registration for tests and embedders.

### Web / Wasm

- Prefer explicit registration.
- Avoid assuming filesystem or dynamic classpath discovery.
- Bundle plugins at build time or inject them from the host app.

### Android / iOS

- Prefer explicit registration.
- Keep plugin loading static and compiler-friendly.

## Plugin Responsibilities

A plugin may contribute:

- node specs
- node executors
- palette metadata
- icons or labels

A plugin should not:

- mutate the workflow model directly
- depend on canvas rendering details
- hardcode host navigation or app shell behavior
- assume it can load itself dynamically on every platform

## Example Plugin

```kotlin
object MathPlugin : GraphynPlugin {
    override val id = "graphyn.math"
    override val version = "1.0.0"
    override val apiVersion = 1

    override fun register(registrar: GraphynPluginRegistrar) {
        registrar.registerNodeSpec(
            NodeSpec(
                type = "math.add",
                label = "Add",
                inputs = listOf(
                    PortSpec("left", WorkflowType.DoubleType),
                    PortSpec("right", WorkflowType.DoubleType),
                ),
                outputs = listOf(
                    PortSpec("result", WorkflowType.DoubleType),
                ),
            ),
        )

        registrar.registerExecutor("math.add") { input ->
            val left = input["left"] as WorkflowValue.DoubleValue
            val right = input["right"] as WorkflowValue.DoubleValue
            mapOf("result" to WorkflowValue.DoubleValue(left.value + right.value))
        }
    }
}
```

## Host Wiring

The host should build plugin registries once, then pass them into the editor and server layers.

```kotlin
val pluginRegistry = DefaultGraphynPluginRegistry()

listOf(MyPluginA, MyPluginB).forEach {
    it.register(pluginRegistry)
}

val nodeSpecs = pluginRegistry.nodeSpecs
val nodeExecutors = pluginRegistry.nodeExecutors
```

The editor shell and server runtime then consume the resulting registries instead of knowing about plugins themselves. Custom editor panels remain registered through the editor plugin context.

## Versioning

Keep compatibility simple at first:

- `apiVersion` must match the host.
- Plugin `version` is informational for now.
- The host can reject a plugin if the API version differs.

Later, we can add:

- semantic version ranges
- plugin capability flags
- optional dependency declarations
- plugin manifests for packaged distribution

## MVP Recommendation

For the first implementation, use:

- explicit registration only
- one plugin interface
- one registrar interface
- one host-side plugin registry

That gives us extensibility without introducing dynamic-loading complexity too early.
