# Core API Draft

This is the first-pass shape for the library core.

The goal is to keep the model:
- UI-agnostic
- registry-driven
- easy to validate
- easy to serialize

## Type Model

```kotlin
sealed interface WorkflowType {
    data object StringType : WorkflowType
    data object IntType : WorkflowType
    data object DoubleType : WorkflowType
    data object BooleanType : WorkflowType

    data class ListType(val elementType: WorkflowType) : WorkflowType
    data class NullableType(val wrappedType: WorkflowType) : WorkflowType
    data class RecordType(
        val fields: Map<String, WorkflowType>,
    ) : WorkflowType

    data class EnumType(
        val values: List<String>,
    ) : WorkflowType

    data object OpaqueType : WorkflowType
}
```

Notes:
- `DoubleType` is the default decimal type.
- `OpaqueType` is for runtime handles or values the workflow model should not inspect.
- `RecordType` lets us model structured input/output without inventing custom node-specific primitives.

## Ports and Nodes

```kotlin
data class PortSpec(
    val name: String,
    val type: WorkflowType,
    val required: Boolean = true,
)

data class NodeSpec(
    val type: String,
    val label: String,
    val inputs: List<PortSpec>,
    val outputs: List<PortSpec>,
    val defaultValues: Map<String, WorkflowValue> = emptyMap(),
)

data class NodeRef(
    val id: String,
    val type: String,
    val config: Map<String, WorkflowValue> = emptyMap(),
)

data class ConnectionRef(
    val fromNodeId: String,
    val fromPort: String,
    val toNodeId: String,
    val toPort: String,
)
```

## Values

```kotlin
sealed interface WorkflowValue {
    data class StringValue(val value: String) : WorkflowValue
    data class IntValue(val value: Int) : WorkflowValue
    data class DoubleValue(val value: Double) : WorkflowValue
    data class BooleanValue(val value: Boolean) : WorkflowValue
    data class ListValue(val items: List<WorkflowValue>) : WorkflowValue
    data class RecordValue(val fields: Map<String, WorkflowValue>) : WorkflowValue
    data object NullValue : WorkflowValue
    data object OpaqueValue : WorkflowValue
}
```

## Workflow Definition

```kotlin
data class WorkflowDefinition(
    val id: String,
    val name: String,
    val nodes: List<NodeRef>,
    val connections: List<ConnectionRef>,
)
```

## Validation

```kotlin
data class ValidationError(
    val code: String,
    val message: String,
    val nodeId: String? = null,
    val port: String? = null,
)

interface WorkflowValidator {
    fun validate(workflow: WorkflowDefinition): List<ValidationError>
}
```

Validation should check:
- node existence
- port existence
- type compatibility
- required input satisfaction
- invalid cycles if the runtime cannot support them

## Execution Contract

```kotlin
interface NodeExecutor {
    suspend fun execute(
        input: Map<String, WorkflowValue>,
    ): Map<String, WorkflowValue>
}

interface NodeExecutorRegistry {
    fun resolve(type: String): NodeExecutor?
}
```

The core should define the contract, but not the UI.
