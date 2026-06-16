# Editor Panel Host

Graphyn treats editor panels as an extension layer on top of the workflow model.

## Rule

- Runtime plugins own node specs and executors.
- Editor plugins own custom panel content.
- The editor shell only resolves panels by `nodeType`.
- The shell should not know panel implementation details.

## Registry Shape

The editor side uses a simple registry contract:

```kotlin
interface EditorPanelRegistry {
    fun resolve(nodeType: String): EditorPanelFactory?
    fun register(nodeType: String, factory: EditorPanelFactory)
}
```

`DefaultEditorPanelRegistry` is the in-memory implementation used by:
- `App(...)` when no registry is injected
- the demo bootstrap
- tests

That keeps the boundary explicit:
- host code decides which panels exist
- plugins register panels into the registry
- the shell renders whatever the registry resolves

## Host Behavior

The shell should:
- show a default inspector when no panel is registered
- show the registered panel when the selected node type has one
- keep validation and workflow state available to the panel through `EditorPanelContext`

## Related Docs

- [Plugin API Draft](./plugins.md)
- [Architecture Overview](./README.md)
- [Agent Rules](../agents.md)

