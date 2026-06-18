# graphyn-plugin-api

Contract for Graphyn runtime plugins — register node specs and executors.

## Dependency

```kotlin
implementation("io.github.ronjunevaldoz:graphyn-plugin-api:0.1.0")
```

## Writing a plugin

```kotlin
object MathPlugin : GraphynPlugin {
    override val metadata = GraphynPluginMetadata(
        id = "com.example.math",
        displayName = "Math",
        version = "1.0.0",
    )

    override fun register(registrar: GraphynPluginRegistrar) {
        registrar.registerNodeSpec(
            NodeSpec(
                type = "math.add",
                label = "Add",
                inputs  = listOf(PortSpec("left",  WorkflowType.DoubleType)),
                outputs = listOf(PortSpec("result", WorkflowType.DoubleType)),
            )
        )
        registrar.registerExecutor("math.add") { inputs ->
            val a = (inputs["left"]  as? WorkflowValue.DoubleValue)?.value ?: 0.0
            val b = (inputs["right"] as? WorkflowValue.DoubleValue)?.value ?: 0.0
            mapOf("result" to WorkflowValue.DoubleValue(a + b))
        }
    }
}
```

## Installing

```kotlin
val registry = DefaultGraphynPluginRegistry()
registry.install(MathPlugin)
```

No Compose dependency — safe for server and pure KMP contexts.
