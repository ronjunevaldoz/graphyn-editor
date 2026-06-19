# Serialization

Graphyn uses `kotlinx.serialization` to serialize workflows to and from JSON. The format is stable across minor versions.

---

## Basic usage

```kotlin
// Serialize
val json: String = workflow.toJson()

// Deserialize
val workflow: WorkflowDefinition = workflowFromJson(json)
```

Both functions are top-level extensions in the `core` module.

---

## JSON schema

A serialized `WorkflowDefinition` looks like:

```json
{
  "id": "my-workflow",
  "name": "My Workflow",
  "nodes": [
    {
      "id": "n1",
      "type": "text.split",
      "config": {
        "delimiter": { "type": "string", "value": "," }
      }
    },
    {
      "id": "n2",
      "type": "listops.map"
    }
  ],
  "connections": [
    {
      "fromNodeId": "n1",
      "fromPort": "parts",
      "toNodeId": "n2",
      "toPort": "list"
    }
  ]
}
```

### `WorkflowValue` encoding

Config values are tagged unions:

| Kotlin type | JSON `type` field | JSON `value` field |
|---|---|---|
| `StringValue("hello")` | `"string"` | `"hello"` |
| `IntValue(42)` | `"int"` | `42` |
| `DoubleValue(3.14)` | `"double"` | `3.14` |
| `BooleanValue(true)` | `"boolean"` | `true` |
| `ListValue([...])` | `"list"` | array of encoded values |
| `RecordValue({...})` | `"record"` | object of encoded values |
| `NullValue` | `"null"` | omitted |

### `WorkflowType` encoding (in `NodeSpec`)

Types are serialized with `@SerialName` tags:

| Type | JSON |
|---|---|
| `StringType` | `"string"` |
| `IntType` | `"int"` |
| `DoubleType` | `"double"` |
| `BooleanType` | `"boolean"` |
| `OpaqueType` | `"opaque"` |
| `ListType(T)` | `{"type":"list","elementType":<T>}` |
| `NullableType(T)` | `{"type":"nullable","wrappedType":<T>}` |
| `RecordType(F)` | `{"type":"record","fields":{...}}` |
| `EnumType(V)` | `{"type":"enum","values":[...]}` |
| `MultiEnumType(V)` | `{"type":"multi_enum","values":[...]}` |

---

## `WorkflowDocument`

For persistence scenarios where you want to store the workflow alongside metadata (schema version, timestamps), use `WorkflowDocument`:

```kotlin
val doc = WorkflowDocument(
    schemaVersion = 1,
    workflow = myWorkflow,
    createdAt = Clock.System.now().toString(),
)
val json = doc.toJson()
```

`WorkflowDocument` wraps `WorkflowDefinition` and adds a `schemaVersion` field. Check the version before loading to handle migrations:

```kotlin
val doc = workflowDocumentFromJson(json)
if (doc.schemaVersion > CURRENT_SCHEMA_VERSION) {
    error("Workflow was saved with a newer version of Graphyn")
}
```

---

## Versioning policy

- **Patch versions** (0.1.x → 0.1.y): JSON format is guaranteed identical. Old JSON loads in new versions and vice versa.
- **Minor versions** (0.1.x → 0.2.x): New optional fields may be added. Old JSON loads correctly; new fields default to null/empty in old readers.
- **Major versions** (0.x → 1.x): Breaking schema changes are possible. Use `WorkflowDocument.schemaVersion` to detect and migrate.

---

## Migration example

```kotlin
fun migrateV1toV2(oldJson: String): WorkflowDefinition {
    val doc = workflowDocumentFromJson(oldJson)
    return when (doc.schemaVersion) {
        1 -> doc.workflow.copy(
            // e.g., rename a node type that changed between versions
            nodes = doc.workflow.nodes.map { node ->
                if (node.type == "legacy.fetch") node.copy(type = "io.http_request")
                else node
            }
        )
        else -> doc.workflow
    }
}
```
