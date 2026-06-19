# Type System

Graphyn uses a `WorkflowType` sealed interface to describe what kind of data flows through a port. The type system is checked when a user draws a connection — incompatible types are rejected with a visual toast.

---

## Type variants

```kotlin
sealed interface WorkflowType {
    data object StringType  : WorkflowType
    data object IntType     : WorkflowType
    data object DoubleType  : WorkflowType
    data object BooleanType : WorkflowType
    data object OpaqueType  : WorkflowType          // accepts any incoming type

    data class ListType(val elementType: WorkflowType) : WorkflowType
    data class NullableType(val wrappedType: WorkflowType) : WorkflowType
    data class RecordType(val fields: Map<String, WorkflowType>) : WorkflowType
    data class EnumType(val values: List<String>) : WorkflowType
    data class MultiEnumType(val values: List<String>) : WorkflowType
}
```

### Primitive types

| Type | Kotlin equivalent | Example value |
|---|---|---|
| `StringType` | `String` | `WorkflowValue.StringValue("hello")` |
| `IntType` | `Int` | `WorkflowValue.IntValue(42)` |
| `DoubleType` | `Double` | `WorkflowValue.DoubleValue(3.14)` |
| `BooleanType` | `Boolean` | `WorkflowValue.BooleanValue(true)` |

### Structural types

**`ListType(elementType)`** — a homogeneous list.

```kotlin
WorkflowType.ListType(WorkflowType.StringType)   // List<String>
WorkflowType.ListType(WorkflowType.OpaqueType)   // List<Any>
```

**`RecordType(fields)`** — a named map of typed fields, similar to a JSON object or a data class.

```kotlin
WorkflowType.RecordType(mapOf(
    "name"  to WorkflowType.StringType,
    "score" to WorkflowType.DoubleType,
))
```

**`NullableType(wrappedType)`** — wraps any type to indicate the value may be absent.

```kotlin
WorkflowType.NullableType(WorkflowType.StringType)  // String?
```

In the `FieldCard` UI, nullable ports render a checkbox that toggles between `null` and the inner type's default value.

**`EnumType(values)`** — a single-choice dropdown from a fixed list of string options.

```kotlin
WorkflowType.EnumType(listOf("GET", "POST", "PUT", "DELETE"))
```

**`MultiEnumType(values)`** — a multi-choice variant of `EnumType`.

### OpaqueType

`OpaqueType` is a structural wildcard — it accepts an incoming connection of **any** type. Use it for pass-through ports, union operations, or when the concrete type is determined at runtime.

```kotlin
PortSpec("value", WorkflowType.OpaqueType)  // accepts String, Int, Record, List, etc.
```

**Only use `OpaqueType` when the node genuinely cannot know the type in advance.** Over-using it defeats type safety and removes all editor feedback for the user.

---

## Compatibility rules

A connection from output port O to input port I is accepted when:

| Input port type | Accepts |
|---|---|
| `OpaqueType` | any output type |
| `StringType` | `StringType` only |
| `IntType` | `IntType` only |
| `DoubleType` | `DoubleType` only |
| `BooleanType` | `BooleanType` only |
| `ListType(E)` | `ListType(E)` where E matches |
| `NullableType(T)` | `T` or `NullableType(T)` |
| `RecordType(F)` | `RecordType(F)` with same field names and matching field types |
| `EnumType(V)` | `EnumType(V)` with same values list |

Type checking runs in `WorkflowTypeCompatibility.isCompatible(outputType, inputType)` in the `core` module.

---

## `displayName()` extension

```kotlin
fun WorkflowType.displayName(): String
```

Returns a human-readable label shown in port tooltips and the inspector:

| Type | `displayName()` |
|---|---|
| `StringType` | `"String"` |
| `IntType` | `"Int"` |
| `ListType(StringType)` | `"List<String>"` |
| `NullableType(IntType)` | `"Int?"` |
| `RecordType(...)` | `"Record"` |
| `OpaqueType` | `"Any"` |

---

## WorkflowValue

Every port value at runtime is a `WorkflowValue`:

```kotlin
sealed interface WorkflowValue {
    data class StringValue(val value: String)    : WorkflowValue
    data class IntValue(val value: Int)          : WorkflowValue
    data class DoubleValue(val value: Double)    : WorkflowValue
    data class BooleanValue(val value: Boolean)  : WorkflowValue
    data class ListValue(val items: List<WorkflowValue>) : WorkflowValue
    data class RecordValue(val fields: Map<String, WorkflowValue>) : WorkflowValue
    data object NullValue : WorkflowValue
}
```

`NullValue` is the runtime representation of a `NullableType` port with no value set.
